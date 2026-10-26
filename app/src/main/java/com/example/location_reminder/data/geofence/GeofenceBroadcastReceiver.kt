package com.example.location_reminder.data.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.local.dao.ReminderDao
import com.example.location_reminder.data.local.database.ReminderDatabase
import com.example.location_reminder.data.local.entity.GeofenceState
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent is null in broadcast receiver. Ignoring intent.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "GeofencingEvent error code: ${geofencingEvent.errorCode} ($errorMessage)")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        if (transitionType != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transitionType != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            Log.d(TAG, "Unhandled geofence transition type: $transitionType")
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "No triggering geofences found in event.")
            return
        }

        Log.d(TAG, "Received geofence transition: type=$transitionType, count=${triggeringGeofences.size}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context.applicationContext)
                val reminderDao = database.reminderDao
                val geofenceManager = AndroidGeofenceManager(context.applicationContext)
                val alarmScheduler = AndroidAlarmScheduler(context.applicationContext)

                for (geofence in triggeringGeofences) {
                    val reminderId = GeofenceManager.extractReminderId(geofence.requestId)
                    if (reminderId == null) {
                        Log.w(TAG, "Could not extract reminder ID from geofence requestId: ${geofence.requestId}")
                        continue
                    }

                    val reminder = reminderDao.getReminderByIdOnce(reminderId)

                    when {
                        reminder == null -> {
                            Log.d(TAG, "Reminder $reminderId was deleted. Removing stale geofence and cancelling alarms.")
                            geofenceManager.removeGeofence(reminderId)
                            alarmScheduler.cancel(reminderId)
                        }
                        !reminder.isEnabled -> {
                            Log.d(TAG, "Reminder $reminderId is disabled. Removing geofence, cancelling alarms, and setting state OUTSIDE.")
                            geofenceManager.removeGeofence(reminderId)
                            alarmScheduler.cancel(reminderId)
                            reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)
                        }
                        reminder.isCompleted -> {
                            Log.d(TAG, "Reminder $reminderId is completed. Removing geofence, cancelling alarms, and setting state OUTSIDE.")
                            geofenceManager.removeGeofence(reminderId)
                            alarmScheduler.cancel(reminderId)
                            reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)
                        }
                        reminder.reminderType != ReminderType.LOCATION.name &&
                                reminder.reminderType != ReminderType.LOCATION_AND_TIME.name -> {
                            Log.w(TAG, "Reminder $reminderId is not a location reminder (${reminder.reminderType}). Skipping.")
                        }
                        else -> {
                            when (transitionType) {
                                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                                    handleEnterTransition(
                                        context = context.applicationContext,
                                        reminder = reminder,
                                        reminderDao = reminderDao,
                                        alarmScheduler = alarmScheduler
                                    )
                                }
                                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                                    handleExitTransition(
                                        context = context.applicationContext,
                                        reminder = reminder,
                                        reminderDao = reminderDao,
                                        alarmScheduler = alarmScheduler
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"

        /**
         * Core ENTER logic: verifies reminder, marks INSIDE, shows arrival notification,
         * and schedules repeat alarm if configured.
         * Idempotent: avoids duplicate notifications and duplicate repeat schedules.
         */
        suspend fun handleEnterTransition(
            context: Context,
            reminder: ReminderEntity,
            reminderDao: ReminderDao,
            alarmScheduler: AndroidAlarmScheduler
        ) {
            val reminderId = reminder.id
            val wasAlreadyInside = reminder.geofenceState == GeofenceState.INSIDE.name

            Log.d(TAG, "handleEnterTransition: reminderId=$reminderId, wasAlreadyInside=$wasAlreadyInside")

            // Always persist state as INSIDE
            reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.INSIDE.name)

            if (wasAlreadyInside) {
                Log.d(TAG, "Reminder $reminderId is already INSIDE. Skipping duplicate enter notification and repeat scheduling.")
                return
            }

            // Trigger notification immediately on ENTER
            NotificationHelper.showGeofenceNotification(
                context,
                reminder.copy(geofenceState = GeofenceState.INSIDE.name),
                Geofence.GEOFENCE_TRANSITION_ENTER
            )

            // Start/schedule repeat alert using configured repeat interval (default 5 min)
            val repeatMinutes = reminder.repeatIntervalMinutes
            if (repeatMinutes > 0L) {
                val nextRepeatTime = System.currentTimeMillis() + (repeatMinutes * 60 * 1000L)
                alarmScheduler.cancelRepeat(reminderId)
                alarmScheduler.scheduleRepeat(
                    reminder = reminder.copy(geofenceState = GeofenceState.INSIDE.name),
                    triggerAtMillis = nextRepeatTime,
                    isLocationReminder = true
                )
                Log.d(TAG, "Scheduled repeat alert for location reminder $reminderId in $repeatMinutes minutes ($nextRepeatTime)")
            }
        }

        /**
         * Core EXIT logic: marks OUTSIDE, cancels repeat alarm, cancels snooze,
         * and dismisses active notification. Does NOT mark completed.
         */
        suspend fun handleExitTransition(
            context: Context,
            reminder: ReminderEntity,
            reminderDao: ReminderDao,
            alarmScheduler: AndroidAlarmScheduler
        ) {
            val reminderId = reminder.id
            Log.d(TAG, "handleExitTransition: reminderId=$reminderId, previousState=${reminder.geofenceState}")

            // Persist state as OUTSIDE
            reminderDao.updateReminderGeofenceState(reminderId, GeofenceState.OUTSIDE.name)

            // Immediately cancel any active repeat notification/alarm
            alarmScheduler.cancelRepeat(reminderId)

            // Cancel any pending snooze
            if (reminder.snoozedUntil != null) {
                reminderDao.updateReminderSnoozedUntil(reminderId, null)
            }

            // Dismiss/cancel the currently displayed location notification
            NotificationHelper.cancelNotification(context, reminderId)

            // Do NOT mark completed. Reminder remains active for re-entry!
            Log.d(TAG, "Geofence EXIT processed for reminder $reminderId: state set to OUTSIDE, repeat cancelled, notification dismissed.")
        }
    }
}
