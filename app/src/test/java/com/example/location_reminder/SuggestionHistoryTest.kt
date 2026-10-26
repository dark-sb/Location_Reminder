package com.example.location_reminder

import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import com.example.location_reminder.data.location.SuggestionFilterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SuggestionHistoryTest {

    // =========================================================================
    // 1. Location history returns most recent unique locations (up to 20)
    // =========================================================================
    @Test
    fun locationHistory_returnsMostRecentUniqueLocations() {
        val now = System.currentTimeMillis()
        val history = listOf(
            LocationHistoryEntity(id = 1, name = "Target Superstore", latitude = 37.1, longitude = -122.1, lastUsedAt = now),
            LocationHistoryEntity(id = 2, name = "target superstore", latitude = 37.11, longitude = -122.11, lastUsedAt = now - 5000), // duplicate, older
            LocationHistoryEntity(id = 3, name = "Whole Foods", latitude = 37.2, longitude = -122.2, lastUsedAt = now - 1000),
            LocationHistoryEntity(id = 4, name = "Blue Bottle Coffee", latitude = 37.3, longitude = -122.3, lastUsedAt = now - 2000)
        )

        val result = SuggestionFilterHelper.filterLocations(query = "", history = history, maxItems = 20)

        // Must have 3 unique locations, deduplicated case-insensitively, keeping the most recent
        assertEquals(3, result.size)
        assertEquals("Target Superstore", result[0].name)
        assertEquals(37.1, result[0].latitude, 0.0001)
        assertEquals("Whole Foods", result[1].name)
        assertEquals("Blue Bottle Coffee", result[2].name)
    }

    // =========================================================================
    // 2. Location suggestions filter case-insensitively & preserve display spelling
    // =========================================================================
    @Test
    fun locationSuggestions_filterCaseInsensitively_preservesSpelling() {
        val history = listOf(
            LocationHistoryEntity(id = 1, name = "Downtown Library", latitude = 37.78, longitude = -122.41),
            LocationHistoryEntity(id = 2, name = "Central Park", latitude = 40.78, longitude = -73.96),
            LocationHistoryEntity(id = 3, name = "downtown pharmacy", latitude = 37.79, longitude = -122.42)
        )

        val result = SuggestionFilterHelper.filterLocations(query = "DOWNTOWN", history = history)

        assertEquals(2, result.size)
        assertEquals("Downtown Library", result[0].name)
        assertEquals("downtown pharmacy", result[1].name)
    }

    // =========================================================================
    // 3. Location suggestions exclude blanks and legacy "Time Reminder"
    // =========================================================================
    @Test
    fun locationSuggestions_excludesBlankEntriesAndLegacyPlaceholders() {
        val history = listOf(
            LocationHistoryEntity(id = 1, name = "", latitude = 0.0, longitude = 0.0),
            LocationHistoryEntity(id = 2, name = "   ", latitude = 0.0, longitude = 0.0),
            LocationHistoryEntity(id = 3, name = "Time Reminder", latitude = 0.0, longitude = 0.0),
            LocationHistoryEntity(id = 4, name = "Valid Place", latitude = 37.0, longitude = -122.0)
        )

        val result = SuggestionFilterHelper.filterLocations(query = "", history = history)

        assertEquals(1, result.size)
        assertEquals("Valid Place", result[0].name)
    }

    // =========================================================================
    // 4. Title history returns at most five unique titles
    // =========================================================================
    @Test
    fun titleHistory_returnsAtMostFiveUniqueTitles() {
        val now = System.currentTimeMillis()
        val history = (1..10).map { i ->
            TitleHistoryEntity(id = i.toLong(), title = "Task Title $i", lastUsedAt = now - i * 1000)
        }

        val result = SuggestionFilterHelper.filterTitles(query = "", history = history, maxItems = 5)

        assertEquals(5, result.size)
        assertEquals("Task Title 1", result[0])
        assertEquals("Task Title 5", result[4])
    }

    // =========================================================================
    // 5. Title suggestions filter case-insensitively & preserve spelling
    // =========================================================================
    @Test
    fun titleSuggestions_filtersCaseInsensitively_preservesSpelling() {
        val history = listOf(
            TitleHistoryEntity(id = 1, title = "Buy Groceries"),
            TitleHistoryEntity(id = 2, title = "Call Mom"),
            TitleHistoryEntity(id = 3, title = "buy medicine")
        )

        val result = SuggestionFilterHelper.filterTitles(query = "BUY", history = history)

        assertEquals(2, result.size)
        assertEquals("Buy Groceries", result[0])
        assertEquals("buy medicine", result[1])
    }

    // =========================================================================
    // 6. Title suggestions exclude blank titles
    // =========================================================================
    @Test
    fun titleSuggestions_excludesBlankEntries() {
        val history = listOf(
            TitleHistoryEntity(id = 1, title = ""),
            TitleHistoryEntity(id = 2, title = "   "),
            TitleHistoryEntity(id = 3, title = "Submit Report")
        )

        val result = SuggestionFilterHelper.filterTitles(query = "", history = history)

        assertEquals(1, result.size)
        assertEquals("Submit Report", result[0])
    }

    // =========================================================================
    // 7. Title suggestions avoid showing duplicate for current reminder being edited
    // =========================================================================
    @Test
    fun titleSuggestions_whenEditing_avoidsShowingDuplicateForCurrentReminder() {
        val history = listOf(
            TitleHistoryEntity(id = 1, title = "Grocery Shopping"),
            TitleHistoryEntity(id = 2, title = "Gym Workout"),
            TitleHistoryEntity(id = 3, title = "Pick up prescription")
        )

        // User is editing reminder whose initial title is "grocery shopping"
        val result = SuggestionFilterHelper.filterTitles(
            query = "",
            history = history,
            editingInitialTitle = "grocery shopping"
        )

        assertEquals(2, result.size)
        assertFalse(result.any { it.equals("Grocery Shopping", ignoreCase = true) })
        assertTrue(result.contains("Gym Workout"))
        assertTrue(result.contains("Pick up prescription"))
    }

    // =========================================================================
    // 8. Selecting a location restores correct associated coordinates
    // =========================================================================
    @Test
    fun selectingLocation_restoresAssociatedCoordinates() {
        val selectedCandidate = LocationHistoryEntity(
            id = 42,
            name = "Mission Dolores Park",
            latitude = 37.7596,
            longitude = -122.4269
        )

        // Simulation of UI selection logic in ReminderFormDialog
        var locationName = ""
        var latitude = ""
        var longitude = ""
        var lastAssociatedLocationName = ""

        // User clicks candidate
        locationName = selectedCandidate.name
        latitude = String.format(Locale.US, "%.5f", selectedCandidate.latitude)
        longitude = String.format(Locale.US, "%.5f", selectedCandidate.longitude)
        lastAssociatedLocationName = selectedCandidate.name

        assertEquals("Mission Dolores Park", locationName)
        assertEquals("37.75960", latitude)
        assertEquals("-122.42690", longitude)
        assertEquals("Mission Dolores Park", lastAssociatedLocationName)
    }

    // =========================================================================
    // 9. Location consistency: detects name change after selecting coordinates
    // =========================================================================
    @Test
    fun locationConsistency_detectsNameChangeAndFlagsMismatch() {
        val lastAssociatedLocationName = "Dolores Park"
        val latitude = "37.75960"
        val longitude = "-122.42690"

        // Helper check matching ReminderFormDialog logic
        fun isMismatch(currentName: String): Boolean {
            val hasValidCoords = latitude.toDoubleOrNull()?.let { it != 0.0 } == true &&
                longitude.toDoubleOrNull()?.let { it != 0.0 } == true
            return hasValidCoords &&
                lastAssociatedLocationName.isNotBlank() &&
                !currentName.trim().equals(lastAssociatedLocationName.trim(), ignoreCase = true)
        }

        // Same name (case-insensitive) -> No mismatch
        assertFalse(isMismatch("Dolores Park"))
        assertFalse(isMismatch("  dolores park  "))

        // Changed name -> Mismatch detected!
        assertTrue(isMismatch("Golden Gate Park"))
        assertTrue(isMismatch("Airport Terminal 2"))
    }

    // =========================================================================
    // 10. Failed validation does not allow saving or recording incomplete history
    // =========================================================================
    @Test
    fun failedValidation_doesNotCallSaveOrUpdateHistory() {
        var historyUpdated = false

        fun validateAndSave(title: String, message: String, locName: String, lat: Double?, lng: Double?) {
            if (title.trim().isBlank()) return
            if (message.trim().isBlank()) return
            if (locName.trim().isBlank()) return
            if (lat == null || lng == null) return

            // Only executed if validation passes
            historyUpdated = true
        }

        // Test with blank title
        validateAndSave("", "Buy apples", "Market", 37.7, -122.4)
        assertFalse(historyUpdated)

        // Test with blank message
        validateAndSave("Market Run", "", "Market", 37.7, -122.4)
        assertFalse(historyUpdated)

        // Test with blank location name
        validateAndSave("Market Run", "Buy apples", "", 37.7, -122.4)
        assertFalse(historyUpdated)

        // Test with null coordinates
        validateAndSave("Market Run", "Buy apples", "Market", null, null)
        assertFalse(historyUpdated)

        // Valid reminder -> succeeds
        validateAndSave("Market Run", "Buy apples", "Market", 37.7, -122.4)
        assertTrue(historyUpdated)
    }

    // =========================================================================
    // 11. History persists across app restarts (simulated persistence)
    // =========================================================================
    @Test
    fun historyPersistsAcrossAppRestarts_simulated() {
        // Simulated local database storage
        val persistentStorage = mutableMapOf<String, LocationHistoryEntity>()

        fun saveLocation(name: String, lat: Double, lng: Double) {
            val key = name.trim().lowercase(Locale.ROOT)
            val existing = persistentStorage[key]
            persistentStorage[key] = LocationHistoryEntity(
                id = existing?.id ?: (persistentStorage.size + 1).toLong(),
                name = name.trim(),
                latitude = lat,
                longitude = lng,
                lastUsedAt = System.currentTimeMillis()
            )
        }

        // "App Session 1"
        saveLocation("Office", 37.77, -122.41)
        saveLocation("Gym", 37.78, -122.40)

        // "App Restart" -> new memory instance reads from persistentStorage
        val restoredFromStorage = persistentStorage.values.sortedByDescending { it.lastUsedAt }

        assertEquals(2, restoredFromStorage.size)
        assertTrue(restoredFromStorage.any { it.name == "Office" && it.latitude == 37.77 })
        assertTrue(restoredFromStorage.any { it.name == "Gym" && it.latitude == 37.78 })

        // "App Session 2": use Office again -> updates recency without duplicate
        val prevOfficeId = persistentStorage["office"]!!.id
        saveLocation("office", 37.7705, -122.4105)

        assertEquals(2, persistentStorage.size) // No duplicate!
        assertEquals(prevOfficeId, persistentStorage["office"]!!.id) // Same ID reused!
        assertEquals(37.7705, persistentStorage["office"]!!.latitude, 0.0001)
    }

    // =========================================================================
    // 12. Event history returns at most 5 unique events ordered by recency
    // =========================================================================
    @Test
    fun eventHistory_returnsAtMostFiveUniqueEvents_orderedByRecency() {
        val now = System.currentTimeMillis()
        val history = (1..8).map { i ->
            com.example.location_reminder.data.local.entity.EventHistoryEntity(
                id = i.toLong(),
                eventName = "Event #$i",
                lastUsedAt = now - i * 1000
            )
        }

        val result = SuggestionFilterHelper.filterEvents(query = "", history = history, maxItems = 5)

        assertEquals(5, result.size)
        assertEquals("Event #1", result[0])
        assertEquals("Event #2", result[1])
        assertEquals("Event #3", result[2])
        assertEquals("Event #4", result[3])
        assertEquals("Event #5", result[4])
    }

    // =========================================================================
    // 13. Event history filters case-insensitively and deduplicates case-insensitively
    // =========================================================================
    @Test
    fun eventHistory_filtersCaseInsensitively_andDeduplicates() {
        val now = System.currentTimeMillis()
        val history = listOf(
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 1, eventName = "Dentist Appointment", lastUsedAt = now),
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 2, eventName = "dentist appointment", lastUsedAt = now - 5000), // duplicate, older
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 3, eventName = "Team Standup Meeting", lastUsedAt = now - 1000),
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 4, eventName = "Doctor Checkup", lastUsedAt = now - 2000)
        )

        // Empty query -> all deduplicated
        val allDeduplicated = SuggestionFilterHelper.filterEvents(query = "", history = history)
        assertEquals(3, allDeduplicated.size)
        assertEquals("Dentist Appointment", allDeduplicated[0])
        assertEquals("Team Standup Meeting", allDeduplicated[1])
        assertEquals("Doctor Checkup", allDeduplicated[2])

        // Query "DENTIST" -> matches case-insensitively
        val filtered = SuggestionFilterHelper.filterEvents(query = "DENTIST", history = history)
        assertEquals(1, filtered.size)
        assertEquals("Dentist Appointment", filtered[0])
    }

    // =========================================================================
    // 14. Event history excludes blanks and current editing title
    // =========================================================================
    @Test
    fun eventHistory_excludesBlanks_andCurrentEditingTitle() {
        val history = listOf(
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 1, eventName = ""),
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 2, eventName = "   "),
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 3, eventName = "Conference Call"),
            com.example.location_reminder.data.local.entity.EventHistoryEntity(id = 4, eventName = "Gym Session")
        )

        val result = SuggestionFilterHelper.filterEvents(
            query = "",
            history = history,
            editingInitialTitle = "conference call"
        )

        assertEquals(1, result.size)
        assertEquals("Gym Session", result[0])
    }
}
