package com.example.location_reminder.data.alarm

import com.example.location_reminder.data.local.entity.ReminderEntity

interface AlarmScheduler {
    /**
     * Schedules a date/time alarm for the given reminder.
     * If the scheduled time is in the past, or if the reminder is disabled or completed,
     * scheduling is safely skipped.
     */
    fun schedule(reminder: ReminderEntity)

    /**
     * Schedules a repeat alert for the given reminder at a specific timestamp.
     */
    fun scheduleRepeat(reminder: ReminderEntity, triggerAtMillis: Long, isLocationReminder: Boolean = false)

    /**
     * Schedules a snooze alert for the given reminder for the specified duration (default 10 minutes).
     */
    fun scheduleSnooze(reminder: ReminderEntity, snoozeDurationMinutes: Long = DEFAULT_SNOOZE_MINUTES, isLocationReminder: Boolean = false)

    /**
     * Cancels any active, pending, repeat, or snooze alarms associated with the given reminder ID.
     */
    fun cancel(reminderId: Long)

    /**
     * Cancels only the repeating alert alarm for the given reminder ID.
     */
    fun cancelRepeat(reminderId: Long)

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10L

        val SUPPORTED_REPEAT_INTERVALS = listOf(0L, 5L, 10L, 15L, 30L, 60L)

        fun formatRepeatInterval(minutes: Long): String {
            return when (minutes) {
                0L -> "Off"
                60L -> "1 hour"
                else -> "$minutes min"
            }
        }
    }
}
