package com.example.location_reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.location_reminder.data.alarm.AndroidAlarmScheduler
import com.example.location_reminder.data.geofence.AndroidGeofenceManager
import com.example.location_reminder.data.local.database.ReminderDatabase
import com.example.location_reminder.data.notification.NotificationHelper
import com.example.location_reminder.data.repository.ReminderRepositoryImpl
import com.example.location_reminder.ui.screens.HomeScreen
import com.example.location_reminder.ui.theme.LocationReminderTheme
import com.example.location_reminder.viewmodel.ReminderViewModel
import com.example.location_reminder.viewmodel.ReminderViewModelFactory

import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.location_reminder.data.sync.ScheduleRestorationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.")
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.")
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "ACCESS_BACKGROUND_LOCATION permission granted by user.")
        } else {
            Log.w(TAG, "ACCESS_BACKGROUND_LOCATION permission denied by user.")
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            Log.d(TAG, "Foreground location permission granted.")
            checkAndRequestBackgroundLocationPermission()
        } else {
            Log.w(TAG, "Foreground location permission denied by user.")
        }
    }

    private val viewModel: ReminderViewModel by viewModels {
        val database = ReminderDatabase.getDatabase(applicationContext)
        val repository = ReminderRepositoryImpl(
            database.reminderDao,
            database.taskDao,
            database.locationHistoryDao,
            database.titleHistoryDao,
            database.eventHistoryDao,
            database.dailyTaskDao
        )
        val alarmScheduler = AndroidAlarmScheduler(applicationContext)
        val geofenceManager = AndroidGeofenceManager(applicationContext)
        val userPreferencesRepository = com.example.location_reminder.data.preferences.UserPreferencesRepository(applicationContext)
        ReminderViewModelFactory(repository, alarmScheduler, geofenceManager, userPreferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        optimizeDisplayRefreshRate()

        // Create notification channels on startup
        NotificationHelper.createNotificationChannels(applicationContext)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        checkAndRequestNotificationPermission()

        // Initialize MapLibre Native SDK for map rendering
        org.maplibre.android.MapLibre.getInstance(applicationContext)

        // Check and request location permissions required for geofencing
        checkAndRequestLocationPermissions()

        // Check if launched from a notification tap
        val reminderIdFromNotification = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)
        if (reminderIdFromNotification != -1L) {
            Log.d(TAG, "Launched from notification tap for reminder ID: $reminderIdFromNotification")
        }

        setContent {
            val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            LocationReminderTheme(darkTheme = currentTheme == com.example.location_reminder.data.preferences.AppTheme.DARK) {
                HomeScreen(
                    viewModel = viewModel,
                    currentTheme = currentTheme,
                    onThemeChanged = { newTheme -> viewModel.setAppTheme(newTheme) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch(Dispatchers.IO) {
                ScheduleRestorationManager.reconcileIfPermissionsRestored(applicationContext)
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.d(TAG, "Requesting foreground location permissions.")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            checkAndRequestBackgroundLocationPermission()
        }
    }

    private fun checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                showBackgroundLocationDisclosureDialog()
            }
        }
    }

    private fun showBackgroundLocationDisclosureDialog() {
        if (isFinishing || isDestroyed) return
        android.app.AlertDialog.Builder(this)
            .setTitle("Background Location Access")
            .setMessage("Location Reminder accesses your device location in the background to trigger alerts when you arrive at or depart from your saved reminder locations, even when the app is closed or not in use.\n\nYour location data is processed locally on your device and is never shared, tracked, or sent to external servers.")
            .setPositiveButton("Continue") { _, _ ->
                Log.d(TAG, "User accepted disclosure, launching background location request.")
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                Log.d(TAG, "User declined background location disclosure.")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun optimizeDisplayRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val highestRefreshMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (highestRefreshMode != null && highestRefreshMode.modeId != 0) {
                val params = window.attributes
                params.preferredDisplayModeId = highestRefreshMode.modeId
                window.attributes = params
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}