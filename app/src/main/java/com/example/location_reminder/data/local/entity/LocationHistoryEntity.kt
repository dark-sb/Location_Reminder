package com.example.location_reminder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_history",
    indices = [Index(value = ["name"], unique = true)]
)
data class LocationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val lastUsedAt: Long = System.currentTimeMillis()
)
