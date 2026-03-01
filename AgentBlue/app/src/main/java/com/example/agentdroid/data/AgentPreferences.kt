package com.example.agentdroid.data

import android.content.Context
import android.content.SharedPreferences

object AgentPreferences {

    private const val PREF_NAME = "agent_settings"
    private const val KEY_MAX_STEPS = "max_steps"
    private const val KEY_STEP_DELAY_MS = "step_delay_ms"
    private const val KEY_DEFAULT_BROWSER = "default_browser"
    private const val KEY_LANGUAGE = "language"

    private const val DEFAULT_MAX_STEPS = 15
    private const val DEFAULT_STEP_DELAY_MS = 1500L
    const val DEFAULT_BROWSER = "기본 브라우저"
    const val DEFAULT_LANGUAGE = "한국어"

    val BROWSER_OPTIONS = listOf("Chrome", "Samsung Internet", "Firefox", "기본 브라우저")
    val LANGUAGE_OPTIONS = listOf("한국어", "English")

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getMaxSteps(): Int {
        return prefs?.getInt(KEY_MAX_STEPS, DEFAULT_MAX_STEPS) ?: DEFAULT_MAX_STEPS
    }

    fun setMaxSteps(value: Int) {
        prefs?.edit()?.putInt(KEY_MAX_STEPS, value.coerceIn(5, 30))?.apply()
    }

    fun getStepDelayMs(): Long {
        return prefs?.getLong(KEY_STEP_DELAY_MS, DEFAULT_STEP_DELAY_MS) ?: DEFAULT_STEP_DELAY_MS
    }

    fun setStepDelayMs(value: Long) {
        prefs?.edit()?.putLong(KEY_STEP_DELAY_MS, value.coerceIn(500L, 3000L))?.apply()
    }

    fun getDefaultBrowser(): String {
        return prefs?.getString(KEY_DEFAULT_BROWSER, DEFAULT_BROWSER) ?: DEFAULT_BROWSER
    }

    fun setDefaultBrowser(value: String) {
        prefs?.edit()?.putString(KEY_DEFAULT_BROWSER, value)?.apply()
    }

    fun getLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(value: String) {
        prefs?.edit()?.putString(KEY_LANGUAGE, value)?.apply()
    }
}
