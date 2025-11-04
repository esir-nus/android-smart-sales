package com.smartsales.data.repository

import com.smartsales.data.local.dao.CrmExportDao
import com.smartsales.data.local.entity.CrmExportEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val crmExportDao: CrmExportDao
) {
    
    /**
     * Record a new export operation
     */
    suspend fun recordExport(
        conversationId: Long,
        exportType: String,
        filePath: String,
        status: String
    ): Long {
        return crmExportDao.insertExport(
            CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = filePath,
                exportedAt = System.currentTimeMillis(),
                status = status
            )
        )
    }
    
    /**
     * Get recent exports
     */
    suspend fun getRecentExports(limit: Int = 50): List<CrmExportEntity> {
        return crmExportDao.getRecentExports(limit)
    }
    
    /**
     * Get all exports for a specific conversation
     */
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity> {
        return crmExportDao.getExportsByConversation(conversationId)
    }
    
    /**
     * Get successful exports only
     */
    suspend fun getSuccessfulExports(limit: Int = 50): List<CrmExportEntity> {
        return crmExportDao.getRecentExports(limit).filter { 
            it.status == "success" 
        }
    }
    
    /**
     * Delete an export record
     */
    suspend fun deleteExport(export: CrmExportEntity) {
        crmExportDao.deleteExport(export)
    }
}
