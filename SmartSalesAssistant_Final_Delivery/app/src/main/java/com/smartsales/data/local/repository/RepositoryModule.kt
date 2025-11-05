package com.smartsales.data.local.repository

import com.smartsales.data.local.dao.ConversationDao
import com.smartsales.data.local.dao.CrmExportDao
import com.smartsales.data.local.dao.DeviceSettingDao
import com.smartsales.data.local.dao.MessageDao
import com.smartsales.data.local.dao.WifiConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for repository implementations.
 * Keeps DI wiring centralised and easy to discover.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao
    ): ConversationRepository = ConversationRepository(
        conversationDao = conversationDao,
        messageDao = messageDao
    )

    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceSettingDao: DeviceSettingDao,
        wifiConfigDao: WifiConfigDao
    ): DeviceRepository = DeviceRepository(
        deviceSettingDao = deviceSettingDao,
        wifiConfigDao = wifiConfigDao
    )

    @Provides
    @Singleton
    fun provideExportRepository(
        crmExportDao: CrmExportDao
    ): ExportRepository = ExportRepository(
        crmExportDao = crmExportDao
    )
}
