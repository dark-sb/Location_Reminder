package com.example.location_reminder.data.repository

import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    // Reminder operations
    fun getAllReminders(): Flow<List<ReminderEntity>>
    fun getAllRemindersWithTasks(): Flow<List<ReminderWithTasks>>
    fun getActiveRemindersWithTasks(): Flow<List<ReminderWithTasks>>
    fun getCompletedRemindersWithTasks(): Flow<List<ReminderWithTasks>>
    fun getReminderById(id: Long): Flow<ReminderEntity?>
    fun getReminderWithTasksById(id: Long): Flow<ReminderWithTasks?>
    suspend fun getReminderByIdOnce(id: Long): ReminderEntity?
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    suspend fun getActiveRemindersOnce(): List<ReminderEntity>
    suspend fun insertReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun deleteReminder(reminder: ReminderEntity)
    suspend fun deleteReminderById(id: Long)
    suspend fun setReminderEnabled(id: Long, isEnabled: Boolean)
    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean)
    suspend fun setReminderSnoozedUntil(id: Long, snoozedUntil: Long?)
    suspend fun setReminderGeofenceState(id: Long, geofenceState: String)

    // Task operations
    fun getTasksForReminder(reminderId: Long): Flow<List<TaskEntity>>
    suspend fun insertTask(task: TaskEntity): Long
    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
    suspend fun deleteTaskById(id: Long)
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean)

    // Combined operations
    suspend fun saveReminderWithTasks(reminder: ReminderEntity, tasks: List<TaskEntity>): Long
    suspend fun updateReminderWithTasks(reminder: ReminderEntity, tasks: List<TaskEntity>)

    // History operations
    fun getRecentLocations(limit: Int = 20): Flow<List<com.example.location_reminder.data.local.entity.LocationHistoryEntity>>
    suspend fun recordLocation(name: String, latitude: Double, longitude: Double, address: String = "")
    fun getRecentTitles(limit: Int = 5): Flow<List<com.example.location_reminder.data.local.entity.TitleHistoryEntity>>
    suspend fun recordTitle(title: String)
    fun getRecentEvents(limit: Int = 5): Flow<List<com.example.location_reminder.data.local.entity.EventHistoryEntity>>
    suspend fun recordEvent(name: String)
    suspend fun clearCompletedHistory()

    // Daily Tasks operations
    fun getAllDailyTasks(): Flow<List<com.example.location_reminder.data.local.entity.DailyTaskEntity>>
    fun getDailyTasksWithCompletion(date: String): Flow<List<com.example.location_reminder.data.local.entity.DailyTaskWithCompletion>>
    fun getDailyTaskCompletionsForDates(dates: List<String>): Flow<List<com.example.location_reminder.data.local.entity.DailyTaskCompletionEntity>>
    suspend fun insertDailyTask(title: String): Long
    suspend fun updateDailyTask(task: com.example.location_reminder.data.local.entity.DailyTaskEntity)
    suspend fun deleteDailyTask(taskId: Long)
    suspend fun toggleDailyTaskCompletion(taskId: Long, date: String, isCompleted: Boolean)
    suspend fun updateDailyTaskCompletionsForDate(date: String, updates: Map<Long, Boolean>)
}