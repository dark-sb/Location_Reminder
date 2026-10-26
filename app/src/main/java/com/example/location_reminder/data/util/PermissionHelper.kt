package com.example.location_reminder.data.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

import androidx.compose.runtime.Immutable

object PermissionHelper {

    @Immutable
    data class CapabilityStatus(
        val hasNotificationPermission: Boolean,
        val hasForegroundLocation: Boolean,
        val hasBackgroundLocation: Boolean,
        val isLocationServicesEnabled: Boolean,
        val canScheduleExactAlarms: Boolean,
        val isIgnoringBatteryOptimizations: Boolean,
        val showLocationDisabledWarning: Boolean,
        val showForegroundLocationWarning: Boolean,
        val showBackgroundLocationWarning: Boolean,
        val showNotificationWarning: Boolean,
        val showExactAlarmWarning: Boolean
    )

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasForegroundLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    /**
     * Pure function to determine active capability warnings based on current state.
     * Fully unit testable without Android framework dependencies.
     */
    fun computeCapabilityStatus(
        hasNotificationPermission: Boolean,
        hasForegroundLocation: Boolean,
        hasBackgroundLocation: Boolean,
        isLocationServicesEnabled: Boolean,
        canScheduleExactAlarms: Boolean,
        isIgnoringBatteryOptimizations: Boolean,
        hasActiveLocationReminders: Boolean,
        hasActiveDateTimeReminders: Boolean
    ): CapabilityStatus {
        val locationDisabled = hasActiveLocationReminders && !isLocationServicesEnabled
        val foregroundLocationMissing = hasActiveLocationReminders && isLocationServicesEnabled && !hasForegroundLocation
        val backgroundLocationMissing = hasActiveLocationReminders && isLocationServicesEnabled && hasForegroundLocation && !hasBackgroundLocation
        val notificationMissing = (hasActiveLocationReminders || hasActiveDateTimeReminders) && !hasNotificationPermission
        val exactAlarmRestricted = hasActiveDateTimeReminders && !canScheduleExactAlarms

        return CapabilityStatus(
            hasNotificationPermission = hasNotificationPermission,
            hasForegroundLocation = hasForegroundLocation,
            hasBackgroundLocation = hasBackgroundLocation,
            isLocationServicesEnabled = isLocationServicesEnabled,
            canScheduleExactAlarms = canScheduleExactAlarms,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            showLocationDisabledWarning = locationDisabled,
            showForegroundLocationWarning = foregroundLocationMissing,
            showBackgroundLocationWarning = backgroundLocationMissing,
            showNotificationWarning = notificationMissing,
            showExactAlarmWarning = exactAlarmRestricted
        )
    }

    fun getCapabilityStatus(
        context: Context,
        hasActiveLocationReminders: Boolean,
        hasActiveDateTimeReminders: Boolean
    ): CapabilityStatus {
        return computeCapabilityStatus(
            hasNotificationPermission = hasNotificationPermission(context),
            hasForegroundLocation = hasForegroundLocationPermission(context),
            hasBackgroundLocation = hasBackgroundLocationPermission(context),
            isLocationServicesEnabled = isLocationServicesEnabled(context),
            canScheduleExactAlarms = canScheduleExactAlarms(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            hasActiveLocationReminders = hasActiveLocationReminders,
            hasActiveDateTimeReminders = hasActiveDateTimeReminders
        )
    }

    fun createAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            createAppDetailsSettingsIntent(context)
        }
    }

    fun createBatteryOptimizationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
