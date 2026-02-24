package com.example.agentdroid.data

import android.content.Context
import android.content.SharedPreferences
import com.example.agentdroid.model.AiProvider

object ModelPreferences {
    private const val PREF_NAME = "ai_model_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_MODEL_PREFIX = "model_"
    private const val KEY_API_KEY_PREFIX = "api_key_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context, defaultOpenAiKey: String? = null) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (!defaultOpenAiKey.isNullOrBlank() && getApiKey(AiProvider.OPENAI).isBlank()) {
            prefs?.edit()
                ?.putString(KEY_API_KEY_PREFIX + AiProvider.OPENAI.name, defaultOpenAiKey)
                ?.apply()
        }
    }

    fun getProvider(): AiProvider {
        val name = prefs?.getString(KEY_PROVIDER, null) ?: return AiProvider.OPENAI
        return try {
            AiProvider.valueOf(name)
        } catch (_: Exception) {
            AiProvider.OPENAI
        }
    }

    fun getModel(): String {
        return getModelForProvider(getProvider())
    }

    fun getModelForProvider(provider: AiProvider): String {
        return prefs?.getString(KEY_MODEL_PREFIX + provider.name, null)
            ?: provider.models.first().id
    }

    fun getApiKey(provider: AiProvider): String {
        return prefs?.getString(KEY_API_KEY_PREFIX + provider.name, "") ?: ""
    }

    fun save(provider: AiProvider, model: String, apiKey: String) {
        prefs?.edit()?.apply {
            putString(KEY_PROVIDER, provider.name)
            putString(KEY_MODEL_PREFIX + provider.name, model)
            putString(KEY_API_KEY_PREFIX + provider.name, apiKey)
            apply()
        }
    }

    fun hasApiKey(): Boolean {
        return getApiKey(getProvider()).isNotBlank()
    }
}
