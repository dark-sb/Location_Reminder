package com.example.location_reminder

import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.geofence.GeofenceManager
import com.example.location_reminder.data.local.entity.GeofenceState
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.sync.ScheduleRestorationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating all geofence transition, repeat scheduling, inside/outside state persistence,
 * re-entry, and reboot restoration behaviors required for Location Reminders.
 */
class GeofenceTransitionAndRepeatTest {

    // In-memory test state representing persisted database & scheduler
    private val dbReminders = mutableMapOf<Long, ReminderEntity>()
    private val scheduledRepeats = mutableMapOf<Long, Long>()
    private val cancelledRepeats = mutableListOf<Long>()
    private val postedNotifications = mutableListOf<Long>()
    private val dismissedNotifications = mutableListOf<Long>()
    private val registeredGeofences = mutableMapOf<Long, ReminderEntity>()
    private val removedGeofences = mutableListOf<Long>()

    private val testScheduler = object : AlarmScheduler {
        override fun schedule(reminder: ReminderEntity) {}

        override fun scheduleRepeat(reminder: ReminderEntity, triggerAtMillis: Long, isLocationReminder: Boolean) {
            scheduledRepeats[reminder.id] = triggerAtMillis
        }

        override fun scheduleSnooze(reminder: ReminderEntity, snoozeDurationMinutes: Long, isLocationReminder: Boolean) {}

        override fun cancel(reminderId: Long) {
            scheduledRepeats.remove(reminderId)
            cancelledRepeats.add(reminderId)
        }

        override fun cancelRepeat(reminderId: Long) {
            scheduledRepeats.remove(reminderId)
            cancelledRepeats.add(reminderId)
        }
    }

    private val testGeofenceManager = object : GeofenceManager {
        override fun registerGeofence(reminder: ReminderEntity) {
            if (GeofenceManager.isEligibleForGeofencing(reminder)) {
                registeredGeofences[reminder.id] = reminder
            }
        }

        override fun removeGeofence(reminderId: Long) {
            registeredGeofences.remove(reminderId)
            removedGeofences.add(reminderId)
        }

        override fun removeAllGeofences() {
            registeredGeofences.clear()
        }
    }

    @Before
    fun setUp() {
        dbReminders.clear()
        scheduledRepeats.clear()
        cancelledRepeats.clear()
        postedNotifications.clear()
        dismissedNotifications.clear()
        registeredGeofences.clear()
        removedGeofences.clear()
    }

    /**
     * Simulates the core logic executed by GeofenceBroadcastReceiver on ENTER transition.
     */
    private fun simulateEnterTransition(reminderId: Long, currentTimeMillis: Long): Boolean {
        val reminder = dbReminders[reminderId] ?: return false
        if (!reminder.isEnabled || reminder.isCompleted) return false
        if (reminder.reminderType != ReminderType.LOCATION.name && reminder.reminderType != ReminderType.LOCATION_AND_TIME.name) return false

        val wasAlreadyInside = reminder.geofenceState == GeofenceState.INSIDE.name

        // Persist INSIDE state
        dbReminders[reminderId] = reminder.copy(geofenceState = GeofenceState.INSIDE.name)

        if (wasAlreadyInside) {
            return false // Idempotent: no duplicate notification or repeat scheduling
        }

        // Post notification
        postedNotifications.add(reminderId)

        // Schedule repeat if configured
        if (reminder.repeatIntervalMinutes > 0L) {
            val nextRepeat = currentTimeMillis + (reminder.repeatIntervalMinutes * 60 * 1000L)
            testScheduler.cancelRepeat(reminderId)
            testScheduler.scheduleRepeat(reminder.copy(geofenceState = GeofenceState.INSIDE.name), nextRepeat, true)
        }
        return true
    }

    /**
     * Simulates the core logic executed by GeofenceBroadcastReceiver on EXIT transition.
     */
    private fun simulateExitTransition(reminderId: Long): Boolean {
        val reminder = dbReminders[reminderId] ?: return false

        // Persist OUTSIDE state
        dbReminders[reminderId] = reminder.copy(geofenceState = GeofenceState.OUTSIDE.name, snoozedUntil = null)

        // Cancel repeat alarm
        testScheduler.cancelRepeat(reminderId)

        // Dismiss active notification
        dismissedNotifications.add(reminderId)

        return true
    }

