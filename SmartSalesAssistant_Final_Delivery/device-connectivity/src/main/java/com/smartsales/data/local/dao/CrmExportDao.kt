package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.CrmExportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmExportDao {
    // ===== QUERY OPERATIONS =====

    /**
     * Get all export records
     * Ordered by export date (newest first)
     */
    @Query("SELECT * FROM crm_exports ORDER BY exportedAt DESC")
    fun getAllExports(): Flow<List<CrmExportEntity>>

    /**
     * Get export by ID
     */
    @Query("SELECT * FROM crm_exports WHERE id = :id")
    suspend fun getExportById(id: Long): CrmExportEntity?

    /**
     * Get all exports for a conversation
     */
    @Query("SELECT * FROM crm_exports WHERE conversationId = :conversationId ORDER BY exportedAt DESC")
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity>

    /**
     * Get exports by conversation as Flow
     */
    @Query("SELECT * FROM crm_exports WHERE conversationId = :conversationId ORDER BY exportedAt DESC")
    fun getExportsByConversationFlow(conversationId: Long): Flow<List<CrmExportEntity>>

    /**
     * Get exports by type (csv or pdf)
     */
    @Query("SELECT * FROM crm_exports WHERE exportType = :type ORDER BY exportedAt DESC")
    fun getExportsByType(type: String): Flow<List<CrmExportEntity>>

    /**
     * Get successful exports only
     */
    @Query("SELECT * FROM crm_exports WHERE status = 'success' ORDER BY exportedAt DESC")
    fun getSuccessfulExports(): Flow<List<CrmExportEntity>>

    /**
     * Get failed exports only
     */
    @Query("SELECT * FROM crm_exports WHERE status = 'failed' ORDER BY exportedAt DESC")
    fun getFailedExports(): Flow<List<CrmExportEntity>>

    /**
     * Get latest export for a conversation
     */
    @Query(
        """
        SELECT * FROM crm_exports 
        WHERE conversationId = :conversationId 
        ORDER BY exportedAt DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestExport(conversationId: Long): CrmExportEntity?

    /**
     * Get latest successful export for a conversation
     */
    @Query(
        """
        SELECT * FROM crm_exports 
        WHERE conversationId = :conversationId 
        AND status = 'success' 
        ORDER BY exportedAt DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestSuccessfulExport(conversationId: Long): CrmExportEntity?

    /**
     * Get exports within time range
     */
    @Query(
        """
        SELECT * FROM crm_exports 
        WHERE exportedAt >= :startTime 
        AND exportedAt < :endTime 
        ORDER BY exportedAt DESC
    """,
    )
    fun getExportsByTimeRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<CrmExportEntity>>

    /**
     * Get recent exports
     */
    @Query("SELECT * FROM crm_exports ORDER BY exportedAt DESC LIMIT :limit")
    fun getRecentExports(limit: Int = 20): Flow<List<CrmExportEntity>>

    /**
     * Check if conversation has been exported
     */
    @Query("SELECT COUNT(*) FROM crm_exports WHERE conversationId = :conversationId")
    suspend fun hasBeenExported(conversationId: Long): Int

    /**
     * Check if conversation has successful export
     */
    @Query(
        """
        SELECT COUNT(*) FROM crm_exports 
        WHERE conversationId = :conversationId 
        AND status = 'success'
    """,
    )
    suspend fun hasSuccessfulExport(conversationId: Long): Int

    /**
     * Count total exports
     */
    @Query("SELECT COUNT(*) FROM crm_exports")
    suspend fun getExportCount(): Int

    /**
     * Count successful exports
     */
    @Query("SELECT COUNT(*) FROM crm_exports WHERE status = 'success'")
    suspend fun getSuccessfulExportCount(): Int

    /**
     * Count failed exports
     */
    @Query("SELECT COUNT(*) FROM crm_exports WHERE status = 'failed'")
    suspend fun getFailedExportCount(): Int

    /**
     * Count exports by type
     */
    @Query("SELECT COUNT(*) FROM crm_exports WHERE exportType = :type")
    suspend fun getExportCountByType(type: String): Int

    // ===== INSERT OPERATIONS =====

    /**
     * Insert export record
     * Returns the new export ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: CrmExportEntity): Long

    /**
     * Insert multiple exports
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExports(exports: List<CrmExportEntity>): List<Long>

    // ===== UPDATE OPERATIONS =====

    /**
     * Update export record
     */
    @Update
    suspend fun updateExport(export: CrmExportEntity)

    /**
     * Update export status
     */
    @Query("UPDATE crm_exports SET status = :status WHERE id = :id")
    suspend fun updateExportStatus(
        id: Long,
        status: String,
    )

    /**
     * Update export file path
     */
    @Query("UPDATE crm_exports SET filePath = :filePath WHERE id = :id")
    suspend fun updateFilePath(
        id: Long,
        filePath: String,
    )

    // ===== DELETE OPERATIONS =====

    /**
     * Delete export record
     */
    @Delete
    suspend fun deleteExport(export: CrmExportEntity)

    /**
     * Delete export by ID
     */
    @Query("DELETE FROM crm_exports WHERE id = :id")
    suspend fun deleteExportById(id: Long)

    /**
     * Delete all exports for a conversation
     */
    @Query("DELETE FROM crm_exports WHERE conversationId = :conversationId")
    suspend fun deleteExportsByConversation(conversationId: Long)

    /**
     * Delete failed exports
     */
    @Query("DELETE FROM crm_exports WHERE status = 'failed'")
    suspend fun deleteFailedExports()

    /**
     * Delete exports by type
     */
    @Query("DELETE FROM crm_exports WHERE exportType = :type")
    suspend fun deleteExportsByType(type: String)

    /**
     * Delete old exports
     */
    @Query("DELETE FROM crm_exports WHERE exportedAt < :timestamp")
    suspend fun deleteOldExports(timestamp: Long)

    /**
     * Delete all exports
     */
    @Query("DELETE FROM crm_exports")
    suspend fun deleteAllExports()
}
