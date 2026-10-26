package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Transaction
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllRemindersWithTasks(): Flow<List<ReminderWithTasks>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderById(id: Long): Flow<ReminderEntity?>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderWithTasksById(id: Long): Flow<ReminderWithTasks?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderByIdOnce(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0")
    suspend fun getActiveRemindersOnce(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateReminderEnabled(id: Long, isEnabled: Boolean)

    @Transaction
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveRemindersWithTasks(): Flow<List<ReminderWithTasks>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY COALESCE(completedAt, createdAt) DESC")
    fun getCompletedRemindersWithTasks(): Flow<List<ReminderWithTasks>>

    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateReminderCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateReminderCompletedWithTimestamp(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE reminders SET snoozedUntil = :snoozedUntil WHERE id = :id")
    suspend fun updateReminderSnoozedUntil(id: Long, snoozedUntil: Long?)

    @Query("UPDATE reminders SET geofenceState = :geofenceState WHERE id = :id")
    suspend fun updateReminderGeofenceState(id: Long, geofenceState: String)

    @Query("DELETE FROM tasks WHERE reminderId IN (SELECT id FROM reminders WHERE isCompleted = 1)")
    suspend fun deleteTasksForCompletedReminders(): Int

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun deleteCompletedRemindersOnly(): Int

    @Transaction
    suspend fun clearCompletedHistory() {
        deleteTasksForCompletedReminders()
        deleteCompletedRemindersOnly()
    }
}