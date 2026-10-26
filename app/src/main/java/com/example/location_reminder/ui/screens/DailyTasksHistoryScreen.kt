package com.example.location_reminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location_reminder.data.local.entity.DailyTaskHistoryDay
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import com.example.location_reminder.data.util.DateTimeFormatterHelper
import com.example.location_reminder.ui.theme.pressScale
import com.example.location_reminder.viewmodel.ReminderViewModel

private val HistoryGreenBorderDark = Color(0xFF4CAF50)
private val HistoryGreenContainerDark = Color(0xFF132A18)
private val HistoryGreenBorderLight = Color(0xFF2E7D32)
private val HistoryGreenContainerLight = Color(0xFFE8F5E9)

private val HistoryOrangeBorderDark = Color(0xFFFF9800)
private val HistoryOrangeContainerDark = Color(0xFF2E1C0C)
private val HistoryOrangeBorderLight = Color(0xFFE65100)
private val HistoryOrangeContainerLight = Color(0xFFFFF3E0)

private val DotGreen = Color(0xFF4CAF50)
private val DotOrange = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTasksHistoryScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentDate()
    }

    val historyDays by viewModel.dailyTasksHistory.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Set of expanded dates
    var expandedDates by rememberSaveable { mutableStateOf(setOf<String>()) }

    // Date currently in edit mode
    var editingDateIso by rememberSaveable { mutableStateOf<String?>(null) }

    // In-memory edit drafts: taskId -> isCompleted
    val editDrafts = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daily Tasks History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to Daily Tasks"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (historyDays.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading history...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = historyDays,
                    key = { day -> day.dateIso }
                ) { day ->
                    val isExpanded = day.dateIso in expandedDates
                    val isEditingThisDate = editingDateIso == day.dateIso

                    DailyTaskHistoryDayCard(
                        day = day,
                        isDark = isDark,
                        isExpanded = isExpanded,
                        isEditing = isEditingThisDate,
                        editDrafts = editDrafts,
                        onToggleExpand = {
                            expandedDates = if (isExpanded) {
                                if (isEditingThisDate) {
                                    editingDateIso = null
                                    editDrafts.clear()
                                }
                                expandedDates - day.dateIso
                            } else {
                                expandedDates + day.dateIso
                            }
                        },
                        onStartEdit = {
                            editingDateIso = day.dateIso
                            editDrafts.clear()
                            day.tasks.forEach { task ->
                                editDrafts[task.taskId] = task.isCompleted
                            }
                        },
                        onCancelEdit = {
                            editingDateIso = null
                            editDrafts.clear()
                        },
                        onSaveEdit = {
                            val changedUpdates = mutableMapOf<Long, Boolean>()
                            day.tasks.forEach { task ->
                                val currentCompletion = editDrafts[task.taskId] ?: task.isCompleted
                                if (currentCompletion != task.isCompleted) {
                                    changedUpdates[task.taskId] = currentCompletion
                                }
                            }
                            if (changedUpdates.isNotEmpty()) {
                                viewModel.saveDailyTaskCompletionsForDate(day.dateIso, changedUpdates)
                            }
                            editingDateIso = null
                            editDrafts.clear()
                        },
                        onToggleDraftTask = { taskId ->
                            val current = editDrafts[taskId] ?: false
                            editDrafts[taskId] = !current
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTaskHistoryDayCard(
    day: DailyTaskHistoryDay,
    isDark: Boolean,
    isExpanded: Boolean,
    isEditing: Boolean,
    editDrafts: Map<Long, Boolean>,
    onToggleExpand: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onToggleDraftTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTasks = day.tasks.size
    val completedCount = day.tasks.count { it.isCompleted }
    val allCompleted = totalTasks > 0 && completedCount == totalTasks

    val cardBorderColor = when {
        allCompleted -> if (isDark) HistoryGreenBorderDark else HistoryGreenBorderLight
        isExpanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f)
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "arrowRotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.99f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Header (Collapsed Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = day.displayDate.ifBlank { day.dateIso },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (day.daysAgo == 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 1.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${day.completionPercentage}% completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            allCompleted -> if (isDark) HistoryGreenBorderDark else HistoryGreenBorderLight
                            day.completionPercentage > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Expand / Collapse Chevron indicator (▶ collapsed, ▼ expanded)
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse date" else "Expand date",
                        tint = if (isExpanded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.graphicsLayer {
                            rotationZ = arrowRotation
                        }
                    )
                }
            }

            // Inline Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Action Bar inside expanded date section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (totalTasks > 0) {
                                if (isEditing) "Editing this date" else "$completedCount of $totalTasks tasks"
                            } else {
                                "No tasks recorded"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (totalTasks > 0) {
                            if (!isEditing) {
                                OutlinedButton(
                                    onClick = onStartEdit,
                                    modifier = Modifier.pressScale(0.95f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = onCancelEdit,
                                        modifier = Modifier.pressScale(0.95f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = onSaveEdit,
                                        modifier = Modifier.pressScale(0.95f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Save",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "Tap any task below to toggle completion for this date, then tap Save.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Task List for that date
                    if (day.tasks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No tasks existed on this date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            day.tasks.forEach { task ->
                                val isTaskCompleted = if (isEditing) {
                                    editDrafts[task.taskId] ?: task.isCompleted
                                } else {
                                    task.isCompleted
                                }

                                DailyTaskHistoryExpandedItemRow(
                                    task = task,
                                    isCompleted = isTaskCompleted,
                                    isDark = isDark,
                                    isEditing = isEditing,
                                    onToggle = if (isEditing) {
                                        { onToggleDraftTask(task.taskId) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTaskHistoryExpandedItemRow(
    task: DailyTaskWithCompletion,
    isCompleted: Boolean,
    isDark: Boolean,
    isEditing: Boolean,
    onToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCompleted) {
        if (isDark) HistoryGreenContainerDark else HistoryGreenContainerLight
    } else {
        if (isDark) HistoryOrangeContainerDark else HistoryOrangeContainerLight
    }

    val borderColor = if (isCompleted) {
        if (isDark) HistoryGreenBorderDark else HistoryGreenBorderLight
    } else {
        if (isDark) HistoryOrangeBorderDark else HistoryOrangeBorderLight
    }

    val textColor = if (isCompleted) {
        if (isDark) Color(0xFFC8E6C9) else Color(0xFF1B5E20)
    } else {
        if (isDark) Color(0xFFFFE0B2) else Color(0xFFBF360C)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onToggle != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator: 🟢 Completed (Green circle with check) or 🟠 Incomplete (Orange circle with cross)
            Surface(
                shape = CircleShape,
                color = if (isCompleted) DotGreen else DotOrange,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Not completed",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.SemiBold
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCompleted && task.completedAt != null && !isEditing) {
                    Text(
                        text = "Completed at " + DateTimeFormatterHelper.formatTimeOnly(task.completedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // State Badge pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = borderColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isCompleted) "COMPLETED" else "INCOMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = borderColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
