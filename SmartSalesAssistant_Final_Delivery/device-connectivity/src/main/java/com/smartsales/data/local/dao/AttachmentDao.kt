package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    // ===== QUERY OPERATIONS =====

    /**
     * Get all attachments for a message
     */
    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY uploadedAt ASC")
    suspend fun getAttachmentsByMessage(messageId: Long): List<AttachmentEntity>

    /**
     * Get all attachments for a message as Flow
     */
    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY uploadedAt ASC")
    fun getAttachmentsByMessageFlow(messageId: Long): Flow<List<AttachmentEntity>>

    /**
     * Get attachment by ID
     */
    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: Long): AttachmentEntity?

    /**
     * Get attachments by type (audio, image, document)
     */
    @Query("SELECT * FROM attachments WHERE type = :type ORDER BY uploadedAt DESC LIMIT :limit")
    suspend fun getAttachmentsByType(
        type: String,
        limit: Int = 50,
    ): List<AttachmentEntity>

    /**
     * Get all audio attachments
     */
    @Query("SELECT * FROM attachments WHERE type = 'audio' ORDER BY uploadedAt DESC")
    fun getAllAudioAttachments(): Flow<List<AttachmentEntity>>

    /**
     * Get all image attachments
     */
    @Query("SELECT * FROM attachments WHERE type = 'image' ORDER BY uploadedAt DESC")
    fun getAllImageAttachments(): Flow<List<AttachmentEntity>>

    /**
     * Get all document attachments
     */
    @Query("SELECT * FROM attachments WHERE type = 'document' ORDER BY uploadedAt DESC")
    fun getAllDocumentAttachments(): Flow<List<AttachmentEntity>>

    /**
     * Get audio attachments with transcription
     */
    @Query(
        """
        SELECT * FROM attachments 
        WHERE type = 'audio' 
        AND transcription IS NOT NULL 
        ORDER BY uploadedAt DESC
    """,
    )
    fun getTranscribedAudioAttachments(): Flow<List<AttachmentEntity>>

    /**
     * Get audio attachments without transcription
     */
    @Query(
        """
        SELECT * FROM attachments 
        WHERE type = 'audio' 
        AND transcription IS NULL 
        ORDER BY uploadedAt DESC
    """,
    )
    fun getUntranscribedAudioAttachments(): Flow<List<AttachmentEntity>>

    /**
     * Get attachment by file path
     */
    @Query("SELECT * FROM attachments WHERE filePath = :filePath LIMIT 1")
    suspend fun getAttachmentByPath(filePath: String): AttachmentEntity?

    /**
     * Get recent attachments
     */
    @Query("SELECT * FROM attachments ORDER BY uploadedAt DESC LIMIT :limit")
    suspend fun getRecentAttachments(limit: Int = 20): List<AttachmentEntity>

    /**
     * Get attachments by file name pattern
     */
    @Query(
        """
        SELECT * FROM attachments 
        WHERE fileName LIKE '%' || :pattern || '%' 
        ORDER BY uploadedAt DESC
    """,
    )
    fun searchAttachmentsByName(pattern: String): Flow<List<AttachmentEntity>>

    /**
     * Count attachments by type
     */
    @Query("SELECT COUNT(*) FROM attachments WHERE type = :type")
    suspend fun countAttachmentsByType(type: String): Int

    /**
     * Get total attachments size
     */
    @Query("SELECT SUM(fileSize) FROM attachments")
    suspend fun getTotalAttachmentsSize(): Long?

    /**
     * Get total attachments size by type
     */
    @Query("SELECT SUM(fileSize) FROM attachments WHERE type = :type")
    suspend fun getTotalSizeByType(type: String): Long?

    /**
     * Count all attachments
     */
    @Query("SELECT COUNT(*) FROM attachments")
    suspend fun getAttachmentCount(): Int

    // ===== INSERT OPERATIONS =====

    /**
     * Insert attachment
     * Returns the new attachment ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    /**
     * Insert multiple attachments
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>): List<Long>

    // ===== UPDATE OPERATIONS =====

    /**
     * Update attachment
     */
    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    /**
     * Update transcription for audio
     */
    @Query("UPDATE attachments SET transcription = :transcription WHERE id = :id")
    suspend fun updateTranscription(
        id: Long,
        transcription: String,
    )

    /**
     * Update file path
     */
    @Query("UPDATE attachments SET filePath = :filePath WHERE id = :id")
    suspend fun updateFilePath(
        id: Long,
        filePath: String,
    )

    // ===== DELETE OPERATIONS =====

    /**
     * Delete attachment
     */
    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    /**
     * Delete attachment by ID
     */
    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: Long)

    /**
     * Delete all attachments for a message
     */
    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsByMessage(messageId: Long)

    /**
     * Delete attachments by type
     */
    @Query("DELETE FROM attachments WHERE type = :type")
    suspend fun deleteAttachmentsByType(type: String)

    /**
     * Delete attachments by file path
     */
    @Query("DELETE FROM attachments WHERE filePath = :filePath")
    suspend fun deleteAttachmentByPath(filePath: String)

    /**
     * Delete old attachments
     */
    @Query("DELETE FROM attachments WHERE uploadedAt < :timestamp")
    suspend fun deleteOldAttachments(timestamp: Long)

    /**
     * Delete all attachments
     */
    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}
