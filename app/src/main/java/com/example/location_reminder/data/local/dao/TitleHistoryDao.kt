package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleHistoryDao {

    @Query("SELECT * FROM title_history ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentTitles(limit: Int = 5): Flow<List<TitleHistoryEntity>>

    @Query("SELECT * FROM title_history ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecentTitlesOnce(limit: Int = 5): List<TitleHistoryEntity>

    @Query("SELECT * FROM title_history WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)) LIMIT 1")
    suspend fun findByTitle(title: String): TitleHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(title: TitleHistoryEntity): Long

    @Query("DELETE FROM title_history WHERE id NOT IN (SELECT id FROM (SELECT id FROM title_history ORDER BY lastUsedAt DESC LIMIT 20))")
    suspend fun trimOldHistory()
}
