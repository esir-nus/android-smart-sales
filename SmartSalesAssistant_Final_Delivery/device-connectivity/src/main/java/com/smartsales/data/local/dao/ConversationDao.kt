package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    // ===== QUERY OPERATIONS =====

    /**
     * Get all conversations ordered by last updated
     * Returns Flow for automatic UI updates
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    /**
     * Get conversation by ID
     */
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    /**
     * Get conversation by ID as Flow (for observing changes)
     */
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationByIdFlow(id: Long): Flow<ConversationEntity?>

    /**
     * Get conversations within a time range
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE createdAt >= :startTime AND createdAt < :endTime 
        ORDER BY createdAt DESC
    """,
    )
    fun getConversationsByTimeRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<ConversationEntity>>

    /**
     * Get analyzed conversations only
     */
    @Query("SELECT * FROM conversations WHERE isAnalyzed = 1 ORDER BY updatedAt DESC")
    fun getAnalyzedConversations(): Flow<List<ConversationEntity>>

    /**
     * Get unanalyzed conversations only
     */
    @Query("SELECT * FROM conversations WHERE isAnalyzed = 0 ORDER BY updatedAt DESC")
    fun getUnanalyzedConversations(): Flow<List<ConversationEntity>>

    /**
     * Search conversations by title
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE title LIKE '%' || :query || '%' 
        ORDER BY updatedAt DESC
    """,
    )
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    /**
     * Get recent conversations (limit)
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentConversations(limit: Int = 20): Flow<List<ConversationEntity>>

    /**
     * Get conversations created today
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE createdAt >= :todayStart 
        ORDER BY createdAt DESC
    """,
    )
    fun getTodayConversations(todayStart: Long): Flow<List<ConversationEntity>>

    /**
     * Count total conversations
     */
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    /**
     * Count analyzed conversations
     */
    @Query("SELECT COUNT(*) FROM conversations WHERE isAnalyzed = 1")
    suspend fun getAnalyzedCount(): Int

    // ===== INSERT OPERATIONS =====

    /**
     * Insert new conversation
     * Returns the new conversation ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    /**
     * Insert multiple conversations
     * Returns list of IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>): List<Long>

    // ===== UPDATE OPERATIONS =====

    /**
     * Update entire conversation
     */
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    /**
     * Update multiple conversations
     */
    @Update
    suspend fun updateConversations(conversations: List<ConversationEntity>)

    /**
     * Update conversation title
     */
    @Query("UPDATE conversations SET title = :title, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTitle(
        id: Long,
        title: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Update conversation summary
     */
    @Query("UPDATE conversations SET summary = :summary, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateSummary(
        id: Long,
        summary: String?,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Update conversation timestamp (for new messages)
     */
    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(
        id: Long,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Mark conversation as analyzed
     */
    @Query("UPDATE conversations SET isAnalyzed = :analyzed, updatedAt = :timestamp WHERE id = :id")
    suspend fun markAsAnalyzed(
        id: Long,
        analyzed: Boolean,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Mark multiple conversations as analyzed
     */
    @Query("UPDATE conversations SET isAnalyzed = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsAnalyzed(ids: List<Long>)

    // ===== DELETE OPERATIONS =====

    /**
     * Delete conversation by entity
     */
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    /**
     * Delete conversation by ID
     */
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    /**
     * Delete multiple conversations
     */
    @Query("DELETE FROM conversations WHERE id IN (:ids)")
    suspend fun deleteConversations(ids: List<Long>)

    /**
     * Delete all conversations
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    /**
     * Delete old conversations (older than timestamp)
     */
    @Query("DELETE FROM conversations WHERE createdAt < :timestamp")
    suspend fun deleteOldConversations(timestamp: Long)

    // ===== TRANSACTION OPERATIONS =====

    /**
     * Transaction: Create conversation and return ID
     */
    @Transaction
    suspend fun createConversationTransaction(
        title: String,
        summary: String? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val conversation =
            ConversationEntity(
                title = title,
                summary = summary,
                createdAt = now,
                updatedAt = now,
                isAnalyzed = false,
            )
        return insertConversation(conversation)
    }
}
