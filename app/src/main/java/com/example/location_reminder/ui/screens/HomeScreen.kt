package com.example.location_reminder.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import com.example.location_reminder.ui.theme.pressScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.location_reminder.data.local.entity.ReminderType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import com.example.location_reminder.data.preferences.AppTheme
import com.example.location_reminder.ui.components.SettingsDialog
import com.example.location_reminder.ui.theme.completedContainer
import com.example.location_reminder.ui.theme.completedContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.ui.components.DeleteConfirmationDialog
import com.example.location_reminder.ui.components.ReminderCard
import com.example.location_reminder.ui.components.ReminderFormDialog
import com.example.location_reminder.ui.components.ReminderTypeChoiceDialog
import com.example.location_reminder.ui.components.TimeReminderFormDialog
import com.example.location_reminder.viewmodel.ReminderViewModel

enum class ReminderFilter {
    ALL,
    ACTIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReminderViewModel,
    currentTheme: AppTheme = AppTheme.DARK,
    onThemeChanged: (AppTheme) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val remindersWithTasks by viewModel.activeRemindersWithTasks.collectAsStateWithLifecycle()
    val recentTitles by viewModel.recentTitles.collectAsStateWithLifecycle()
    val recentLocations by viewModel.recentLocations.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(ReminderFilter.ALL) }
    var showFormDialog by remember { mutableStateOf(false) }
    var showTimeFormDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showDailyTasksScreen by remember { mutableStateOf(false) }
    var showDailyTasksHistoryScreen by remember { mutableStateOf(false) }
    var showReminderTypeChoiceDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingReminderWithTasks by remember { mutableStateOf<ReminderWithTasks?>(null) }
    var deletingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    val totalCount = remember(remindersWithTasks) { remindersWithTasks.size }
    val activeCount = remember(remindersWithTasks) { remindersWithTasks.count { it.reminder.isEnabled && !it.reminder.isCompleted } }

    val filteredReminders = remember(remindersWithTasks, selectedFilter) {
        when (selectedFilter) {
            ReminderFilter.ALL -> remindersWithTasks
            ReminderFilter.ACTIVE -> remindersWithTasks.filter { it.reminder.isEnabled && !it.reminder.isCompleted }
        }
    }

    val context = LocalContext.current
    val hasActiveLocationReminders = remember(remindersWithTasks) {
        remindersWithTasks.any {
            it.reminder.isEnabled && !it.reminder.isCompleted &&
                    (it.reminder.reminderType == ReminderType.LOCATION.name || it.reminder.reminderType == ReminderType.LOCATION_AND_TIME.name)
        }
    }
    val hasActiveDateTimeReminders = remember(remindersWithTasks) {
        remindersWithTasks.any {
            it.reminder.isEnabled && !it.reminder.isCompleted &&
                    (it.reminder.reminderType == ReminderType.DATE_TIME.name || it.reminder.reminderType == ReminderType.LOCATION_AND_TIME.name)
        }
    }

    val capabilityStatus = remember(hasActiveLocationReminders, hasActiveDateTimeReminders) {
        com.example.location_reminder.data.util.PermissionHelper.getCapabilityStatus(
            context = context,
            hasActiveLocationReminders = hasActiveLocationReminders,
            hasActiveDateTimeReminders = hasActiveDateTimeReminders
        )
    }

    // Hoisted stable callbacks for LazyColumn items
    val onToggleEnabled = remember(viewModel) {
        { reminderId: Long, isEnabled: Boolean ->
            viewModel.toggleReminderEnabled(reminderId, isEnabled)
        }
    }
    val onToggleCompleted = remember(viewModel) {
        { reminderId: Long, isCompleted: Boolean ->
            viewModel.toggleReminderCompleted(reminderId, isCompleted)
        }
    }
    val onEditReminder = remember {
        { item: ReminderWithTasks ->
            editingReminderWithTasks = item
            if (item.reminder.reminderType == ReminderType.DATE_TIME.name) {
                showTimeFormDialog = true
            } else {
                showFormDialog = true
            }
        }
    }
    val onDeleteReminder = remember {
        { reminder: ReminderEntity ->
            deletingReminder = reminder
        }
    }
    val onAddTask = remember(viewModel) {
        { reminderId: Long, taskTitle: String ->
            viewModel.addTask(reminderId, taskTitle)
        }
    }
    val onToggleTaskCompleted = remember(viewModel) {
        { taskId: Long, isCompleted: Boolean ->
            viewModel.toggleTaskCompleted(taskId, isCompleted)
        }
    }
    val onDeleteTask = remember(viewModel) {
        { task: TaskEntity ->
            viewModel.deleteTask(task)
        }
    }

    val currentScreen = when {
        showDailyTasksHistoryScreen -> 5
        showDailyTasksScreen -> 4
        showTimeFormDialog -> 1
        showFormDialog -> 2
        showHistoryScreen -> 3
        else -> 0
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == 5 || targetState == 4) {
                (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(200)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(150)))
            } else if (initialState == 5 || initialState == 4) {
                (slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(180)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(150)))
            } else {
                (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.98f, animationSpec = tween(150)))
                    .togetherWith(fadeOut(animationSpec = tween(120)))
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            1 -> {
                BackHandler {
                    showTimeFormDialog = false
                    editingReminderWithTasks = null
                }
                TimeReminderFormDialog(
                    initialReminderWithTasks = editingReminderWithTasks,
                    recentEvents = recentEvents,
                    onDismiss = {
                        showTimeFormDialog = false
                        editingReminderWithTasks = null
                    },
                    onSave = { reminderToSave, tasksToSave ->
                        if (editingReminderWithTasks != null) {
                            viewModel.updateReminderWithTasks(reminderToSave, tasksToSave)
                        } else {
                            viewModel.addReminderWithTasks(reminderToSave, tasksToSave)
                        }
                        showTimeFormDialog = false
                        editingReminderWithTasks = null
                    }
                )
            }
            2 -> {
                BackHandler {
                    showFormDialog = false
                    editingReminderWithTasks = null
                }
                ReminderFormDialog(
                    initialReminderWithTasks = editingReminderWithTasks,
                    recentTitles = recentTitles,
                    recentLocations = recentLocations,
                    onDismiss = {
                        showFormDialog = false
                        editingReminderWithTasks = null
                    },
                    onSave = { reminderToSave, tasksToSave ->
                        if (editingReminderWithTasks != null) {
                            viewModel.updateReminderWithTasks(reminderToSave, tasksToSave)
                        } else {
                            viewModel.addReminderWithTasks(reminderToSave, tasksToSave)
                        }
                        showFormDialog = false
                        editingReminderWithTasks = null
                    }
                )
            }
            3 -> {
                BackHandler {
                    showHistoryScreen = false
                }
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showHistoryScreen = false }
                )
            }
            4 -> {
                BackHandler {
                    showDailyTasksScreen = false
                }
                DailyTasksScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showDailyTasksScreen = false },
                    onNavigateToHistory = { showDailyTasksHistoryScreen = true }
                )
            }
            5 -> {
                BackHandler {
                    showDailyTasksHistoryScreen = false
                }
                DailyTasksHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showDailyTasksHistoryScreen = false }
                )
            }
            else -> {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location Reminder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.pressScale(0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showReminderTypeChoiceDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .pressScale(0.96f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reminders",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                showDailyTasksScreen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .pressScale(0.96f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Tasks",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header summary banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tasks & reminders at your places & schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Total: $totalCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Active: $activeCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Capability & Permission Guidance Banners

            if (capabilityStatus.showLocationDisabledWarning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device Location Turned Off",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Location services are turned off. Turn on location for arrival reminders to work.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createLocationSettingsIntent())
                                } catch (e: Exception) {
                                    context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createAppDetailsSettingsIntent(context))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .pressScale(0.95f)
                        ) {
                            Text("Turn On", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else if (capabilityStatus.showForegroundLocationWarning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Location Permission Needed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Location access is needed so the app can detect when you arrive at reminder locations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createAppDetailsSettingsIntent(context))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .pressScale(0.95f)
                        ) {
                            Text("Settings", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else if (capabilityStatus.showBackgroundLocationWarning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Location Recommended",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "To trigger arrival alerts while your screen is locked, allow 'All the time' in location settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createAppDetailsSettingsIntent(context))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .pressScale(0.95f)
                        ) {
                            Text("Settings", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (capabilityStatus.showNotificationWarning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications Disabled",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "App notifications are turned off. Alerts cannot be displayed until notifications are enabled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createAppDetailsSettingsIntent(context))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .pressScale(0.95f)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (capabilityStatus.showExactAlarmWarning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarms Restricted",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Scheduled alerts may be slightly delayed. Allow exact alarms in settings for precise timing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(com.example.location_reminder.data.util.PermissionHelper.createExactAlarmSettingsIntent(context))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .pressScale(0.95f)
                        ) {
                            Text("Allow", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ReminderFilter.ALL,
                    onClick = { selectedFilter = ReminderFilter.ALL },
                    label = { Text("All ($totalCount)") },
                    modifier = Modifier.pressScale(0.96f)
                )
                FilterChip(
                    selected = selectedFilter == ReminderFilter.ACTIVE,
                    onClick = { selectedFilter = ReminderFilter.ACTIVE },
                    label = { Text("Active ($activeCount)") },
                    modifier = Modifier.pressScale(0.96f)
                )
            }

            // Reminders List or Empty State
            if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedFilter) {
                                ReminderFilter.ALL -> "No Active Reminders"
                                ReminderFilter.ACTIVE -> "No Active Reminders"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedFilter) {
                                ReminderFilter.ALL -> "You don't have any active reminders.\nTap 'Reminders' below to set one up."
                                ReminderFilter.ACTIVE -> "All your reminders are currently completed or disabled."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedFilter == ReminderFilter.ALL) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    showReminderTypeChoiceDialog = true
                                },
                                modifier = Modifier.pressScale(0.95f)
                            ) {
                                Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Reminder")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredReminders,
                        key = { it.reminder.id },
                        contentType = { "reminder_card" }
                    ) { item ->
                        ReminderCard(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(150),
                                fadeOutSpec = tween(150),
                                placementSpec = tween(150)
                            ),
                            reminderWithTasks = item,
                            onToggleEnabled = onToggleEnabled,
                            onToggleCompleted = onToggleCompleted,
                            onEdit = onEditReminder,
                            onDelete = onDeleteReminder,
                            onAddTask = onAddTask,
                            onToggleTaskCompleted = onToggleTaskCompleted,
                            onDeleteTask = onDeleteTask
                        )
                    }
                }
            }
            }
        }
    }
}

        // Delete Confirmation Dialog
        deletingReminder?.let { reminder ->
            DeleteConfirmationDialog(
                reminder = reminder,
                onConfirm = {
                    viewModel.deleteReminder(reminder)
                    deletingReminder = null
                },
                onDismiss = {
                    deletingReminder = null
                }
            )
        }

        // Reminder Type Choice Dialog (Location vs Time)
        if (showReminderTypeChoiceDialog) {
            ReminderTypeChoiceDialog(
                onSelectLocationBased = {
                    showReminderTypeChoiceDialog = false
                    editingReminderWithTasks = null
                    showFormDialog = true
                },
                onSelectTimeBased = {
                    showReminderTypeChoiceDialog = false
                    editingReminderWithTasks = null
                    showTimeFormDialog = true
                },
                onSelectHistory = {
                    showReminderTypeChoiceDialog = false
                    showHistoryScreen = true
                },
                onDismiss = {
                    showReminderTypeChoiceDialog = false
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}