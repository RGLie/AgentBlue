package com.example.agentdroid

import android.content.Context
import android.util.Log
import com.example.agentdroid.data.AppDatabase
import com.example.agentdroid.data.ExecutionEntity
import com.example.agentdroid.data.SessionPreferences
import com.example.agentdroid.model.AgentStatus
import com.example.agentdroid.model.StepLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AgentStateManager {

    private const val TAG = "AgentStateManager"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()

    private var database: AppDatabase? = null

    private val _status = MutableStateFlow(AgentStatus.IDLE)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    private val _currentCommand = MutableStateFlow<String?>(null)
    val currentCommand: StateFlow<String?> = _currentCommand.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _maxSteps = MutableStateFlow(0)
    val maxSteps: StateFlow<Int> = _maxSteps.asStateFlow()

    private val _currentReasoning = MutableStateFlow<String?>(null)
    val currentReasoning: StateFlow<String?> = _currentReasoning.asStateFlow()

    private val _liveSteps = MutableStateFlow<List<StepLog>>(emptyList())
    val liveSteps: StateFlow<List<StepLog>> = _liveSteps.asStateFlow()

    private val _cancelRequested = MutableStateFlow(false)
    val cancelRequested: StateFlow<Boolean> = _cancelRequested.asStateFlow()

    private var currentRecordStartTime: Long = 0L

    fun init(context: Context) {
        database = AppDatabase.getInstance(context)
        SessionPreferences.init(context)
    }

    fun getHistoryFlow(): Flow<List<ExecutionEntity>> {
        return database?.executionDao()?.getAllFlow()
            ?: MutableStateFlow(emptyList())
    }

    fun onExecutionStarted(command: String, maxSteps: Int) {
        _status.value = AgentStatus.RUNNING
        _currentCommand.value = command
        _currentStep.value = 0
        _maxSteps.value = maxSteps
        _currentReasoning.value = null
        _liveSteps.value = emptyList()
        _cancelRequested.value = false
        currentRecordStartTime = System.currentTimeMillis()
        syncToFirestore()
    }

    fun onStepCompleted(stepLog: StepLog) {
        _currentStep.value = stepLog.step
        _currentReasoning.value = stepLog.reasoning
        _liveSteps.update { it + stepLog }
        syncToFirestore()
    }

    fun onExecutionFinished(status: AgentStatus, resultMessage: String) {
        val entity = ExecutionEntity(
            command = _currentCommand.value ?: "",
            status = status.name,
            resultMessage = resultMessage,
            stepsJson = gson.toJson(_liveSteps.value),
            startTime = currentRecordStartTime,
            endTime = System.currentTimeMillis()
        )

        scope.launch {
            database?.executionDao()?.insert(entity)
        }

        _status.value = status
        _currentReasoning.value = resultMessage
        syncToFirestore()
    }

    fun requestCancel() {
        _cancelRequested.value = true
    }

    fun isCancelRequested(): Boolean = _cancelRequested.value

    fun clearHistory() {
        scope.launch {
            database?.executionDao()?.deleteAll()
        }
    }

    fun reset() {
        _status.value = AgentStatus.IDLE
        _currentCommand.value = null
        _currentStep.value = 0
        _currentReasoning.value = null
        _liveSteps.value = emptyList()
        _cancelRequested.value = false
        syncToFirestore()
    }

    fun parseStepsFromJson(json: String): List<StepLog> {
        return try {
            val type = object : TypeToken<List<StepLog>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun syncToFirestore() {
        val sessionId = SessionPreferences.getSessionId() ?: return

        val stepsData = _liveSteps.value.map { step ->
            mapOf(
                "step" to step.step,
                "actionType" to step.actionType,
                "targetText" to step.targetText,
                "reasoning" to step.reasoning,
                "success" to step.success,
                "timestamp" to step.timestamp
            )
        }

        val data = mapOf(
            "status" to _status.value.name,
            "currentCommand" to _currentCommand.value,
            "currentStep" to _currentStep.value,
            "maxSteps" to _maxSteps.value,
            "currentReasoning" to _currentReasoning.value,
            "liveSteps" to stepsData,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("sessions").document(sessionId)
            .collection("agentState").document("current")
            .set(data)
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore 상태 동기화 실패: ${e.message}")
            }
    }
}
