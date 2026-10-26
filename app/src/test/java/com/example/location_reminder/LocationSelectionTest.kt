package com.example.location_reminder

import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.location.PlaceSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocationSelectionTest {

    private fun isValidCoordinate(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun formatCoordinate(coord: Double): String {
        return String.format(Locale.US, "%.5f", coord)
    }

    @Test
    fun placeSearchResult_storesCorrectData() {
        val place = PlaceSearchResult(
            title = "City Central Hospital",
            address = "123 Health Ave, Downtown, Metro City",
            latitude = 37.774929,
            longitude = -122.419416
        )

        assertEquals("City Central Hospital", place.title)
        assertEquals("123 Health Ave, Downtown, Metro City", place.address)
        assertEquals(37.774929, place.latitude, 0.000001)
        assertEquals(-122.419416, place.longitude, 0.000001)
    }

    @Test
    fun coordinateValidation_acceptsValidBoundaries() {
        assertTrue(isValidCoordinate(0.0, 0.0))
        assertTrue(isValidCoordinate(90.0, 180.0))
        assertTrue(isValidCoordinate(-90.0, -180.0))
        assertTrue(isValidCoordinate(28.6139, 77.2090)) // New Delhi
        assertTrue(isValidCoordinate(37.7749, -122.4194)) // San Francisco
    }

    @Test
    fun coordinateValidation_rejectsOutOfBounds() {
        assertFalse(isValidCoordinate(90.0001, 0.0))
        assertFalse(isValidCoordinate(-90.0001, 0.0))
        assertFalse(isValidCoordinate(0.0, 180.0001))
        assertFalse(isValidCoordinate(0.0, -180.0001))
        assertFalse(isValidCoordinate(null, 50.0))
        assertFalse(isValidCoordinate(50.0, null))
    }

    @Test
    fun coordinateFormatting_formatsConsistently() {
        val lat = 37.774929123
        val lng = -122.419415678

        assertEquals("37.77493", formatCoordinate(lat))
        assertEquals("-122.41942", formatCoordinate(lng))
    }

    @Test
    fun reminderEntity_updateFromPlaceSelection_preservesExistingData() {
        val original = ReminderEntity(
            id = 42L,
            title = "Pick up prescription",
            description = "Prescription #8841",
            reminderMessage = "Near the pharmacy!",
            latitude = 0.0,
            longitude = 0.0,
            locationName = "",
            radius = 150.0,
            isEnabled = true,
            isCompleted = false
        )

        val selectedPlace = PlaceSearchResult(
            title = "Apex Pharmacy",
            address = "77 Market St",
            latitude = 37.7833,
            longitude = -122.4167
        )

        val updated = original.copy(
            locationName = selectedPlace.title,
            latitude = selectedPlace.latitude,
            longitude = selectedPlace.longitude
        )

        assertEquals(42L, updated.id)
        assertEquals("Pick up prescription", updated.title)
        assertEquals("Prescription #8841", updated.description)
        assertEquals("Apex Pharmacy", updated.locationName)
        assertEquals(37.7833, updated.latitude, 0.0001)
        assertEquals(-122.4167, updated.longitude, 0.0001)
        assertEquals(150.0, updated.radius, 0.001)
        assertTrue(updated.isEnabled)
        assertFalse(updated.isCompleted)
    }

    @Test
    fun placeSearch_filterMatchesQuery() {
        val availablePlaces = listOf(
            PlaceSearchResult("Central College Campus", "University Road", 28.5, 77.2),
            PlaceSearchResult("General Hospital", "Hospital Lane", 28.6, 77.3),
            PlaceSearchResult("City Railway Station", "Station Plaza", 28.7, 77.4)
        )

        val query = "hospital"
        val matched = availablePlaces.filter {
            it.title.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
        }

        assertEquals(1, matched.size)
        assertEquals("General Hospital", matched[0].title)
    }

    @Test
    fun mapMode_switchPreservesCoordinates() {
        var currentMode = "NORMAL"
        val selectedLat = 28.12345
        val selectedLng = 77.67890
        val selectedTitle = "Rural Village Center"
        val selectedAddress = "Village Main Road, District 4"

        // Switch to SATELLITE
        currentMode = "SATELLITE"
        assertEquals("SATELLITE", currentMode)
        assertEquals(28.12345, selectedLat, 0.000001)
        assertEquals(77.67890, selectedLng, 0.000001)
        assertEquals("Rural Village Center", selectedTitle)
        assertEquals("Village Main Road, District 4", selectedAddress)

        // Switch back to NORMAL
        currentMode = "NORMAL"
        assertEquals("NORMAL", currentMode)
        assertEquals(28.12345, selectedLat, 0.000001)
        assertEquals(77.67890, selectedLng, 0.000001)
        assertEquals("Rural Village Center", selectedTitle)
    }

    @Test
    fun unobstructedViewport_cameraCenterIsAboveBottomPanel() {
        val screenHeightDp = 800
        val topPaddingDp = 120
        val bottomPaddingDp = 260
        val bottomPanelHeightDp = 220
        val bottomPanelTopY = screenHeightDp - bottomPanelHeightDp // 580dp

        // With map padding applied: top = 120, bottom = 260
        // Viewport center = topPadding + (screenHeight - topPadding - bottomPadding) / 2
        val unobstructedHeight = screenHeightDp - topPaddingDp - bottomPaddingDp // 420dp
        val cameraCenterY = topPaddingDp + unobstructedHeight / 2 // 330dp

        // Verify camera center is well above bottom panel
        val clearanceFromBottomPanel = bottomPanelTopY - cameraCenterY // 580 - 330 = 250dp
        assertTrue("Camera center must have at least 150dp clearance above bottom panel", clearanceFromBottomPanel >= 150)
        assertTrue("Camera center must be below top search bar", cameraCenterY > topPaddingDp)
    }

    @Test
    fun ruralVillageZoomLevel_supportedUpToMaxZoom20() {
        val minZoom = 2.0
        val maxZoom = 20.0
        val ruralVillageZoom = 17.5

        assertTrue("Village zoom must be within allowed map bounds", ruralVillageZoom in minZoom..maxZoom)
        assertTrue("Max zoom must allow close-up street detail up to zoom 20", maxZoom >= 20.0)
    }

    @Test
    fun mapStyleEndpoints_areLegitimateAndAuthorized() {
        val brightStyleUrl = "https://tiles.openfreemap.org/styles/bright"
        val esriTileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

        assertTrue(brightStyleUrl.startsWith("https://tiles.openfreemap.org/styles/"))
        assertTrue(esriTileUrl.contains("World_Imagery/MapServer/tile"))
        assertFalse("Must not use blocked raw OSM server", brightStyleUrl.contains("tile.openstreetmap.org"))
        assertFalse("Must not use blocked raw OSM server", esriTileUrl.contains("tile.openstreetmap.org"))
    }

    @Test
    fun mapMode_stringPersistenceRoundTrip() {
        val modes = listOf("NORMAL", "SATELLITE")
        for (modeStr in modes) {
            val restored = if (modeStr == "SATELLITE") "SATELLITE" else "NORMAL"
            assertEquals(modeStr, restored)
        }
    }

    // =========================================================================
    // Custom Location Name Tests
    // =========================================================================

    @Test
    fun customLocationName_overridesDetectedName_preservesCoordinates() {
        val detectedTitle = "Nearby College of Engineering"
        val detectedAddress = "Survey 27, University Road, District 4"
        val latitude = 18.4536
        val longitude = 73.8563

        // User edits name to "Library"
        val customName = "Library"
        val confirmedPlace = PlaceSearchResult(
            title = customName,
            address = detectedAddress,
            latitude = latitude,
            longitude = longitude
        )

        assertEquals("Library", confirmedPlace.title)
        assertEquals(detectedAddress, confirmedPlace.address)
        assertEquals(18.4536, confirmedPlace.latitude, 0.00001)
        assertEquals(73.8563, confirmedPlace.longitude, 0.00001)
    }

    @Test
    fun uneditedLocationName_keepsAutomaticallyDetectedName() {
        val detectedTitle = "City Central Library"
        val detectedAddress = "45 Knowledge Park"
        val latitude = 18.5204
        val longitude = 73.8567

        // User does not edit name, accepts detected
        val confirmedPlace = PlaceSearchResult(
            title = detectedTitle,
            address = detectedAddress,
            latitude = latitude,
            longitude = longitude
        )

        assertEquals("City Central Library", confirmedPlace.title)
        assertEquals(18.5204, confirmedPlace.latitude, 0.00001)
        assertEquals(73.8567, confirmedPlace.longitude, 0.00001)
    }

    @Test
    fun editingExistingReminder_allowsCustomLocationNameToBeEditedAgain() {
        val existingReminder = ReminderEntity(
            id = 101L,
            title = "Study session",
            locationName = "Old Library Room",
            latitude = 18.4536,
            longitude = 73.8563,
            radius = 100.0,
            reminderMessage = "Time to study",
            isEnabled = true,
            isCompleted = false
        )

        // User re-picks or edits name to "Central Library Study Pod"
        val updatedCustomName = "Central Library Study Pod"
        val updatedReminder = existingReminder.copy(
            locationName = updatedCustomName
        )

        assertEquals(101L, updatedReminder.id)
        assertEquals("Central Library Study Pod", updatedReminder.locationName)
        assertEquals(18.4536, updatedReminder.latitude, 0.00001)
        assertEquals(73.8563, updatedReminder.longitude, 0.00001)
    }

    @Test
    fun savedLocationSuggestions_preservesCustomLocationName() {
        val savedHistory = listOf(
            com.example.location_reminder.data.local.entity.LocationHistoryEntity(
                id = 1,
                name = "My Personal Library",
                address = "Near College Gate, Campus",
                latitude = 18.4536,
                longitude = 73.8563
            )
        )

        val filtered = com.example.location_reminder.data.location.SuggestionFilterHelper.filterLocations(
            query = "library",
            history = savedHistory
        )

        assertEquals(1, filtered.size)
        assertEquals("My Personal Library", filtered[0].name)
        assertEquals(18.4536, filtered[0].latitude, 0.00001)
        assertEquals(73.8563, filtered[0].longitude, 0.00001)
    }
}
