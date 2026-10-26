package com.example.location_reminder.data.sync

import android.content.Context
import android.util.Log
import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.geofence.AndroidGeofenceManager
import com.example.location_reminder.data.geofence.GeofenceManager
import com.example.location_reminder.data.local.database.ReminderDatabase
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.notification.NotificationHelper

/**
 * Manages reliable restoration and reconciliation of date/time alarms and geofences
 * after device reboot, app package update, or capability/permission restoration.
 */
object ScheduleRestorationManager {

    private const val TAG = "ScheduleRestoration"

    sealed class RestorationAction {
        data class RestoreSnooze(val remainingMinutes: Long, val isLocation: Boolean) : RestorationAction()
        object ClearExpiredSnooze : RestorationAction()
        data class RestoreScheduledAlarm(val dueTime: Long) : RestorationAction()
        object SkipPastAlarm : RestorationAction()
        object IneligibleOrCompleted : RestorationAction()
    }

    data class RestorationSummary(
        val totalActiveExamined: Int,
        val alarmsScheduled: Int,
        val geofencesRegistered: Int,
        val snoozesRestored: Int,
        val expiredSnoozesCleared: Int
    )

    /**
     * Pure function to determine the alarm restoration action for a given reminder.
     * Fully unit testable without Android Context or AlarmManager dependencies.
     */
    fun evaluateAlarmAction(reminder: ReminderEntity, nowMillis: Long): RestorationAction {
        if (!reminder.isEnabled || reminder.isCompleted) {
            return RestorationAction.IneligibleOrCompleted
        }

        val snoozedUntil = reminder.snoozedUntil
        if (snoozedUntil != null) {
            return if (snoozedUntil > nowMillis) {
                val remainingMinutes = ((snoozedUntil - nowMillis) / 60000L).coerceAtLeast(1L)
                val isLocation = reminder.reminderType == ReminderType.LOCATION.name
                RestorationAction.RestoreSnooze(remainingMinutes, isLocation)
            } else {
                RestorationAction.ClearExpiredSnooze
            }
        }

        val dueTime = reminder.dueDateTime
        return if (dueTime != null) {
            if (dueTime > nowMillis) {
                RestorationAction.RestoreScheduledAlarm(dueTime)
            } else {
                RestorationAction.SkipPastAlarm
            }
        } else {
            RestorationAction.IneligibleOrCompleted
        }
    }

    /**
     * Checks if a reminder is eligible for geofence registration on restoration.
     */
    fun isEligibleForGeofence(reminder: ReminderEntity): Boolean {
        return GeofenceManager.isEligibleForGeofencing(reminder)
    }

    /**
     * Restores all active schedules from the local database.
     * Invoked on BOOT_COMPLETED and MY_PACKAGE_REPLACED.
     */
    suspend fun restoreAllSchedules(context: Context): RestorationSummary {
        val appContext = context.applicationContext
        Log.d(TAG, "Starting full schedule restoration from Room database.")

        // 1. Ensure notification channels are registered
        NotificationHelper.createNotificationChannels(appContext)

        val database = ReminderDatabase.getDatabase(appContext)
        val reminderDao = database.reminderDao
        val alarmScheduler = AndroidAlarmScheduler(appContext)
        val geofenceManager = AndroidGeofenceManager(appContext)

        val activeReminders = try {
            reminderDao.getActiveRemindersOnce()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load active reminders from Room", e)
            emptyList()
        }

        var alarmsScheduled = 0
        var geofencesRegistered = 0
        var snoozesRestored = 0
        var expiredSnoozesCleared = 0

        val now = System.currentTimeMillis()

        for (reminder in activeReminders) {
            try {
                // Restore Geofences
                if (isEligibleForGeofence(reminder)) {
                    reminderDao.updateReminderGeofenceState(reminder.id, com.example.location_reminder.data.local.entity.GeofenceState.UNKNOWN.name)
                    geofenceManager.registerGeofence(reminder)
                    geofencesRegistered++
                }

                // Restore Alarms
                when (val action = evaluateAlarmAction(reminder, now)) {
                    is RestorationAction.RestoreSnooze -> {
                        alarmScheduler.scheduleSnooze(
                            reminder = reminder,
                            snoozeDurationMinutes = action.remainingMinutes,
                            isLocationReminder = action.isLocation
                        )
                        snoozesRestored++
                        alarmsScheduled++
                    }
                    is RestorationAction.ClearExpiredSnooze -> {
                        reminderDao.updateReminderSnoozedUntil(reminder.id, null)
                        expiredSnoozesCleared++
                        // If it also had a future primary due date, schedule that
                        val dueTime = reminder.dueDateTime
                        if (dueTime != null && dueTime > now) {
                            alarmScheduler.schedule(reminder)
                            alarmsScheduled++
                        }
                    }
                    is RestorationAction.RestoreScheduledAlarm -> {
                        alarmScheduler.schedule(reminder)
                        alarmsScheduled++
                    }
                    RestorationAction.SkipPastAlarm -> {
                        Log.d(TAG, "Skipping past alarm for reminder ${reminder.id} (due ${reminder.dueDateTime} <= $now)")
                    }
                    RestorationAction.IneligibleOrCompleted -> {
                        // Location-only or completed; no alarm needed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring schedule for reminder ${reminder.id}", e)
            }
        }

        val summary = RestorationSummary(
            totalActiveExamined = activeReminders.size,
            alarmsScheduled = alarmsScheduled,
            geofencesRegistered = geofencesRegistered,
            snoozesRestored = snoozesRestored,
            expiredSnoozesCleared = expiredSnoozesCleared
        )

        Log.d(TAG, "Schedule restoration finished: $summary")
        return summary
    }

    /**
     * Reconciles eligible geofences when location permissions are granted after being revoked.
     */
    suspend fun reconcileIfPermissionsRestored(context: Context) {
        val appContext = context.applicationContext
        try {
            val database = ReminderDatabase.getDatabase(appContext)
            val reminderDao = database.reminderDao
            val activeReminders = reminderDao.getActiveRemindersOnce()
            val geofenceManager = AndroidGeofenceManager(appContext)

            for (reminder in activeReminders) {
                if (isEligibleForGeofence(reminder)) {
                    reminderDao.updateReminderGeofenceState(reminder.id, com.example.location_reminder.data.local.entity.GeofenceState.UNKNOWN.name)
                    geofenceManager.registerGeofence(reminder)
                }
            }
            Log.d(TAG, "Reconciled geofences after permission state change.")
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling geofences on permission restore", e)
        }
    }
}
