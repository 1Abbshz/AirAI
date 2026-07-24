package com.zen.airai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zen.airai.data.db.dao.ChatDao
import com.zen.airai.data.db.dao.MessageDao
import com.zen.airai.data.db.entity.ChatEntity
import com.zen.airai.data.db.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "airai.db"
            ).build()
        }
    }
}
