package com.example.location_reminder.data.geofence

import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType

interface GeofenceManager {
    /**
     * Registers a geofence for the given reminder if eligible.
     * Uses saved latitude, longitude, and radius.
     */
    fun registerGeofence(reminder: ReminderEntity)

    /**
     * Removes the geofence associated with the given reminder ID.
     */
    fun removeGeofence(reminderId: Long)

    /**
     * Removes all registered geofences for this application.
     */
    fun removeAllGeofences()

    companion object {
        /**
         * Checks whether a reminder is eligible for geofence location monitoring.
         * Eligibility criteria:
         * 1. Reminder type must be LOCATION or LOCATION_AND_TIME.
         * 2. Reminder must be enabled (isEnabled == true).
         * 3. Reminder must not be completed (isCompleted == false).
         * 4. Must have valid non-zero latitude and longitude within geographical bounds.
         * 5. Radius must be strictly positive (> 0.0m).
         */
        fun isEligibleForGeofencing(reminder: ReminderEntity): Boolean {
            val isLocationType = reminder.reminderType == ReminderType.LOCATION.name ||
                    reminder.reminderType == ReminderType.LOCATION_AND_TIME.name
            val hasValidCoords = (reminder.latitude != 0.0 || reminder.longitude != 0.0) &&
                    reminder.latitude in -90.0..90.0 &&
                    reminder.longitude in -180.0..180.0
            val hasValidRadius = reminder.radius > 0.0

            return isLocationType && reminder.isEnabled && !reminder.isCompleted && hasValidCoords && hasValidRadius
        }

        /**
         * Formats a stable, unique geofence Request ID for a reminder.
         * Ensures multiple reminders at the same location never collide.
         */
        fun getGeofenceRequestId(reminderId: Long): String {
            return "geofence_reminder_$reminderId"
        }

        /**
         * Extracts the reminder ID from a geofence Request ID.
         */
        fun extractReminderId(requestId: String): Long? {
            return if (requestId.startsWith("geofence_reminder_")) {
                requestId.removePrefix("geofence_reminder_").toLongOrNull()
            } else {
                requestId.toLongOrNull()
            }
        }
    }
}
