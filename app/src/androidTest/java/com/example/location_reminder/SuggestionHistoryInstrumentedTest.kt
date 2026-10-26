package com.example.location_reminder

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.location_reminder.data.local.dao.LocationHistoryDao
import com.example.location_reminder.data.local.dao.TitleHistoryDao
import com.example.location_reminder.data.local.database.ReminderDatabase
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuggestionHistoryInstrumentedTest {

    private lateinit var db: ReminderDatabase
    private lateinit var titleDao: TitleHistoryDao
    private lateinit var locationDao: LocationHistoryDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        titleDao = db.titleHistoryDao
        locationDao = db.locationHistoryDao
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun titleHistory_onDevicePersistence_storesAndRetrievesRecentTitles() = runBlocking {
        val now = System.currentTimeMillis()
        titleDao.insertOrUpdate(TitleHistoryEntity(title = "Buy Milk", lastUsedAt = now - 3000))
        titleDao.insertOrUpdate(TitleHistoryEntity(title = "Doctor Appointment", lastUsedAt = now - 2000))
        titleDao.insertOrUpdate(TitleHistoryEntity(title = "Gym Session", lastUsedAt = now - 1000))

        val recent = titleDao.getRecentTitlesOnce(5)
        assertEquals(3, recent.size)
        // Most recent first
        assertEquals("Gym Session", recent[0].title)
        assertEquals("Doctor Appointment", recent[1].title)
        assertEquals("Buy Milk", recent[2].title)
    }

    @Test
    fun titleHistory_onDevicePersistence_deduplicatesCaseInsensitively() = runBlocking {
        val now = System.currentTimeMillis()
        titleDao.insertOrUpdate(TitleHistoryEntity(title = "Weekly Review", lastUsedAt = now - 5000))

        // Find existing by case-insensitive title and update
        val existing = titleDao.findByTitle("weekly review")
        assertNotNull(existing)
        titleDao.insertOrUpdate(existing!!.copy(title = "Weekly Review", lastUsedAt = now))

        val recent = titleDao.getRecentTitlesOnce(5)
        assertEquals(1, recent.size)
        assertEquals("Weekly Review", recent[0].title)
        assertEquals(now, recent[0].lastUsedAt)
    }

    @Test
    fun locationHistory_onDevicePersistence_storesAndRetrievesLocationsWithCoordinates() = runBlocking {
        val now = System.currentTimeMillis()
        locationDao.insertOrUpdate(LocationHistoryEntity(name = "Central Park", latitude = 40.7829, longitude = -73.9654, lastUsedAt = now - 2000))
        locationDao.insertOrUpdate(LocationHistoryEntity(name = "Times Square", latitude = 40.7580, longitude = -73.9855, lastUsedAt = now - 1000))

        val recent = locationDao.getRecentLocationsOnce(20)
        assertEquals(2, recent.size)
        assertEquals("Times Square", recent[0].name)
        assertEquals(40.7580, recent[0].latitude, 0.0001)
        assertEquals(-73.9855, recent[0].longitude, 0.0001)

        assertEquals("Central Park", recent[1].name)
        assertEquals(40.7829, recent[1].latitude, 0.0001)
        assertEquals(-73.9654, recent[1].longitude, 0.0001)
    }

    @Test
    fun locationHistory_onDevicePersistence_deduplicatesAndUpdatesCoordinates() = runBlocking {
        val now = System.currentTimeMillis()
        locationDao.insertOrUpdate(LocationHistoryEntity(name = "Office HQ", latitude = 37.77, longitude = -122.41, lastUsedAt = now - 5000))

        val existing = locationDao.findByName("office hq")
        assertNotNull(existing)
        locationDao.insertOrUpdate(existing!!.copy(name = "Office HQ", latitude = 37.7749, longitude = -122.4194, lastUsedAt = now))

        val recent = locationDao.getRecentLocationsOnce(20)
        assertEquals(1, recent.size)
        assertEquals("Office HQ", recent[0].name)
        assertEquals(37.7749, recent[0].latitude, 0.0001)
        assertEquals(-122.4194, recent[0].longitude, 0.0001)
    }
}
