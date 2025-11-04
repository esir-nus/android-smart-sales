package com.smartsales.data.repository

import com.smartsales.data.local.dao.AttachmentDao
import com.smartsales.data.local.dao.ConversationDao
import com.smartsales.data.local.dao.MessageDao
import com.smartsales.data.local.entity.AttachmentEntity
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.entity.MessageEntity
import com.smartsales.domain.model.Attachment
import com.smartsales.domain.model.ConversationWithMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao
) {
    
    val allConversations: Flow<List<ConversationEntity>> = 
        conversationDao.getAllConversations()
    
    /**
     * Get conversation with all its messages
     */
    suspend fun getConversationWithMessages(conversationId: Long): ConversationWithMessages? {
        val conversation = conversationDao.getConversationById(conversationId) 
            ?: return null
        val messages = messageDao.getMessagesByConversation(conversationId).first()
        return ConversationWithMessages(conversation, messages)
    }
    
    /**
     * Create a new conversation
     */
    suspend fun createConversation(title: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insertConversation(
            ConversationEntity(
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )
    }
    
    /**
     * Add a message to conversation
     */
    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        attachments: List<Attachment>? = null
    ): Long {
        // Update conversation timestamp
        conversationDao.getConversationById(conversationId)?.let { conversation ->
            conversationDao.updateConversation(
                conversation.copy(updatedAt = System.currentTimeMillis())
            )
        }
        
        // Insert message
        val messageId = messageDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                attachments = attachments?.let { 
                    com.google.gson.Gson().toJson(it) 
                }
            )
        )
        
        // Insert attachments
        attachments?.forEach { attachment ->
            attachmentDao.insertAttachment(
                AttachmentEntity(
                    messageId = messageId,
                    type = attachment.type,
                    filePath = attachment.path,
                    fileName = attachment.name,
                    fileSize = attachment.size,
                    mimeType = attachment.mimeType,
                    uploadedAt = System.currentTimeMillis()
                )
            )
        }
        
        return messageId
    }
    
    /**
     * Delete a conversation and all its messages
     */
    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversationById(conversationId)
    }
    
    /**
     * Update conversation title
     */
    suspend fun updateTitle(conversationId: Long, title: String) {
        conversationDao.updateTitle(conversationId, title)
    }
    
    /**
     * Mark conversation as analyzed
     */
    suspend fun markAsAnalyzed(conversationId: Long) {
        conversationDao.markAsAnalyzed(conversationId, true)
    }
    
    /**
     * Get conversations grouped by time periods
     * Returns map of "7å¤©å†…", "30å¤©å†…", "2025å¹´9æœˆ" etc.
     */
    suspend fun getConversationsGroupedByTime(): Map<String, List<ConversationEntity>> {
        val conversations = conversationDao.getAllConversations().first()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
        val thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000L
        
        return conversations.groupBy { conversation ->
            when {
                conversation.createdAt >= sevenDaysAgo -> "7å¤©å†…"
                conversation.createdAt >= thirtyDaysAgo -> "30å¤©å†…"
                else -> {
                    val date = Date(conversation.createdAt)
                    SimpleDateFormat("yyyyå¹´MMæœˆ", Locale.CHINESE).format(date)
                }
            }
        }
    }
    
    /**
     * Get messages for a conversation as Flow
     */
    fun getMessagesFlow(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByConversation(conversationId)
    }
    
    /**
     * Search conversations by title or content
     */
    fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        return allConversations.map { conversations ->
            conversations.filter { 
                it.title.contains(query, ignoreCase = true) ||
                it.summary?.contains(query, ignoreCase = true) == true
            }
        }
    }
}
