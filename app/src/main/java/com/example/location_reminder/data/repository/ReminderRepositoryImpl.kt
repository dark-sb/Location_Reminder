package com.example.location_reminder.data.repository

import com.example.location_reminder.data.local.dao.DailyTaskDao
import com.example.location_reminder.data.local.dao.EventHistoryDao
import com.example.location_reminder.data.local.dao.LocationHistoryDao
import com.example.location_reminder.data.local.dao.ReminderDao
import com.example.location_reminder.data.local.dao.TaskDao
import com.example.location_reminder.data.local.dao.TitleHistoryDao
import com.example.location_reminder.data.local.entity.DailyTaskCompletionEntity
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import com.example.location_reminder.data.local.entity.EventHistoryEntity
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao,
    private val locationHistoryDao: LocationHistoryDao? = null,
    private val titleHistoryDao: TitleHistoryDao? = null,
    private val eventHistoryDao: EventHistoryDao? = null,
    private val dailyTaskDao: DailyTaskDao? = null
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<ReminderEntity>> {
        return reminderDao.getAllReminders()
    }

    override fun getAllRemindersWithTasks(): Flow<List<ReminderWithTasks>> {
        return reminderDao.getAllRemindersWithTasks()
    }

    override fun getActiveRemindersWithTasks(): Flow<List<ReminderWithTasks>> {
        return reminderDao.getActiveRemindersWithTasks()
    }

    override fun getCompletedRemindersWithTasks(): Flow<List<ReminderWithTasks>> {
        return reminderDao.getCompletedRemindersWithTasks()
    }

    override fun getReminderById(id: Long): Flow<ReminderEntity?> {
        return reminderDao.getReminderById(id)
    }

    override fun getReminderWithTasksById(id: Long): Flow<ReminderWithTasks?> {
        return reminderDao.getReminderWithTasksById(id)
    }

    override fun getTasksForReminder(reminderId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksForReminder(reminderId)
    }

    override suspend fun getReminderByIdOnce(id: Long): ReminderEntity? {
        return reminderDao.getReminderByIdOnce(id)
    }

    override fun getActiveReminders(): Flow<List<ReminderEntity>> {
        return reminderDao.getActiveReminders()
    }

    override suspend fun getActiveRemindersOnce(): List<ReminderEntity> {
        return reminderDao.getActiveRemindersOnce()
    }

    override suspend fun insertReminder(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    override suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    override suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
    }

    override suspend fun setReminderEnabled(id: Long, isEnabled: Boolean) {
        reminderDao.updateReminderEnabled(id, isEnabled)
    }

    override suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        reminderDao.updateReminderCompletedWithTimestamp(id, isCompleted, completedAt)
    }

    override suspend fun setReminderSnoozedUntil(id: Long, snoozedUntil: Long?) {
        reminderDao.updateReminderSnoozedUntil(id, snoozedUntil)
    }

    override suspend fun setReminderGeofenceState(id: Long, geofenceState: String) {
        reminderDao.updateReminderGeofenceState(id, geofenceState)
    }

    override suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    override suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompleted(id, isCompleted)
    }

    override suspend fun saveReminderWithTasks(
        reminder: ReminderEntity,
        tasks: List<TaskEntity>
    ): Long {
        val reminderId = reminderDao.insertReminder(reminder)
        val tasksWithReminderId = tasks.map { it.copy(reminderId = reminderId) }
        if (tasksWithReminderId.isNotEmpty()) {
            taskDao.insertTasks(tasksWithReminderId)
        }
        return reminderId
    }

    override suspend fun updateReminderWithTasks(
        reminder: ReminderEntity,
        tasks: List<TaskEntity>
    ) {
        reminderDao.updateReminder(reminder)
        taskDao.deleteTasksForReminder(reminder.id)
        val tasksWithReminderId = tasks.map { it.copy(reminderId = reminder.id) }
        if (tasksWithReminderId.isNotEmpty()) {
            taskDao.insertTasks(tasksWithReminderId)
        }
    }

    override fun getRecentLocations(limit: Int): Flow<List<LocationHistoryEntity>> {
        return locationHistoryDao?.getRecentLocations(limit) ?: flowOf(emptyList())
    }

    override suspend fun recordLocation(name: String, latitude: Double, longitude: Double, address: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.equals("Time Reminder", ignoreCase = true) || locationHistoryDao == null) return
        val existing = locationHistoryDao.findByName(trimmed)
        val entity = LocationHistoryEntity(
            id = existing?.id ?: 0,
            name = trimmed,
            address = address.ifBlank { existing?.address ?: "" },
            latitude = latitude,
            longitude = longitude,
            lastUsedAt = System.currentTimeMillis()
        )
        locationHistoryDao.insertOrUpdate(entity)
        locationHistoryDao.trimOldHistory()
    }

    override fun getRecentTitles(limit: Int): Flow<List<TitleHistoryEntity>> {
        return titleHistoryDao?.getRecentTitles(limit) ?: flowOf(emptyList())
    }

    override suspend fun recordTitle(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank() || titleHistoryDao == null) return
        val existing = titleHistoryDao.findByTitle(trimmed)
        val entity = TitleHistoryEntity(
            id = existing?.id ?: 0,
            title = trimmed,
            lastUsedAt = System.currentTimeMillis()
        )
        titleHistoryDao.insertOrUpdate(entity)
        titleHistoryDao.trimOldHistory()
    }

    override fun getRecentEvents(limit: Int): Flow<List<EventHistoryEntity>> {
        return eventHistoryDao?.getRecentEvents(limit) ?: flowOf(emptyList())
    }

    override suspend fun recordEvent(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || eventHistoryDao == null) return
        val existing = eventHistoryDao.findByName(trimmed)
        val entity = EventHistoryEntity(
            id = existing?.id ?: 0,
            eventName = trimmed,
            lastUsedAt = System.currentTimeMillis()
        )
        eventHistoryDao.insertOrUpdate(entity)
        eventHistoryDao.trimOldHistory()
    }

    override suspend fun clearCompletedHistory() {
        reminderDao.clearCompletedHistory()
    }

    override fun getAllDailyTasks(): Flow<List<DailyTaskEntity>> {
        return dailyTaskDao?.getAllDailyTasks() ?: flowOf(emptyList())
    }

    override fun getDailyTasksWithCompletion(date: String): Flow<List<DailyTaskWithCompletion>> {
        return dailyTaskDao?.getDailyTasksWithCompletion(date) ?: flowOf(emptyList())
    }

    override fun getDailyTaskCompletionsForDates(dates: List<String>): Flow<List<DailyTaskCompletionEntity>> {
        return dailyTaskDao?.getCompletionsForDates(dates) ?: flowOf(emptyList())
    }

    override suspend fun insertDailyTask(title: String): Long {
        val trimmed = title.trim()
        if (trimmed.isBlank() || dailyTaskDao == null) return -1L
        return dailyTaskDao.insertTask(DailyTaskEntity(title = trimmed))
    }

    override suspend fun updateDailyTask(task: DailyTaskEntity) {
        val trimmed = task.title.trim()
        if (trimmed.isBlank() || dailyTaskDao == null) return
        dailyTaskDao.updateTask(task.copy(title = trimmed))
    }

    override suspend fun deleteDailyTask(taskId: Long) {
        dailyTaskDao?.softDeleteTaskById(taskId, System.currentTimeMillis())
    }

    override suspend fun toggleDailyTaskCompletion(taskId: Long, date: String, isCompleted: Boolean) {
        if (dailyTaskDao == null) return
        val existing = dailyTaskDao.getCompletion(taskId, date)
        if (isCompleted) {
            val completion = DailyTaskCompletionEntity(
                id = existing?.id ?: 0L,
                taskId = taskId,
                date = date,
                isCompleted = true,
                completedAt = existing?.completedAt ?: System.currentTimeMillis()
            )
            dailyTaskDao.insertOrUpdateCompletion(completion)
        } else {
            if (existing != null) {
                dailyTaskDao.deleteCompletion(taskId, date)
            }
        }
    }

    override suspend fun updateDailyTaskCompletionsForDate(date: String, updates: Map<Long, Boolean>) {
        if (dailyTaskDao == null) return
        dailyTaskDao.updateCompletionsForDate(date, updates)
    }
}