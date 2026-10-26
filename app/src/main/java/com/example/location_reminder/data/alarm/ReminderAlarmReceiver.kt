package com.example.location_reminder.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_reminder.data.geofence.AndroidGeofenceManager
import com.example.location_reminder.data.local.database.ReminderDatabase
import com.example.location_reminder.data.local.entity.GeofenceState
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val isLocationReminder = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_IS_LOCATION_REMINDER, false)
        val action = intent.action

        Log.d(TAG, "onReceive: action=$action, reminderId=$reminderId, isLocation=$isLocationReminder")

        if (reminderId == -1L) {
            Log.w(TAG, "Received broadcast with invalid reminder ID: $reminderId")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context.applicationContext)
                val reminderDao = database.reminderDao
                val alarmScheduler = AndroidAlarmScheduler(context.applicationContext)
                val geofenceManager = AndroidGeofenceManager(context.applicationContext)

                when (action) {
                    ACTION_MARK_COMPLETED -> {
                        Log.d(TAG, "Action: Mark reminder $reminderId as completed")
                        reminderDao.updateReminderCompletedWithTimestamp(reminderId, true, System.currentTimeMillis())
                        reminderDao.updateReminderSnoozedUntil(reminderId, null)
                        reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)
                        alarmScheduler.cancel(reminderId)
                        geofenceManager.removeGeofence(reminderId)
                        NotificationHelper.cancelNotification(context.applicationContext, reminderId)
                    }

                    ACTION_SNOOZE -> {
                        Log.d(TAG, "Action: Snooze reminder $reminderId for ${AlarmScheduler.DEFAULT_SNOOZE_MINUTES} minutes")
                        // 1. Dismiss active notification
                        NotificationHelper.cancelNotification(context.applicationContext, reminderId)

                        // 2. Cancel pending repeat alarm so it doesn't fire while snoozed
                        alarmScheduler.cancelRepeat(reminderId)

                        val reminder = reminderDao.getReminderByIdOnce(reminderId)
                        if (reminder != null && reminder.isEnabled && !reminder.isCompleted) {
                            val snoozeMinutes = AlarmScheduler.DEFAULT_SNOOZE_MINUTES
                            val snoozeDeadline = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                            reminderDao.updateReminderSnoozedUntil(reminderId, snoozeDeadline)

                            // 3. Schedule the snooze alarm
                            alarmScheduler.scheduleSnooze(
                                reminder = reminder,
                                snoozeDurationMinutes = snoozeMinutes,
                                isLocationReminder = isLocationReminder
                            )
                        } else {
                            Log.d(TAG, "Cannot snooze reminder $reminderId: deleted, disabled, or completed.")
                        }
                    }

                    else -> {
                        // Alarm fired (Primary scheduled, repeating alert, or snooze alert)
                        val reminder = reminderDao.getReminderByIdOnce(reminderId)
                        when {
                            reminder == null -> {
                                Log.d(TAG, "Reminder $reminderId was deleted. Stopping alerts and cleaning up.")
                                alarmScheduler.cancel(reminderId)
                                geofenceManager.removeGeofence(reminderId)
                            }
                            !reminder.isEnabled -> {
                                Log.d(TAG, "Reminder $reminderId is disabled. Stopping alerts and cleaning up.")
                                alarmScheduler.cancel(reminderId)
                                geofenceManager.removeGeofence(reminderId)
                                reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)
                            }
                            reminder.isCompleted -> {
                                Log.d(TAG, "Reminder $reminderId is already completed. Stopping alerts and cleaning up.")
                                alarmScheduler.cancel(reminderId)
                                geofenceManager.removeGeofence(reminderId)
                                reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)
                            }
                            else -> {
                                val isLocation = isLocationReminder ||
                                        reminder.reminderType == ReminderType.LOCATION.name ||
                                        reminder.reminderType == ReminderType.LOCATION_AND_TIME.name

                                // Safety check for location reminders:
                                // Repeat or snooze alerts must ONLY execute if the reminder is currently INSIDE the geofence!
                                if (isLocation && (action == AndroidAlarmScheduler.ACTION_REPEAT_ALARM || action == AndroidAlarmScheduler.ACTION_SNOOZE_ALARM)) {
                                    if (reminder.geofenceState != GeofenceState.INSIDE.name) {
                                        Log.d(TAG, "Location reminder $reminderId alert fired, but geofenceState is '${reminder.geofenceState}' (not INSIDE). Skipping alert and cancelling repeat.")
                                        alarmScheduler.cancelRepeat(reminderId)
                                        reminderDao.updateReminderSnoozedUntil(reminderId, null)
                                        return@launch
                                    }
                                }

                                Log.d(TAG, "Triggering alert for reminder $reminderId: '${reminder.title}' (isLocation=$isLocation, repeat=${reminder.repeatIntervalMinutes}m)")

                                // Clear snoozedUntil flag on fire
                                reminderDao.updateReminderSnoozedUntil(reminderId, null)

                                // Post appropriate notification
                                if (isLocation) {
                                    NotificationHelper.showGeofenceNotification(
                                        context.applicationContext,
                                        reminder,
                                        Geofence.GEOFENCE_TRANSITION_ENTER
                                    )
                                } else {
                                    NotificationHelper.showScheduledReminderNotification(
                                        context.applicationContext,
                                        reminder
                                    )
                                }

                                // If repeating alerts are configured, schedule the next repeat
                                // For location reminders, ONLY if still INSIDE!
                                if (reminder.repeatIntervalMinutes > 0L) {
                                    if (!isLocation || reminder.geofenceState == GeofenceState.INSIDE.name) {
                                        val nextRepeatTime = System.currentTimeMillis() + (reminder.repeatIntervalMinutes * 60 * 1000L)
                                        alarmScheduler.scheduleRepeat(
                                            reminder = reminder,
                                            triggerAtMillis = nextRepeatTime,
                                            isLocationReminder = isLocation
                                        )
                                        Log.d(TAG, "Scheduled next repeat alert for reminder $reminderId in ${reminder.repeatIntervalMinutes} minutes ($nextRepeatTime)")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in ReminderAlarmReceiver for ID $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val ACTION_MARK_COMPLETED = "com.example.location_reminder.ACTION_MARK_COMPLETED"
        const val ACTION_SNOOZE = "com.example.location_reminder.ACTION_SNOOZE"
    }
}
