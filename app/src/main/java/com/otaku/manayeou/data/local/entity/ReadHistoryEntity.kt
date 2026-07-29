package com.otaku.manayeou.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_history")
data class ReadHistoryEntity(
    @PrimaryKey val chapterUrl: String,
    val seriesId: String,
    val seriesUrl: String = "",
    val seriesTitle: String,
    val coverUrl: String,
    val chapterTitle: String,
    val lastPage: Int = 0,
    val totalPages: Int = 0,
    val readAt: Long = System.currentTimeMillis()
)