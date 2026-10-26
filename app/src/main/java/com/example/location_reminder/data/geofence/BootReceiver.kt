package com.example.location_reminder.data.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_reminder.data.sync.ScheduleRestorationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Ignoring unsupported action: $action")
            return
        }

        Log.d(TAG, "Received trigger '$action'. Restoring active geofences and scheduled alarms.")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = ScheduleRestorationManager.restoreAllSchedules(context.applicationContext)
                Log.d(TAG, "Restoration on '$action' succeeded: $summary")
            } catch (e: Exception) {
                Log.e(TAG, "Error during restoration on '$action'", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
