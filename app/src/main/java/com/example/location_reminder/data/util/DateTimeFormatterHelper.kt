package com.example.location_reminder.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe, cached date and time formatters to avoid expensive SimpleDateFormat
 * allocations during Compose rendering and LazyColumn scrolling.
 */
object DateTimeFormatterHelper {

    private val dueDateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault())
    }

    private val snoozeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    }

    private val fullDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    }

    private val timeOnlyFormat = ThreadLocal.withInitial {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    }

    private val completedDateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    }

    private val isoDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val todayHeaderFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    }

    fun getTodayIsoDate(): String {
        return isoDateFormat.get()?.format(Date()) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun formatTodayHeader(date: Date = Date()): String {
        return todayHeaderFormat.get()?.format(date) ?: ""
    }

    fun formatDueDateTime(millis: Long): String {
        return dueDateTimeFormat.get()?.format(Date(millis)) ?: ""
    }

    fun formatCompletedDateTime(millis: Long): String {
        return completedDateTimeFormat.get()?.format(Date(millis)) ?: ""
    }

    fun formatSnoozeTime(millis: Long): String {
        return snoozeFormat.get()?.format(Date(millis)) ?: ""
    }

    fun formatFullDate(millis: Long): String {
        return fullDateFormat.get()?.format(Date(millis)) ?: ""
    }

    fun formatTimeOnly(millis: Long): String {
        return timeOnlyFormat.get()?.format(Date(millis)) ?: ""
    }

    private val historyDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("d MMM yyyy", Locale.US)
    }

    private val dayOfWeekFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE", Locale.US)
    }

    fun getIsoDate(date: Date): String {
        return isoDateFormat.get()?.format(date) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    fun parseIsoDate(isoDate: String): Date? {
        return try {
            isoDateFormat.get()?.parse(isoDate) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
        } catch (_: Exception) {
            null
        }
    }

    fun getLast7DaysIsoDates(baseDate: Date = Date()): List<String> {
        return getLastNDaysIsoDates(7, baseDate)
    }

    fun getLast30DaysIsoDates(baseDate: Date = Date()): List<String> {
        return getLastNDaysIsoDates(30, baseDate)
    }

    fun getLastNDaysIsoDates(days: Int, baseDate: Date = Date()): List<String> {
        val dates = ArrayList<String>(days)
        val calendar = java.util.Calendar.getInstance()
        calendar.time = baseDate
        for (i in 0 until days) {
            dates.add(getIsoDate(calendar.time))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return dates
    }

    fun formatDisplayDate(date: Date): String {
        return historyDateFormat.get()?.format(date) ?: SimpleDateFormat("d MMM yyyy", Locale.US).format(date)
    }

    fun getDayStartMillis(dateIso: String): Long? {
        val date = parseIsoDate(dateIso) ?: return null
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getDayEndMillis(dateIso: String): Long? {
        val date = parseIsoDate(dateIso) ?: return null
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun formatHistoryHeader(date: Date, daysAgo: Int): String {
        val dateStr = historyDateFormat.get()?.format(date)?.uppercase(Locale.US) ?: ""
        return when (daysAgo) {
            0 -> "TODAY — $dateStr"
            1 -> "YESTERDAY — $dateStr"
            else -> {
                val dayOfWeek = dayOfWeekFormat.get()?.format(date)?.uppercase(Locale.US) ?: ""
                "$dayOfWeek — $dateStr"
            }
        }
    }
}
