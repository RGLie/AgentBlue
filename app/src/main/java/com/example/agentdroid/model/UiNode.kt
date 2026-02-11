package com.example.agentdroid.model

import android.graphics.Rect

data class UiNode(
    val text: String?,
    val hintText: String?, // [추가됨] 입력창의 힌트 텍스트 (예: "검색")
    val viewIdResourceName: String?,
    val packageName: String?,
    val className: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean, // [추가 추천] 입력 가능한지 여부도 알면 AI 판단에 도움됨
    val children: List<UiNode> = emptyList()
)