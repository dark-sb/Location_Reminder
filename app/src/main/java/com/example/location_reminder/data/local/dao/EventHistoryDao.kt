package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.location_reminder.data.local.entity.EventHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventHistoryDao {

    @Query("SELECT * FROM event_history ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 5): Flow<List<EventHistoryEntity>>

    @Query("SELECT * FROM event_history ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecentEventsOnce(limit: Int = 5): List<EventHistoryEntity>

    @Query("SELECT * FROM event_history WHERE LOWER(TRIM(eventName)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByName(name: String): EventHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(event: EventHistoryEntity): Long

    @Query("DELETE FROM event_history WHERE id NOT IN (SELECT id FROM (SELECT id FROM event_history ORDER BY lastUsedAt DESC LIMIT 10))")
    suspend fun trimOldHistory()
}
