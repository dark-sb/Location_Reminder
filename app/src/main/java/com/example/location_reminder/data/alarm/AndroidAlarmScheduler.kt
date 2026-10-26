package com.example.location_reminder.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.location_reminder.data.local.entity.ReminderEntity

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(reminder: ReminderEntity) {
        val dueTime = reminder.dueDateTime
        if (dueTime == null || dueTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping scheduling for reminder ${reminder.id}: time is null or in the past ($dueTime)")
            return
        }

        if (!reminder.isEnabled || reminder.isCompleted) {
            Log.d(TAG, "Skipping scheduling for reminder ${reminder.id}: isEnabled=${reminder.isEnabled}, isCompleted=${reminder.isCompleted}")
            return
        }

        val pendingIntent = createAlarmPendingIntent(
            reminderId = reminder.id,
            action = ACTION_FIRE_ALARM,
            requestCode = reminder.id.toInt(),
            isLocationReminder = false
        )

        scheduleAlarmInternal(dueTime, pendingIntent, "primary alarm for reminder ${reminder.id}")
    }

    override fun scheduleRepeat(reminder: ReminderEntity, triggerAtMillis: Long, isLocationReminder: Boolean) {
        if (!reminder.isEnabled || reminder.isCompleted || triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping repeat scheduling for reminder ${reminder.id}: isEnabled=${reminder.isEnabled}, isCompleted=${reminder.isCompleted}, triggerAt=$triggerAtMillis")
            return
        }

        val pendingIntent = createAlarmPendingIntent(
            reminderId = reminder.id,
            action = ACTION_REPEAT_ALARM,
            requestCode = reminder.id.toInt() + REPEAT_REQUEST_CODE_OFFSET,
            isLocationReminder = isLocationReminder
        )

        scheduleAlarmInternal(triggerAtMillis, pendingIntent, "repeat alert for reminder ${reminder.id}")
    }

    override fun scheduleSnooze(reminder: ReminderEntity, snoozeDurationMinutes: Long, isLocationReminder: Boolean) {
        if (!reminder.isEnabled || reminder.isCompleted) {
            Log.d(TAG, "Skipping snooze scheduling for reminder ${reminder.id}: isEnabled=${reminder.isEnabled}, isCompleted=${reminder.isCompleted}")
            return
        }

        val triggerAt = System.currentTimeMillis() + (snoozeDurationMinutes * 60 * 1000L)
        val pendingIntent = createAlarmPendingIntent(
            reminderId = reminder.id,
            action = ACTION_SNOOZE_ALARM,
            requestCode = reminder.id.toInt() + SNOOZE_REQUEST_CODE_OFFSET,
            isLocationReminder = isLocationReminder
        )

        scheduleAlarmInternal(triggerAt, pendingIntent, "snooze alert ($snoozeDurationMinutes min) for reminder ${reminder.id}")
    }

    override fun cancelRepeat(reminderId: Long) {
        cancelPendingIntent(
            action = ACTION_REPEAT_ALARM,
            requestCode = reminderId.toInt() + REPEAT_REQUEST_CODE_OFFSET,
            logTag = "repeat alarm for reminder $reminderId"
        )
    }

    override fun cancel(reminderId: Long) {
        // Cancel primary scheduled alarm
        cancelPendingIntent(
            action = ACTION_FIRE_ALARM,
            requestCode = reminderId.toInt(),
            logTag = "primary alarm for reminder $reminderId"
        )

        // Cancel repeating alert alarm
        cancelPendingIntent(
            action = ACTION_REPEAT_ALARM,
            requestCode = reminderId.toInt() + REPEAT_REQUEST_CODE_OFFSET,
            logTag = "repeat alarm for reminder $reminderId"
        )

        // Cancel snooze alarm
        cancelPendingIntent(
            action = ACTION_SNOOZE_ALARM,
            requestCode = reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET,
            logTag = "snooze alarm for reminder $reminderId"
        )

        // Dismiss active notifications for this reminder
        com.example.location_reminder.data.notification.NotificationHelper.cancelNotification(context, reminderId)
    }

    private fun createAlarmPendingIntent(
        reminderId: Long,
        action: String,
        requestCode: Int,
        isLocationReminder: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_LOCATION_REMINDER, isLocationReminder)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flag
        )
    }

    private fun cancelPendingIntent(action: String, requestCode: Int, logTag: String) {
        if (alarmManager == null) return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            this.action = action
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flag
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled $logTag (requestCode=$requestCode)")
    }

    private fun scheduleAlarmInternal(triggerAtMillis: Long, pendingIntent: PendingIntent, description: String) {
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available on this device")
            return
        }

        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled exact $description at $triggerAtMillis")
            } else {
                // Inexact fallback for Android 12+ if exact alarms are restricted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled inexact fallback $description at $triggerAtMillis")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact $description, attempting inexact fallback", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to schedule fallback for $description", fallbackException)
            }
        }
    }

    companion object {
        private const val TAG = "AndroidAlarmScheduler"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_IS_LOCATION_REMINDER = "EXTRA_IS_LOCATION_REMINDER"

        const val ACTION_FIRE_ALARM = "com.example.location_reminder.ACTION_FIRE_ALARM"
        const val ACTION_REPEAT_ALARM = "com.example.location_reminder.ACTION_REPEAT_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.location_reminder.ACTION_SNOOZE_ALARM"

        const val REPEAT_REQUEST_CODE_OFFSET = 300000
        const val SNOOZE_REQUEST_CODE_OFFSET = 400000
    }
}
