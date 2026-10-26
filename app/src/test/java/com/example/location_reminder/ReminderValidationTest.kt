package com.example.location_reminder

import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderValidationTest {

    // Helper validation functions mirroring the UI validation rules
    private fun isTitleValid(title: String): Boolean = title.trim().isNotBlank()
    private fun isLocationNameValid(name: String): Boolean = name.trim().isNotBlank()
    private fun isMessageValid(msg: String): Boolean = msg.trim().isNotBlank()
    private fun isLatitudeValid(lat: Double?): Boolean = lat != null && lat in -90.0..90.0
    private fun isLongitudeValid(lng: Double?): Boolean = lng != null && lng in -180.0..180.0
    private fun isRadiusValid(rad: Double?): Boolean = rad != null && rad > 0.0
    private fun isDateTimeValid(dueMillis: Long?): Boolean = dueMillis != null && dueMillis > 0L

    @Test
    fun validReminder_passesAllValidationRules() {
        val title = "Buy Groceries"
        val location = "Supermarket"
        val message = "Remember to buy milk"
        val lat = 37.7749
        val lng = -122.4194
        val radius = 200.0

        assertTrue(isTitleValid(title))
        assertTrue(isLocationNameValid(location))
        assertTrue(isMessageValid(message))
        assertTrue(isLatitudeValid(lat))
        assertTrue(isLongitudeValid(lng))
        assertTrue(isRadiusValid(radius))
    }

    @Test
    fun invalidTitle_failsValidation() {
        assertFalse(isTitleValid(""))
        assertFalse(isTitleValid("   "))
    }

    @Test
    fun invalidCoordinates_failsValidation() {
        // Latitude out of bounds
        assertFalse(isLatitudeValid(91.0))
        assertFalse(isLatitudeValid(-90.1))
        assertFalse(isLatitudeValid(null))
        assertTrue(isLatitudeValid(0.0))
        assertTrue(isLatitudeValid(-90.0))
        assertTrue(isLatitudeValid(90.0))

        // Longitude out of bounds
        assertFalse(isLongitudeValid(180.1))
        assertFalse(isLongitudeValid(-180.1))
        assertFalse(isLongitudeValid(null))
        assertTrue(isLongitudeValid(0.0))
        assertTrue(isLongitudeValid(-180.0))
        assertTrue(isLongitudeValid(180.0))
    }

    @Test
    fun invalidRadius_failsValidation() {
        assertFalse(isRadiusValid(0.0))
        assertFalse(isRadiusValid(-50.0))
        assertFalse(isRadiusValid(null))
        assertTrue(isRadiusValid(1.0))
        assertTrue(isRadiusValid(500.0))
    }

    @Test
    fun reminderEntity_preservesIdAndCreatedAtWhenEdited() {
        val originalCreatedAt = 1727170000000L
        val original = ReminderEntity(
            id = 42L,
            title = "Original Title",
            description = "Original Description",
            latitude = 12.9716,
            longitude = 77.5946,
            locationName = "Original Place",
            radius = 150.0,
            reminderMessage = "Original Message",
            isEnabled = true,
            createdAt = originalCreatedAt
        )

        // Simulate edit
        val updated = original.copy(
            title = "Updated Title",
            description = "Updated Description",
            locationName = "Updated Place",
            latitude = 13.0827,
            longitude = 80.2707,
            radius = 300.0,
            reminderMessage = "Updated Message"
        )

        assertEquals(42L, updated.id)
        assertEquals(originalCreatedAt, updated.createdAt)
        assertTrue(updated.isEnabled)
        assertEquals("Updated Title", updated.title)
        assertEquals(300.0, updated.radius, 0.001)
    }

    @Test
    fun reminderEntity_toggleEnabled_updatesCorrectly() {
        val reminder = ReminderEntity(
            id = 1L,
            title = "Test",
            description = "",
            latitude = 0.0,
            longitude = 0.0,
            locationName = "Point",
            radius = 100.0,
            reminderMessage = "Msg",
            isEnabled = true
        )

        val disabled = reminder.copy(isEnabled = false)
        assertFalse(disabled.isEnabled)
        assertEquals(1L, disabled.id)

        val reEnabled = disabled.copy(isEnabled = true)
        assertTrue(reEnabled.isEnabled)
    }

    @Test
    fun dateTimeReminder_creationAndValidation() {
        val targetMillis = System.currentTimeMillis() + 86400000L // Tomorrow
        val reminder = ReminderEntity(
            id = 10L,
            title = "Team Meeting",
            description = "Discuss sprint plan",
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = targetMillis,
            reminderMessage = "Join meeting in room 4B"
        )

        assertEquals(ReminderType.DATE_TIME.name, reminder.reminderType)
        assertEquals(targetMillis, reminder.dueDateTime)
        assertTrue(isDateTimeValid(reminder.dueDateTime))
        assertTrue(isTitleValid(reminder.title))
        assertFalse(reminder.isCompleted)
    }

    @Test
    fun locationAndDateTimeReminder_creationAndValidation() {
        val targetMillis = System.currentTimeMillis() + 3600000L
        val reminder = ReminderEntity(
            id = 11L,
            title = "Doctor Appointment",
            description = "Clinic visit",
            latitude = 19.0760,
            longitude = 72.8777,
            locationName = "City Hospital",
            radius = 250.0,
            reminderType = ReminderType.LOCATION_AND_TIME.name,
            dueDateTime = targetMillis,
            reminderMessage = "Bring medical report"
        )

        assertEquals(ReminderType.LOCATION_AND_TIME.name, reminder.reminderType)
        assertTrue(isDateTimeValid(reminder.dueDateTime))
        assertTrue(isLatitudeValid(reminder.latitude))
        assertTrue(isLongitudeValid(reminder.longitude))
        assertTrue(isLocationNameValid(reminder.locationName))
    }

    @Test
    fun reminderEntity_completionToggle_updatesCorrectly() {
        val reminder = ReminderEntity(
            id = 15L,
            title = "Car Service",
            description = "Oil change",
            isCompleted = false
        )

        assertFalse(reminder.isCompleted)

        val completed = reminder.copy(isCompleted = true)
        assertTrue(completed.isCompleted)

        val uncompleted = completed.copy(isCompleted = false)
        assertFalse(uncompleted.isCompleted)
    }

    @Test
    fun taskEntity_creationAndCompletionToggle() {
        val task = TaskEntity(
            id = 101L,
            reminderId = 15L,
            title = "Check tire pressure",
            isCompleted = false
        )

        assertEquals(101L, task.id)
        assertEquals(15L, task.reminderId)
        assertEquals("Check tire pressure", task.title)
        assertFalse(task.isCompleted)

        val completedTask = task.copy(isCompleted = true)
        assertTrue(completedTask.isCompleted)
    }

    @Test
    fun reminderWithTasks_aggregation() {
        val reminder = ReminderEntity(
            id = 20L,
            title = "Weekly Groceries",
            reminderType = ReminderType.LOCATION.name
        )

        val tasks = listOf(
            TaskEntity(id = 1L, reminderId = 20L, title = "Apples", isCompleted = true),
            TaskEntity(id = 2L, reminderId = 20L, title = "Milk", isCompleted = false),
            TaskEntity(id = 3L, reminderId = 20L, title = "Bread", isCompleted = false)
        )

        val reminderWithTasks = ReminderWithTasks(
            reminder = reminder,
            tasks = tasks
        )

        assertEquals(20L, reminderWithTasks.reminder.id)
        assertEquals(3, reminderWithTasks.tasks.size)
        assertEquals(1, reminderWithTasks.tasks.count { it.isCompleted })
        assertEquals(2, reminderWithTasks.tasks.count { !it.isCompleted })
    }
}