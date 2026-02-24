package com.example.agentdroid.model

import android.graphics.Rect

data class UiNode(
    val text: String?,
    val hintText: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val packageName: String?,
    val className: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val children: List<UiNode> = emptyList()
)