package com.example.location_reminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_task_completions",
    foreignKeys = [
        ForeignKey(
            entity = DailyTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index(value = ["taskId", "date"], unique = true)
    ]
)
data class DailyTaskCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val date: String, // e.g. "yyyy-MM-dd"
    val isCompleted: Boolean,
    val completedAt: Long? = null
)
