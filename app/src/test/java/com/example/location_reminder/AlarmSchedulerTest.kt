package com.example.location_reminder

import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAlarmScheduler : AlarmScheduler {
    val scheduledAlarms = mutableMapOf<Long, ReminderEntity>()
    val cancelledAlarms = mutableListOf<Long>()

    override fun schedule(reminder: ReminderEntity) {
        val dueTime = reminder.dueDateTime
        // Mirror scheduling rules
        if (dueTime == null || dueTime <= System.currentTimeMillis()) {
            return
        }
        if (!reminder.isEnabled || reminder.isCompleted) {
            return
        }
        scheduledAlarms[reminder.id] = reminder
    }

    val repeatAlarms = mutableMapOf<Long, Long>() // reminderId -> triggerAtMillis
    val snoozeAlarms = mutableMapOf<Long, Long>() // reminderId -> triggerAtMillis

    override fun scheduleRepeat(reminder: ReminderEntity, triggerAtMillis: Long, isLocationReminder: Boolean) {
        if (!reminder.isEnabled || reminder.isCompleted || reminder.repeatIntervalMinutes <= 0L) {
            return
        }
        repeatAlarms[reminder.id] = triggerAtMillis
    }

    override fun scheduleSnooze(reminder: ReminderEntity, snoozeDurationMinutes: Long, isLocationReminder: Boolean) {
        if (!reminder.isEnabled || reminder.isCompleted) {
            return
        }
        snoozeAlarms[reminder.id] = System.currentTimeMillis() + (snoozeDurationMinutes * 60000L)
    }

    override fun cancelRepeat(reminderId: Long) {
        repeatAlarms.remove(reminderId)
    }

    override fun cancel(reminderId: Long) {
        scheduledAlarms.remove(reminderId)
        repeatAlarms.remove(reminderId)
        snoozeAlarms.remove(reminderId)
        cancelledAlarms.add(reminderId)
    }
}

class AlarmSchedulerTest {

    private lateinit var scheduler: FakeAlarmScheduler

    @Before
    fun setUp() {
        scheduler = FakeAlarmScheduler()
    }

    @Test
    fun schedule_futureActiveReminder_schedulesAlarmSuccessfully() {
        val futureTime = System.currentTimeMillis() + 3600000L // 1 hour from now
        val reminder = ReminderEntity(
            id = 1L,
            title = "Dentist Appointment",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = futureTime,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)

        assertTrue(scheduler.scheduledAlarms.containsKey(1L))
        assertEquals(futureTime, scheduler.scheduledAlarms[1L]?.dueDateTime)
        assertEquals(0, scheduler.cancelledAlarms.size)
    }

    @Test
    fun schedule_pastTimeReminder_skipsScheduling() {
        val pastTime = System.currentTimeMillis() - 60000L // 1 minute ago
        val reminder = ReminderEntity(
            id = 2L,
            title = "Past Meeting",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = pastTime,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)

        assertFalse(scheduler.scheduledAlarms.containsKey(2L))
        assertNull(scheduler.scheduledAlarms[2L])
    }

    @Test
    fun schedule_disabledReminder_skipsScheduling() {
        val futureTime = System.currentTimeMillis() + 7200000L
        val reminder = ReminderEntity(
            id = 3L,
            title = "Disabled Workout",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = futureTime,
            isEnabled = false,
            isCompleted = false
        )

        scheduler.schedule(reminder)

        assertFalse(scheduler.scheduledAlarms.containsKey(3L))
    }

    @Test
    fun schedule_completedReminder_skipsScheduling() {
        val futureTime = System.currentTimeMillis() + 7200000L
        val reminder = ReminderEntity(
            id = 4L,
            title = "Already Completed",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = futureTime,
            isEnabled = true,
            isCompleted = true
        )

        scheduler.schedule(reminder)

        assertFalse(scheduler.scheduledAlarms.containsKey(4L))
    }

    @Test
    fun cancel_cancelsAndRemovesScheduledAlarm() {
        val futureTime = System.currentTimeMillis() + 3600000L
        val reminder = ReminderEntity(
            id = 5L,
            title = "Flight Check-in",
            dueDateTime = futureTime,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        assertTrue(scheduler.scheduledAlarms.containsKey(5L))

        scheduler.cancel(5L)
        assertFalse(scheduler.scheduledAlarms.containsKey(5L))
        assertTrue(scheduler.cancelledAlarms.contains(5L))
    }

    @Test
    fun updateReminder_updatesScheduledTime() {
        val initialTime = System.currentTimeMillis() + 3600000L
        val reminder = ReminderEntity(
            id = 6L,
            title = "Conference Call",
            dueDateTime = initialTime,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        assertEquals(initialTime, scheduler.scheduledAlarms[6L]?.dueDateTime)

        // Reschedule to a new time
        val updatedTime = System.currentTimeMillis() + 86400000L
        val updatedReminder = reminder.copy(dueDateTime = updatedTime)
        scheduler.schedule(updatedReminder)

        assertEquals(updatedTime, scheduler.scheduledAlarms[6L]?.dueDateTime)
        assertEquals(1, scheduler.scheduledAlarms.size) // No duplicate instances
    }

    @Test
    fun completeReminder_cancelsAlarm() {
        val futureTime = System.currentTimeMillis() + 3600000L
        val reminder = ReminderEntity(
            id = 7L,
            title = "Submit Report",
            dueDateTime = futureTime,
            isEnabled = true,
            isCompleted = false
        )

        scheduler.schedule(reminder)
        assertTrue(scheduler.scheduledAlarms.containsKey(7L))

        // When reminder is completed: cancel is called
        scheduler.cancel(7L)
        assertFalse(scheduler.scheduledAlarms.containsKey(7L))
        assertTrue(scheduler.cancelledAlarms.contains(7L))
    }
}
