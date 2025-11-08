package com.smartsales.data.local.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsales.data.local.dao.*
import com.smartsales.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Main Room Database for Smart Sales Assistant
 *
 * Features:
 * - 6 entities with relationships
 * - Type converters for complex data
 * - Database callbacks for initialization
 * - Migration strategies
 * - Export schema for version control
 */
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
    exportSchema = false,
    autoMigrations = []
)
@TypeConverters(Converters::class)
abstract class SmartSalesDatabase : RoomDatabase() {

    // ===== DAO ABSTRACT METHODS =====

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun wifiConfigDao(): WifiConfigDao
    abstract fun deviceSettingDao(): DeviceSettingDao
    abstract fun crmExportDao(): CrmExportDao

    // ===== COMPANION OBJECT =====

    companion object {
        const val DATABASE_NAME = "smart_sales.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: SmartSalesDatabase? = null

        /**
         * Get database instance (singleton pattern)
         * Use this only if not using Hilt DI
         */
        fun getInstance(context: Context): SmartSalesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Build database with configuration
         */
        private fun buildDatabase(context: Context): SmartSalesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmartSalesDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback(context))
                .fallbackToDestructiveMigration() // Use with caution in production
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // Better performance
                .setQueryExecutor(Executors.newFixedThreadPool(4)) // Parallel queries
                .build()
        }

        /**
         * Clear all data (for testing or reset)
         */
        suspend fun clearAllData(database: SmartSalesDatabase) {
            database.clearAllTables()
        }
    }

    // ===== DATABASE CALLBACK =====

    /**
     * Database creation and open callbacks
     */
    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Database created for the first time
            CoroutineScope(Dispatchers.IO).launch {
                // Optional: Prepopulate database with default data
                prepopulateDatabase()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Database opened
            // Optional: Check database integrity
        }

        private fun prepopulateDatabase() {
            // Add any default data here
            // Example: Default WiFi config, sample conversations, etc.
        }
    }
}

/**
 * Type Converters for complex data types
 */
class Converters {

    /**
     * Convert List<String> to String (JSON)
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * Convert String to List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }

    /**
     * Convert Map<String, String> to String (JSON-like)
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.entries?.joinToString(";") { "${it.key}:${it.value}" }
    }

    /**
     * Convert String to Map<String, String>
     */
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.split(";")
            ?.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            ?.toMap()
    }
}
