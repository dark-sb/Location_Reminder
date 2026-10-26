package com.example.location_reminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import com.example.location_reminder.ui.theme.CompletedContainerDark
import com.example.location_reminder.ui.theme.CompletedContainerLight
import com.example.location_reminder.ui.theme.CompletedGreenDark
import com.example.location_reminder.ui.theme.CompletedGreenLight
import com.example.location_reminder.ui.theme.DailyTaskGreenBorderDark
import com.example.location_reminder.ui.theme.DailyTaskGreenBorderLight
import com.example.location_reminder.ui.theme.DailyTaskGreenContainerDark
import com.example.location_reminder.ui.theme.DailyTaskGreenContainerLight
import com.example.location_reminder.ui.theme.DailyTaskGreenDark
import com.example.location_reminder.ui.theme.DailyTaskGreenLight
import com.example.location_reminder.ui.theme.DailyTaskOrangeBorderDark
import com.example.location_reminder.ui.theme.DailyTaskOrangeBorderLight
import com.example.location_reminder.ui.theme.DailyTaskOrangeContainerDark
import com.example.location_reminder.ui.theme.DailyTaskOrangeContainerLight
import com.example.location_reminder.ui.theme.DailyTaskOrangeDark
import com.example.location_reminder.ui.theme.DailyTaskOrangeLight
import com.example.location_reminder.ui.theme.pressScale
import com.example.location_reminder.viewmodel.ReminderViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTasksScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentDate()
    }

    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<DailyTaskWithCompletion?>(null) }
    var taskToDelete by remember { mutableStateOf<DailyTaskWithCompletion?>(null) }

    val totalCount = remember(dailyTasks) { dailyTasks.size }
    val completedCount = remember(dailyTasks) { dailyTasks.count { it.isCompleted } }
    val completionFraction = remember(totalCount, completedCount) {
        if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    }
    val todayHeader = remember { DateTimeFormatterHelper.formatTodayHeader(Date()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = todayHeader,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Daily Tasks History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Daily Task") },
                modifier = Modifier.pressScale(0.95f)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header summary card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (totalCount > 0 && completedCount == totalCount) {
                                if (isDark) CompletedContainerDark else CompletedContainerLight
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                text = "$completedCount / $totalCount completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalCount > 0 && completedCount == totalCount) {
                                    if (isDark) CompletedGreenDark else CompletedGreenLight
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (totalCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { completionFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (completedCount == totalCount) {
                                if (isDark) DailyTaskGreenDark else DailyTaskGreenLight
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recurring daily • Resets automatically every midnight",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dailyTasks.isEmpty()) {
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
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Daily Tasks Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add everyday habits like 'Drink 4L water', 'Gym', or 'Study DSA'.\nTasks automatically reset to incomplete every new day.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.pressScale(0.95f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Daily Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = dailyTasks,
                        key = { it.taskId }
                    ) { item ->
                        DailyTaskCard(
                            item = item,
                            isDark = isDark,
                            onToggleComplete = {
                                viewModel.toggleDailyTaskCompleted(item.taskId, !item.isCompleted)
                            },
                            onEdit = { taskToEdit = item },
                            onDelete = { taskToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Add Daily Task Dialog
    if (showAddDialog) {
        AddEditDailyTaskDialog(
            initialTitle = "",
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onConfirm = { newTitle ->
                viewModel.addDailyTask(newTitle)
                showAddDialog = false
            }
        )
    }

    // Edit Daily Task Dialog
    taskToEdit?.let { item ->
        AddEditDailyTaskDialog(
            initialTitle = item.title,
            isEdit = true,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedTitle ->
                viewModel.updateDailyTask(
                    DailyTaskEntity(
                        id = item.taskId,
                        title = updatedTitle,
                        createdAt = item.createdAt
                    )
                )
                taskToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    taskToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Daily Task?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${item.title}\"? Past completion history will be preserved.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDailyTask(item.taskId)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.pressScale(0.95f)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { taskToDelete = null },
                    modifier = Modifier.pressScale(0.95f)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DailyTaskCard(
    item: DailyTaskWithCompletion,
    isDark: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = item.isCompleted

    // Theme-aware colors: ORANGE for incomplete, GREEN for completed
    val targetBorderColor = if (isCompleted) {
        if (isDark) DailyTaskGreenBorderDark else DailyTaskGreenBorderLight
    } else {
        if (isDark) DailyTaskOrangeBorderDark else DailyTaskOrangeBorderLight
    }

    val targetContainerColor = if (isCompleted) {
        if (isDark) DailyTaskGreenContainerDark else DailyTaskGreenContainerLight
    } else {
        if (isDark) DailyTaskOrangeContainerDark else DailyTaskOrangeContainerLight
    }

    val targetAccentColor = if (isCompleted) {
        if (isDark) DailyTaskGreenDark else DailyTaskGreenLight
    } else {
        if (isDark) DailyTaskOrangeDark else DailyTaskOrangeLight
    }

    val borderColor by animateColorAsState(targetBorderColor, animationSpec = tween(150), label = "cardBorder")
    val containerColor by animateColorAsState(targetContainerColor, animationSpec = tween(150), label = "cardContainer")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .clickable { onToggleComplete() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion Checkbox Button
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onToggleComplete() },
                shape = CircleShape,
                color = if (isCompleted) targetAccentColor else Color.Transparent,
                border = BorderStroke(2.dp, targetAccentColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task title and status chip
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = targetAccentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isCompleted) "TODAY: COMPLETED" else "TODAY: PENDING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = targetAccentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isCompleted && item.completedAt != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateTimeFormatterHelper.formatTimeOnly(item.completedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .pressScale(0.90f)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .pressScale(0.90f)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEditDailyTaskDialog(
    initialTitle: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val isError = title.isNotBlank() && title.trim().isEmpty()
    val canSave = title.trim().isNotBlank()

    val quickSuggestions = remember {
        listOf(
            "Drink 4 liters water",
            "Gym workout",
            "Complete Python class",
            "Study DSA",
            "Read 20 pages",
            "10,000 steps walk",
            "Meditation 15m"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Daily Task" else "New Daily Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task name *") },
                    placeholder = { Text("e.g. Drink 4 liters water") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isEdit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickSuggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { title = suggestion },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.pressScale(0.96f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "This task recurs everyday and automatically resets each morning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onConfirm(title.trim())
                    }
                },
                enabled = canSave,
                modifier = Modifier.pressScale(0.95f)
            ) {
                Text(if (isEdit) "Update" else "Save Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pressScale(0.95f)
            ) {
                Text("Cancel")
            }
        }
    )
}
