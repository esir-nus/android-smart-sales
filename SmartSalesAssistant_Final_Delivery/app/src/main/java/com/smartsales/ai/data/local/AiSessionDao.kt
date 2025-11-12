package com.smartsales.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiSessionDao {
    @Query("SELECT * FROM ai_sessions ORDER BY timestampMillis DESC")
    fun observeSessions(): Flow<List<AiSessionEntity>>

    @Query("SELECT * FROM ai_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun findById(sessionId: String): AiSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiSessionEntity)

    @Query("DELETE FROM ai_sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
}
