package com.example.agentdroid.service

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.agentdroid.model.UiNode
import com.google.gson.Gson

class UiTreeParser {

    companion object {
        private const val TAG = "UiTreeParser"
    }

    private val gson = Gson()

    fun parse(rootNode: AccessibilityNodeInfo): UiNode = parseNode(rootNode)

    fun toJson(uiTree: UiNode): String = gson.toJson(uiTree)

    fun logTree(node: UiNode, depth: Int = 0) {
        if (!node.text.isNullOrEmpty()
            || !node.viewIdResourceName.isNullOrEmpty()
            || !node.contentDescription.isNullOrEmpty()
        ) {
            val indent = "  ".repeat(depth)
            Log.i(
                TAG,
                "$indent [${node.className}] ID: ${node.viewIdResourceName} " +
                        "| Text: ${node.text} | Desc: ${node.contentDescription} " +
                        "| Editable: ${node.isEditable} | Clickable: ${node.isClickable}"
            )
        }
        for (child in node.children) {
            logTree(child, depth + 1)
        }
    }

    private fun parseNode(node: AccessibilityNodeInfo): UiNode {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val children = buildList {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    add(parseNode(child))
                    child.recycle()
                }
            }
        }

        val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()
        } else {
            null
        }

        return UiNode(
            text = node.text?.toString(),
            hintText = hintText,
            contentDescription = node.contentDescription?.toString(),
            viewIdResourceName = node.viewIdResourceName,
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            bounds = rect,
            isClickable = node.isClickable,
            isEditable = node.isEditable,
            children = children
        )
    }
}
