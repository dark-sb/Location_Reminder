package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationHistoryDao {

    @Query("SELECT * FROM location_history ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentLocations(limit: Int = 20): Flow<List<LocationHistoryEntity>>

    @Query("SELECT * FROM location_history ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecentLocationsOnce(limit: Int = 20): List<LocationHistoryEntity>

    @Query("SELECT * FROM location_history WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByName(name: String): LocationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(location: LocationHistoryEntity): Long

    @Query("DELETE FROM location_history WHERE id NOT IN (SELECT id FROM (SELECT id FROM location_history ORDER BY lastUsedAt DESC LIMIT 20))")
    suspend fun trimOldHistory()
}
