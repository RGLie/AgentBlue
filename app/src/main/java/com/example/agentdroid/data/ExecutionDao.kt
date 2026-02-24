package com.example.agentdroid.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {
    @Insert
    suspend fun insert(execution: ExecutionEntity): Long

    @Query("SELECT * FROM executions ORDER BY startTime DESC")
    fun getAllFlow(): Flow<List<ExecutionEntity>>

    @Query("SELECT * FROM executions ORDER BY startTime DESC")
    suspend fun getAll(): List<ExecutionEntity>

    @Query("DELETE FROM executions")
    suspend fun deleteAll()
}
