package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?
    
    @Query("""
        SELECT * FROM conversations 
        WHERE createdAt >= :startTime AND createdAt < :endTime 
        ORDER BY createdAt DESC
    """)
    fun getConversationsByTimeRange(startTime: Long, endTime: Long): Flow<List<ConversationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)
    
    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
    
    @Query("UPDATE conversations SET isAnalyzed = :analyzed WHERE id = :id")
    suspend fun markAsAnalyzed(id: Long, analyzed: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)
    
    @Query("""
        SELECT m.* FROM messages m 
        INNER JOIN conversations c ON m.conversationId = c.id 
        WHERE m.attachments IS NOT NULL 
        ORDER BY m.timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessagesWithAttachments(limit: Int = 20): List<MessageEntity>
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessage(messageId: Long): List<AttachmentEntity>
    
    @Query("SELECT * FROM attachments WHERE type = :type ORDER BY uploadedAt DESC LIMIT :limit")
    suspend fun getAttachmentsByType(type: String, limit: Int = 50): List<AttachmentEntity>
    
    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity): Long
    
    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)
    
    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsByMessage(messageId: Long)
}

@Dao
interface WifiConfigDao {
    @Query("SELECT * FROM wifi_configs ORDER BY lastUsed DESC")
    fun getAllWifiConfigs(): Flow<List<WifiConfigEntity>>
    
    @Query("SELECT * FROM wifi_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWifiConfig(): WifiConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiConfig(config: WifiConfigEntity): Long
    
    @Update
    suspend fun updateWifiConfig(config: WifiConfigEntity)
    
    @Delete
    suspend fun deleteWifiConfig(config: WifiConfigEntity)
    
    @Query("UPDATE wifi_configs SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE wifi_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)
}

@Dao
interface DeviceSettingDao {
    @Query("SELECT * FROM device_settings WHERE isPaired = 1 LIMIT 1")
    suspend fun getCurrentDevice(): DeviceSettingEntity?
    
    @Query("SELECT * FROM device_settings ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<DeviceSettingEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceSettingEntity)
    
    @Update
    suspend fun updateDevice(device: DeviceSettingEntity)
    
    @Query("UPDATE device_settings SET lastConnected = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastConnected(deviceId: String, timestamp: Long)
    
    @Query("UPDATE device_settings SET currentImage = :imageName WHERE deviceId = :deviceId")
    suspend fun updateCurrentImage(deviceId: String, imageName: String?)
    
    @Query("UPDATE device_settings SET currentText = :text WHERE deviceId = :deviceId")
    suspend fun updateCurrentText(deviceId: String, text: String?)
}

@Dao
interface CrmExportDao {
    @Query("SELECT * FROM crm_exports ORDER BY exportedAt DESC LIMIT :limit")
    suspend fun getRecentExports(limit: Int = 50): List<CrmExportEntity>
    
    @Query("SELECT * FROM crm_exports WHERE conversationId = :conversationId")
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity>
    
    @Insert
    suspend fun insertExport(export: CrmExportEntity): Long
    
    @Delete
    suspend fun deleteExport(export: CrmExportEntity)
}
