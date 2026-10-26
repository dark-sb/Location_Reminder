package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reminderId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
