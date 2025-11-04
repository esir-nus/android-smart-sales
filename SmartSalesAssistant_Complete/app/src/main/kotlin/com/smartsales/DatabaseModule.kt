package com.smartsales.di

import android.content.Context
import androidx.room.Room
import com.smartsales.data.local.dao.*
import com.smartsales.data.local.database.SmartSalesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSmartSalesDatabase(
        @ApplicationContext context: Context
    ): SmartSalesDatabase {
        return Room.databaseBuilder(
            context,
            SmartSalesDatabase::class.java,
            "smart_sales_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideConversationDao(database: SmartSalesDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    fun provideMessageDao(database: SmartSalesDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    fun provideAttachmentDao(database: SmartSalesDatabase): AttachmentDao {
        return database.attachmentDao()
    }
    
    @Provides
    fun provideWifiConfigDao(database: SmartSalesDatabase): WifiConfigDao {
        return database.wifiConfigDao()
    }
    
    @Provides
    fun provideDeviceSettingDao(database: SmartSalesDatabase): DeviceSettingDao {
        return database.deviceSettingDao()
    }
    
    @Provides
    fun provideCrmExportDao(database: SmartSalesDatabase): CrmExportDao {
        return database.crmExportDao()
    }
}
