package com.example.agentdroid.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.agentdroid.model.LlmAction

class ActionExecutor {

    companion object {
        private const val TAG = "ActionExecutor"

        private const val ACTION_CLICK = "CLICK"
        private const val ACTION_TYPE = "TYPE"
        private const val ACTION_SCROLL = "SCROLL"
        private const val ACTION_BACK = "BACK"
        private const val ACTION_DONE = "DONE"
    }

    fun execute(
        rootNode: AccessibilityNodeInfo,
        action: LlmAction,
        service: AccessibilityService
    ): Boolean {
        if (action.isDone()) {
            Log.d(TAG, "목표 달성 완료: ${action.reasoning}")
            return true
        }

        val success = when (action.actionType.uppercase()) {
            ACTION_CLICK -> smartClick(rootNode, action)
            ACTION_TYPE -> findAndType(rootNode, action.targetText ?: "", action.inputText ?: "")
            ACTION_SCROLL -> performScroll(rootNode, action.targetText)
            ACTION_BACK -> performBack(service)
            else -> {
                Log.w(TAG, "알 수 없는 액션 타입: ${action.actionType}")
                false
            }
        }

        if (success) {
            Log.d(TAG, "행동 수행 성공: ${action.actionType} -> ${action.targetText ?: action.targetId}")
        } else {
            Log.e(TAG, "행동 수행 실패: ${action.actionType} 타겟 '${action.targetText ?: action.targetId}'")
        }

        return success
    }

    // --- CLICK: 5단계 우선순위 매칭 ---
    //
    // 안드로이드 UI 패턴:
    //   ViewGroup (clickable, text=null)      ← 실제 클릭 대상
    //     ├── TextView (not clickable, text="아이유")
    //     └── ImageView (clickable, desc="아이유 제안 수정")  ← 잘못 클릭 위험
    //
    // node.text 매칭과 contentDescription 매칭을 분리하여,
    // bubble-up이 contentDescription 매칭보다 먼저 실행되도록 한다.

    private fun smartClick(rootNode: AccessibilityNodeInfo, action: LlmAction): Boolean {
        val targetText = action.targetText ?: ""
        val targetId = action.targetId

        // 1순위: target_id — 리소스 ID로 정확히 매칭
        if (!targetId.isNullOrEmpty()) {
            Log.d(TAG, "1순위: target_id='$targetId'")
            val found = findClickableById(rootNode, targetId, targetText)
            if (found) return true
        }

        // 2순위: node.text 직접 매칭 (contentDescription 제외, editable 제외)
        Log.d(TAG, "2순위: node.text로 '$targetText' 직접 매칭")
        val textMatch = findClickableByNodeText(rootNode, targetText)
        if (textMatch) return true

        // 3순위: bubble-up — 자식의 node.text가 매칭 → 클릭 가능한 부모 클릭
        Log.d(TAG, "3순위: bubble-up — 자식 text '$targetText' → 클릭 가능한 부모")
        val bubbleUp = findClickableParentByChildText(rootNode, targetText)
        if (bubbleUp) return true

        // 4순위: contentDescription 매칭 (2순위에서 못 찾은 아이콘 버튼 등)
        Log.d(TAG, "4순위: contentDescription으로 '$targetText' 매칭")
        val descMatch = findClickableByDescription(rootNode, targetText)
        if (descMatch) return true

        // 5순위: 폴백 — editable 포함 전체 탐색
        Log.d(TAG, "5순위: 폴백 — 전체 탐색")
        return findAndClickFallback(rootNode, targetText)
    }

    // 1순위: 리소스 ID로 매칭
    private fun findClickableById(
        node: AccessibilityNodeInfo,
        targetId: String,
        targetText: String
    ): Boolean {
        if (node.viewIdResourceName != null &&
            node.viewIdResourceName.contains(targetId, ignoreCase = true) &&
            node.isClickable
        ) {
            if (targetText.isEmpty() || containsTextInSubtree(node, targetText)) {
                Log.i(TAG, "ID 매칭 성공: ${node.viewIdResourceName}")
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findClickableById(child, targetId, targetText)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // 2순위: node.text만으로 매칭 (contentDescription 무시, editable 제외)
    private fun findClickableByNodeText(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.isClickable && !node.isEditable && matchesNodeText(node, target)) {
            Log.i(TAG, "text 직접 매칭 성공: text='${node.text}'")
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findClickableByNodeText(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // 3순위: 자식의 node.text → 클릭 가능한 부모
    private fun findClickableParentByChildText(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.isClickable && !node.isEditable && hasChildWithText(node, target)) {
            Log.i(TAG, "Bubble-up 매칭 성공: 자식 텍스트 '$target' → 부모 ID: ${node.viewIdResourceName}")
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findClickableParentByChildText(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // 4순위: contentDescription으로 매칭
    private fun findClickableByDescription(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.isClickable && !node.isEditable && matchesDescription(node, target)) {
            Log.i(TAG, "desc 매칭 성공: desc='${node.contentDescription}'")
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findClickableByDescription(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // 5순위: 폴백
    private fun findAndClickFallback(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.isClickable && (matchesNodeText(node, target) || matchesDescription(node, target))) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickFallback(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // --- 텍스트 매칭 유틸리티 ---

    private fun matchesNodeText(node: AccessibilityNodeInfo, target: String): Boolean {
        return node.text?.toString()?.contains(target, ignoreCase = true) == true
    }

    private fun matchesDescription(node: AccessibilityNodeInfo, target: String): Boolean {
        return node.contentDescription?.toString()?.contains(target, ignoreCase = true) == true
    }

    private fun hasChildWithText(node: AccessibilityNodeInfo, target: String): Boolean {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (matchesNodeText(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun containsTextInSubtree(node: AccessibilityNodeInfo, target: String): Boolean {
        if (matchesNodeText(node, target) || matchesDescription(node, target)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsTextInSubtree(child, target)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    // --- TYPE ---

    private fun findAndType(
        node: AccessibilityNodeInfo,
        target: String,
        textToInput: String
    ): Boolean {
        val isTargetMatch = isTextOrHintMatch(node, target) || isIdMatch(node)

        if (node.isEditable && isTargetMatch) {
            Log.i(TAG, "입력 타겟 발견 (ID: ${node.viewIdResourceName}) -> '$textToInput' 입력 시도")

            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    textToInput
                )
            }

            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "입력 결과: $result")
            return result
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndType(child, target, textToInput)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    // --- SCROLL / BACK ---

    private fun performScroll(rootNode: AccessibilityNodeInfo, direction: String?): Boolean {
        val scrollable = findScrollableNode(rootNode)
        if (scrollable == null) {
            Log.w(TAG, "스크롤 가능한 노드를 찾지 못했습니다.")
            return false
        }

        val action = if (direction?.uppercase() == "UP") {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        return scrollable.performAction(action)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
            child.recycle()
        }
        return null
    }

    private fun performBack(service: AccessibilityService): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    // --- 공통 유틸리티 ---

    private fun isTextOrHintMatch(node: AccessibilityNodeInfo, target: String): Boolean {
        val textMatch = node.text?.toString()?.contains(target, ignoreCase = true) == true
        val hintMatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.contains(target, ignoreCase = true) == true
        } else {
            false
        }
        return textMatch || hintMatch
    }

    private fun isIdMatch(node: AccessibilityNodeInfo): Boolean {
        return node.viewIdResourceName?.contains("search_edit_text") == true
    }
}
