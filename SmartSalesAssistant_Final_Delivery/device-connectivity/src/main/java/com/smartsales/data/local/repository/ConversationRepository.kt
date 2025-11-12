package com.smartsales.data.local.repository

import com.smartsales.data.local.dao.ConversationDao
import com.smartsales.data.local.dao.MessageDao
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository
    @Inject
    constructor(
        private val conversationDao: ConversationDao,
        private val messageDao: MessageDao,
    ) {
        /**
         * Get all conversations as Flow
         * Updates automatically when database changes
         */
        fun getAllConversations(): Flow<List<ConversationEntity>> {
            return conversationDao.getAllConversations()
        }

        /**
         * Get conversations within a time range
         */
        fun getConversationsByTimeRange(
            startTime: Long,
            endTime: Long,
        ): Flow<List<ConversationEntity>> {
            return conversationDao.getConversationsByTimeRange(startTime, endTime)
        }

        /**
         * Get specific conversation by ID
         */
        suspend fun getConversationById(id: Long): ConversationEntity? {
            return conversationDao.getConversationById(id)
        }

        /**
         * Create new conversation
         * Returns the new conversation ID
         */
        suspend fun createConversation(title: String): Result<Long> {
            return try {
                val conversation =
                    ConversationEntity(
                        title = title,
                        summary = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isAnalyzed = false,
                    )
                val id = conversationDao.insertConversation(conversation)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update conversation title
         */
        suspend fun updateTitle(
            id: Long,
            title: String,
        ): Result<Unit> {
            return try {
                conversationDao.updateTitle(id, title)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update entire conversation
         */
        suspend fun updateConversation(conversation: ConversationEntity): Result<Unit> {
            return try {
                conversationDao.updateConversation(conversation)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete conversation (cascades to messages)
         */
        suspend fun deleteConversation(id: Long): Result<Unit> {
            return try {
                conversationDao.deleteConversationById(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Mark conversation as analyzed
         */
        suspend fun markAsAnalyzed(id: Long): Result<Unit> {
            return try {
                conversationDao.markAsAnalyzed(id, true)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // ===== MESSAGE OPERATIONS =====

        /**
         * Get all messages for a conversation
         */
        fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
            return messageDao.getMessagesByConversation(conversationId)
        }

        /**
         * Get specific message by ID
         */
        suspend fun getMessageById(id: Long): MessageEntity? {
            return messageDao.getMessageById(id)
        }

        /**
         * Add message to conversation
         * Automatically updates conversation's updatedAt timestamp
         */
        suspend fun addMessage(
            conversationId: Long,
            role: String, // "user" or "assistant"
            content: String,
            attachments: String? = null,
            metadata: String? = null,
        ): Result<Long> {
            return try {
                // Create message
                val message =
                    MessageEntity(
                        conversationId = conversationId,
                        role = role,
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        attachments = attachments,
                        metadata = metadata,
                    )
                val messageId = messageDao.insertMessage(message)

                // Update conversation timestamp
                conversationDao.getConversationById(conversationId)?.let { conv ->
                    conversationDao.updateConversation(
                        conv.copy(updatedAt = System.currentTimeMillis()),
                    )
                }

                Result.success(messageId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Add multiple messages at once (bulk insert)
         */
        suspend fun addMessages(messages: List<MessageEntity>): Result<Unit> {
            return try {
                messageDao.insertMessages(messages)

                // Update conversation timestamp if messages exist
                messages.firstOrNull()?.conversationId?.let { convId ->
                    conversationDao.getConversationById(convId)?.let { conv ->
                        conversationDao.updateConversation(
                            conv.copy(updatedAt = System.currentTimeMillis()),
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete specific message
         */
        suspend fun deleteMessage(message: MessageEntity): Result<Unit> {
            return try {
                messageDao.deleteMessage(message)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete all messages in a conversation
         */
        suspend fun deleteMessagesByConversation(conversationId: Long): Result<Unit> {
            return try {
                messageDao.deleteMessagesByConversation(conversationId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Get recent messages with attachments (for quick access)
         */
        suspend fun getRecentMessagesWithAttachments(limit: Int = 20): List<MessageEntity> {
            return messageDao.getRecentMessagesWithAttachments(limit)
        }
    }
