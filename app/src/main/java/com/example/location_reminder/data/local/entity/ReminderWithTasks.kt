package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class ReminderWithTasks(
    @Embedded
    val reminder: ReminderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val tasks: List<TaskEntity> = emptyList()
)
