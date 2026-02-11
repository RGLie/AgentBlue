package com.example.agentdroid.network

import com.example.agentdroid.model.OpenAiRequest
import com.example.agentdroid.model.OpenAiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgentApiService {

    @POST("v1/chat/completions")
    suspend fun getAction(
        @Header("Authorization") apiKey: String, // 헤더에 키를 넣어서 보냄
        @Body request: OpenAiRequest
    ): Response<OpenAiResponse>
}