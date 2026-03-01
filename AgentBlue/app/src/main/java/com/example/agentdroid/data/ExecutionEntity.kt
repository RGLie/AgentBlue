package com.example.agentdroid.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "executions")
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val status: String,
    val resultMessage: String?,
    val stepsJson: String,
    val startTime: Long,
    val endTime: Long?
)
