package com.example.agentdroid.model

import com.google.gson.annotations.SerializedName

// 1. OpenAI 요청 (Request)
data class OpenAiRequest(
    val model: String = "gpt-4o-mini", // 사용할 모델 (가성비 좋은 gpt-4o-mini 추천)
    val messages: List<OpenAiMessage>,
    @SerializedName("response_format") val responseFormat: ResponseFormat = ResponseFormat(type = "json_object") // 무조건 JSON으로 줘!
)

data class OpenAiMessage(
    val role: String, // "system" 또는 "user"
    val content: String
)

data class ResponseFormat(
    val type: String
)

// 2. OpenAI 응답 (Response)
data class OpenAiResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: OpenAiMessage
)

data class LlmAction(
    @SerializedName("action_type") val actionType: String,
    @SerializedName("target_text") val targetText: String?,
    @SerializedName("target_id") val targetId: String?,
    @SerializedName("input_text") val inputText: String?,
    @SerializedName("reasoning") val reasoning: String?
) {
    fun isDone(): Boolean = actionType.uppercase() == "DONE"

    fun toHistoryEntry(step: Int, success: Boolean): String {
        val status = if (success) "SUCCESS" else "FAILED"
        return "Step $step [$status]: $actionType" +
                (targetText?.let { " on '$it'" } ?: "") +
                (inputText?.let { " with text '$it'" } ?: "") +
                " — $reasoning"
    }
}