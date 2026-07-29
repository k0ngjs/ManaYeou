package com.otaku.manayeou.data.local.dao

import androidx.room.*
import com.otaku.manayeou.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE seriesId = :seriesId)")
    fun isBookmarked(seriesId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE seriesId = :seriesId")
    suspend fun delete(seriesId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}
