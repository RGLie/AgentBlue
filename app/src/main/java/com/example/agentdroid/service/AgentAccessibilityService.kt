package com.example.agentdroid.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.agentdroid.AgentStateManager
import com.example.agentdroid.data.ModelPreferences
import com.example.agentdroid.model.AgentStatus
import com.example.agentdroid.model.StepLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AgentDroid_Service"
        private const val MAX_STEPS = 15
        private const val DELAY_BETWEEN_STEPS_MS = 1500L
        private const val INITIAL_DELAY_MS = 500L
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private val uiTreeParser = UiTreeParser()
    private val screenAnalyzer = ScreenAnalyzer(uiTreeParser)
    private val actionExecutor = ActionExecutor()

    private var floatingWindowManager: FloatingWindowManager? = null
    private var floatingPanelManager: FloatingPanelManager? = null
    private var firebaseCommandListener: FirebaseCommandListener? = null
    private var currentJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected!")

        AgentStateManager.init(this)
        ModelPreferences.init(this)

        floatingPanelManager = FloatingPanelManager(this)

        floatingWindowManager = FloatingWindowManager(this) { command ->
            handleCommand(command)
        }
        floatingWindowManager?.show()

        firebaseCommandListener = FirebaseCommandListener(this)
        firebaseCommandListener?.startListening()
        Log.d(TAG, "Firebase Command Listener 시작")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 명령 기반으로 동작 — 이벤트 기반 동작 없음
    }

    fun executeRemoteCommand(command: String, onResult: (success: Boolean, message: String) -> Unit) {
        Log.i(TAG, "원격 명령 수신: $command")
        currentJob = serviceScope.launch {
            val result = runReActLoop(command)
            onResult(result.first, result.second)
        }
    }

    private fun handleCommand(command: String) {
        Log.i(TAG, "명령 실행: $command")
        currentJob = serviceScope.launch {
            runReActLoop(command)
        }
    }

    private suspend fun runReActLoop(userCommand: String): Pair<Boolean, String> {
        Log.d(TAG, "=== ReAct 루프 시작: '$userCommand' (최대 ${MAX_STEPS}스텝) ===")

        AgentStateManager.onExecutionStarted(userCommand, MAX_STEPS)
        floatingPanelManager?.show(userCommand, MAX_STEPS)

        val actionHistory = mutableListOf<String>()

        delay(INITIAL_DELAY_MS)

        for (step in 1..MAX_STEPS) {
            if (AgentStateManager.isCancelRequested()) {
                Log.d(TAG, "=== 사용자에 의해 취소됨 (Step $step) ===")
                val msg = "사용자가 실행을 중단했습니다. (${step - 1}스텝 완료)"
                AgentStateManager.onExecutionFinished(AgentStatus.CANCELLED, msg)
                floatingPanelManager?.showResult(false, msg)
                return Pair(false, msg)
            }

            Log.d(TAG, "--- Step $step/$MAX_STEPS ---")

            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "Step $step: 활성 윈도우를 찾을 수 없습니다. 재시도 대기...")
                delay(DELAY_BETWEEN_STEPS_MS)
                continue
            }

            val uiTree = uiTreeParser.parse(rootNode)
            val analysisResult = screenAnalyzer.analyze(uiTree, userCommand, actionHistory)

            if (analysisResult.isFailure) {
                val error = analysisResult.exceptionOrNull()!!
                Log.e(TAG, "Step $step: AI 분석 실패 — ${error.message}")
                actionHistory.add("Step $step [ERROR]: ${error.message}")

                val stepLog = StepLog(step, "ERROR", null, error.message, false)
                AgentStateManager.onStepCompleted(stepLog)
                floatingPanelManager?.updateStep(step, MAX_STEPS, "오류: ${error.message}")

                delay(DELAY_BETWEEN_STEPS_MS)
                continue
            }
            val action = analysisResult.getOrThrow()

            if (action.isDone()) {
                Log.d(TAG, "=== 목표 달성 완료 (Step $step): ${action.reasoning} ===")
                val msg = "목표가 성공적으로 달성되었습니다. (${step}스텝)"

                AgentStateManager.onStepCompleted(
                    StepLog(step, "DONE", null, action.reasoning, true)
                )
                AgentStateManager.onExecutionFinished(AgentStatus.COMPLETED, msg)
                floatingPanelManager?.showResult(true, msg)
                return Pair(true, msg)
            }

            val success = actionExecutor.execute(rootNode, action, this)
            actionHistory.add(action.toHistoryEntry(step, success))

            val stepLog = StepLog(
                step = step,
                actionType = action.actionType,
                targetText = action.targetText,
                reasoning = action.reasoning,
                success = success
            )
            AgentStateManager.onStepCompleted(stepLog)
            floatingPanelManager?.updateStep(step, MAX_STEPS, action.reasoning)

            delay(DELAY_BETWEEN_STEPS_MS)
        }

        Log.w(TAG, "=== 최대 스텝 도달 ($MAX_STEPS). 루프 종료 ===")
        val msg = "최대 스텝($MAX_STEPS)에 도달했지만 목표를 완료하지 못했습니다."
        AgentStateManager.onExecutionFinished(AgentStatus.FAILED, msg)
        floatingPanelManager?.showResult(false, msg)
        return Pair(false, msg)
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        currentJob?.cancel()
        currentJob = null

        firebaseCommandListener?.stopListening()
        firebaseCommandListener = null

        floatingWindowManager?.remove()
        floatingWindowManager = null

        floatingPanelManager?.dismiss()
        floatingPanelManager = null

        AgentStateManager.reset()
        serviceScope.cancel()
    }
}
