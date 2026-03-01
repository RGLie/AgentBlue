package com.example.agentdroid.service

import android.util.Log
import com.example.agentdroid.data.SessionPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Firestore의 세션 기반 commands 컬렉션을 실시간으로 리스닝하여
 * 새로운 명령(status == "pending")을 감지하고 AgentAccessibilityService로 전달합니다.
 *
 * 세션이 페어링된 경우 sessions/{sessionId}/commands/ 를 리스닝하고,
 * 페어링되지 않은 경우 기존 commands/ 컬렉션을 폴백으로 리스닝합니다.
 */
class FirebaseCommandListener(
    private val accessibilityService: AgentAccessibilityService
) {
    companion object {
        private const val TAG = "FirebaseCmdListener"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private fun getCommandsPath(): String {
        val sessionId = SessionPreferences.getSessionId()
        return if (sessionId != null) {
            "sessions/$sessionId/commands"
        } else {
            "commands"
        }
    }

    fun startListening() {
        val path = getCommandsPath()
        Log.d(TAG, "Firestore 명령 리스닝 시작: $path")

        listenerRegistration = firestore.collection(path)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "리스닝 오류: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (change in snapshots.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val command = doc.getString("command") ?: continue
                        val documentId = doc.id

                        Log.i(TAG, "새 명령 감지: [$documentId] $command")
                        processCommand(documentId, command)
                    }
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
            Log.d(TAG, "상태 업데이트: processing [$documentId]")

            accessibilityService.executeRemoteCommand(command) { success, message ->
                val finalStatus = if (success) "completed" else "failed"

                docRef.update(
                    mapOf(
                        "status" to finalStatus,
                        "result" to message,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).addOnSuccessListener {
                    Log.i(TAG, "명령 완료: $finalStatus [$documentId] - $message")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "결과 업데이트 실패: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "processing 상태 업데이트 실패: ${e.message}")
        }
    }

    fun restartListening() {
        stopListening()
        startListening()
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Firestore 리스닝 중지")
    }
}
