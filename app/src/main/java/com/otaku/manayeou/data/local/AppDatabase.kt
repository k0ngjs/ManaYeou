package com.otaku.manayeou.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.otaku.manayeou.data.local.dao.BookmarkDao
import com.otaku.manayeou.data.local.dao.ReadHistoryDao
import com.otaku.manayeou.data.local.entity.BookmarkEntity
import com.otaku.manayeou.data.local.entity.ReadHistoryEntity

@Database(
    entities = [BookmarkEntity::class, ReadHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readHistoryDao(): ReadHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manayeou.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
