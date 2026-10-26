package com.example.location_reminder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import com.example.location_reminder.ui.components.DeleteConfirmationDialog
import com.example.location_reminder.ui.theme.completedContainer
import com.example.location_reminder.ui.theme.completedContent
import com.example.location_reminder.ui.theme.pressScale
import com.example.location_reminder.viewmodel.ReminderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedReminders by viewModel.completedRemindersWithTasks.collectAsStateWithLifecycle()
    var deletingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val onReactivate = remember(viewModel) {
        { reminderId: Long -> viewModel.reactivateReminder(reminderId) }
    }
    val onDelete = remember {
        { reminder: ReminderEntity -> deletingReminder = reminder }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (completedReminders.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${completedReminders.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressScale(0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (completedReminders.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(34.dp)
                                .pressScale(0.95f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear History",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
        if (completedReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No History Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed Location Reminders and Time Events will automatically appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = completedReminders,
                    key = { it.reminder.id },
                    contentType = { "history_card" }
                ) { item ->
                    HistoryReminderCard(
                        reminderWithTasks = item,
                        onReactivate = onReactivate,
                        onDelete = onDelete
                    )
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Clear History?")
            },
            text = {
                Text(text = "This will permanently delete all completed reminders from History.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.pressScale(0.95f)
                ) {
                    Text(
                        text = "Clear History",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false },
                    modifier = Modifier.pressScale(0.95f)
                ) {
                    Text("Cancel")
                }
            }
        )
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
}

@Composable
private fun HistoryReminderCard(
    reminderWithTasks: ReminderWithTasks,
    onReactivate: (reminderId: Long) -> Unit,
    onDelete: (reminder: ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder = reminderWithTasks.reminder
    val tasks = reminderWithTasks.tasks

    val isDateTime = reminder.reminderType == ReminderType.DATE_TIME.name

    val completedTimeStr = remember(reminder.completedAt, reminder.createdAt) {
        DateTimeFormatterHelper.formatCompletedDateTime(reminder.completedAt ?: reminder.createdAt)
    }

    val scheduledTimeStr = remember(reminder.dueDateTime) {
        reminder.dueDateTime?.let { DateTimeFormatterHelper.formatDueDateTime(it) }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Full-width Completed Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.completedContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.completedContent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Completed: $completedTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.completedContent
                    )
                }
            }

            // Title
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Event Details / Description
            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Scheduled Date/Time info if applicable
            if (scheduledTimeStr != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Scheduled",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scheduled: $scheduledTimeStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Location information if applicable
            if (!isDateTime && reminder.locationName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reminder.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (reminder.radius > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${reminder.radius.toInt()}m)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (reminder.formattedAddress.isNotBlank()) {
                    Text(
                        text = reminder.formattedAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }

            // Tasks / Items count if present for Location reminders
            if (!isDateTime && tasks.isNotEmpty()) {
                val completedCount = remember(tasks) { tasks.count { it.isCompleted } }
                Text(
                    text = "Items: $completedCount/${tasks.size} completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Bottom Actions: Reactivate & Delete
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reactivateInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onReactivate(reminder.id) },
                    interactionSource = reactivateInteraction,
                    modifier = Modifier
                        .height(36.dp)
                        .pressScale(reactivateInteraction, 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reactivate",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                val deleteInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { onDelete(reminder) },
                    interactionSource = deleteInteraction,
                    modifier = Modifier
                        .size(36.dp)
                        .pressScale(deleteInteraction, 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from history",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
