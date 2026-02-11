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

// 3. AI가 최종적으로 뱉을 JSON 내용을 담을 객체 (우리가 쓸 진짜 데이터)
data class LlmAction(
    @SerializedName("action_type") val actionType: String, // "CLICK" 또는 "TYPE"
    @SerializedName("target_text") val targetText: String?, // 찾을 대상 (힌트 텍스트 등)
    @SerializedName("input_text") val inputText: String?, // [추가] 타이핑할 내용
    @SerializedName("reasoning") val reasoning: String?
)