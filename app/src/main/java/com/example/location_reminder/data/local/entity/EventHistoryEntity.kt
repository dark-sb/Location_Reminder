package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "event_history",
    indices = [Index(value = ["eventName"], unique = true)]
)
data class EventHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventName: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)
