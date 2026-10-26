package com.example.location_reminder.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.location_reminder.MainActivity
import com.example.location_reminder.R
import com.example.location_reminder.data.alarm.ReminderAlarmReceiver
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.google.android.gms.location.Geofence

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    const val CHANNEL_SCHEDULED_ID = "scheduled_reminders_channel"
    private const val CHANNEL_SCHEDULED_NAME = "Scheduled Reminders"
    private const val CHANNEL_SCHEDULED_DESC = "Notifications and alerts for scheduled date and time reminders."

    const val CHANNEL_LOCATION_ID = "location_reminder_alerts_channel"
    private const val CHANNEL_LOCATION_NAME = "Location Reminder Alerts"
    private const val CHANNEL_LOCATION_DESC = "Alerts triggered when arriving at or leaving designated reminder locations."

    /**
     * Creates required notification channels for scheduled and location reminders.
     * Safe to call multiple times; Android system preserves user customisations.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            // Channel 1: Scheduled Date/Time Reminders
            val scheduledChannel = NotificationChannel(
                CHANNEL_SCHEDULED_ID,
                CHANNEL_SCHEDULED_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_SCHEDULED_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(scheduledChannel)

            // Channel 2: Location Reminders
            val locationChannel = NotificationChannel(
                CHANNEL_LOCATION_ID,
                CHANNEL_LOCATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_LOCATION_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(locationChannel)

            Log.d(TAG, "Notification channels registered successfully.")
        }
    }

    /**
     * Checks if notification permission is granted (always true on Android 12 and below).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Displays a notification for a scheduled date/time reminder.
     * Features:
     * - Unique notification ID per reminder (reminder.id.toInt())
     * - Tap content intent opening MainActivity with reminder ID
     * - "Mark as Completed" action button to complete reminder directly
     * - BigTextStyle for full message visibility
     */
    fun showScheduledReminderNotification(
        context: Context,
        reminder: ReminderEntity
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted.")
            return
        }

        // Tap Action: Opens MainActivity with reminder ID
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_REMINDER_ID", reminder.id)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            tapIntent,
            flag
        )

        // Action Button: "Mark as Completed"
        val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_MARK_COMPLETED
            putExtra("EXTRA_REMINDER_ID", reminder.id)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id.toInt() + 100000), // Distinct request code
            completeIntent,
            flag
        )

        // Action Button: "Snooze"
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SNOOZE
            putExtra(com.example.location_reminder.data.alarm.AndroidAlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
            putExtra(com.example.location_reminder.data.alarm.AndroidAlarmScheduler.EXTRA_IS_LOCATION_REMINDER, false)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id.toInt() + 150000), // Distinct request code
            snoozeIntent,
            flag
        )

        // Title and Content resolution
        val title = if (reminder.title.isNotBlank()) reminder.title else "Scheduled Reminder"
        val contentText = when {
            reminder.reminderMessage.isNotBlank() -> reminder.reminderMessage
            reminder.description.isNotBlank() -> reminder.description
            else -> "It's time for your scheduled reminder!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SCHEDULED_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze (10m)",
                snoozePendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Completed",
                completePendingIntent
            )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(reminder.id.toInt(), builder.build())
        Log.d(TAG, "Posted notification for reminder ${reminder.id}")
    }

    private const val LOCATION_NOTIFICATION_ID_OFFSET = 50000

    /**
     * Cancels active notifications (both scheduled and location) for the given reminder ID.
     */
    fun cancelNotification(context: Context, reminderId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(reminderId.toInt())
        notificationManager?.cancel(reminderId.toInt() + LOCATION_NOTIFICATION_ID_OFFSET)
        Log.d(TAG, "Cancelled notification for reminder $reminderId")
    }

    /**
     * Displays a geofence transition alert for location reminders.
     */
    fun showGeofenceNotification(
        context: Context,
        reminder: ReminderEntity,
        transitionType: Int
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post geofence notification: permission not granted.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_REMINDER_ID", reminder.id)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt() + LOCATION_NOTIFICATION_ID_OFFSET,
            intent,
            flag
        )

        // Action Button: "Snooze"
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SNOOZE
            putExtra(com.example.location_reminder.data.alarm.AndroidAlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
            putExtra(com.example.location_reminder.data.alarm.AndroidAlarmScheduler.EXTRA_IS_LOCATION_REMINDER, true)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id.toInt() + 250000), // Distinct request code
            snoozeIntent,
            flag
        )

        // Action Button: "Mark as Completed"
        val completeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_MARK_COMPLETED
            putExtra("EXTRA_REMINDER_ID", reminder.id)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id.toInt() + 200000), // Distinct request code
            completeIntent,
            flag
        )

        val (titlePrefix, defaultMsg) = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "📍 Arrived at: " to "You are near ${reminder.locationName}"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "👋 Departing: " to "You have left ${reminder.locationName}"
            else -> "Location Alert: " to "Location update for ${reminder.locationName}"
        }

        val title = "$titlePrefix${reminder.title}"
        val content = if (reminder.reminderMessage.isNotBlank()) {
            reminder.reminderMessage
        } else {
            defaultMsg
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze (10m)",
                snoozePendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Completed",
                completePendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(reminder.id.toInt() + LOCATION_NOTIFICATION_ID_OFFSET, notification)
        Log.d(TAG, "Posted location geofence notification for reminder ${reminder.id}")
    }
}