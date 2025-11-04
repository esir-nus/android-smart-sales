package com.smartsales.di

import android.content.Context
import com.smartsales.data.bluetooth.BleManager
import com.smartsales.data.local.dao.*
import com.smartsales.data.repository.ConversationRepository
import com.smartsales.data.repository.DeviceRepository
import com.smartsales.data.repository.ExportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        attachmentDao: AttachmentDao
    ): ConversationRepository {
        return ConversationRepository(conversationDao, messageDao, attachmentDao)
    }
    
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceSettingDao: DeviceSettingDao,
        wifiConfigDao: WifiConfigDao
    ): DeviceRepository {
        return DeviceRepository(deviceSettingDao, wifiConfigDao)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(
        crmExportDao: CrmExportDao
    ): ExportRepository {
        return ExportRepository(crmExportDao)
    }
    
    @Provides
    @Singleton
    fun provideBleManager(
        @ApplicationContext context: Context
    ): BleManager {
        return BleManager(context)
    }
}
