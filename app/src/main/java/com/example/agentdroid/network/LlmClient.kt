package com.example.agentdroid.network

import android.util.Log
import com.example.agentdroid.data.ModelPreferences
import com.example.agentdroid.model.AiProvider
import com.example.agentdroid.model.AnthropicMessage
import com.example.agentdroid.model.AnthropicRequest
import com.example.agentdroid.model.OpenAiMessage
import com.example.agentdroid.model.OpenAiRequest

object LlmClient {
    private const val TAG = "LlmClient"

    suspend fun chat(systemPrompt: String, userMessage: String): Result<String> {
        val provider = ModelPreferences.getProvider()
        val model = ModelPreferences.getModel()
        val apiKey = ModelPreferences.getApiKey(provider)

        if (apiKey.isBlank()) {
            return Result.failure(Exception("API 키가 설정되지 않았습니다. 앱 설정에서 입력해주세요."))
        }

        Log.d(TAG, "요청: provider=${provider.displayName}, model=$model")

        return try {
            if (provider.isOpenAiCompatible) {
                chatOpenAiCompatible(provider, model, apiKey, systemPrompt, userMessage)
            } else {
                chatAnthropic(provider, model, apiKey, systemPrompt, userMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM 요청 실패: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun chatOpenAiCompatible(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): Result<String> {
        val request = OpenAiRequest(
            model = model,
            messages = listOf(
                OpenAiMessage("system", systemPrompt),
                OpenAiMessage("user", userMessage)
            )
        )

        val headers = mapOf("Authorization" to "Bearer $apiKey")

        val response = RetrofitClient.llmService.openAiChat(
            url = provider.chatEndpointUrl,
            headers = headers,
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "${provider.displayName} API 오류: ${response.code()} $errorBody")
            return Result.failure(Exception(formatApiError(provider.displayName, response.code(), errorBody)))
        }

        val content = response.body()?.choices?.firstOrNull()?.message?.content
            ?: return Result.failure(Exception("AI 응답이 비어있습니다."))

        return Result.success(content)
    }

    private suspend fun chatAnthropic(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): Result<String> {
        val request = AnthropicRequest(
            model = model,
            system = systemPrompt,
            messages = listOf(AnthropicMessage("user", userMessage))
        )

        val headers = mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01"
        )

        val response = RetrofitClient.llmService.anthropicChat(
            url = provider.chatEndpointUrl,
            headers = headers,
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Claude API 오류: ${response.code()} $errorBody")
            return Result.failure(Exception(formatApiError("Claude", response.code(), errorBody)))
        }

        val content = response.body()?.content?.firstOrNull { it.type == "text" }?.text
            ?: return Result.failure(Exception("AI 응답이 비어있습니다."))

        return Result.success(content)
    }

    private fun formatApiError(providerName: String, code: Int, rawBody: String?): String {
        val friendly = when (code) {
            401 -> "API 키가 유효하지 않습니다. 설정에서 키를 확인해주세요."
            402 -> "계정 잔액이 부족합니다. $providerName 대시보드에서 크레딧을 충전해주세요."
            403 -> "이 API 키로는 접근 권한이 없습니다. 키 권한을 확인해주세요."
            429 -> "요청 한도를 초과했습니다. 잠시 후 다시 시도하거나 $providerName 플랜을 업그레이드해주세요."
            500, 502, 503 -> "$providerName 서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요."
            else -> "$providerName API 오류 ($code)"
        }
        return "[$providerName] $friendly"
    }
}
