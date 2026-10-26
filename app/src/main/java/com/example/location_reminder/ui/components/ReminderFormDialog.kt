package com.example.location_reminder.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Warning
import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.ui.theme.pressScale
import com.example.location_reminder.ui.theme.bounceClickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity
import com.example.location_reminder.data.location.SuggestionFilterHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    initialReminderWithTasks: ReminderWithTasks? = null,
    recentTitles: List<TitleHistoryEntity> = emptyList(),
    recentLocations: List<LocationHistoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReminderEntity, List<TaskEntity>) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val initialReminder = initialReminderWithTasks?.reminder
    val isEditing = initialReminder != null

    // Title
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }

    // Title Dropdown State
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

    val filteredTitles = remember(title, recentTitles, initialReminder) {
        SuggestionFilterHelper.filterTitles(
            query = title,
            history = recentTitles,
            editingInitialTitle = if (isEditing) initialReminder?.title else null,
            maxItems = 5
        )
    }

    // Location Fields
    var locationName by remember { mutableStateOf(initialReminder?.locationName ?: "") }
    var latitude by remember {
        mutableStateOf(
            if (initialReminder != null && (initialReminder.latitude != 0.0 || initialReminder.longitude != 0.0)) {
                String.format(Locale.US, "%.5f", initialReminder.latitude)
            } else {
                ""
            }
        )
    }
    var longitude by remember {
        mutableStateOf(
            if (initialReminder != null && (initialReminder.latitude != 0.0 || initialReminder.longitude != 0.0)) {
                String.format(Locale.US, "%.5f", initialReminder.longitude)
            } else {
                ""
            }
        )
    }
    var radius by remember { mutableStateOf(initialReminder?.radius?.toInt()?.toString() ?: "100") }

    // Location Dropdown & Consistency State
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var locationFieldWidthPx by remember { mutableIntStateOf(0) }
    val locationInteractionSource = remember { MutableInteractionSource() }
    var lastAssociatedLocationName by remember { mutableStateOf(initialReminder?.locationName ?: "") }
    var showMismatchConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationInteractionSource) {
        locationInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                locationDropdownExpanded = true
            }
        }
    }

    val filteredLocations = remember(locationName, recentLocations) {
        SuggestionFilterHelper.filterLocations(
            query = locationName,
            history = recentLocations,
            maxItems = 20
        )
    }

    // Detect if location name was changed from its associated coordinates
    val hasValidCoordinates = latitude.trim().toDoubleOrNull()?.let { it != 0.0 } == true &&
        longitude.trim().toDoubleOrNull()?.let { it != 0.0 } == true
    val isLocationNameChangedFromAssociated = hasValidCoordinates &&
        lastAssociatedLocationName.isNotBlank() &&
        !locationName.trim().equals(lastAssociatedLocationName.trim(), ignoreCase = true)

    // Map picker dialog state
    var showMapPicker by remember { mutableStateOf(false) }

    // Repeat Alert Setting (Default: 5 minutes)
    var repeatIntervalMinutes by remember {
        mutableStateOf(initialReminder?.repeatIntervalMinutes ?: 5L)
    }

    // Individual Items Checklist State
    val draftTasks = remember {
        mutableStateListOf<TaskEntity>().apply {
            initialReminderWithTasks?.tasks?.let { addAll(it) }
        }
    }
    var newTaskTitle by remember { mutableStateOf("") }

    // Validation Errors
    var titleError by remember { mutableStateOf<String?>(null) }
    var locationNameError by remember { mutableStateOf<String?>(null) }
    var latitudeError by remember { mutableStateOf<String?>(null) }
    var longitudeError by remember { mutableStateOf<String?>(null) }
    var radiusError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (title.trim().isBlank()) {
            titleError = "Title cannot be empty"
            isValid = false
        } else {
            titleError = null
        }

        if (locationName.trim().isBlank()) {
            locationNameError = "Location name cannot be empty"
            isValid = false
        } else {
            locationNameError = null
        }

        val lat = latitude.trim().toDoubleOrNull()
        if (lat == null || lat < -90.0 || lat > 90.0) {
            latitudeError = "Select location on map or enter latitude between -90 and 90"
            isValid = false
        } else {
            latitudeError = null
        }

        val lng = longitude.trim().toDoubleOrNull()
        if (lng == null || lng < -180.0 || lng > 180.0) {
            longitudeError = "Select location on map or enter longitude between -180 and 180"
            isValid = false
        } else {
            longitudeError = null
        }

        val rad = radius.trim().toDoubleOrNull()
        if (rad == null || rad <= 0.0) {
            radiusError = "Please select a radius"
            isValid = false
        } else {
            radiusError = null
        }

        return isValid
    }

    fun performSave() {
        val lat = latitude.trim().toDoubleOrNull() ?: 0.0
        val lng = longitude.trim().toDoubleOrNull() ?: 0.0
        val rad = radius.trim().toDoubleOrNull() ?: 100.0
        val locName = locationName.trim()

        val reminderToSave = if (initialReminder != null) {
            initialReminder.copy(
                title = title.trim(),
                description = "",
                locationName = locName,
                latitude = lat,
                longitude = lng,
                radius = rad,
                reminderMessage = title.trim(),
                reminderType = ReminderType.LOCATION.name,
                dueDateTime = null,
                repeatIntervalMinutes = repeatIntervalMinutes
            )
        } else {
            ReminderEntity(
                title = title.trim(),
                description = "",
                locationName = locName,
                latitude = lat,
                longitude = lng,
                radius = rad,
                reminderMessage = title.trim(),
                isEnabled = true,
                reminderType = ReminderType.LOCATION.name,
                dueDateTime = null,
                repeatIntervalMinutes = repeatIntervalMinutes,
                isCompleted = false
            )
        }

        onSave(reminderToSave, draftTasks.toList())
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Location Reminder" else "New Location Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.pressScale(0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back / Close"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (validate()) {
                                if (isLocationNameChangedFromAssociated) {
                                    showMismatchConfirmDialog = true
                                } else {
                                    performSave()
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .pressScale(0.95f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Update" else "Save")
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
            // Title with Autocomplete Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                            title = it
                            if (titleError != null) titleError = null
                            titleDropdownExpanded = true
                        },
                        label = { Text("Title *") },
                        placeholder = { Text("e.g., Grocery Shopping") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        trailingIcon = {
                            if (filteredTitles.isNotEmpty() || (recentTitles.isNotEmpty() && !titleDropdownExpanded)) {
                                IconButton(onClick = { titleDropdownExpanded = !titleDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (titleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle title suggestions",
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
                        expanded = titleDropdownExpanded && filteredTitles.isNotEmpty(),
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
                            text = "RECENT TITLES (LAST 5)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        filteredTitles.forEach { suggestedTitle ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestedTitle,
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
                                    title = suggestedTitle
                                    if (titleError != null) titleError = null
                                    titleDropdownExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // ITEMS SECTION (moved to where Description and Reminder Message were)
                var editingItemIndex by remember { mutableStateOf<Int?>(null) }
                var editingItemText by remember { mutableStateOf("") }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Items (${draftTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Add new item input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                placeholder = { Text("Add item...", style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newTaskTitle.trim().isNotBlank()) {
                                        draftTasks.add(
                                            TaskEntity(
                                                reminderId = initialReminder?.id ?: 0L,
                                                title = newTaskTitle.trim(),
                                                isCompleted = false
                                            )
                                        )
                                        newTaskTitle = ""
                                    }
                                }),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (newTaskTitle.trim().isNotBlank()) {
                                        draftTasks.add(
                                            TaskEntity(
                                                reminderId = initialReminder?.id ?: 0L,
                                                title = newTaskTitle.trim(),
                                                isCompleted = false
                                            )
                                        )
                                        newTaskTitle = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.pressScale(0.95f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add item")
                            }
                        }

                        // List of items
                        if (draftTasks.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                draftTasks.forEachIndexed { index, task ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        if (editingItemIndex == index) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editingItemText,
                                                    onValueChange = { editingItemText = it },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        if (editingItemText.trim().isNotBlank()) {
                                                            draftTasks[index] = task.copy(title = editingItemText.trim())
                                                        }
                                                        editingItemIndex = null
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .pressScale(0.88f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Save item edit",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { editingItemIndex = null },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .pressScale(0.88f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Cancel item edit",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = task.isCompleted,
                                                    onCheckedChange = { checked ->
                                                        draftTasks[index] = task.copy(isCompleted = checked)
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .pressScale(0.90f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            editingItemIndex = index
                                                            editingItemText = task.title
                                                        }
                                                )
                                                IconButton(
                                                    onClick = {
                                                        editingItemIndex = index
                                                        editingItemText = task.title
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .pressScale(0.88f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit item",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { draftTasks.removeAt(index) },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .pressScale(0.88f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete item",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // LOCATION SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Location Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Prominent Map Location Picker Button
                            Button(
                                onClick = { showMapPicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(0.96f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Location on Map")
                            }

                            // Location Name with Autocomplete Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = locationName,
                                    onValueChange = {
                                        locationName = it
                                        if (locationNameError != null) locationNameError = null
                                        locationDropdownExpanded = true
                                    },
                                    label = { Text("Location Name *") },
                                    placeholder = { Text("e.g., Downtown Supermarket") },
                                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                    trailingIcon = {
                                        if (filteredLocations.isNotEmpty() || (recentLocations.isNotEmpty() && !locationDropdownExpanded)) {
                                            IconButton(onClick = { locationDropdownExpanded = !locationDropdownExpanded }) {
                                                Icon(
                                                    imageVector = if (locationDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = "Toggle location suggestions",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    isError = locationNameError != null,
                                    supportingText = locationNameError?.let { { Text(it) } },
                                    singleLine = true,
                                    interactionSource = locationInteractionSource,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coordinates ->
                                            if (locationFieldWidthPx != coordinates.size.width) {
                                                locationFieldWidthPx = coordinates.size.width
                                            }
                                        }
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                locationDropdownExpanded = true
                                            }
                                        }
                                )

                                DropdownMenu(
                                    expanded = locationDropdownExpanded && filteredLocations.isNotEmpty(),
                                    onDismissRequest = { locationDropdownExpanded = false },
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
                                        .width(if (locationFieldWidthPx > 0) with(density) { locationFieldWidthPx.toDp() } else 300.dp)
                                        .heightIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = "RECENT LOCATIONS (UP TO 20)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    filteredLocations.forEach { loc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = loc.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.4f", loc.latitude)}, ${String.format(Locale.US, "%.4f", loc.longitude)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            onClick = {
                                                locationName = loc.name
                                                latitude = String.format(Locale.US, "%.5f", loc.latitude)
                                                longitude = String.format(Locale.US, "%.5f", loc.longitude)
                                                lastAssociatedLocationName = loc.name
                                                locationNameError = null
                                                latitudeError = null
                                                longitudeError = null
                                                locationDropdownExpanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Location Consistency Warning: name changed from associated location
                            if (isLocationNameChangedFromAssociated) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Coordinates belong to \"$lastAssociatedLocationName\"",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Text(
                                            text = "Name was changed to \"${locationName.trim()}\". Re-select on map or confirm using current coordinates.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    lastAssociatedLocationName = locationName.trim()
                                                }
                                            ) {
                                                Text("Confirm for this name", style = MaterialTheme.typography.labelSmall)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            OutlinedButton(
                                                onClick = { showMapPicker = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .pressScale(0.95f)
                                            ) {
                                                Text("Re-pick Map", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            // Latitude and Longitude Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = latitude,
                                    onValueChange = {
                                        latitude = it
                                        if (latitudeError != null) latitudeError = null
                                        lastAssociatedLocationName = locationName.trim()
                                    },
                                    label = { Text("Latitude *") },
                                    placeholder = { Text("37.7749") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = latitudeError != null,
                                    supportingText = latitudeError?.let { { Text(it) } },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = longitude,
                                    onValueChange = {
                                        longitude = it
                                        if (longitudeError != null) longitudeError = null
                                        lastAssociatedLocationName = locationName.trim()
                                    },
                                    label = { Text("Longitude *") },
                                    placeholder = { Text("-122.4194") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = longitudeError != null,
                                    supportingText = longitudeError?.let { { Text(it) } },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Radius - exactly three selectable options: 100m, 200m, 300m
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Radius *",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val radiusOptions = listOf(100, 200, 300)
                                    val currentRadius = radius.toDoubleOrNull()?.toInt() ?: 100
                                    radiusOptions.forEach { r ->
                                        val isSelected = currentRadius == r
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .pressScale(0.97f)
                                                .clickable {
                                                    radius = r.toString()
                                                    radiusError = null
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        radius = r.toString()
                                                        radiusError = null
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$r m",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                radiusError?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                // REPEAT ALERTS SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Repeat Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = if (repeatIntervalMinutes == 0L) {
                                "Alert repeats: Off (alert rings once)"
                            } else {
                                "Repeats every ${AlarmScheduler.formatRepeatInterval(repeatIntervalMinutes)} until completed or snoozed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = repeatIntervalMinutes == 0L,
                                onClick = { repeatIntervalMinutes = 0L },
                                label = { Text("Off", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                            FilterChip(
                                selected = repeatIntervalMinutes == 5L,
                                onClick = { repeatIntervalMinutes = 5L },
                                label = { Text("5m", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                            FilterChip(
                                selected = repeatIntervalMinutes == 10L,
                                onClick = { repeatIntervalMinutes = 10L },
                                label = { Text("10m", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = repeatIntervalMinutes == 15L,
                                onClick = { repeatIntervalMinutes = 15L },
                                label = { Text("15m", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                            FilterChip(
                                selected = repeatIntervalMinutes == 30L,
                                onClick = { repeatIntervalMinutes = 30L },
                                label = { Text("30m", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                            FilterChip(
                                selected = repeatIntervalMinutes == 60L,
                                onClick = { repeatIntervalMinutes = 60L },
                                label = { Text("1h", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.96f)
                            )
                        }
                    }
                }

                // Bottom Action Area: Cancel and Save buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .pressScale(0.96f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = {
                            if (validate()) {
                                if (isLocationNameChangedFromAssociated) {
                                    showMismatchConfirmDialog = true
                                } else {
                                    performSave()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .pressScale(0.96f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEditing) "Update Reminder" else "Save Reminder", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

    // Location Consistency Confirmation Dialog
    if (showMismatchConfirmDialog) {
        val lat = latitude.trim().toDoubleOrNull() ?: 0.0
        val lng = longitude.trim().toDoubleOrNull() ?: 0.0
        val rad = radius.trim().toDoubleOrNull() ?: 100.0
        val locName = locationName.trim()

        AlertDialog(
            onDismissRequest = { showMismatchConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Location Coordinates") },
            text = {
                Text(
                    "You changed the location name to \"${locationName.trim()}\", but coordinates currently point to \"$lastAssociatedLocationName\".\n\nWould you like to keep these coordinates for \"${locationName.trim()}\", or re-select the location on the map?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        lastAssociatedLocationName = locationName.trim()
                        showMismatchConfirmDialog = false
                        val reminderToSave = if (initialReminder != null) {
                            initialReminder.copy(
                                title = title.trim(),
                                description = "",
                                locationName = locName,
                                latitude = lat,
                                longitude = lng,
                                radius = rad,
                                reminderMessage = title.trim(),
                                reminderType = ReminderType.LOCATION.name,
                                dueDateTime = null,
                                repeatIntervalMinutes = repeatIntervalMinutes
                            )
                        } else {
                            ReminderEntity(
                                title = title.trim(),
                                description = "",
                                locationName = locName,
                                latitude = lat,
                                longitude = lng,
                                radius = rad,
                                reminderMessage = title.trim(),
                                isEnabled = true,
                                reminderType = ReminderType.LOCATION.name,
                                dueDateTime = null,
                                repeatIntervalMinutes = repeatIntervalMinutes,
                                isCompleted = false
                            )
                        }
                        onSave(reminderToSave, draftTasks.toList())
                    },
                    modifier = Modifier.pressScale(0.95f)
                ) {
                    Text("Confirm & Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { showMismatchConfirmDialog = false },
                        modifier = Modifier.pressScale(0.95f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = {
                            showMismatchConfirmDialog = false
                            showMapPicker = true
                        },
                        modifier = Modifier.pressScale(0.95f)
                    ) {
                        Text("Re-pick Map")
                    }
                }
            }
        )
    }

    // Dedicated Map-Based Location Picker Dialog
    if (showMapPicker) {
        LocationPickerDialog(
            initialLatitude = latitude.trim().toDoubleOrNull() ?: 0.0,
            initialLongitude = longitude.trim().toDoubleOrNull() ?: 0.0,
            initialLocationName = locationName,
            recentLocations = recentLocations,
            onDismiss = { showMapPicker = false },
            onLocationConfirmed = { place ->
                locationName = place.title
                latitude = String.format(Locale.US, "%.5f", place.latitude)
                longitude = String.format(Locale.US, "%.5f", place.longitude)
                lastAssociatedLocationName = place.title
                if (locationNameError != null) locationNameError = null
                if (latitudeError != null) latitudeError = null
                if (longitudeError != null) longitudeError = null
                showMapPicker = false
            }
        )
    }
}

/**
 * Backward compatibility alias for [AddEditReminderScreen].
 */
@Composable
fun ReminderFormDialog(
    initialReminderWithTasks: ReminderWithTasks? = null,
    recentTitles: List<TitleHistoryEntity> = emptyList(),
    recentLocations: List<LocationHistoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReminderEntity, List<TaskEntity>) -> Unit
) {
    AddEditReminderScreen(
        initialReminderWithTasks = initialReminderWithTasks,
        recentTitles = recentTitles,
        recentLocations = recentLocations,
        onDismiss = onDismiss,
        onSave = onSave
    )
}