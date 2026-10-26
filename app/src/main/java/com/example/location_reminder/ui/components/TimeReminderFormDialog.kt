package com.example.location_reminder.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.example.location_reminder.ui.theme.pressScale
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.local.entity.EventHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.location.SuggestionFilterHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReminderFormDialog(
    initialReminderWithTasks: ReminderWithTasks? = null,
    recentEvents: List<EventHistoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReminderEntity, List<TaskEntity>) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val initialReminder = initialReminderWithTasks?.reminder
    val isEditing = initialReminder != null

    // Event title & details state
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var eventDescription by remember { mutableStateOf(initialReminder?.description ?: "") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var titleDropdownExpanded by remember { mutableStateOf(false) }
    var titleFieldWidthPx by remember { mutableIntStateOf(0) }
    val titleInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(titleInteractionSource) {
        titleInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                titleDropdownExpanded = true
            }
        }
    }

    val filteredEvents = remember(title, recentEvents, initialReminder) {
        SuggestionFilterHelper.filterEvents(
            query = title,
            history = recentEvents,
            editingInitialTitle = if (isEditing) initialReminder?.title else null,
            maxItems = 5
        )
    }

    // Date & Time selection state
    val defaultCalendar = remember {
        Calendar.getInstance().apply {
            if (initialReminder?.dueDateTime != null) {
                timeInMillis = initialReminder.dueDateTime
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    var selectedCalendar by remember { mutableStateOf(defaultCalendar) }
    var scheduledDateTimeMillis by remember {
        mutableLongStateOf(initialReminder?.dueDateTime ?: defaultCalendar.timeInMillis)
    }
    var dateTimeError by remember { mutableStateOf<String?>(null) }

    val formattedDate = remember(scheduledDateTimeMillis) {
        DateTimeFormatterHelper.formatFullDate(scheduledDateTimeMillis)
    }
    val formattedTime = remember(scheduledDateTimeMillis) {
        DateTimeFormatterHelper.formatTimeOnly(scheduledDateTimeMillis)
    }

    // Repeat alert interval state (default 5m)
    var repeatIntervalMinutes by remember {
        mutableLongStateOf(
            if (initialReminder != null) initialReminder.repeatIntervalMinutes else 5L
        )
    }

    fun showDatePicker() {
        val currentYear = selectedCalendar.get(Calendar.YEAR)
        val currentMonth = selectedCalendar.get(Calendar.MONTH)
        val currentDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = (selectedCalendar.clone() as Calendar).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedCalendar = newCal
                scheduledDateTimeMillis = newCal.timeInMillis
                if (scheduledDateTimeMillis > System.currentTimeMillis()) {
                    dateTimeError = null
                }
            },
            currentYear,
            currentMonth,
            currentDay
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    fun showTimePicker() {
        val currentHour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = selectedCalendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = (selectedCalendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedCalendar = newCal
                scheduledDateTimeMillis = newCal.timeInMillis
                if (scheduledDateTimeMillis > System.currentTimeMillis()) {
                    dateTimeError = null
                }
            },
            currentHour,
            currentMinute,
            false
        ).show()
    }

    fun validate(): Boolean {
        var isValid = true
        if (title.trim().isBlank()) {
            titleError = "Event name is required"
            isValid = false
        } else {
            titleError = null
        }

        if (scheduledDateTimeMillis <= System.currentTimeMillis()) {
            dateTimeError = "Please select a date and time in the future"
            isValid = false
        } else {
            dateTimeError = null
        }

        return isValid
    }

    fun performSave() {
        if (!validate()) return

        val reminderToSave = ReminderEntity(
            id = initialReminder?.id ?: 0L,
            title = title.trim(),
            description = eventDescription.trim(),
            latitude = 0.0,
            longitude = 0.0,
            locationName = "",
            formattedAddress = "",
            placeId = "",
            radius = 0.0,
            reminderMessage = if (eventDescription.trim().isNotBlank()) eventDescription.trim() else title.trim(),
            isEnabled = true,
            reminderType = ReminderType.DATE_TIME.name,
            dueDateTime = scheduledDateTimeMillis,
            isCompleted = initialReminder?.isCompleted ?: false,
            repeatIntervalMinutes = repeatIntervalMinutes,
            snoozedUntil = null,
            createdAt = initialReminder?.createdAt ?: System.currentTimeMillis()
        )

        onSave(reminderToSave, emptyList())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditing) "Edit Event" else "Add Event",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.pressScale(0.90f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { performSave() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .pressScale(0.95f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Event Name with Autocomplete Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (titleError != null) titleError = null
                            titleDropdownExpanded = true
                        },
                        label = { Text("Event Name *") },
                        placeholder = { Text("e.g., Doctor Appointment, Team Standup") },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        trailingIcon = {
                            if (filteredEvents.isNotEmpty() || (recentEvents.isNotEmpty() && !titleDropdownExpanded)) {
                                IconButton(onClick = { titleDropdownExpanded = !titleDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (titleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle event suggestions",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        isError = titleError != null,
                        supportingText = titleError?.let { { Text(it) } },
                        singleLine = true,
                        interactionSource = titleInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (titleFieldWidthPx != coordinates.size.width) {
                                    titleFieldWidthPx = coordinates.size.width
                                }
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    titleDropdownExpanded = true
                                }
                            }
                    )

                    DropdownMenu(
                        expanded = titleDropdownExpanded && filteredEvents.isNotEmpty(),
                        onDismissRequest = { titleDropdownExpanded = false },
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        ),
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .width(if (titleFieldWidthPx > 0) with(density) { titleFieldWidthPx.toDp() } else 300.dp)
                            .heightIn(max = 240.dp)
                    ) {
                        Text(
                            text = "PREVIOUS EVENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        filteredEvents.forEach { suggestedEvent ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestedEvent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    title = suggestedEvent
                                    if (titleError != null) titleError = null
                                    titleDropdownExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // DATE & TIME SELECTION SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Schedule Date & Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date selector button
                            OutlinedButton(
                                onClick = { showDatePicker() },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Time selector button
                            OutlinedButton(
                                onClick = { showTimePicker() },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (dateTimeError != null) {
                            Text(
                                text = dateTimeError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Alarm will fire at $formattedDate at $formattedTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // REPEAT ALERT SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Repeat Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (repeatIntervalMinutes > 0L) {
                                "Repeats every ${AlarmScheduler.formatRepeatInterval(repeatIntervalMinutes)} until completed or snoozed"
                            } else {
                                "Repeat alert is off — reminder will alert only once"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val repeatOptions = listOf(
                            0L to "Off",
                            5L to "5m",
                            10L to "10m",
                            15L to "15m",
                            30L to "30m",
                            60L to "1h"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeatOptions.take(3).forEach { (minutes, label) ->
                                FilterChip(
                                    selected = repeatIntervalMinutes == minutes,
                                    onClick = { repeatIntervalMinutes = minutes },
                                    label = { Text(label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .pressScale(0.96f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeatOptions.drop(3).forEach { (minutes, label) ->
                                FilterChip(
                                    selected = repeatIntervalMinutes == minutes,
                                    onClick = { repeatIntervalMinutes = minutes },
                                    label = { Text(label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .pressScale(0.96f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }                // EVENT DETAILS SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Event Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = eventDescription,
                            onValueChange = { eventDescription = it },
                            label = { Text("Event Notes / Description (Optional)") },
                            placeholder = { Text("Add any details, agenda, or notes for this event...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pressScale(0.96f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { performSave() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pressScale(0.96f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEditing) "Save Event" else "Create Event")
                    }
                }
            }
        }
    }
}
