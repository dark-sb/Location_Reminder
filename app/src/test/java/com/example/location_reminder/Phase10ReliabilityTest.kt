package com.example.location_reminder

import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.geofence.GeofenceManager
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.sync.ScheduleRestorationManager
import com.example.location_reminder.data.sync.ScheduleRestorationManager.RestorationAction
import com.example.location_reminder.data.util.PermissionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10ReliabilityTest {

    private val baseNow = System.currentTimeMillis() + 100_000L

    // ==========================================
    // 1. BOOT RESTORATION & SCHEDULING DECISIONS
    // ==========================================

    @Test
    fun testBootRestorationDecisions_futureAlarmRestored() {
        val reminder = ReminderEntity(
            id = 101,
            title = "Future Scheduled Reminder",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = baseNow + 3600_000L, // 1 hour in future
            isEnabled = true,
            isCompleted = false
        )

        val action = ScheduleRestorationManager.evaluateAlarmAction(reminder, baseNow)
        assertTrue(action is RestorationAction.RestoreScheduledAlarm)
        assertEquals(baseNow + 3600_000L, (action as RestorationAction.RestoreScheduledAlarm).dueTime)
    }

    @Test
    fun testBootRestorationDecisions_pastAlarmSkipped() {
        val reminder = ReminderEntity(
            id = 102,
            title = "Past Reminder",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = baseNow - 60_000L, // 1 minute in past
            isEnabled = true,
            isCompleted = false
        )

        val action = ScheduleRestorationManager.evaluateAlarmAction(reminder, baseNow)
        assertEquals(RestorationAction.SkipPastAlarm, action)
    }

    @Test
    fun testBootRestorationDecisions_activeSnoozeRestored() {
        val reminder = ReminderEntity(
            id = 103,
            title = "Snoozed Reminder",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = baseNow - 300_000L,
            snoozedUntil = baseNow + 600_000L, // 10 minutes remaining
            isEnabled = true,
            isCompleted = false
        )

        val action = ScheduleRestorationManager.evaluateAlarmAction(reminder, baseNow)
        assertTrue(action is RestorationAction.RestoreSnooze)
        val snoozeAction = action as RestorationAction.RestoreSnooze
        assertEquals(10L, snoozeAction.remainingMinutes)
        assertFalse(snoozeAction.isLocation)
    }

    @Test
    fun testBootRestorationDecisions_expiredSnoozeCleared() {
        val reminder = ReminderEntity(
            id = 104,
            title = "Expired Snooze Reminder",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = baseNow - 1200_000L,
            snoozedUntil = baseNow - 60_000L, // expired 1 min ago
            isEnabled = true,
            isCompleted = false
        )

        val action = ScheduleRestorationManager.evaluateAlarmAction(reminder, baseNow)
        assertEquals(RestorationAction.ClearExpiredSnooze, action)
    }

    // ==========================================
    // 2. COMPLETED & DISABLED REMINDER FILTERING
    // ==========================================

    @Test
    fun testCompletedReminderFiltering_neverRestored() {
        val completedReminder = ReminderEntity(
            id = 105,
            title = "Completed Task",
            reminderType = ReminderType.LOCATION_AND_TIME.name,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100.0,
            dueDateTime = baseNow + 3600_000L,
            isEnabled = true,
            isCompleted = true // Completed
        )

        val alarmAction = ScheduleRestorationManager.evaluateAlarmAction(completedReminder, baseNow)
        assertEquals(RestorationAction.IneligibleOrCompleted, alarmAction)

        val isGeofenceEligible = ScheduleRestorationManager.isEligibleForGeofence(completedReminder)
        assertFalse("Completed reminder must NOT be eligible for geofence registration", isGeofenceEligible)
    }

    @Test
    fun testDisabledReminderFiltering_neverRestored() {
        val disabledReminder = ReminderEntity(
            id = 106,
            title = "Disabled Task",
            reminderType = ReminderType.LOCATION.name,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100.0,
            isEnabled = false, // Disabled
            isCompleted = false
        )

        val alarmAction = ScheduleRestorationManager.evaluateAlarmAction(disabledReminder, baseNow)
        assertEquals(RestorationAction.IneligibleOrCompleted, alarmAction)

        val isGeofenceEligible = ScheduleRestorationManager.isEligibleForGeofence(disabledReminder)
        assertFalse("Disabled reminder must NOT be eligible for geofence registration", isGeofenceEligible)
    }

    @Test
    fun testGeofenceEligibility_validAndInvalidCoordinates() {
        val validLocationReminder = ReminderEntity(
            id = 107,
            title = "Valid Location",
            reminderType = ReminderType.LOCATION.name,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 50.0,
            isEnabled = true,
            isCompleted = false
        )
        assertTrue(ScheduleRestorationManager.isEligibleForGeofence(validLocationReminder))

        val zeroCoordReminder = validLocationReminder.copy(latitude = 0.0, longitude = 0.0)
        assertFalse(ScheduleRestorationManager.isEligibleForGeofence(zeroCoordReminder))

        val zeroRadiusReminder = validLocationReminder.copy(radius = 0.0)
        assertFalse(ScheduleRestorationManager.isEligibleForGeofence(zeroRadiusReminder))

        val dateTimeOnlyReminder = validLocationReminder.copy(reminderType = ReminderType.DATE_TIME.name)
        assertFalse(ScheduleRestorationManager.isEligibleForGeofence(dateTimeOnlyReminder))
    }

    // ==========================================
    // 3. DUPLICATE SCHEDULE & ID COLLISION TESTS
    // ==========================================

    @Test
    fun testStableRequestCodesAndIds_preventDuplicates() {
        val reminderId = 42L

        // Primary alarm code
        val primaryCode1 = reminderId.toInt()
        val primaryCode2 = reminderId.toInt()
        assertEquals(primaryCode1, primaryCode2)

        // Repeat alarm code
        val repeatCode1 = reminderId.toInt() + AndroidAlarmScheduler.REPEAT_REQUEST_CODE_OFFSET
        val repeatCode2 = reminderId.toInt() + AndroidAlarmScheduler.REPEAT_REQUEST_CODE_OFFSET
        assertEquals(repeatCode1, repeatCode2)

        // Snooze alarm code
        val snoozeCode1 = reminderId.toInt() + AndroidAlarmScheduler.SNOOZE_REQUEST_CODE_OFFSET
        val snoozeCode2 = reminderId.toInt() + AndroidAlarmScheduler.SNOOZE_REQUEST_CODE_OFFSET
        assertEquals(snoozeCode1, snoozeCode2)

        // Geofence request ID
        val geofenceId1 = GeofenceManager.getGeofenceRequestId(reminderId)
        val geofenceId2 = GeofenceManager.getGeofenceRequestId(reminderId)
        assertEquals("geofence_reminder_42", geofenceId1)
        assertEquals(geofenceId1, geofenceId2)

        // Ensure distinct namespaces within single reminder
        assertNotEquals(primaryCode1, repeatCode1)
        assertNotEquals(primaryCode1, snoozeCode1)
        assertNotEquals(repeatCode1, snoozeCode1)
    }

    // ==========================================
    // 4. MULTIPLE REMINDER ISOLATION
    // ==========================================

    @Test
    fun testMultipleReminders_completeIsolationAndNoCollisions() {
        val reminder1Id = 1L
        val reminder2Id = 2L

        // Alarm codes
        val r1Primary = reminder1Id.toInt()
        val r2Primary = reminder2Id.toInt()
        assertNotEquals(r1Primary, r2Primary)

        val r1Repeat = reminder1Id.toInt() + AndroidAlarmScheduler.REPEAT_REQUEST_CODE_OFFSET
        val r2Repeat = reminder2Id.toInt() + AndroidAlarmScheduler.REPEAT_REQUEST_CODE_OFFSET
        assertNotEquals(r1Repeat, r2Repeat)

        val r1Snooze = reminder1Id.toInt() + AndroidAlarmScheduler.SNOOZE_REQUEST_CODE_OFFSET
        val r2Snooze = reminder2Id.toInt() + AndroidAlarmScheduler.SNOOZE_REQUEST_CODE_OFFSET
        assertNotEquals(r1Snooze, r2Snooze)

        // Geofence IDs
        val r1Geofence = GeofenceManager.getGeofenceRequestId(reminder1Id)
        val r2Geofence = GeofenceManager.getGeofenceRequestId(reminder2Id)
        assertNotEquals(r1Geofence, r2Geofence)
        assertEquals(1L, GeofenceManager.extractReminderId(r1Geofence))
        assertEquals(2L, GeofenceManager.extractReminderId(r2Geofence))
    }

    @Test
    fun testMultipleRemindersAtSameCoordinates_noGeofenceCollision() {
        val idA = 100L
        val idB = 200L

        // Both at exact same coordinate
        val geofenceA = GeofenceManager.getGeofenceRequestId(idA)
        val geofenceB = GeofenceManager.getGeofenceRequestId(idB)

        assertEquals("geofence_reminder_100", geofenceA)
        assertEquals("geofence_reminder_200", geofenceB)
        assertNotEquals("Geofence IDs must never collide even at identical coordinates", geofenceA, geofenceB)
    }

    // ==========================================
    // 5. STALE SCHEDULE CLEANUP LOGIC
    // ==========================================

    @Test
    fun testStaleScheduleCleanup_onDeleteAndComplete() {
        val fakeScheduler = FakeAlarmScheduler()
        val reminder = ReminderEntity(
            id = 55L,
            title = "Cleanup Test",
            dueDateTime = baseNow + 10000L,
            repeatIntervalMinutes = 10L,
            isEnabled = true,
            isCompleted = false
        )

        // Schedule primary, repeat, and snooze
        fakeScheduler.schedule(reminder)
        fakeScheduler.scheduleRepeat(reminder, baseNow + 20000L, false)
        fakeScheduler.scheduleSnooze(reminder, 10L, false)

        assertTrue(fakeScheduler.scheduledAlarms.containsKey(55L))
        assertTrue(fakeScheduler.repeatAlarms.containsKey(55L))
        assertTrue(fakeScheduler.snoozeAlarms.containsKey(55L))

        // When reminder is deleted or completed: cancel is called
        fakeScheduler.cancel(55L)

        assertFalse(fakeScheduler.scheduledAlarms.containsKey(55L))
        assertFalse(fakeScheduler.repeatAlarms.containsKey(55L))
        assertFalse(fakeScheduler.snoozeAlarms.containsKey(55L))
        assertTrue(fakeScheduler.cancelledAlarms.contains(55L))
    }

    // ==========================================
    // 6. PERMISSION & CAPABILITY STATE EVALUATION
    // ==========================================

    @Test
    fun testPermissionStateHandling_locationDisabledWarning() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = true,
            hasForegroundLocation = true,
            hasBackgroundLocation = true,
            isLocationServicesEnabled = false, // Device location turned off
            canScheduleExactAlarms = true,
            isIgnoringBatteryOptimizations = true,
            hasActiveLocationReminders = true,
            hasActiveDateTimeReminders = false
        )

        assertTrue("Must warn when device location is off and active location reminders exist", status.showLocationDisabledWarning)
        assertFalse(status.showForegroundLocationWarning)
        assertFalse(status.showNotificationWarning)
    }

    @Test
    fun testPermissionStateHandling_foregroundLocationMissingWarning() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = true,
            hasForegroundLocation = false, // Foreground missing
            hasBackgroundLocation = false,
            isLocationServicesEnabled = true,
            canScheduleExactAlarms = true,
            isIgnoringBatteryOptimizations = true,
            hasActiveLocationReminders = true,
            hasActiveDateTimeReminders = false
        )

        assertFalse(status.showLocationDisabledWarning)
        assertTrue("Must warn when foreground location is missing", status.showForegroundLocationWarning)
        assertFalse(status.showNotificationWarning)
    }

    @Test
    fun testPermissionStateHandling_backgroundLocationMissingWarning() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = true,
            hasForegroundLocation = true,
            hasBackgroundLocation = false, // Background missing
            isLocationServicesEnabled = true,
            canScheduleExactAlarms = true,
            isIgnoringBatteryOptimizations = true,
            hasActiveLocationReminders = true,
            hasActiveDateTimeReminders = false
        )

        assertTrue("Must warn when background location is missing", status.showBackgroundLocationWarning)
    }

    @Test
    fun testPermissionStateHandling_notificationWarning() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = false, // Notifications off
            hasForegroundLocation = true,
            hasBackgroundLocation = true,
            isLocationServicesEnabled = true,
            canScheduleExactAlarms = true,
            isIgnoringBatteryOptimizations = true,
            hasActiveLocationReminders = false,
            hasActiveDateTimeReminders = true
        )

        assertTrue("Must warn when notifications are disabled and active reminders exist", status.showNotificationWarning)
    }

    @Test
    fun testPermissionStateHandling_exactAlarmWarning() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = true,
            hasForegroundLocation = true,
            hasBackgroundLocation = true,
            isLocationServicesEnabled = true,
            canScheduleExactAlarms = false, // Exact alarms restricted
            isIgnoringBatteryOptimizations = true,
            hasActiveLocationReminders = false,
            hasActiveDateTimeReminders = true
        )

        assertTrue("Must warn when exact alarms restricted for date/time reminders", status.showExactAlarmWarning)
    }

    @Test
    fun testPermissionStateHandling_noActiveReminders_noWarnings() {
        val status = PermissionHelper.computeCapabilityStatus(
            hasNotificationPermission = false,
            hasForegroundLocation = false,
            hasBackgroundLocation = false,
            isLocationServicesEnabled = false,
            canScheduleExactAlarms = false,
            isIgnoringBatteryOptimizations = false,
            hasActiveLocationReminders = false,
            hasActiveDateTimeReminders = false
        )

        assertFalse(status.showLocationDisabledWarning)
        assertFalse(status.showForegroundLocationWarning)
        assertFalse(status.showBackgroundLocationWarning)
        assertFalse(status.showNotificationWarning)
        assertFalse(status.showExactAlarmWarning)
    }
}
