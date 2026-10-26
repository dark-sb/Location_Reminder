package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable

@Immutable
data class DailyTaskWithCompletion(
    val taskId: Long,
    val title: String,
    val createdAt: Long,
    val isCompleted: Boolean,
    val completedAt: Long? = null
)
