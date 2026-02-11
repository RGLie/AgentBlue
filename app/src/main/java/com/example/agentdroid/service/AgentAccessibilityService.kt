package com.example.agentdroid.service

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.example.agentdroid.BuildConfig
import com.example.agentdroid.R
import com.example.agentdroid.model.OpenAiMessage
import com.example.agentdroid.model.OpenAiRequest
import com.example.agentdroid.model.UiNode
import com.example.agentdroid.network.RetrofitClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.* // Flow 관련 기능 임포트

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AgentDroid_Service"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // 1. 이벤트를 받아들이는 파이프라인 (Flow) 생성
    // Replay = 0: 지난 이벤트는 기억하지 않음
    // ExtraBufferCapacity = 1: 처리 중일 때 하나 정도는 대기
    private val accessibilityEventFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected! Flow 초기화 시작")

        showFloatingWindow()
    }

    private fun showFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 200

        val robotButton = floatingView?.findViewById<View>(R.id.btn_robot)
        robotButton?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val xDiff = (event.rawX - initialTouchX).toInt()
                        val yDiff = (event.rawY - initialTouchY).toInt()
                        if (xDiff < 10 && yDiff < 10) {
                            showCommandDialog()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "플로팅 뷰 추가 실패: ${e.message}")
        }
    }

    private fun showCommandDialog() {
        val editText = EditText(this)
        editText.hint = "명령을 입력하세요 (ex. 설정 열어줘)"

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("AgentDroid Command")
            .setView(editText)
            .setPositiveButton("실행") { _, _ ->
                val command = editText.text.toString()

                Log.d(TAG, "명령 입력: $command")

                requestAnalysisWithCommand(command)
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun requestAnalysisWithCommand(command: String) {
        Log.i(TAG, "명령 실행: $command")
        serviceScope.launch {
            delay(500L)
            analyzeScreen(command)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        accessibilityEventFlow.tryEmit(Unit)
    }

    private suspend fun analyzeScreen(userCommand: String) {
        val rootNode = rootInActiveWindow ?: return

        try {
            Log.d(TAG, ">>> 화면 분석 및 AI 요청 시작 <<<")

            val uiTree = parseNode(rootNode)
            val uiJson = com.google.gson.Gson().toJson(uiTree)
            printTree(uiTree, 0)

            val systemPrompt = """
                You are an Android Automation Agent.
                I will give you the current UI tree structure (JSON).
                
                Commands:
                1. If the user wants to click something -> return "action_type": "CLICK"
                2. If the user wants to input text -> return "action_type": "TYPE", "input_text": "text to type"
                
                Output Format (JSON Only):
                {
                  "action_type": "CLICK" or "TYPE",
                  "target_text": "Button text or Hint text of EditText",
                  "input_text": "Content to type (only for TYPE action)",
                  "reasoning": "Reason for selection"
                }
            """.trimIndent()

            val request = OpenAiRequest(
                messages = listOf(
                    OpenAiMessage("system", systemPrompt),
                    OpenAiMessage("user", "Current UI: $uiJson\n\nCommand: $userCommand")
                )
            )

            val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"
            val response = RetrofitClient.apiService.getAction(apiKey, request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                Log.d(TAG, "AI 응답: $content")

                if (content != null) {
                    performActionFromAi(rootNode, content)
                }
            } else {
                Log.e(TAG, "API 오류: ${response.code()} ${response.errorBody()?.string()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "에러 발생: ${e.message}", e)
        }
    }

    private fun parseNode(node: AccessibilityNodeInfo): UiNode {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val childrenList = mutableListOf<UiNode>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                childrenList.add(parseNode(child))
                child.recycle()
            }
        }

        val capturedHintText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString()
        } else {
            null
        }

        return UiNode(
            text = node.text?.toString(),
            hintText = capturedHintText, 
            viewIdResourceName = node.viewIdResourceName,
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            bounds = rect,
            isClickable = node.isClickable,
            isEditable = node.isEditable, 
            children = childrenList
        )
    }

    private fun printTree(node: UiNode, depth: Int) {
        if (!node.text.isNullOrEmpty() || !node.viewIdResourceName.isNullOrEmpty()) {
            val indent = "  ".repeat(depth)
            Log.i(TAG, "$indent [${node.className}] ID: ${node.viewIdResourceName} | Text: ${node.text} | Editable: ${node.isEditable} | Clickable: ${node.isClickable}")
        }

        for (child in node.children) {
            printTree(child, depth + 1)
        }
    }


    private fun performActionFromAi(rootNode: AccessibilityNodeInfo, jsonString: String) {
        try {
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            val action = com.google.gson.Gson().fromJson(cleanJson, com.example.agentdroid.model.LlmAction::class.java)

            Log.d(TAG, "🤖 AI 결정: ${action.actionType} -> ${action.targetText} (이유: ${action.reasoning})")

            val isSuccess = when (action.actionType.uppercase()) {
                "CLICK" -> findAndClickNode(rootNode, action.targetText ?: "")
                "TYPE" -> findAndTypeNode(rootNode, action.targetText ?: "", action.inputText ?: "")
                else -> false
            }

            if (isSuccess) {
                Log.d(TAG, "✅ 행동 수행 성공!")
            } else {
                Log.e(TAG, "❌ 행동 수행 실패: 타겟을 찾을 수 없습니다.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "JSON 파싱 또는 실행 중 오류: ${e.message}")
        }
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.text != null && node.text.toString().contains(target) && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (findAndClickNode(child, target)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun findAndTypeNode(node: AccessibilityNodeInfo, target: String, textToInput: String): Boolean {

        val isEditable = node.isEditable

        val isTextMatch = (node.text != null && node.text.toString().contains(target, ignoreCase = true)) ||
                (node.hintText != null && node.hintText.toString().contains(target, ignoreCase = true))

        val isIdMatch = node.viewIdResourceName != null &&
                node.viewIdResourceName.contains("search_edit_text")

        val isEditText = node.className != null && node.className.toString().contains("EditText")

        if (isEditable && (isTextMatch || isIdMatch)) {
            Log.i(TAG, "📝 입력 타겟 발견! (ID: ${node.viewIdResourceName}) -> '$textToInput' 입력 시도")

            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            val arguments = android.os.Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                textToInput
            )

            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "입력 결과: $result")
            return result
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (findAndTypeNode(child, target, textToInput)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (floatingView != null && windowManager != null){
            windowManager?.removeView(floatingView)
        }

        serviceScope.cancel()
    }
}
