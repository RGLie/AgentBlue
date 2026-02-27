package com.example.agentdroid.data

import android.content.Context
import android.content.SharedPreferences

object ConsentPreferences {

    private const val PREF_NAME = "consent_settings"
    private const val KEY_PRIVACY_AGREED = "privacy_agreed"
    private const val KEY_TERMS_AGREED = "terms_agreed"
    private const val KEY_ACCESSIBILITY_DISCLOSED = "accessibility_disclosed"
    private const val KEY_CONSENT_VERSION = "consent_version"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"

    const val CURRENT_CONSENT_VERSION = 1

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun hasFullConsent(): Boolean {
        val p = prefs ?: return false
        return p.getBoolean(KEY_PRIVACY_AGREED, false)
                && p.getBoolean(KEY_TERMS_AGREED, false)
                && p.getBoolean(KEY_ACCESSIBILITY_DISCLOSED, false)
                && p.getInt(KEY_CONSENT_VERSION, 0) >= CURRENT_CONSENT_VERSION
    }

    fun saveConsent() {
        prefs?.edit()?.apply {
            putBoolean(KEY_PRIVACY_AGREED, true)
            putBoolean(KEY_TERMS_AGREED, true)
            putBoolean(KEY_ACCESSIBILITY_DISCLOSED, true)
            putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun revokeConsent() {
        prefs?.edit()?.clear()?.apply()
    }

    fun getConsentTimestamp(): Long {
        return prefs?.getLong(KEY_CONSENT_TIMESTAMP, 0L) ?: 0L
    }
}
