package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GeofenceState {
    UNKNOWN,
    INSIDE,
    OUTSIDE
}

@Immutable
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val formattedAddress: String = "",
    val placeId: String = "",
    val radius: Double = 100.0,
    val reminderMessage: String = "",
    val isEnabled: Boolean = true,
    val reminderType: String = ReminderType.LOCATION.name,
    val dueDateTime: Long? = null,
    val isCompleted: Boolean = false,
    val repeatIntervalMinutes: Long = 0L,
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val geofenceState: String = GeofenceState.UNKNOWN.name
)