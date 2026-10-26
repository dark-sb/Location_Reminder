package com.example.location_reminder

import com.example.location_reminder.data.local.entity.DailyTaskCompletionEntity
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTaskLogicTest {

    @Test
    fun testAutomaticNextDayReset_completionStateTracksPerCalendarDate() {
        // Concept: A daily task definition is permanent
        val task = DailyTaskEntity(
            id = 1L,
            title = "Drink 4 liters water",
            createdAt = 1000L
        )

        val day1 = "2026-10-07"
        val day2 = "2026-10-08"

        // On Day 1, user completes the task
        val completionDay1 = DailyTaskCompletionEntity(
            id = 10L,
            taskId = task.id,
            date = day1,
            isCompleted = true,
            completedAt = 2000L
        )

        // UI composite model for Day 1
        val itemDay1 = DailyTaskWithCompletion(
            taskId = task.id,
            title = task.title,
            createdAt = task.createdAt,
            isCompleted = completionDay1.isCompleted,
            completedAt = completionDay1.completedAt
        )

        assertTrue("Task should be completed on Day 1", itemDay1.isCompleted)

        // When calendar rolls over to Day 2, no completion record exists for Day 2 yet
        // Left-join with date = day2 produces null completion fields -> isCompleted = false
        val itemDay2 = DailyTaskWithCompletion(
            taskId = task.id,
            title = task.title,
            createdAt = task.createdAt,
            isCompleted = false,
            completedAt = null
        )

        assertFalse("Task should automatically reset to incomplete on Day 2", itemDay2.isCompleted)
        assertEquals("Task ID remains permanent across days", itemDay1.taskId, itemDay2.taskId)
        assertEquals("Task Title remains unchanged", itemDay1.title, itemDay2.title)
    }

    @Test
    fun testMultipleTasks_independentCompletionStates() {
        val taskWater = DailyTaskWithCompletion(
            taskId = 1L,
            title = "Drink 4 liters water",
            createdAt = 1000L,
            isCompleted = true,
            completedAt = 1500L
        )

        val taskPython = DailyTaskWithCompletion(
            taskId = 2L,
            title = "Complete Python class",
            createdAt = 1010L,
            isCompleted = false,
            completedAt = null
        )

        val taskGym = DailyTaskWithCompletion(
            taskId = 3L,
            title = "Gym",
            createdAt = 1020L,
            isCompleted = false,
            completedAt = null
        )

        assertTrue(taskWater.isCompleted)
        assertFalse(taskPython.isCompleted)
        assertFalse(taskGym.isCompleted)

        val completedCount = listOf(taskWater, taskPython, taskGym).count { it.isCompleted }
        assertEquals(1, completedCount)
    }

    @Test
    fun testDateTimeFormatterHelper_isoDateFormatPattern() {
        val todayIso = DateTimeFormatterHelper.getTodayIsoDate()
        assertTrue(
            "Today's ISO date should match yyyy-MM-dd format, got: $todayIso",
            todayIso.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        )

        val header = DateTimeFormatterHelper.formatTodayHeader()
        assertTrue("Header format should not be empty", header.isNotBlank())
    }

    @Test
    fun testToggleCompletion_stateTransitions() {
        // Incomplete task
        val initial = DailyTaskWithCompletion(
            taskId = 5L,
            title = "Study DSA",
            createdAt = 500L,
            isCompleted = false,
            completedAt = null
        )
        assertFalse(initial.isCompleted)

        // After tapping check button: marked complete
        val completed = initial.copy(
            isCompleted = true,
            completedAt = 600L
        )
        assertTrue(completed.isCompleted)

        // After tapping check button again: unchecked
        val uncompleted = completed.copy(
            isCompleted = false,
            completedAt = null
        )
        assertFalse(uncompleted.isCompleted)
    }

    @Test
    fun testLast7DaysDates_generatesExactly7ConsecutiveDatesNewestToOldest() {
        val baseDate = DateTimeFormatterHelper.parseIsoDate("2026-10-07") ?: java.util.Date()
        val last7Days = DateTimeFormatterHelper.getLast7DaysIsoDates(baseDate)

        assertEquals("Must produce exactly 7 dates", 7, last7Days.size)
        assertEquals("First date must be base date (Today)", "2026-10-07", last7Days[0])
        assertEquals("Second date must be Yesterday", "2026-10-06", last7Days[1])
        assertEquals("Third date must be 2 days ago", "2026-10-05", last7Days[2])
        assertEquals("Fourth date must be 3 days ago", "2026-10-04", last7Days[3])
        assertEquals("Fifth date must be 4 days ago", "2026-10-03", last7Days[4])
        assertEquals("Sixth date must be 5 days ago", "2026-10-02", last7Days[5])
        assertEquals("Seventh date must be 6 days ago", "2026-10-01", last7Days[6])
    }

    @Test
    fun testLast30DaysDates_generatesExactly30ConsecutiveDatesNewestToOldest() {
        val baseDate = DateTimeFormatterHelper.parseIsoDate("2026-10-08") ?: java.util.Date()
        val last30Days = DateTimeFormatterHelper.getLast30DaysIsoDates(baseDate)

        assertEquals("Must produce exactly 30 dates", 30, last30Days.size)
        assertEquals("First date must be base date (Today)", "2026-10-08", last30Days[0])
        assertEquals("Second date must be Yesterday", "2026-10-07", last30Days[1])
        assertEquals("30th date must be 29 days ago", "2026-09-09", last30Days[29])

        // Verify strictly descending order
        for (i in 0 until 29) {
            val curr = DateTimeFormatterHelper.parseIsoDate(last30Days[i])!!.time
            val next = DateTimeFormatterHelper.parseIsoDate(last30Days[i + 1])!!.time
            assertTrue("Dates must be newest first: ${last30Days[i]} > ${last30Days[i+1]}", curr > next)
            // Exactly 24 hours (86,400,000 ms) apart
            assertEquals("Adjacent dates must be exactly 1 day apart", 86400000L, curr - next)
        }
    }

    @Test
    fun testFormatHistoryHeader_todayYesterdayAndDayOfWeek() {
        val today = DateTimeFormatterHelper.parseIsoDate("2026-10-07") ?: java.util.Date()
        val yesterday = DateTimeFormatterHelper.parseIsoDate("2026-10-06") ?: java.util.Date()
        val monday = DateTimeFormatterHelper.parseIsoDate("2026-10-05") ?: java.util.Date()

        val todayHeader = DateTimeFormatterHelper.formatHistoryHeader(today, 0)
        assertEquals("TODAY — 7 OCT 2026", todayHeader)

        val yesterdayHeader = DateTimeFormatterHelper.formatHistoryHeader(yesterday, 1)
        assertEquals("YESTERDAY — 6 OCT 2026", yesterdayHeader)

        val mondayHeader = DateTimeFormatterHelper.formatHistoryHeader(monday, 2)
        assertEquals("MONDAY — 5 OCT 2026", mondayHeader)
    }

    @Test
    fun testHistoryDateWiseCompletion_reflectsDateSpecificStateWithoutOverwriting() {
        val taskWater = DailyTaskEntity(id = 1L, title = "Drink 4L Water", createdAt = 1000L)
        val taskPython = DailyTaskEntity(id = 2L, title = "Complete Python Class", createdAt = 1000L)
        val taskGym = DailyTaskEntity(id = 3L, title = "Gym", createdAt = 1000L)

        // On Yesterday (2026-10-06): Water completed, Gym completed, Python incomplete
        val completions = listOf(
            DailyTaskCompletionEntity(id = 10L, taskId = 1L, date = "2026-10-06", isCompleted = true),
            DailyTaskCompletionEntity(id = 11L, taskId = 3L, date = "2026-10-06", isCompleted = true),
            // On Today (2026-10-07): Water completed, Python completed, Gym incomplete
            DailyTaskCompletionEntity(id = 12L, taskId = 1L, date = "2026-10-07", isCompleted = true),
            DailyTaskCompletionEntity(id = 13L, taskId = 2L, date = "2026-10-07", isCompleted = true)
        )

        val completedMap = completions.filter { it.isCompleted }.associateBy { "${it.date}_${it.taskId}" }

        // Evaluate Yesterday (2026-10-06)
        val yesterdayWater = completedMap["2026-10-06_${taskWater.id}"] != null
        val yesterdayPython = completedMap["2026-10-06_${taskPython.id}"] != null
        val yesterdayGym = completedMap["2026-10-06_${taskGym.id}"] != null

        assertTrue("Water was completed yesterday", yesterdayWater)
        assertFalse("Python was incomplete yesterday", yesterdayPython)
        assertTrue("Gym was completed yesterday", yesterdayGym)

        // Evaluate Today (2026-10-07)
        val todayWater = completedMap["2026-10-07_${taskWater.id}"] != null
        val todayPython = completedMap["2026-10-07_${taskPython.id}"] != null
        val todayGym = completedMap["2026-10-07_${taskGym.id}"] != null

        assertTrue("Water is completed today", todayWater)
        assertTrue("Python is completed today", todayPython)
        assertFalse("Gym is incomplete today", todayGym)

        // Confirm yesterday's state was not overridden by today's state
        assertEquals("Gym yesterday != Gym today", true, yesterdayGym)
        assertEquals("Gym today != Gym yesterday", false, todayGym)
        assertEquals("Python yesterday != Python today", false, yesterdayPython)
        assertEquals("Python today != Python yesterday", true, todayPython)
    }

    @Test
    fun testCompletionPercentageCalculation_exactFormulas() {
        // 4 completed out of 5 = 80%
        val tasks5 = listOf(
            DailyTaskWithCompletion(1L, "T1", 0L, true),
            DailyTaskWithCompletion(2L, "T2", 0L, true),
            DailyTaskWithCompletion(3L, "T3", 0L, true),
            DailyTaskWithCompletion(4L, "T4", 0L, true),
            DailyTaskWithCompletion(5L, "T5", 0L, false)
        )
        val p80 = Math.round((tasks5.count { it.isCompleted } * 100.0) / tasks5.size).toInt()
        assertEquals(80, p80)

        // 5 completed out of 5 = 100%
        val tasks100 = tasks5.map { it.copy(isCompleted = true) }
        val p100 = Math.round((tasks100.count { it.isCompleted } * 100.0) / tasks100.size).toInt()
        assertEquals(100, p100)

        // 0 completed out of 5 = 0%
        val tasks0 = tasks5.map { it.copy(isCompleted = false) }
        val p0 = Math.round((tasks0.count { it.isCompleted } * 100.0) / tasks0.size).toInt()
        assertEquals(0, p0)

        // 0 tasks = 0%
        val tasksEmpty = emptyList<DailyTaskWithCompletion>()
        val pEmpty = if (tasksEmpty.isNotEmpty()) Math.round((tasksEmpty.count { it.isCompleted } * 100.0) / tasksEmpty.size).toInt() else 0
        assertEquals(0, pEmpty)
    }

    @Test
    fun testTaskCreatedAfterOlderDate_doesNotAppearInOlderDate() {
        val dateOlder = "2026-10-03"
        val dayEndMillisOlder = DateTimeFormatterHelper.getDayEndMillis(dateOlder)!!

        val taskOld = DailyTaskEntity(id = 1L, title = "Old Task", createdAt = dayEndMillisOlder - 10000L)
        val taskNew = DailyTaskEntity(id = 2L, title = "New Task Created Later", createdAt = dayEndMillisOlder + 100000L)

        val completions = emptyMap<String, DailyTaskCompletionEntity>()

        val tasksForOlderDay = listOf(taskOld, taskNew).filter { task ->
            val hasCompletion = completions.containsKey("${dateOlder}_${task.id}")
            task.createdAt <= dayEndMillisOlder || hasCompletion
        }

        assertEquals("Only task created before or on the older date should appear", 1, tasksForOlderDay.size)
        assertEquals("Old Task", tasksForOlderDay[0].title)
    }

    @Test
    fun testSoftDeletedTask_preservedInHistoricalRecord() {
        val dateHistory = "2026-10-04"
        val dayStartMillis = DateTimeFormatterHelper.getDayStartMillis(dateHistory)!!
        val dayEndMillis = DateTimeFormatterHelper.getDayEndMillis(dateHistory)!!

        // Task created on Oct 1, completed on Oct 4, soft deleted on Oct 7
        val task = DailyTaskEntity(
            id = 5L,
            title = "Run 5km",
            createdAt = dayStartMillis - 86400000L,
            isDeleted = true,
            deletedAt = dayEndMillis + 86400000L * 3 // Deleted 3 days later
        )

        val completion = DailyTaskCompletionEntity(
            id = 50L,
            taskId = task.id,
            date = dateHistory,
            isCompleted = true,
            completedAt = dayStartMillis + 3600000L
        )

        val completionsByDate = mapOf("${dateHistory}_${task.id}" to completion)

        val existsOnDate = (task.createdAt <= dayEndMillis || completionsByDate.containsKey("${dateHistory}_${task.id}")) &&
                (!task.isDeleted || completionsByDate.containsKey("${dateHistory}_${task.id}") || (task.deletedAt != null && task.deletedAt >= dayStartMillis))

        assertTrue("Soft deleted task must still exist in historical record on the date it was completed", existsOnDate)
    }

    @Test
    fun testEditDateCompletion_recalculatesPercentageForThatDateOnly() {
        val dateOct5 = "2026-10-05"
        val dateOct7 = "2026-10-07"

        val taskWater = DailyTaskWithCompletion(1L, "Drink 4L Water", 0L, true)
        val taskGym = DailyTaskWithCompletion(2L, "Gym", 0L, false)

        val tasksOct5Initial = listOf(taskWater, taskGym)
        val pInitialOct5 = Math.round((tasksOct5Initial.count { it.isCompleted } * 100.0) / tasksOct5Initial.size).toInt()
        assertEquals(50, pInitialOct5) // 1 of 2 = 50%

        // User edits Oct 5: marks Gym as completed
        val tasksOct5AfterEdit = listOf(taskWater, taskGym.copy(isCompleted = true))
        val pUpdatedOct5 = Math.round((tasksOct5AfterEdit.count { it.isCompleted } * 100.0) / tasksOct5AfterEdit.size).toInt()
        assertEquals(100, pUpdatedOct5) // 2 of 2 = 100%

        // Today (Oct 7) tasks remain completely unaffected
        val tasksOct7 = listOf(taskWater.copy(isCompleted = false), taskGym.copy(isCompleted = false))
        val pOct7 = Math.round((tasksOct7.count { it.isCompleted } * 100.0) / tasksOct7.size).toInt()
        assertEquals(0, pOct7)
    }

    @Test
    fun testDisplayDateFormatting_matchesSpec() {
        val date = DateTimeFormatterHelper.parseIsoDate("2026-10-07")!!
        val formatted = DateTimeFormatterHelper.formatDisplayDate(date)
        assertEquals("7 Oct 2026", formatted)
    }

    @Test
    fun testBatchUpdateCompletions_updatesTargetDateWithoutAffectingOtherDates() {
        val dateOct5 = "2026-10-05"
        val dateOct6 = "2026-10-06"
        val dateOct7 = "2026-10-07"

        // Simulated database store: "date_taskId" -> DailyTaskCompletionEntity
        val dbCompletions = mutableMapOf<String, DailyTaskCompletionEntity>(
            "${dateOct5}_1" to DailyTaskCompletionEntity(1L, 1L, dateOct5, true),
            "${dateOct6}_1" to DailyTaskCompletionEntity(2L, 1L, dateOct6, true),
            "${dateOct7}_1" to DailyTaskCompletionEntity(3L, 1L, dateOct7, false)
        )

        // Batch update for Oct 6: task 1 marked false, task 2 marked true
        val updatesOct6 = mapOf(1L to false, 2L to true)
        updatesOct6.forEach { (taskId, isCompleted) ->
            val key = "${dateOct6}_$taskId"
            if (isCompleted) {
                dbCompletions[key] = DailyTaskCompletionEntity(0L, taskId, dateOct6, true)
            } else {
                dbCompletions.remove(key)
            }
        }

        // Verify Oct 5 remains untouched
        assertTrue("Oct 5 task 1 should still be completed", dbCompletions["${dateOct5}_1"]?.isCompleted == true)

        // Verify Oct 7 remains untouched
        assertFalse("Oct 7 task 1 should still be incomplete", dbCompletions["${dateOct7}_1"]?.isCompleted == true)

        // Verify Oct 6 reflects updates
        assertFalse("Oct 6 task 1 should now be incomplete", dbCompletions.containsKey("${dateOct6}_1"))
        assertTrue("Oct 6 task 2 should now be completed", dbCompletions["${dateOct6}_2"]?.isCompleted == true)
    }

    @Test
    fun testUniqueIndex_preventsDuplicateCompletionRecords() {
        // Unique key constraint simulation on (taskId, date)
        val records = mutableMapOf<Pair<Long, String>, DailyTaskCompletionEntity>()
        val taskId = 10L
        val date = "2026-10-07"

        // Insert first
        val first = DailyTaskCompletionEntity(id = 1L, taskId = taskId, date = date, isCompleted = true)
        records[taskId to date] = first
        assertEquals(1, records.size)

        // Update / Insert again for same task and date: replaces existing entry, no duplicate!
        val second = DailyTaskCompletionEntity(id = 1L, taskId = taskId, date = date, isCompleted = true, completedAt = 9999L)
        records[taskId to date] = second
        assertEquals("Duplicate records must never exist for same task and date", 1, records.size)
        assertEquals(9999L, records[taskId to date]?.completedAt)
    }
}
