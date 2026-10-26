package com.example.location_reminder

import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepeatAndSnoozeTest {

    private lateinit var scheduler: FakeAlarmScheduler

    @Before
    fun setUp() {
        scheduler = FakeAlarmScheduler()
    }

    // 1. Repeat Interval Validation & Formatting
    @Test
    fun repeatInterval_supportedOptionsValidation() {
        val supported = AlarmScheduler.SUPPORTED_REPEAT_INTERVALS
        assertEquals(listOf(0L, 5L, 10L, 15L, 30L, 60L), supported)

        assertEquals("Off", AlarmScheduler.formatRepeatInterval(0L))
        assertEquals("5 min", AlarmScheduler.formatRepeatInterval(5L))
        assertEquals("10 min", AlarmScheduler.formatRepeatInterval(10L))
        assertEquals("15 min", AlarmScheduler.formatRepeatInterval(15L))
        assertEquals("30 min", AlarmScheduler.formatRepeatInterval(30L))
        assertEquals("1 hour", AlarmScheduler.formatRepeatInterval(60L))
        assertEquals("45 min", AlarmScheduler.formatRepeatInterval(45L))
        assertEquals("120 min", AlarmScheduler.formatRepeatInterval(120L))
    }

    // 2. Repeat Scheduling Calculation
    @Test
    fun repeatScheduling_calculatesNextTriggerCorrectly() {
        val reminder = ReminderEntity(
            id = 101L,
            title = "Medicine Reminder",
            reminderMessage = "Take vitamins",
            repeatIntervalMinutes = 15L,
            isEnabled = true,
            isCompleted = false
        )

        val triggerAt = 1_000_000_000L
        val nextRepeatTime = triggerAt + (reminder.repeatIntervalMinutes * 60 * 1000L)

        scheduler.scheduleRepeat(reminder, nextRepeatTime, isLocationReminder = false)

        assertTrue(scheduler.repeatAlarms.containsKey(101L))
        assertEquals(1_000_900_000L, scheduler.repeatAlarms[101L])
    }

    // 3. Snooze Deadline Calculation
    @Test
    fun snoozeDeadline_calculatesCorrectTimeAndPreservesOriginalDueDate() {
        val originalDueDate = System.currentTimeMillis() + 3600_000L
        val reminder = ReminderEntity(
            id = 102L,
            title = "Standup Meeting",
            reminderMessage = "Team sync",
            dueDateTime = originalDueDate,
            repeatIntervalMinutes = 10L,
            isEnabled = true,
            isCompleted = false
        )

        val beforeSnooze = System.currentTimeMillis()
        scheduler.scheduleSnooze(reminder, AlarmScheduler.DEFAULT_SNOOZE_MINUTES, isLocationReminder = false)
        val afterSnooze = System.currentTimeMillis()

        assertTrue(scheduler.snoozeAlarms.containsKey(102L))
        val scheduledSnoozeTime = scheduler.snoozeAlarms[102L]!!
        val expectedOffset = AlarmScheduler.DEFAULT_SNOOZE_MINUTES * 60 * 1000L

        assertTrue(scheduledSnoozeTime >= beforeSnooze + expectedOffset)
        assertTrue(scheduledSnoozeTime <= afterSnooze + expectedOffset)

        // Verify original due date is unchanged
        assertEquals(originalDueDate, reminder.dueDateTime)
        // Verify reminder is NOT completed
        assertFalse(reminder.isCompleted)
    }

    // 4. Duplicate Alarm Prevention & Request Code Separation
    @Test
    fun duplicateAlarmPrevention_requestCodesAreDisjointAndUnique() {
        val reminderId = 42L
        val primaryCode = reminderId.toInt()
        val repeatCode = reminderId.toInt() + AndroidAlarmScheduler.REPEAT_REQUEST_CODE_OFFSET
        val snoozeCode = reminderId.toInt() + AndroidAlarmScheduler.SNOOZE_REQUEST_CODE_OFFSET

        // Ensure all codes are distinct
        assertNotEquals(primaryCode, repeatCode)
        assertNotEquals(primaryCode, snoozeCode)
        assertNotEquals(repeatCode, snoozeCode)

        // Ensure offsets match constants
        assertEquals(42, primaryCode)
        assertEquals(300042, repeatCode)
        assertEquals(400042, snoozeCode)
    }

    // 5. Reminder Eligibility Checks Before Repeat Delivery
    @Test
    fun eligibilityCheck_disabledReminderRejectsRepeatAndSnooze() {
        val disabledReminder = ReminderEntity(
            id = 103L,
            title = "Disabled Workout",
            repeatIntervalMinutes = 10L,
            isEnabled = false,
            isCompleted = false
        )

        scheduler.scheduleRepeat(disabledReminder, System.currentTimeMillis() + 600_000L, false)
        scheduler.scheduleSnooze(disabledReminder, 10L, false)

        assertFalse(scheduler.repeatAlarms.containsKey(103L))
        assertFalse(scheduler.snoozeAlarms.containsKey(103L))
    }

    @Test
    fun eligibilityCheck_completedReminderRejectsRepeatAndSnooze() {
        val completedReminder = ReminderEntity(
            id = 104L,
            title = "Done Task",
            repeatIntervalMinutes = 10L,
            isEnabled = true,
            isCompleted = true
        )

        scheduler.scheduleRepeat(completedReminder, System.currentTimeMillis() + 600_000L, false)
        scheduler.scheduleSnooze(completedReminder, 10L, false)

        assertFalse(scheduler.repeatAlarms.containsKey(104L))
        assertFalse(scheduler.snoozeAlarms.containsKey(104L))
    }

    @Test
    fun eligibilityCheck_offRepeatIntervalRejectsRepeatSchedule() {
        val offReminder = ReminderEntity(
            id = 105L,
            title = "Single Alert Task",
            repeatIntervalMinutes = 0L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.scheduleRepeat(offReminder, System.currentTimeMillis() + 600_000L, false)
        assertFalse(scheduler.repeatAlarms.containsKey(105L))
    }

    // 6. Cancellation on Completion
    @Test
    fun cancellation_onCompletionStopsAllAlarms() {
        val reminder = ReminderEntity(
            id = 106L,
            title = "Daily Walk",
            dueDateTime = System.currentTimeMillis() + 3600_000L,
            repeatIntervalMinutes = 15L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        scheduler.scheduleRepeat(reminder, System.currentTimeMillis() + 900_000L, false)
        scheduler.scheduleSnooze(reminder, 10L, false)

        assertTrue(scheduler.scheduledAlarms.containsKey(106L))
        assertTrue(scheduler.repeatAlarms.containsKey(106L))
        assertTrue(scheduler.snoozeAlarms.containsKey(106L))

        // When user marks completed: cancel() is invoked
        scheduler.cancel(106L)

        assertFalse(scheduler.scheduledAlarms.containsKey(106L))
        assertFalse(scheduler.repeatAlarms.containsKey(106L))
        assertFalse(scheduler.snoozeAlarms.containsKey(106L))
        assertTrue(scheduler.cancelledAlarms.contains(106L))
    }

    // 7. Cancellation on Disable
    @Test
    fun cancellation_onDisableStopsAllAlarms() {
        val reminder = ReminderEntity(
            id = 107L,
            title = "Car Service",
            dueDateTime = System.currentTimeMillis() + 7200_000L,
            repeatIntervalMinutes = 30L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        scheduler.scheduleRepeat(reminder, System.currentTimeMillis() + 1800_000L, false)

        assertTrue(scheduler.scheduledAlarms.containsKey(107L))
        assertTrue(scheduler.repeatAlarms.containsKey(107L))

        // When toggled to disabled: cancel() is called
        scheduler.cancel(107L)

        assertFalse(scheduler.scheduledAlarms.containsKey(107L))
        assertFalse(scheduler.repeatAlarms.containsKey(107L))
        assertTrue(scheduler.cancelledAlarms.contains(107L))
    }

    // 8. Cancellation on Deletion
    @Test
    fun cancellation_onDeleteStopsAllAlarms() {
        val reminder = ReminderEntity(
            id = 108L,
            title = "Deleted Project",
            dueDateTime = System.currentTimeMillis() + 7200_000L,
            repeatIntervalMinutes = 5L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        scheduler.scheduleRepeat(reminder, System.currentTimeMillis() + 300_000L, false)
        scheduler.scheduleSnooze(reminder, 10L, false)

        scheduler.cancel(108L)

        assertFalse(scheduler.scheduledAlarms.containsKey(108L))
        assertFalse(scheduler.repeatAlarms.containsKey(108L))
        assertFalse(scheduler.snoozeAlarms.containsKey(108L))
        assertTrue(scheduler.cancelledAlarms.contains(108L))
    }

    // 9. Editing Repeat Settings
    @Test
    fun editingRepeatSettings_updatesIntervalSafely() {
        val reminder = ReminderEntity(
            id = 109L,
            title = "Water Plants",
            dueDateTime = System.currentTimeMillis() + 3600_000L,
            repeatIntervalMinutes = 5L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.scheduleRepeat(reminder, 1000L, false)
        assertEquals(1000L, scheduler.repeatAlarms[109L])

        // User edits repeat from 5m to 30m
        val updatedReminder = reminder.copy(repeatIntervalMinutes = 30L)
        scheduler.cancelRepeat(109L)
        assertFalse(scheduler.repeatAlarms.containsKey(109L))

        scheduler.scheduleRepeat(updatedReminder, 5000L, false)
        assertEquals(5000L, scheduler.repeatAlarms[109L])

        // User disables repeat (0L)
        val disabledRepeatReminder = reminder.copy(repeatIntervalMinutes = 0L)
        scheduler.cancelRepeat(109L)
        scheduler.scheduleRepeat(disabledRepeatReminder, 10000L, false)
        assertFalse(scheduler.repeatAlarms.containsKey(109L))
    }

    // 10. Snooze Not Marking Reminder Completed
    @Test
    fun snoozeAction_preservesIncompleteStateAndOriginalData() {
        val originalDue = System.currentTimeMillis() + 1800_000L
        val originalTitle = "Tax Filing"
        val reminder = ReminderEntity(
            id = 110L,
            title = originalTitle,
            reminderMessage = "Gather W2 forms",
            dueDateTime = originalDue,
            repeatIntervalMinutes = 10L,
            snoozedUntil = null,
            isEnabled = true,
            isCompleted = false
        )

        // Simulate Snooze action:
        val snoozeDuration = 10L
        val snoozeDeadline = System.currentTimeMillis() + (snoozeDuration * 60000L)
        val snoozedReminder = reminder.copy(snoozedUntil = snoozeDeadline)

        // Assertions
        assertFalse(snoozedReminder.isCompleted)
        assertTrue(snoozedReminder.isEnabled)
        assertEquals(originalDue, snoozedReminder.dueDateTime)
        assertEquals(originalTitle, snoozedReminder.title)
        assertNotNull(snoozedReminder.snoozedUntil)
        assertTrue(snoozedReminder.snoozedUntil!! > System.currentTimeMillis())
    }

    // 11. Separate Reminders Maintain Independent Schedules
    @Test
    fun independentSchedules_modifyingOneDoesNotAffectAnother() {
        val reminder1 = ReminderEntity(
            id = 201L,
            title = "Reminder 1",
            dueDateTime = System.currentTimeMillis() + 3600_000L,
            repeatIntervalMinutes = 5L,
            isEnabled = true,
            isCompleted = false
        )
        val reminder2 = ReminderEntity(
            id = 202L,
            title = "Reminder 2",
            dueDateTime = System.currentTimeMillis() + 7200_000L,
            repeatIntervalMinutes = 30L,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder1)
        scheduler.schedule(reminder2)
        scheduler.scheduleRepeat(reminder1, 2000L, false)
        scheduler.scheduleRepeat(reminder2, 4000L, false)
        scheduler.scheduleSnooze(reminder1, 10L, false)

        // Cancel Reminder 1 (e.g. marked completed or deleted)
        scheduler.cancel(201L)

        // Reminder 1 schedules are gone
        assertFalse(scheduler.scheduledAlarms.containsKey(201L))
        assertFalse(scheduler.repeatAlarms.containsKey(201L))
        assertFalse(scheduler.snoozeAlarms.containsKey(201L))

        // Reminder 2 schedules remain intact!
        assertTrue(scheduler.scheduledAlarms.containsKey(202L))
        assertTrue(scheduler.repeatAlarms.containsKey(202L))
        assertEquals(4000L, scheduler.repeatAlarms[202L])
    }
}
