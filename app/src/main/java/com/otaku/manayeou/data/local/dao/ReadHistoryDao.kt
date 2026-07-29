package com.otaku.manayeou.data.local.dao

import androidx.room.*
import com.otaku.manayeou.data.local.entity.ReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadHistoryDao {
    @Query("SELECT * FROM read_history ORDER BY readAt DESC")
    fun getAll(): Flow<List<ReadHistoryEntity>>

    @Query("SELECT * FROM read_history ORDER BY readAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<ReadHistoryEntity>>

    @Query("SELECT * FROM read_history WHERE chapterUrl = :url LIMIT 1")
    suspend fun getByUrl(url: String): ReadHistoryEntity?

    @Query("SELECT * FROM read_history WHERE seriesId = :seriesId ORDER BY readAt DESC LIMIT 1")
    fun getLatestForSeries(seriesId: String): Flow<ReadHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadHistoryEntity)

    @Query("DELETE FROM read_history")
    suspend fun clearAll()

    @Query("DELETE FROM read_history WHERE seriesId = :seriesId")
    suspend fun deleteBySeriesId(seriesId: String)
}
