package com.example.location_reminder.data.local.entity

import androidx.compose.runtime.Immutable

/**
 * UI and domain model representing a single calendar day in the 7-day history view.
 * Annotated with @Immutable to enable Compose compiler smart recomposition skipping.
 */
@Immutable
data class DailyTaskHistoryDay(
    val dateIso: String,
    val headerTitle: String,
    val displayDate: String = "",
    val daysAgo: Int = 0,
    val tasks: List<DailyTaskWithCompletion> = emptyList(),
    val completionPercentage: Int = if (tasks.isNotEmpty()) ((tasks.count { it.isCompleted } * 100.0) / tasks.size).toInt() else 0
)
