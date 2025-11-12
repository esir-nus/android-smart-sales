package com.smartsales.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsales.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * Hilt Module for Database Dependencies
 *
 * Provides:
 * - SmartSalesDatabase instance (singleton)
 * - All DAO instances
 * - Database configuration
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // ===== DATABASE PROVIDER =====

    /**
     * Provides the main database instance
     * Configured with:
     * - Write-Ahead Logging for better performance
     * - Multi-threaded query executor
     * - Fallback to destructive migration (dev mode)
     * - Database callbacks
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SmartSalesDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SmartSalesDatabase::class.java,
            SmartSalesDatabase.DATABASE_NAME,
        )
            // Performance optimizations
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryExecutor(Executors.newFixedThreadPool(4))
            // Migration strategy
            .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
            // Callbacks
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created - can add initial data here
                    }
                },
            )
            .build()
    }

    // ===== DAO PROVIDERS =====

    /**
     * Provides ConversationDao
     * Used for conversation and message operations
     */
    @Provides
    @Singleton
    fun provideConversationDao(database: SmartSalesDatabase): ConversationDao {
        return database.conversationDao()
    }

    /**
     * Provides MessageDao
     * Used for message CRUD operations
     */
    @Provides
    @Singleton
    fun provideMessageDao(database: SmartSalesDatabase): MessageDao {
        return database.messageDao()
    }

    /**
     * Provides AttachmentDao
     * Used for file attachment management
     */
    @Provides
    @Singleton
    fun provideAttachmentDao(database: SmartSalesDatabase): AttachmentDao {
        return database.attachmentDao()
    }

    /**
     * Provides WifiConfigDao
     * Used for WiFi configuration storage
     */
    @Provides
    @Singleton
    fun provideWifiConfigDao(database: SmartSalesDatabase): WifiConfigDao {
        return database.wifiConfigDao()
    }

    /**
     * Provides DeviceSettingDao
     * Used for BLE device settings
     */
    @Provides
    @Singleton
    fun provideDeviceSettingDao(database: SmartSalesDatabase): DeviceSettingDao {
        return database.deviceSettingDao()
    }

    /**
     * Provides CrmExportDao
     * Used for export history tracking
     */
    @Provides
    @Singleton
    fun provideCrmExportDao(database: SmartSalesDatabase): CrmExportDao {
        return database.crmExportDao()
    }
}
