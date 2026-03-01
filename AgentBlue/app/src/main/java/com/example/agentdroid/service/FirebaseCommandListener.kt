package com.example.agentdroid.service

import android.util.Log
import com.example.agentdroid.AgentStateManager
import com.example.agentdroid.data.SessionPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Listens to the session-based commands collection in Firestore and forwards
 * new commands (status == "pending") to AgentAccessibilityService.
 *
 * Also listens to sessions/{sessionId}/control/current for cancel requests
 * sent from AgentBlueCLI (/stop command).
 */
class FirebaseCommandListener(
    private val accessibilityService: AgentAccessibilityService,
    private val agentStateManager: AgentStateManager
) {
    companion object {
        private const val TAG = "FirebaseCmdListener"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var cancelListenerRegistration: ListenerRegistration? = null

    private fun getSessionId(): String? = SessionPreferences.getSessionId()

    private fun getCommandsPath(): String {
        val sessionId = getSessionId()
        return if (sessionId != null) "sessions/$sessionId/commands" else "commands"
    }

    fun startListening() {
        val path = getCommandsPath()
        Log.d(TAG, "Starting command listener: $path")

        listenerRegistration = firestore.collection(path)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener

                for (change in snapshots.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val command = doc.getString("command") ?: continue
                        Log.i(TAG, "New command detected: [${doc.id}] $command")
                        processCommand(doc.id, command)
                    }
                }
            }

        listenForCancelRequests()
    }

    private fun listenForCancelRequests() {
        val sessionId = getSessionId() ?: return
        val controlRef = firestore.document("sessions/$sessionId/control/current")

        cancelListenerRegistration = controlRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "Cancel listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snap?.getString("action") == "cancel") {
                Log.i(TAG, "Cancel request received from CLI")
                agentStateManager.requestCancel()
                // Reset action so it doesn't trigger again on reconnect
                controlRef.update("action", "idle")
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to reset cancel action: ${e.message}") }
            }
        }
    }

    private fun processCommand(documentId: String, command: String) {
        val path = getCommandsPath()
        val docRef = firestore.collection(path).document(documentId)

        docRef.update(
            mapOf(
                "status" to "processing",
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
        ).addOnSuccessListener {
            Log.d(TAG, "Status updated to processing [$documentId]")

            accessibilityService.executeRemoteCommand(command) { success, message ->
                val finalStatus = if (success) "completed" else "failed"
                docRef.update(
                    mapOf(
                        "status" to finalStatus,
                        "result" to message,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).addOnSuccessListener {
                    Log.i(TAG, "Command finished: $finalStatus [$documentId] - $message")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update result: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to update status to processing: ${e.message}")
        }
    }

    fun restartListening() {
        stopListening()
        startListening()
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        cancelListenerRegistration?.remove()
        cancelListenerRegistration = null
        Log.d(TAG, "Listeners stopped")
    }
}
