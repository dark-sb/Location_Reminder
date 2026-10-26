package com.example.location_reminder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "title_history",
    indices = [Index(value = ["title"], unique = true)]
)
data class TitleHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)