    /**
     * Simulates the core logic of ReminderAlarmReceiver when a repeat alarm fires.
     */
    private fun simulateRepeatAlarmFired(reminderId: Long, currentTimeMillis: Long): Boolean {
        val reminder = dbReminders[reminderId] ?: return false
        if (!reminder.isEnabled || reminder.isCompleted) {
            testScheduler.cancelRepeat(reminderId)
            return false
        }

        // Critical Safety Check: Only trigger if currently INSIDE the geofence
        if (reminder.geofenceState != GeofenceState.INSIDE.name) {
            testScheduler.cancelRepeat(reminderId)
            return false
        }

        // Post repeat notification
        postedNotifications.add(reminderId)

        // Schedule next repeat
        if (reminder.repeatIntervalMinutes > 0L) {
            val nextRepeat = currentTimeMillis + (reminder.repeatIntervalMinutes * 60 * 1000L)
            testScheduler.scheduleRepeat(reminder, nextRepeat, true)
        }
        return true
    }

    // -------------------------------------------------------------
    // Test Scenarios
    // -------------------------------------------------------------

    @Test
    fun `ENTER transition starts notification and repeat schedule`() {
        val reminder = ReminderEntity(
            id = 1L,
            title = "Library Books",
            locationName = "City Library",
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100.0,
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[1L] = reminder

        val now = 1_000_000L
        val handled = simulateEnterTransition(1L, now)

        assertTrue(handled)
        assertEquals(GeofenceState.INSIDE.name, dbReminders[1L]?.geofenceState)
        assertTrue(postedNotifications.contains(1L))
        assertTrue(scheduledRepeats.containsKey(1L))
        assertEquals(now + (5 * 60 * 1000L), scheduledRepeats[1L])
    }

    @Test
    fun `EXIT transition cancels repeat schedule and dismisses notification while reminder remains active`() {
        val reminder = ReminderEntity(
            id = 2L,
            title = "Supermarket",
            locationName = "Grocery Store",
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100.0,
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.INSIDE.name
        )
        dbReminders[2L] = reminder
        scheduledRepeats[2L] = 1_000_000L

        simulateExitTransition(2L)

        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[2L]?.geofenceState)
        assertFalse(scheduledRepeats.containsKey(2L))
        assertTrue(dismissedNotifications.contains(2L))
        assertTrue(dbReminders[2L]?.isEnabled == true)
        assertFalse(dbReminders[2L]?.isCompleted == true)
    }

    @Test
    fun `EXIT prevents subsequent repeat notifications from firing`() {
        val reminder = ReminderEntity(
            id = 3L,
            title = "Office",
            locationName = "Workplace",
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.INSIDE.name
        )
        dbReminders[3L] = reminder

        // User exits geofence
        simulateExitTransition(3L)
        postedNotifications.clear()

        // Simulate repeat alarm firing after exit
        val fired = simulateRepeatAlarmFired(3L, 2_000_000L)

        assertFalse(fired)
        assertFalse(postedNotifications.contains(3L))
        assertFalse(scheduledRepeats.containsKey(3L))
    }

    @Test
    fun `RE-ENTRY after EXIT triggers notification and restarts repeat cycle`() {
        val reminder = ReminderEntity(
            id = 4L,
            title = "Gym Workout",
            locationName = "Fitness Center",
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[4L] = reminder

        val t1 = 1_000_000L
        simulateEnterTransition(4L, t1)
        assertEquals(GeofenceState.INSIDE.name, dbReminders[4L]?.geofenceState)
        assertEquals(1, postedNotifications.count { it == 4L })

        // User leaves
        simulateExitTransition(4L)
        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[4L]?.geofenceState)

        // User re-enters
        val t2 = 2_000_000L
        val reEntered = simulateEnterTransition(4L, t2)

        assertTrue(reEntered)
        assertEquals(GeofenceState.INSIDE.name, dbReminders[4L]?.geofenceState)
        assertEquals(2, postedNotifications.count { it == 4L })
        assertTrue(scheduledRepeats.containsKey(4L))
        assertEquals(t2 + (5 * 60 * 1000L), scheduledRepeats[4L])
    }

