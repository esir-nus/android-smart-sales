package com.smartsales.data.local.repository

import com.smartsales.data.local.dao.CrmExportDao
import com.smartsales.data.local.entity.CrmExportEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository
    @Inject
    constructor(
        private val crmExportDao: CrmExportDao,
    ) {
        /**
         * Get all export records
         * Sorted by export date (newest first)
         */
        fun getAllExports(): Flow<List<CrmExportEntity>> {
            return crmExportDao.getAllExports()
        }

        /**
         * Get all exports for a specific conversation
         */
        suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity> {
            return crmExportDao.getExportsByConversation(conversationId)
        }

        /**
         * Check if conversation has been exported
         */
        suspend fun hasBeenExported(conversationId: Long): Boolean {
            return crmExportDao.getExportsByConversation(conversationId).isNotEmpty()
        }

        /**
         * Get exports by type (csv or pdf)
         */
        fun getExportsByType(exportType: String): Flow<List<CrmExportEntity>> {
            return crmExportDao.getExportsByType(exportType)
        }

        /**
         * Save export record after successful export
         */
        suspend fun saveExport(
            conversationId: Long,
            exportType: String, // "csv" or "pdf"
            filePath: String,
            status: String = "success",
        ): Result<Long> {
            return try {
                val export =
                    CrmExportEntity(
                        conversationId = conversationId,
                        exportType = exportType,
                        filePath = filePath,
                        exportedAt = System.currentTimeMillis(),
                        status = status,
                    )
                val id = crmExportDao.insertExport(export)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Save failed export record
         */
        suspend fun saveFailedExport(
            conversationId: Long,
            exportType: String,
            errorMessage: String,
        ): Result<Long> {
            return try {
                val export =
                    CrmExportEntity(
                        conversationId = conversationId,
                        exportType = exportType,
                        filePath = errorMessage, // Store error message in filePath
                        exportedAt = System.currentTimeMillis(),
                        status = "failed",
                    )
                val id = crmExportDao.insertExport(export)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete export record
         */
        suspend fun deleteExport(export: CrmExportEntity): Result<Unit> {
            return try {
                crmExportDao.deleteExport(export)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete all exports for a conversation
         */
        suspend fun deleteExportsByConversation(conversationId: Long): Result<Unit> {
            return try {
                val exports = crmExportDao.getExportsByConversation(conversationId)
                exports.forEach { export ->
                    crmExportDao.deleteExport(export)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Get most recent export for a conversation
         */
        suspend fun getLatestExport(conversationId: Long): CrmExportEntity? {
            return crmExportDao.getExportsByConversation(conversationId).firstOrNull()
        }

        /**
         * Count total successful exports
         */
        suspend fun getSuccessfulExportCount(): Int {
            return crmExportDao.getSuccessfulExportCount()
        }
    }
