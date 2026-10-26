package com.example.location_reminder.ui.components

import com.example.location_reminder.ui.theme.completedContainer
import com.example.location_reminder.ui.theme.completedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.example.location_reminder.ui.theme.pressScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.location_reminder.data.alarm.AlarmScheduler
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.ReminderType
import com.example.location_reminder.data.local.entity.ReminderWithTasks
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import java.util.Locale

@Composable
fun ReminderCard(
    reminderWithTasks: ReminderWithTasks,
    onToggleEnabled: (reminderId: Long, isEnabled: Boolean) -> Unit,
    onToggleCompleted: (reminderId: Long, isCompleted: Boolean) -> Unit,
    onEdit: (ReminderWithTasks) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onAddTask: (reminderId: Long, title: String) -> Unit,
    onToggleTaskCompleted: (taskId: Long, isCompleted: Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder = reminderWithTasks.reminder
    val tasks = reminderWithTasks.tasks

    val formattedDueDateTime = remember(reminder.dueDateTime) {
        reminder.dueDateTime?.let { DateTimeFormatterHelper.formatDueDateTime(it) }
    }
    val formattedSnoozeTime = remember(reminder.snoozedUntil) {
        reminder.snoozedUntil?.let { DateTimeFormatterHelper.formatSnoozeTime(it) }
    }
    val formattedCoords = remember(reminder.latitude, reminder.longitude) {
        if (reminder.latitude != 0.0 || reminder.longitude != 0.0) {
            String.format(Locale.US, "Coords: %.4f, %.4f", reminder.latitude, reminder.longitude)
        } else null
    }
    val formattedRepeat = remember(reminder.repeatIntervalMinutes) {
        if (reminder.repeatIntervalMinutes > 0L) {
            AlarmScheduler.formatRepeatInterval(reminder.repeatIntervalMinutes)
        } else null
    }
    val isSnoozed = remember(reminder.snoozedUntil) {
        reminder.snoozedUntil != null && reminder.snoozedUntil > System.currentTimeMillis()
    }
    val typeLabel = remember(reminder.reminderType) {
        when (reminder.reminderType) {
            ReminderType.DATE_TIME.name -> "⏰ Date & Time"
            ReminderType.LOCATION_AND_TIME.name -> "📍+⏰ Location & Time"
            else -> "📍 Location"
        }
    }

    val completedTasksCount = remember(tasks) { tasks.count { it.isCompleted } }
    val isReminderCompleted = reminder.isCompleted

    val handleToggleEnabled = remember(reminder.id, onToggleEnabled) {
        { isChecked: Boolean -> onToggleEnabled(reminder.id, isChecked) }
    }
    val handleToggleCompleted = remember(reminder.id, isReminderCompleted, onToggleCompleted) {
        { onToggleCompleted(reminder.id, !isReminderCompleted) }
    }
    val handleMarkCompleted = remember(reminder.id, onToggleCompleted) {
        { onToggleCompleted(reminder.id, true) }
    }
    val handleEdit = remember(reminderWithTasks, onEdit) {
        { onEdit(reminderWithTasks) }
    }
    val handleDelete = remember(reminder, onDelete) {
        { onDelete(reminder) }
    }

    val containerColor = when {
        isReminderCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        reminder.isEnabled -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(150),
        label = "cardContainerColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isReminderCompleted) 1.0f else 0.92f,
        animationSpec = tween(150),
        label = "checkScale"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = when {
            isReminderCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            reminder.isEnabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(150),
        label = "titleColor"
    )
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = animatedContainerColor),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (reminder.isEnabled && !isReminderCompleted) 3.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Type Pill + Completion Status / Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Switch for Enabled / Disabled
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = handleToggleEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description + Complete Checkbox Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completion Toggle Icon
                val toggleInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = handleToggleCompleted,
                    interactionSource = toggleInteraction,
                    modifier = Modifier
                        .size(36.dp)
                        .pressScale(toggleInteraction, 0.88f)
                ) {
                    Icon(
                        imageVector = if (isReminderCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (isReminderCompleted) "Mark as active" else "Mark as completed",
                        tint = if (isReminderCompleted) {
                            MaterialTheme.colorScheme.completedContent
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isReminderCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = animatedTitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Scheduled Date & Time (if present)
            if (reminder.dueDateTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Due: ${formattedDueDateTime ?: ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Location details (if location reminder)
            if (reminder.reminderType != ReminderType.DATE_TIME.name) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(18.dp),
                        tint = if (reminder.isEnabled && !isReminderCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reminder.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Radius: ${reminder.radius.toInt()}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (formattedCoords != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedCoords,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Repeat Alert & Snooze Status Badges
            if (formattedRepeat != null || isSnoozed) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (formattedRepeat != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🔁 Repeats: $formattedRepeat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (isSnoozed) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "💤 Snoozed until ${formattedSnoozeTime ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder message box
            if (reminder.reminderMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (reminder.isEnabled && !isReminderCompleted) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Message",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reminder.reminderMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // INDIVIDUAL ITEMS / CHECKLIST SECTION (Only for Location Reminders)
            if (reminder.reminderType != ReminderType.DATE_TIME.name) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Items (${completedTasksCount}/${tasks.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Existing tasks list - keyed for stable identity and fast diffing
                        if (tasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            tasks.forEach { task ->
                                key(task.id) {
                                    TaskItemRow(
                                        task = task,
                                        onToggleTaskCompleted = onToggleTaskCompleted,
                                        onDeleteTask = onDeleteTask
                                    )
                                }
                            }
                        }

                        // Inline Add Task Input
                        Spacer(modifier = Modifier.height(6.dp))
                        InlineAddTaskRow(
                            reminderId = reminder.id,
                            onAddTask = onAddTask
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill & Completed Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .background(
                                color = when {
                                    isReminderCompleted -> MaterialTheme.colorScheme.completedContainer
                                    reminder.isEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isReminderCompleted -> "COMPLETED"
                                reminder.isEnabled -> "ACTIVE"
                                else -> "DISABLED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isReminderCompleted -> MaterialTheme.colorScheme.completedContent
                                reminder.isEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (!isReminderCompleted) {
                        val completeInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = handleMarkCompleted,
                            interactionSource = completeInteraction,
                            modifier = Modifier
                                .height(32.dp)
                                .pressScale(completeInteraction, 0.95f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.completedContent
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.completedContent.copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.completedContent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val editInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = handleEdit,
                        interactionSource = editInteraction,
                        modifier = Modifier
                            .size(36.dp)
                            .pressScale(editInteraction, 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit reminder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    val deleteInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = handleDelete,
                        interactionSource = deleteInteraction,
                        modifier = Modifier
                            .size(36.dp)
                            .pressScale(deleteInteraction, 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete reminder",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight, skippable Task item row to avoid recomposing the whole card when a single task changes.
 */
@Composable
private fun TaskItemRow(
    task: TaskEntity,
    onToggleTaskCompleted: (taskId: Long, isCompleted: Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleToggle = remember(task.id, onToggleTaskCompleted) {
        { isChecked: Boolean -> onToggleTaskCompleted(task.id, isChecked) }
    }
    val handleDelete = remember(task, onDeleteTask) {
        { onDeleteTask(task) }
    }

    val animatedTextColor by animateColorAsState(
        targetValue = if (task.isCompleted) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(150),
        label = "taskTextColor"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.75f else 1f,
        animationSpec = tween(150),
        label = "taskAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val checkInteraction = remember { MutableInteractionSource() }
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = handleToggle,
            interactionSource = checkInteraction,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.completedContent
            ),
            modifier = Modifier
                .size(28.dp)
                .pressScale(checkInteraction, 0.90f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = animatedTextColor,
            modifier = Modifier.weight(1f)
        )
        val deleteTaskInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = handleDelete,
            interactionSource = deleteTaskInteraction,
            modifier = Modifier
                .size(24.dp)
                .pressScale(deleteTaskInteraction, 0.88f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete task",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Fast, lightweight inline task input row. Uses [BasicTextField] with an outlined container
 * to avoid the heavy measurement and decoration pipeline of [OutlinedTextField] on every LazyColumn item.
 */
@Composable
private fun InlineAddTaskRow(
    reminderId: Long,
    onAddTask: (reminderId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newInlineTaskTitle by remember { mutableStateOf("") }

    val submitTask = remember(reminderId, onAddTask) {
        {
            val text = newInlineTaskTitle.trim()
            if (text.isNotBlank()) {
                onAddTask(reminderId, text)
                newInlineTaskTitle = ""
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicTextField(
            value = newInlineTaskTitle,
            onValueChange = { newInlineTaskTitle = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitTask() }),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (newInlineTaskTitle.isEmpty()) {
                        Text(
                            text = "Add task...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        val addInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = submitTask,
            interactionSource = addInteraction,
            modifier = Modifier
                .size(36.dp)
                .pressScale(addInteraction, 0.90f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add task",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Backward compatibility overload for [ReminderCard].
 */
@Composable
fun ReminderCard(
    reminderWithTasks: ReminderWithTasks,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddTask: (reminderId: Long, title: String) -> Unit,
    onToggleTaskCompleted: (taskId: Long, isCompleted: Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) = ReminderCard(
    reminderWithTasks = reminderWithTasks,
    onToggleEnabled = { _, isEnabled -> onToggleEnabled(isEnabled) },
    onToggleCompleted = { _, isCompleted -> onToggleCompleted(isCompleted) },
    onEdit = { onEdit() },
    onDelete = { onDelete() },
    onAddTask = onAddTask,
    onToggleTaskCompleted = onToggleTaskCompleted,
    onDeleteTask = onDeleteTask,
    modifier = modifier
)