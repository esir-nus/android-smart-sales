package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // ===== QUERY OPERATIONS =====

    /**
     * Get all messages for a conversation
     * Ordered chronologically (oldest first)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Get message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    /**
     * Get latest message in conversation
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestMessage(conversationId: Long): MessageEntity?

    /**
     * Get messages by role (user or assistant)
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId AND role = :role 
        ORDER BY timestamp ASC
    """)
    fun getMessagesByRole(conversationId: Long, role: String): Flow<List<MessageEntity>>

    /**
     * Get user messages only
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId AND role = 'user' 
        ORDER BY timestamp ASC
    """)
    fun getUserMessages(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Get assistant messages only
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId AND role = 'assistant' 
        ORDER BY timestamp ASC
    """)
    fun getAssistantMessages(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Search messages by content
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND content LIKE '%' || :query || '%' 
        ORDER BY timestamp ASC
    """)
    fun searchMessages(conversationId: Long, query: String): Flow<List<MessageEntity>>

    /**
     * Get messages with attachments
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND attachments IS NOT NULL 
        ORDER BY timestamp ASC
    """)
    fun getMessagesWithAttachments(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Get recent messages with attachments (across all conversations)
     */
    @Query("""
        SELECT m.* FROM messages m 
        INNER JOIN conversations c ON m.conversationId = c.id 
        WHERE m.attachments IS NOT NULL 
        ORDER BY m.timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessagesWithAttachments(limit: Int = 20): List<MessageEntity>

    /**
     * Get messages within time range
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND timestamp >= :startTime 
        AND timestamp < :endTime 
        ORDER BY timestamp ASC
    """)
    fun getMessagesByTimeRange(
        conversationId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<MessageEntity>>

    /**
     * Count messages in conversation
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

    /**
     * Count messages by role
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE conversationId = :conversationId AND role = :role
    """)
    suspend fun getMessageCountByRole(conversationId: Long, role: String): Int

    /**
     * Get last N messages in conversation
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getLastNMessages(conversationId: Long, limit: Int): List<MessageEntity>

    // ===== INSERT OPERATIONS =====

    /**
     * Insert single message
     * Returns the new message ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Insert multiple messages (bulk)
     * Returns list of IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    // ===== UPDATE OPERATIONS =====

    /**
     * Update message
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Update message content
     */
    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    /**
     * Update message metadata
     */
    @Query("UPDATE messages SET metadata = :metadata WHERE id = :id")
    suspend fun updateMetadata(id: Long, metadata: String?)

    /**
     * Update message attachments
     */
    @Query("UPDATE messages SET attachments = :attachments WHERE id = :id")
    suspend fun updateAttachments(id: Long, attachments: String?)

    // ===== DELETE OPERATIONS =====

    /**
     * Delete single message
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    /**
     * Delete message by ID
     */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    /**
     * Delete all messages in conversation
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)

    /**
     * Delete multiple messages
     */
    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteMessages(ids: List<Long>)

    /**
     * Delete messages by role
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND role = :role")
    suspend fun deleteMessagesByRole(conversationId: Long, role: String)

    /**
     * Delete old messages (older than timestamp)
     */
    @Query("DELETE FROM messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)

    /**
     * Delete all messages
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}