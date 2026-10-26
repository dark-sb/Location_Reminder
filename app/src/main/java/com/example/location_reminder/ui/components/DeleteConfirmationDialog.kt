package com.example.location_reminder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.location_reminder.data.local.entity.ReminderEntity

import androidx.compose.ui.Modifier
import com.example.location_reminder.ui.theme.pressScale

@Composable
fun DeleteConfirmationDialog(
    reminder: ReminderEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Delete Reminder?")
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${reminder.title}\" (${reminder.locationName})? This action cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.pressScale(0.95f)
            ) {
                Text("Delete")
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