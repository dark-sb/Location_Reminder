package com.example.location_reminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.geofence.GeofenceManager
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.DailyTaskHistoryDay
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import com.example.location_reminder.data.local.entity.EventHistoryEntity
import com.example.location_reminder.data.local.entity.GeofenceState
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import com.example.location_reminder.data.preferences.AppTheme
import com.example.location_reminder.data.preferences.UserPreferencesRepository
import com.example.location_reminder.data.repository.ReminderRepository
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler? = null,
    private val geofenceManager: GeofenceManager? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    val remindersWithTasks: StateFlow<List<ReminderWithTasks>> = repository.getAllRemindersWithTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeRemindersWithTasks: StateFlow<List<ReminderWithTasks>> = repository.getActiveRemindersWithTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val completedRemindersWithTasks: StateFlow<List<ReminderWithTasks>> = repository.getCompletedRemindersWithTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val reminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentLocations: StateFlow<List<LocationHistoryEntity>> = repository.getRecentLocations(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentTitles: StateFlow<List<TitleHistoryEntity>> = repository.getRecentTitles(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentEvents: StateFlow<List<EventHistoryEntity>> = repository.getRecentEvents(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val appTheme: StateFlow<AppTheme> = userPreferencesRepository?.themeFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.DARK
        ) ?: MutableStateFlow(AppTheme.DARK)

    private val _currentDate = MutableStateFlow(DateTimeFormatterHelper.getTodayIsoDate())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyTasks: StateFlow<List<DailyTaskWithCompletion>> = _currentDate
        .flatMapLatest { date ->
            repository.getDailyTasksWithCompletion(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyTasksHistory: StateFlow<List<DailyTaskHistoryDay>> = _currentDate
        .flatMapLatest { todayIso ->
            val baseDate = DateTimeFormatterHelper.parseIsoDate(todayIso) ?: Date()
            val last30DaysIso = DateTimeFormatterHelper.getLast30DaysIsoDates(baseDate)
            combine(
                repository.getAllDailyTasks(),
                repository.getDailyTaskCompletionsForDates(last30DaysIso)
            ) { allTasks, completions ->
                val completionsByDateAndTask = completions
                    .filter { it.isCompleted }
                    .associateBy { "${it.date}_${it.taskId}" }

                last30DaysIso.mapIndexed { daysAgo, dateIso ->
                    val dateObj = DateTimeFormatterHelper.parseIsoDate(dateIso) ?: Date()
                    val header = DateTimeFormatterHelper.formatHistoryHeader(dateObj, daysAgo)
                    val displayDate = DateTimeFormatterHelper.formatDisplayDate(dateObj)
                    val dayStartMillis = DateTimeFormatterHelper.getDayStartMillis(dateIso) ?: 0L
                    val dayEndMillis = DateTimeFormatterHelper.getDayEndMillis(dateIso) ?: Long.MAX_VALUE

                    val tasksForDay = allTasks.filter { task ->
                        val hasCompletionRecord = completionsByDateAndTask.containsKey("${dateIso}_${task.id}")
                        val existedBeforeDayEnded = task.createdAt <= dayEndMillis || hasCompletionRecord
                        if (!existedBeforeDayEnded) return@filter false

                        if (task.isDeleted) {
                            val deletedAfterDayStarted = task.deletedAt == null || task.deletedAt >= dayStartMillis
                            hasCompletionRecord || deletedAfterDayStarted
                        } else {
                            true
                        }
                    }.map { task ->
                        val completion = completionsByDateAndTask["${dateIso}_${task.id}"]
                        DailyTaskWithCompletion(
                            taskId = task.id,
                            title = task.title,
                            createdAt = task.createdAt,
                            isCompleted = completion != null,
                            completedAt = completion?.completedAt
                        )
                    }

                    val totalTasks = tasksForDay.size
                    val completedTasks = tasksForDay.count { it.isCompleted }
                    val percentage = if (totalTasks > 0) {
                        Math.round((completedTasks * 100.0) / totalTasks).toInt()
                    } else {
                        0
                    }

                    DailyTaskHistoryDay(
                        dateIso = dateIso,
                        headerTitle = header,
                        displayDate = displayDate,
                        daysAgo = daysAgo,
                        tasks = tasksForDay,
                        completionPercentage = percentage
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun refreshCurrentDate() {
        _currentDate.value = DateTimeFormatterHelper.getTodayIsoDate()
    }

    fun addDailyTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDailyTask(trimmed)
        }
    }

    fun updateDailyTask(task: DailyTaskEntity) {
        val trimmed = task.title.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDailyTask(task.copy(title = trimmed))
        }
    }

    fun deleteDailyTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDailyTask(taskId)
        }
    }

    fun toggleDailyTaskCompleted(taskId: Long, isCompleted: Boolean) {
        toggleDailyTaskCompletionForDate(taskId, _currentDate.value, isCompleted)
    }

    fun toggleDailyTaskCompletionForDate(taskId: Long, dateIso: String, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleDailyTaskCompletion(taskId, dateIso, isCompleted)
        }
    }

    fun saveDailyTaskCompletionsForDate(dateIso: String, updates: Map<Long, Boolean>) {
        if (updates.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDailyTaskCompletionsForDate(dateIso, updates)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository?.setAppTheme(theme)
        }
    }

    fun recordEvent(name: String) {
        viewModelScope.launch {
            repository.recordEvent(name)
        }
    }

    fun recordHistory(reminder: ReminderEntity) {
        viewModelScope.launch {
            if (reminder.reminderType == ReminderType.DATE_TIME.name) {
                if (reminder.title.isNotBlank()) {
                    repository.recordEvent(reminder.title)
                }
            } else {
                if (reminder.title.isNotBlank()) {
                    repository.recordTitle(reminder.title)
                }
                if (reminder.locationName.isNotBlank()) {
                    repository.recordLocation(reminder.locationName, reminder.latitude, reminder.longitude, reminder.formattedAddress)
                }
            }
        }
    }

    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val id = repository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id)
            if (savedReminder.isEnabled && !savedReminder.isCompleted && savedReminder.dueDateTime != null) {
                alarmScheduler?.schedule(savedReminder)
            }
            if (GeofenceManager.isEligibleForGeofencing(savedReminder)) {
                geofenceManager?.registerGeofence(savedReminder)
            }
            if (savedReminder.reminderType == ReminderType.DATE_TIME.name) {
                if (savedReminder.title.isNotBlank()) {
                    repository.recordEvent(savedReminder.title)
                }
            } else {
                if (savedReminder.title.isNotBlank()) {
                    repository.recordTitle(savedReminder.title)
                }
                if (savedReminder.locationName.isNotBlank()) {
                    repository.recordLocation(savedReminder.locationName, savedReminder.latitude, savedReminder.longitude, savedReminder.formattedAddress)
                }
            }
        }
    }

    fun addReminderWithTasks(reminder: ReminderEntity, tasks: List<TaskEntity> = emptyList()) {
        viewModelScope.launch {
            val id = repository.saveReminderWithTasks(reminder, tasks)
            val savedReminder = reminder.copy(id = id)
            if (savedReminder.isEnabled && !savedReminder.isCompleted && savedReminder.dueDateTime != null) {
                alarmScheduler?.schedule(savedReminder)
            }
            if (GeofenceManager.isEligibleForGeofencing(savedReminder)) {
                geofenceManager?.registerGeofence(savedReminder)
            }
            if (savedReminder.reminderType == ReminderType.DATE_TIME.name) {
                if (savedReminder.title.isNotBlank()) {
                    repository.recordEvent(savedReminder.title)
                }
            } else {
                if (savedReminder.title.isNotBlank()) {
                    repository.recordTitle(savedReminder.title)
                }
                if (savedReminder.locationName.isNotBlank()) {
                    repository.recordLocation(savedReminder.locationName, savedReminder.latitude, savedReminder.longitude, savedReminder.formattedAddress)
                }
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            if (reminder.isEnabled && !reminder.isCompleted && reminder.dueDateTime != null) {
                alarmScheduler?.schedule(reminder)
            } else {
                alarmScheduler?.cancel(reminder.id)
            }
            if (GeofenceManager.isEligibleForGeofencing(reminder)) {
                geofenceManager?.removeGeofence(reminder.id)
                geofenceManager?.registerGeofence(reminder)
            } else {
                geofenceManager?.removeGeofence(reminder.id)
            }
            if (reminder.reminderType == ReminderType.DATE_TIME.name) {
                if (reminder.title.isNotBlank()) {
                    repository.recordEvent(reminder.title)
                }
            } else {
                if (reminder.title.isNotBlank()) {
                    repository.recordTitle(reminder.title)
                }
                if (reminder.locationName.isNotBlank()) {
                    repository.recordLocation(reminder.locationName, reminder.latitude, reminder.longitude, reminder.formattedAddress)
                }
            }
        }
    }

    fun updateReminderWithTasks(reminder: ReminderEntity, tasks: List<TaskEntity> = emptyList()) {
        viewModelScope.launch {
            repository.updateReminderWithTasks(reminder, tasks)
            if (reminder.isEnabled && !reminder.isCompleted && reminder.dueDateTime != null) {
                alarmScheduler?.schedule(reminder)
            } else {
                alarmScheduler?.cancel(reminder.id)
            }
            if (GeofenceManager.isEligibleForGeofencing(reminder)) {
                geofenceManager?.removeGeofence(reminder.id)
                geofenceManager?.registerGeofence(reminder)
            } else {
                geofenceManager?.removeGeofence(reminder.id)
            }
            if (reminder.reminderType == ReminderType.DATE_TIME.name) {
                if (reminder.title.isNotBlank()) {
                    repository.recordEvent(reminder.title)
                }
            } else {
                if (reminder.title.isNotBlank()) {
                    repository.recordTitle(reminder.title)
                }
                if (reminder.locationName.isNotBlank()) {
                    repository.recordLocation(reminder.locationName, reminder.latitude, reminder.longitude, reminder.formattedAddress)
                }
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            alarmScheduler?.cancel(reminder.id)
            geofenceManager?.removeGeofence(reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    fun toggleReminderEnabled(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(id, isEnabled)
            val reminder = repository.getReminderByIdOnce(id)
            if (reminder != null) {
                if (isEnabled && !reminder.isCompleted) {
                    repository.setReminderGeofenceState(id, GeofenceState.UNKNOWN.name)
                    if (reminder.dueDateTime != null) {
                        alarmScheduler?.schedule(reminder.copy(isEnabled = true))
                    }
                    if (GeofenceManager.isEligibleForGeofencing(reminder.copy(isEnabled = true))) {
                        geofenceManager?.registerGeofence(reminder.copy(isEnabled = true))
                    }
                } else {
                    repository.setReminderGeofenceState(id, GeofenceState.OUTSIDE.name)
                    alarmScheduler?.cancel(id)
                    geofenceManager?.removeGeofence(id)
                }
            }
        }
    }

    fun toggleReminderCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setReminderCompleted(id, isCompleted)
            val reminder = repository.getReminderByIdOnce(id)
            if (reminder != null) {
                if (!isCompleted && reminder.isEnabled) {
                    repository.setReminderGeofenceState(id, GeofenceState.UNKNOWN.name)
                    if (reminder.dueDateTime != null) {
                        alarmScheduler?.schedule(reminder.copy(isCompleted = false))
                    }
                    if (GeofenceManager.isEligibleForGeofencing(reminder.copy(isCompleted = false))) {
                        geofenceManager?.registerGeofence(reminder.copy(isCompleted = false))
                    }
                } else {
                    repository.setReminderGeofenceState(id, GeofenceState.OUTSIDE.name)
                    alarmScheduler?.cancel(id)
                    geofenceManager?.removeGeofence(id)
                }
            }
        }
    }

    fun reactivateReminder(id: Long) {
        viewModelScope.launch {
            repository.setReminderCompleted(id, false)
            repository.setReminderEnabled(id, true)
            repository.setReminderGeofenceState(id, GeofenceState.UNKNOWN.name)
            val reminder = repository.getReminderByIdOnce(id)
            if (reminder != null) {
                val activeReminder = reminder.copy(isCompleted = false, isEnabled = true)
                if (activeReminder.dueDateTime != null && activeReminder.dueDateTime > System.currentTimeMillis()) {
                    alarmScheduler?.schedule(activeReminder)
                } else {
                    alarmScheduler?.cancel(activeReminder.id)
                }
                if (GeofenceManager.isEligibleForGeofencing(activeReminder)) {
                    geofenceManager?.registerGeofence(activeReminder)
                } else {
                    geofenceManager?.removeGeofence(activeReminder.id)
                }
            }
        }
    }

    fun markHistoryItemCompleted(id: Long) {
        viewModelScope.launch {
            alarmScheduler?.cancel(id)
            geofenceManager?.removeGeofence(id)
            repository.setReminderGeofenceState(id, GeofenceState.OUTSIDE.name)
            repository.setReminderCompleted(id, true)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearCompletedHistory()
        }
    }

    fun addTask(reminderId: Long, title: String) {
        if (title.trim().isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    reminderId = reminderId,
                    title = title.trim(),
                    isCompleted = false
                )
            )
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(id, isCompleted)
        }
    }
}

class ReminderViewModelFactory(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler? = null,
    private val geofenceManager: GeofenceManager? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            return ReminderViewModel(repository, alarmScheduler, geofenceManager, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}