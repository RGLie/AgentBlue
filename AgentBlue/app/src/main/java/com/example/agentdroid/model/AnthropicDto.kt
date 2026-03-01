package com.example.agentdroid.model

import com.google.gson.annotations.SerializedName

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val system: String,
    val messages: List<AnthropicMessage>
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicResponse(
    val content: List<AnthropicContent>
)

data class AnthropicContent(
    val type: String,
    val text: String
)