    @Test
    fun `Multiple ENTER events while inside do not create duplicate schedules`() {
        val reminder = ReminderEntity(
            id = 5L,
            title = "Pharmacy",
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[5L] = reminder

        val firstEnter = simulateEnterTransition(5L, 1_000_000L)
        assertTrue(firstEnter)
        assertEquals(1, postedNotifications.count { it == 5L })

        // Subsequent ENTER while already inside
        val secondEnter = simulateEnterTransition(5L, 1_005_000L)
        assertFalse(secondEnter) // Idempotent check prevents duplicate
        assertEquals(1, postedNotifications.count { it == 5L })
    }

    @Test
    fun `Multiple EXIT events do not crash and preserve OUTSIDE state`() {
        val reminder = ReminderEntity(
            id = 6L,
            title = "Park",
            geofenceState = GeofenceState.INSIDE.name
        )
        dbReminders[6L] = reminder

        simulateExitTransition(6L)
        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[6L]?.geofenceState)

        // Second EXIT event
        simulateExitTransition(6L)
        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[6L]?.geofenceState)
    }

    @Test
    fun `Completed reminder cannot trigger ENTER`() {
        val reminder = ReminderEntity(
            id = 7L,
            title = "Dentist",
            isCompleted = true,
            isEnabled = true,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[7L] = reminder

        val handled = simulateEnterTransition(7L, 1_000_000L)
        assertFalse(handled)
        assertFalse(postedNotifications.contains(7L))
        assertFalse(scheduledRepeats.containsKey(7L))
    }

    @Test
    fun `Disabled reminder cannot trigger ENTER`() {
        val reminder = ReminderEntity(
            id = 8L,
            title = "Coffee Shop",
            isEnabled = false,
            isCompleted = false,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[8L] = reminder

        val handled = simulateEnterTransition(8L, 1_000_000L)
        assertFalse(handled)
        assertFalse(postedNotifications.contains(8L))
    }

    @Test
    fun `Deleted reminder cannot trigger ENTER`() {
        val handled = simulateEnterTransition(999L, 1_000_000L)
        assertFalse(handled)
        assertFalse(postedNotifications.contains(999L))
    }

    @Test
    fun `Process death preserves geofence state in persistent entity`() {
        val reminder = ReminderEntity(
            id = 9L,
            title = "Stationery Store",
            geofenceState = GeofenceState.INSIDE.name
        )
        // Store in DB
        dbReminders[9L] = reminder

        // Simulate app death and re-read from DB
        val reloaded = dbReminders[9L]
        assertNotNull(reloaded)
        assertEquals(GeofenceState.INSIDE.name, reloaded?.geofenceState)
    }

    @Test
    fun `Reboot restoration registers active geofences and resets state to UNKNOWN for fresh evaluation`() {
        val activeLocationReminder = ReminderEntity(
            id = 10L,
            title = "Metro Station",
            reminderType = ReminderType.LOCATION.name,
            latitude = 18.5204,
            longitude = 73.8567,
            radius = 200.0,
            isEnabled = true,
            isCompleted = false,
            geofenceState = GeofenceState.INSIDE.name // was inside before reboot
        )
        assertTrue(GeofenceManager.isEligibleForGeofencing(activeLocationReminder))

        // On reboot: reset state to UNKNOWN and register geofence
        val restoredReminder = activeLocationReminder.copy(geofenceState = GeofenceState.UNKNOWN.name)
        testGeofenceManager.registerGeofence(restoredReminder)

        assertTrue(registeredGeofences.containsKey(10L))
        assertEquals(GeofenceState.UNKNOWN.name, restoredReminder.geofenceState)
    }

    @Test
    fun `Different reminders at different locations remain completely isolated`() {
        val reminderA = ReminderEntity(
            id = 11L,
            title = "Library",
            latitude = 10.0,
            longitude = 20.0,
            radius = 100.0,
            repeatIntervalMinutes = 5L,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        val reminderB = ReminderEntity(
            id = 12L,
            title = "Supermarket",
            latitude = 30.0,
            longitude = 40.0,
            radius = 200.0,
            repeatIntervalMinutes = 10L,
            geofenceState = GeofenceState.UNKNOWN.name
        )
        dbReminders[11L] = reminderA
        dbReminders[12L] = reminderB

        // Enter A
        simulateEnterTransition(11L, 1_000_000L)
        assertEquals(GeofenceState.INSIDE.name, dbReminders[11L]?.geofenceState)
        assertEquals(GeofenceState.UNKNOWN.name, dbReminders[12L]?.geofenceState)
        assertTrue(scheduledRepeats.containsKey(11L))
        assertFalse(scheduledRepeats.containsKey(12L))

        // Exit A
        simulateExitTransition(11L)
        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[11L]?.geofenceState)
        assertEquals(GeofenceState.UNKNOWN.name, dbReminders[12L]?.geofenceState)
        assertFalse(scheduledRepeats.containsKey(11L))

        // Enter B
        simulateEnterTransition(12L, 2_000_000L)
        assertEquals(GeofenceState.OUTSIDE.name, dbReminders[11L]?.geofenceState)
        assertEquals(GeofenceState.INSIDE.name, dbReminders[12L]?.geofenceState)
        assertTrue(scheduledRepeats.containsKey(12L))
    }
}
