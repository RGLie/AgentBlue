package com.example.agentdroid.service

import android.util.Log
import com.example.agentdroid.data.AgentPreferences
import com.example.agentdroid.model.LlmAction
import com.example.agentdroid.model.UiNode
import com.example.agentdroid.network.LlmClient
import com.google.gson.Gson

class ScreenAnalyzer(
    private val uiTreeParser: UiTreeParser = UiTreeParser()
) {
    companion object {
        private const val TAG = "ScreenAnalyzer"

        fun buildSystemPrompt(defaultBrowser: String, language: String): String {
            val browserInstruction = if (defaultBrowser != AgentPreferences.DEFAULT_BROWSER) {
                "\n            - When the user's goal involves web search or opening a website, prefer opening the \"$defaultBrowser\" app."
            } else {
                ""
            }

            val languageInstruction = if (language == "English") {
                "Write all reasoning fields in English."
            } else {
                "Write all reasoning fields in Korean (한국어)."
            }

            return """
            You are an Android Automation Agent operating in a step-by-step ReAct loop.
            
            At each step you will receive:
            1. The user's original goal
            2. The history of actions you have already taken and their results
            3. The CURRENT screen's UI tree (JSON)
            
            Your job: decide the SINGLE NEXT action to take toward completing the goal.
            
            Available actions:
            - "CLICK": Tap on a UI element.
            - "TYPE": Enter text into an input field.
            - "SCROLL": Scroll the screen. Set "target_text" to "DOWN" or "UP".
            - "BACK": Press the Android back button. Use when the current screen is not relevant to the goal.
            - "HOME": Press the Android home button. Use as a last resort to return to the launcher and start over.
            - "DONE": The user's goal has been fully achieved.
            
            Targeting elements:
            - "target_text": The visible text, hint text, or content description of the element.
            - "target_id": (Optional) The viewIdResourceName from the UI tree. Use this when multiple elements share the same text and you need to be precise. Use just the ID part (e.g. "linearLayout"), not the full path.
            - For TYPE action: "target_text" is the hint/label, "input_text" is the text to type.
            
            IMPORTANT - Clicking behavior:
            - In Android, clickable containers (ViewGroup, LinearLayout) often wrap non-clickable TextViews.
            - When you want to click a list item or suggestion, target the TEXT of the child element.
            - The system will automatically find the clickable parent if the text element itself is not clickable.
            - When target_text matches an EditText you already typed in, prefer using target_id to specify the actual suggestion or button instead.
            
            Navigation Recovery Rules (CRITICAL):
            - BEFORE choosing CLICK, verify the target element actually exists in the CURRENT UI tree.
            - If the current screen has NO elements related to the goal, do NOT click random elements. Use BACK instead.
            - If you have used BACK 2+ times in recent history and still haven't reached a relevant screen, use HOME to return to the launcher.
            - After HOME, look for the target app icon on the home screen and tap it to restart.
            - If the screen's package name differs from the target app, prefer BACK or HOME over clicking.
            
            Stuck Prevention Rules (CRITICAL):
            - NEVER repeat an action that already FAILED with the same target_text and action_type.
            - If the last 2 actions in history are both FAILED, you MUST change strategy: try SCROLL to reveal hidden elements, BACK to leave the screen, or HOME to start over.
            - If you see a "[SYSTEM HINT]" in the action history, follow its guidance immediately.
            - When no viable action exists on the current screen, use BACK or HOME. Do NOT guess or click unrelated elements.
            
            User Preferences:$browserInstruction
            - $languageInstruction
            
            Rules:
            - Return exactly ONE action per response.
            - Analyze what has already been done (history) to avoid repeating actions.
            - If a previous action FAILED, try an alternative: different target_text, use target_id, scroll to find it, etc.
            - If stuck after multiple retries, return DONE with reasoning explaining why.
            - Always output valid JSON only, no markdown.
            
            Output Format:
            {
              "action_type": "CLICK" | "TYPE" | "SCROLL" | "BACK" | "HOME" | "DONE",
              "target_text": "visible text or content description",
              "target_id": "resource ID (optional, for disambiguation)",
              "input_text": "text to type (only for TYPE)",
              "reasoning": "why you chose this action"
            }
        """.trimIndent()
        }
    }

    private val gson = Gson()

    suspend fun analyze(
        uiTree: UiNode,
        userCommand: String,
        actionHistory: List<String>
    ): Result<LlmAction> {
        return try {
            val uiJson = uiTreeParser.toJson(uiTree)
            uiTreeParser.logTree(uiTree)

            val historyText = if (actionHistory.isEmpty()) {
                "No actions taken yet. This is the first step."
            } else {
                actionHistory.joinToString("\n")
            }

            val userMessage = buildString {
                appendLine("=== USER GOAL ===")
                appendLine(userCommand)
                appendLine()
                appendLine("=== ACTION HISTORY ===")
                appendLine(historyText)
                appendLine()
                appendLine("=== CURRENT SCREEN UI ===")
                append(uiJson)
            }

            val systemPrompt = buildSystemPrompt(
                defaultBrowser = AgentPreferences.getDefaultBrowser(),
                language = AgentPreferences.getLanguage()
            )

            val result = LlmClient.chat(systemPrompt, userMessage)

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }

            val content = result.getOrThrow()
            Log.d(TAG, "AI 응답: $content")

            val action = parseAction(content)
            Log.d(TAG, "AI 결정: ${action.actionType} -> text='${action.targetText}' id='${action.targetId}' (이유: ${action.reasoning})")
            Result.success(action)
        } catch (e: Exception) {
            Log.e(TAG, "화면 분석 중 에러: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseAction(jsonString: String): LlmAction {
        val cleanJson = jsonString
            .replace("```json", "")
            .replace("```", "")
            .trim()
        return gson.fromJson(cleanJson, LlmAction::class.java)
    }
}
