package com.otaku.manayeou.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val seriesId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val sourceUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)