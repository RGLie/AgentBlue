package com.example.agentdroid.data

import android.content.Context
import android.content.SharedPreferences

object SessionPreferences {
    private const val PREF_NAME = "session_settings"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_CODE = "session_code"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getSessionId(): String? = prefs?.getString(KEY_SESSION_ID, null)

    fun getSessionCode(): String? = prefs?.getString(KEY_SESSION_CODE, null)

    fun save(sessionId: String, sessionCode: String) {
        prefs?.edit()?.apply {
            putString(KEY_SESSION_ID, sessionId)
            putString(KEY_SESSION_CODE, sessionCode)
            apply()
        }
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    fun hasPairedSession(): Boolean = getSessionId() != null
}
