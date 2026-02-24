package com.example.agentdroid.model

enum class AgentStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class StepLog(
    val step: Int,
    val actionType: String,
    val targetText: String?,
    val reasoning: String?,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExecutionRecord(
    val id: Long = System.currentTimeMillis(),
    val command: String,
    val status: AgentStatus,
    val steps: List<StepLog>,
    val resultMessage: String?,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)
