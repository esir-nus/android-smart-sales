package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isAnalyzed: Boolean = false
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val attachments: String? = null,
    val metadata: String? = null
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val type: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: Long,
    val transcription: String? = null
)

@Entity(tableName = "wifi_configs")
data class WifiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val password: String,
    val isDefault: Boolean = false,
    val lastUsed: Long,
    val addedAt: Long
)

@Entity(tableName = "device_settings")
data class DeviceSettingEntity(
    @PrimaryKey
    val deviceId: String,
    val deviceName: String,
    val lastConnected: Long,
    val currentImage: String? = null,
    val currentText: String? = null,
    val isPaired: Boolean = true
)

@Entity(tableName = "crm_exports")
data class CrmExportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val exportType: String,
    val filePath: String,
    val exportedAt: Long,
    val status: String
)
