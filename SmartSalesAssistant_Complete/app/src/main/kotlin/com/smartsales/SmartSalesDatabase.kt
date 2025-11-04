package com.smartsales.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartsales.data.local.dao.*
import com.smartsales.data.local.entity.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        WifiConfigEntity::class,
        DeviceSettingEntity::class,
        CrmExportEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SmartSalesDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun wifiConfigDao(): WifiConfigDao
    abstract fun deviceSettingDao(): DeviceSettingDao
    abstract fun crmExportDao(): CrmExportDao
}
