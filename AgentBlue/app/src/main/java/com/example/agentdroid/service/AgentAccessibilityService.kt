package com.example.agentdroid.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.agentdroid.AgentStateManager
import com.example.agentdroid.data.AgentPreferences
import com.example.agentdroid.data.ModelPreferences
import com.example.agentdroid.data.SessionPreferences
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
        private const val INITIAL_DELAY_MS = 500L
        private const val STUCK_HINT_THRESHOLD = 3
        private const val STUCK_FORCE_BACK_THRESHOLD = 5

        var instance: AgentAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private val uiTreeParser = UiTreeParser()
    private val screenAnalyzer = ScreenAnalyzer(uiTreeParser)
    private val actionExecutor = ActionExecutor()

    private var floatingWindowManager: FloatingWindowManager? = null
    private var floatingPanelManager: FloatingPanelManager? = null
    private var firebaseCommandListener: FirebaseCommandListener? = null
    private var firebaseSettingsListener: FirebaseSettingsListener? = null
    private var currentJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected!")
        instance = this

        AgentStateManager.init(this)
        AgentPreferences.init(this)
        ModelPreferences.init(this)
        SessionPreferences.init(this)

        floatingPanelManager = FloatingPanelManager(this)

        floatingWindowManager = FloatingWindowManager(this) { command ->
            handleCommand(command)
        }
        floatingWindowManager?.show()

        firebaseCommandListener = FirebaseCommandListener(this, AgentStateManager)
        firebaseCommandListener?.startListening()

        firebaseSettingsListener = FirebaseSettingsListener()
        firebaseSettingsListener?.startListening()

        Log.d(TAG, "Firebase listeners started")
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
        val maxSteps = AgentPreferences.getMaxSteps()
        val delayBetweenStepsMs = AgentPreferences.getStepDelayMs()

        Log.d(TAG, "=== ReAct 루프 시작: '$userCommand' (최대 ${maxSteps}스텝, 딜레이 ${delayBetweenStepsMs}ms) ===")

        AgentStateManager.onExecutionStarted(userCommand, maxSteps)
        floatingPanelManager?.show(userCommand, maxSteps)

        val actionHistory = mutableListOf<String>()
        var consecutiveFailures = 0

        delay(INITIAL_DELAY_MS)

        for (step in 1..maxSteps) {
            if (AgentStateManager.isCancelRequested()) {
                Log.d(TAG, "=== 사용자에 의해 취소됨 (Step $step) ===")
                val msg = "사용자가 실행을 중단했습니다. (${step - 1}스텝 완료)"
                AgentStateManager.onExecutionFinished(AgentStatus.CANCELLED, msg)
                floatingPanelManager?.showResult(false, msg)
                return Pair(false, msg)
            }

            Log.d(TAG, "--- Step $step/$maxSteps ---")

            // 연속 5회 이상 실패 시 강제 BACK 수행 (LLM 판단 우회)
            if (consecutiveFailures >= STUCK_FORCE_BACK_THRESHOLD) {
                Log.w(TAG, "Step $step: 연속 ${consecutiveFailures}회 실패 — 강제 BACK 수행")
                val forceBackSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
                actionHistory.add("Step $step [SYSTEM]: Forced BACK due to $consecutiveFailures consecutive failures (result: $forceBackSuccess)")

                val stepLog = StepLog(step, "BACK", null, "시스템 강제 복구: 연속 ${consecutiveFailures}회 실패", forceBackSuccess)
                AgentStateManager.onStepCompleted(stepLog)
                floatingPanelManager?.updateStep(step, maxSteps, "시스템 강제 복구 (BACK)")

                consecutiveFailures = 0
                delay(delayBetweenStepsMs)
                continue
            }

            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "Step $step: 활성 윈도우를 찾을 수 없습니다. 재시도 대기...")
                delay(delayBetweenStepsMs)
                continue
            }

            // 연속 3회 이상 실패 시 LLM에 힌트 삽입
            if (consecutiveFailures >= STUCK_HINT_THRESHOLD) {
                val hint = "[SYSTEM HINT] $consecutiveFailures consecutive failures detected. " +
                    "You are likely stuck on a wrong screen. Use BACK or HOME to navigate to a relevant screen. " +
                    "Do NOT click random elements."
                actionHistory.add(hint)
                Log.w(TAG, "Step $step: stuck 감지 — LLM에 힌트 삽입 (연속 ${consecutiveFailures}회 실패)")
            }

            val uiTree = uiTreeParser.parse(rootNode)
            val analysisResult = screenAnalyzer.analyze(uiTree, userCommand, actionHistory)

            if (analysisResult.isFailure) {
                val error = analysisResult.exceptionOrNull()!!
                Log.e(TAG, "Step $step: AI 분석 실패 — ${error.message}")
                actionHistory.add("Step $step [ERROR]: ${error.message}")

                val stepLog = StepLog(step, "ERROR", null, error.message, false)
                AgentStateManager.onStepCompleted(stepLog)
                floatingPanelManager?.updateStep(step, maxSteps, "오류: ${error.message}")

                consecutiveFailures++
                delay(delayBetweenStepsMs)
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

            if (success) {
                consecutiveFailures = 0
            } else {
                consecutiveFailures++
            }

            val stepLog = StepLog(
                step = step,
                actionType = action.actionType,
                targetText = action.targetText,
                reasoning = action.reasoning,
                success = success
            )
            AgentStateManager.onStepCompleted(stepLog)
            floatingPanelManager?.updateStep(step, maxSteps, action.reasoning)

            delay(delayBetweenStepsMs)
        }

        Log.w(TAG, "=== 최대 스텝 도달 ($maxSteps). 루프 종료 ===")
        val msg = "최대 스텝($maxSteps)에 도달했지만 목표를 완료하지 못했습니다."
        AgentStateManager.onExecutionFinished(AgentStatus.FAILED, msg)
        floatingPanelManager?.showResult(false, msg)
        return Pair(false, msg)
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
    }

    fun restartCommandListener() {
        Log.d(TAG, "Restarting Firebase listeners (session changed)")
        firebaseCommandListener?.restartListening()
        firebaseSettingsListener?.restartListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null

        currentJob?.cancel()
        currentJob = null

        firebaseCommandListener?.stopListening()
        firebaseCommandListener = null

        firebaseSettingsListener?.stopListening()
        firebaseSettingsListener = null

        floatingWindowManager?.remove()
        floatingWindowManager = null

        floatingPanelManager?.dismiss()
        floatingPanelManager = null

        AgentStateManager.reset()
        serviceScope.cancel()
    }
}
