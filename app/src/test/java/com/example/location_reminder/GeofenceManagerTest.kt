package com.example.location_reminder

import com.example.location_reminder.data.geofence.GeofenceManager
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGeofenceManager : GeofenceManager {
    val registeredGeofences = mutableMapOf<Long, ReminderEntity>()
    val removedGeofences = mutableListOf<Long>()

    override fun registerGeofence(reminder: ReminderEntity) {
        if (!GeofenceManager.isEligibleForGeofencing(reminder)) {
            removeGeofence(reminder.id)
            return
        }
        registeredGeofences[reminder.id] = reminder
    }

    override fun removeGeofence(reminderId: Long) {
        registeredGeofences.remove(reminderId)
        removedGeofences.add(reminderId)
    }

    override fun removeAllGeofences() {
        registeredGeofences.clear()
    }
}

class GeofenceManagerTest {

    private lateinit var geofenceManager: FakeGeofenceManager

    @Before
    fun setUp() {
        geofenceManager = FakeGeofenceManager()
    }

    // --- Eligibility Tests ---

    @Test
    fun isEligibleForGeofencing_activeLocationReminder_returnsTrue() {
        val reminder = ReminderEntity(
            id = 1L,
            title = "Supermarket Grocery",
            reminderType = ReminderType.LOCATION.name,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 150.0,
            isEnabled = true,
            isCompleted = false
        )

        assertTrue(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_activeLocationAndTimeReminder_returnsTrue() {
        val reminder = ReminderEntity(
            id = 2L,
            title = "Doctor Appointment at Clinic",
            reminderType = ReminderType.LOCATION_AND_TIME.name,
            latitude = 28.6139,
            longitude = 77.2090,
            radius = 100.0,
            dueDateTime = System.currentTimeMillis() + 3600000L,
            isEnabled = true,
            isCompleted = false
        )

        assertTrue(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_dateTimeOnlyReminder_returnsFalse() {
        val reminder = ReminderEntity(
            id = 3L,
            title = "Call Accountant",
            reminderType = ReminderType.DATE_TIME.name,
            latitude = 28.6139,
            longitude = 77.2090,
            radius = 100.0,
            isEnabled = true,
            isCompleted = false
        )

        assertFalse(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_disabledLocationReminder_returnsFalse() {
        val reminder = ReminderEntity(
            id = 4L,
            title = "Pharmacy",
            reminderType = ReminderType.LOCATION.name,
            latitude = 40.7128,
            longitude = -74.0060,
            radius = 100.0,
            isEnabled = false,
            isCompleted = false
        )

        assertFalse(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_completedLocationReminder_returnsFalse() {
        val reminder = ReminderEntity(
            id = 5L,
            title = "Post Office",
            reminderType = ReminderType.LOCATION.name,
            latitude = 51.5074,
            longitude = -0.1278,
            radius = 100.0,
            isEnabled = true,
            isCompleted = true
        )

        assertFalse(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_zeroCoordinates_returnsFalse() {
        val reminder = ReminderEntity(
            id = 6L,
            title = "Invalid Place",
            reminderType = ReminderType.LOCATION.name,
            latitude = 0.0,
            longitude = 0.0,
            radius = 100.0,
            isEnabled = true,
            isCompleted = false
        )

        assertFalse(GeofenceManager.isEligibleForGeofencing(reminder))
    }

    @Test
    fun isEligibleForGeofencing_outOfBoundsCoordinates_returnsFalse() {
        val invalidLat = ReminderEntity(
            id = 7L,
            title = "North of Pole",
            reminderType = ReminderType.LOCATION.name,
            latitude = 95.0,
            longitude = 10.0,
            radius = 100.0,
            isEnabled = true,
            isCompleted = false
        )
        assertFalse(GeofenceManager.isEligibleForGeofencing(invalidLat))

        val invalidLng = ReminderEntity(
            id = 8L,
            title = "Beyond Date Line",
            reminderType = ReminderType.LOCATION.name,
            latitude = 10.0,
            longitude = 185.0,
            radius = 100.0,
            isEnabled = true,
            isCompleted = false
        )
        assertFalse(GeofenceManager.isEligibleForGeofencing(invalidLng))
    }

    @Test
    fun isEligibleForGeofencing_zeroOrNegativeRadius_returnsFalse() {
        val zeroRadius = ReminderEntity(
            id = 9L,
            title = "Zero Radius Point",
            reminderType = ReminderType.LOCATION.name,
            latitude = 34.0522,
            longitude = -118.2437,
            radius = 0.0,
            isEnabled = true,
            isCompleted = false
        )
        assertFalse(GeofenceManager.isEligibleForGeofencing(zeroRadius))

        val negativeRadius = ReminderEntity(
            id = 10L,
            title = "Negative Radius Point",
            reminderType = ReminderType.LOCATION.name,
            latitude = 34.0522,
            longitude = -118.2437,
            radius = -50.0,
            isEnabled = true,
            isCompleted = false
        )
        assertFalse(GeofenceManager.isEligibleForGeofencing(negativeRadius))
    }

    // --- Geofence ID Helpers ---

    @Test
    fun getGeofenceRequestId_formatsCorrectly() {
        val requestId = GeofenceManager.getGeofenceRequestId(42L)
        assertEquals("geofence_reminder_42", requestId)
    }

    @Test
    fun extractReminderId_validPrefix_extractsLong() {
        val id = GeofenceManager.extractReminderId("geofence_reminder_42")
        assertEquals(42L, id)
    }

    @Test
    fun extractReminderId_plainNumeric_extractsLong() {
        val id = GeofenceManager.extractReminderId("99")
        assertEquals(99L, id)
    }

    @Test
    fun extractReminderId_malformedString_returnsNull() {
        val id = GeofenceManager.extractReminderId("invalid_geofence_id")
        assertNull(id)
    }

    // --- Geofence Lifecycle Management ---

    @Test
    fun registerGeofence_eligibleReminder_storedInManager() {
        val reminder = ReminderEntity(
            id = 101L,
            title = "Office",
            reminderType = ReminderType.LOCATION.name,
            latitude = 12.9716,
            longitude = 77.5946,
            radius = 200.0,
            isEnabled = true,
            isCompleted = false
        )

        geofenceManager.registerGeofence(reminder)

        assertTrue(geofenceManager.registeredGeofences.containsKey(101L))
        assertEquals("Office", geofenceManager.registeredGeofences[101L]?.title)
    }

    @Test
    fun registerGeofence_ineligibleReminder_removesFromManager() {
        val active = ReminderEntity(
            id = 102L,
            title = "Gym",
            reminderType = ReminderType.LOCATION.name,
            latitude = 12.9716,
            longitude = 77.5946,
            radius = 100.0,
            isEnabled = true,
            isCompleted = false
        )
        geofenceManager.registerGeofence(active)
        assertTrue(geofenceManager.registeredGeofences.containsKey(102L))

        // Now update it to completed
        val completed = active.copy(isCompleted = true)
        geofenceManager.registerGeofence(completed)

        assertFalse(geofenceManager.registeredGeofences.containsKey(102L))
        assertTrue(geofenceManager.removedGeofences.contains(102L))
    }

    @Test
    fun removeGeofence_removesSpecificReminderGeofence() {
        val reminder1 = ReminderEntity(id = 201L, title = "A", reminderType = ReminderType.LOCATION.name, latitude = 10.0, longitude = 20.0, radius = 50.0, isEnabled = true)
        val reminder2 = ReminderEntity(id = 202L, title = "B", reminderType = ReminderType.LOCATION.name, latitude = 10.0, longitude = 20.0, radius = 50.0, isEnabled = true)

        geofenceManager.registerGeofence(reminder1)
        geofenceManager.registerGeofence(reminder2)

        geofenceManager.removeGeofence(201L)

        assertFalse(geofenceManager.registeredGeofences.containsKey(201L))
        assertTrue(geofenceManager.registeredGeofences.containsKey(202L))
        assertTrue(geofenceManager.removedGeofences.contains(201L))
    }

    @Test
    fun multipleRemindersAtSameLocation_haveDistinctRequestIds() {
        val reqId1 = GeofenceManager.getGeofenceRequestId(301L)
        val reqId2 = GeofenceManager.getGeofenceRequestId(302L)

        // Multiple reminders at identical coordinates must not collide
        org.junit.Assert.assertNotEquals(reqId1, reqId2)
        assertEquals(301L, GeofenceManager.extractReminderId(reqId1))
        assertEquals(302L, GeofenceManager.extractReminderId(reqId2))
    }
}
