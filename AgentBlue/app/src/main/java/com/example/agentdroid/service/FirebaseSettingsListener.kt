package com.example.agentdroid.service

import android.util.Log
import com.example.agentdroid.data.AgentPreferences
import com.example.agentdroid.data.ModelPreferences
import com.example.agentdroid.data.SessionPreferences
import com.example.agentdroid.model.AiProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Listens to sessions/{sessionId}/settings/current in Firestore and applies
 * any changes to AgentPreferences and ModelPreferences on the device.
 *
 * This enables remote configuration from AgentBlueCLI via:
 *   agentblue setting  — agent behavior settings
 *   agentblue model    — AI model and API key settings
 */
class FirebaseSettingsListener(
    private val agentPreferences: AgentPreferences = AgentPreferences,
    private val modelPreferences: ModelPreferences = ModelPreferences
) {
    companion object {
        private const val TAG = "FirebaseSettingsListener"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        val sessionId = SessionPreferences.getSessionId() ?: return
        val settingsRef = firestore.document("sessions/$sessionId/settings/current")

        Log.d(TAG, "Starting settings listener for session: $sessionId")

        listenerRegistration = settingsRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "Settings listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) return@addSnapshotListener

            Log.i(TAG, "Remote settings received, applying...")
            applyAgentSettings(snap)
            applyModelSettings(snap)
        }
    }

    private fun applyAgentSettings(snap: com.google.firebase.firestore.DocumentSnapshot) {
        snap.getLong("maxSteps")?.let { value ->
            agentPreferences.setMaxSteps(value.toInt())
            Log.d(TAG, "maxSteps → $value")
        }
        snap.getLong("stepDelayMs")?.let { value ->
            agentPreferences.setStepDelayMs(value)
            Log.d(TAG, "stepDelayMs → $value")
        }
        snap.getString("defaultBrowser")?.let { value ->
            if (value in AgentPreferences.BROWSER_OPTIONS) {
                agentPreferences.setDefaultBrowser(value)
                Log.d(TAG, "defaultBrowser → $value")
            }
        }
        snap.getString("language")?.let { value ->
            if (value in AgentPreferences.LANGUAGE_OPTIONS) {
                agentPreferences.setLanguage(value)
                Log.d(TAG, "language → $value")
            }
        }
    }

    private fun applyModelSettings(snap: com.google.firebase.firestore.DocumentSnapshot) {
        val providerName = snap.getString("provider") ?: return
        val provider = try {
            AiProvider.valueOf(providerName)
        } catch (_: Exception) {
            Log.w(TAG, "Unknown provider: $providerName")
            return
        }

        val model = snap.getString("model") ?: provider.models.first().id
        val apiKey = snap.getString("apiKey") ?: ""

        if (apiKey.isNotBlank()) {
            modelPreferences.save(provider, model, apiKey)
            Log.i(TAG, "Model settings applied: provider=$providerName model=$model (API key updated)")
        } else {
            // Preserve existing API key — only update provider and model
            val existingKey = modelPreferences.getApiKey(provider)
            modelPreferences.save(provider, model, existingKey)
            Log.i(TAG, "Model settings applied: provider=$providerName model=$model (API key unchanged)")
        }
    }

    fun restartListening() {
        stopListening()
        startListening()
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Settings listener stopped")
    }
}
