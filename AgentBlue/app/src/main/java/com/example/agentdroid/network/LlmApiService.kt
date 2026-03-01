package com.example.agentdroid.network

import com.example.agentdroid.model.AnthropicRequest
import com.example.agentdroid.model.AnthropicResponse
import com.example.agentdroid.model.OpenAiRequest
import com.example.agentdroid.model.OpenAiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

interface LlmApiService {

    @POST
    suspend fun openAiChat(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: OpenAiRequest
    ): Response<OpenAiResponse>

    @POST
    suspend fun anthropicChat(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: AnthropicRequest
    ): Response<AnthropicResponse>
}
