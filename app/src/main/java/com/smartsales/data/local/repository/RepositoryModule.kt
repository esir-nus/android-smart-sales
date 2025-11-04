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
 * Hilt module for providing Repository instances
 *
 * This module tells Hilt how to create and provide repositories
 * to other parts of the app that need them.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides ConversationRepository
     * Manages conversations and messages
     */
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao
    ): ConversationRepository {
        return ConversationRepository(
            conversationDao = conversationDao,
            messageDao = messageDao
        )
    }

    /**
     * Provides DeviceRepository
     * Manages BLE devices and WiFi configurations
     */
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceSettingDao: DeviceSettingDao,
        wifiConfigDao: WifiConfigDao
    ): DeviceRepository {
        return DeviceRepository(
            deviceSettingDao = deviceSettingDao,
            wifiConfigDao = wifiConfigDao
        )
    }

    /**
     * Provides ExportRepository
     * Tracks export records (PDF/CSV)
     */
    @Provides
    @Singleton
    fun provideExportRepository(
        crmExportDao: CrmExportDao
    ): ExportRepository {
        return ExportRepository(
            crmExportDao = crmExportDao
        )
    }
}
```

---

## ✅ Quick Verification Checklist

After creating these files:

1. **Check imports** - Android Studio should auto-import most dependencies
2. **Sync Gradle** - File → Sync Project with Gradle Files
3. **Build project** - Build → Make Project

### Expected Result:
```
✅ Build successful
✅ No compilation errors
✅ Repositories ready to use in ViewModels