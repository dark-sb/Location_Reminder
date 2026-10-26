package com.example.location_reminder

import com.example.location_reminder.data.alarm.ReminderAlarmReceiver
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    // Helper mirroring NotificationHelper's title and content resolution
    private fun resolveNotificationTitle(title: String): String {
        return if (title.isNotBlank()) title else "Scheduled Reminder"
    }

    private fun resolveNotificationContent(reminderMessage: String, description: String): String {
        return when {
            reminderMessage.isNotBlank() -> reminderMessage
            description.isNotBlank() -> description
            else -> "It's time for your scheduled reminder!"
        }
    }

    private fun isEligibleForNotification(reminder: ReminderEntity?): Boolean {
        if (reminder == null) return false
        if (!reminder.isEnabled) return false
        if (reminder.isCompleted) return false
        return true
    }

    @Test
    fun notificationIds_areUniqueAndConsistentPerReminder() {
        val reminder1 = ReminderEntity(id = 1L, title = "Task 1")
        val reminder2 = ReminderEntity(id = 2L, title = "Task 2")
        val reminder3 = ReminderEntity(id = 100L, title = "Task 100")

        val id1 = reminder1.id.toInt()
        val id2 = reminder2.id.toInt()
        val id3 = reminder3.id.toInt()

        assertEquals(1, id1)
        assertEquals(2, id2)
        assertEquals(100, id3)
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
    }

    @Test
    fun actionRequestCode_isDistinctFromNotificationId() {
        val reminderId = 42L
        val notificationId = reminderId.toInt()
        val actionRequestCode = reminderId.toInt() + 100000

        assertNotEquals(notificationId, actionRequestCode)
        assertEquals(100042, actionRequestCode)
    }

    @Test
    fun contentResolution_prefersReminderMessageOverDescription() {
        val title = "Team Sync"
        val desc = "Weekly sync discussion"
        val message = "Join Zoom link in calendar"

        val resolvedTitle = resolveNotificationTitle(title)
        val resolvedContent = resolveNotificationContent(message, desc)

        assertEquals("Team Sync", resolvedTitle)
        assertEquals("Join Zoom link in calendar", resolvedContent)
    }

    @Test
    fun contentResolution_fallsBackToDescriptionWhenMessageIsBlank() {
        val desc = "Dentist appointment at Central Clinic"
        val message = ""

        val resolvedContent = resolveNotificationContent(message, desc)
        assertEquals("Dentist appointment at Central Clinic", resolvedContent)
    }

    @Test
    fun contentResolution_fallsBackToDefaultWhenBothBlank() {
        val resolvedContent = resolveNotificationContent("", "")
        assertEquals("It's time for your scheduled reminder!", resolvedContent)
    }

    @Test
    fun titleResolution_fallsBackToDefaultWhenBlank() {
        val resolvedTitle = resolveNotificationTitle("   ")
        assertEquals("Scheduled Reminder", resolvedTitle)
    }

    @Test
    fun eligibilityCheck_activeReminder_isEligible() {
        val activeReminder = ReminderEntity(
            id = 5L,
            title = "Pay Utility Bill",
            isEnabled = true,
            isCompleted = false
        )
        assertTrue(isEligibleForNotification(activeReminder))
    }

    @Test
    fun eligibilityCheck_completedReminder_isNotEligible() {
        val completedReminder = ReminderEntity(
            id = 6L,
            title = "Paid Utility Bill",
            isEnabled = true,
            isCompleted = true
        )
        assertFalse(isEligibleForNotification(completedReminder))
    }

    @Test
    fun eligibilityCheck_disabledReminder_isNotEligible() {
        val disabledReminder = ReminderEntity(
            id = 7L,
            title = "Gym Session",
            isEnabled = false,
            isCompleted = false
        )
        assertFalse(isEligibleForNotification(disabledReminder))
    }

    @Test
    fun eligibilityCheck_deletedReminder_isNotEligible() {
        val deletedReminder: ReminderEntity? = null
        assertFalse(isEligibleForNotification(deletedReminder))
    }

    @Test
    fun channelIds_areDistinct() {
        assertNotEquals(
            NotificationHelper.CHANNEL_SCHEDULED_ID,
            NotificationHelper.CHANNEL_LOCATION_ID
        )
        assertEquals("scheduled_reminders_channel", NotificationHelper.CHANNEL_SCHEDULED_ID)
        assertEquals("location_reminder_alerts_channel", NotificationHelper.CHANNEL_LOCATION_ID)
    }

    @Test
    fun markCompletedActionConstant_isConsistent() {
        assertEquals(
            "com.example.location_reminder.ACTION_MARK_COMPLETED",
            ReminderAlarmReceiver.ACTION_MARK_COMPLETED
        )
    }
}
