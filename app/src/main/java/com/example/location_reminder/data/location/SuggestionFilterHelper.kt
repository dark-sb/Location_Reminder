package com.example.location_reminder.data.location

import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import java.util.Locale

/**
 * Pure helper for filtering and ordering title and location suggestions.
 * Guarantees case-insensitive filtering, exclusion of blanks and duplicates,
 * recency preservation, and limit constraints.
 */
object SuggestionFilterHelper {

    /**
     * Filters title history:
     * - Returns up to [maxItems] (default 5) unique titles.
     * - Preserves most-recently-used order.
     * - Excludes blank titles.
     * - Deduplicates case-insensitively, keeping the most recent spelling.
     * - Excludes [editingInitialTitle] if user is editing an existing reminder.
     * - Immediate case-insensitive filtering if [query] is non-empty.
     */
    fun filterTitles(
        query: String,
        history: List<TitleHistoryEntity>,
        editingInitialTitle: String? = null,
        maxItems: Int = 5
    ): List<String> {
        val trimmedQuery = query.trim()
        val initialTrimmed = editingInitialTitle?.trim()

        return history
            .map { it.title.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .filter { candidate ->
                if (!initialTrimmed.isNullOrBlank() && candidate.equals(initialTrimmed, ignoreCase = true)) {
                    false
                } else {
                    true
                }
            }
            .filter { candidate ->
                if (trimmedQuery.isBlank()) true else candidate.contains(trimmedQuery, ignoreCase = true)
            }
            .take(maxItems)
    }

    /**
     * Filters location history:
     * - Returns up to [maxItems] (default 20) unique locations.
     * - Preserves most-recently-used order.
     * - Excludes blank names and legacy placeholder names (e.g. "Time Reminder").
     * - Deduplicates case-insensitively, keeping the most recent entry with its coordinates.
     * - Immediate case-insensitive filtering if [query] is non-empty.
     */
    fun filterLocations(
        query: String,
        history: List<LocationHistoryEntity>,
        maxItems: Int = 20
    ): List<LocationHistoryEntity> {
        val trimmedQuery = query.trim()

        return history
            .filter { it.name.trim().isNotBlank() && !it.name.trim().equals("Time Reminder", ignoreCase = true) }
            .distinctBy { it.name.trim().lowercase(Locale.ROOT) }
            .filter { candidate ->
                if (trimmedQuery.isBlank()) true else candidate.name.contains(trimmedQuery, ignoreCase = true)
            }
            .take(maxItems)
    }

    /**
     * Filters event history:
     * - Returns up to [maxItems] (default 5) unique events.
     * - Preserves most-recently-used order.
     * - Excludes blank event names.
     * - Deduplicates case-insensitively, keeping the most recent spelling.
     * - Excludes [editingInitialTitle] if user is editing an existing reminder.
     * - Immediate case-insensitive filtering if [query] is non-empty.
     */
    fun filterEvents(
        query: String,
        history: List<com.example.location_reminder.data.local.entity.EventHistoryEntity>,
        editingInitialTitle: String? = null,
        maxItems: Int = 5
    ): List<String> {
        val trimmedQuery = query.trim()
        val initialTrimmed = editingInitialTitle?.trim()

        return history
            .map { it.eventName.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .filter { candidate ->
                if (!initialTrimmed.isNullOrBlank() && candidate.equals(initialTrimmed, ignoreCase = true)) {
                    false
                } else {
                    true
                }
            }
            .filter { candidate ->
                if (trimmedQuery.isBlank()) true else candidate.contains(trimmedQuery, ignoreCase = true)
            }
            .take(maxItems)
    }
}
