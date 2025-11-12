package com.smartsales.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AiSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AiSessionDatabase : RoomDatabase() {
    abstract fun aiSessionDao(): AiSessionDao

    companion object {
        const val DATABASE_NAME = "ai_sessions.db"
    }
}
