package com.example.location_reminder.data.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class AndroidGeofenceManager(
    private val context: Context,
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
) : GeofenceManager {

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_PENDING_INTENT_REQUEST_CODE,
            intent,
            flags
        )
    }

    override fun registerGeofence(reminder: ReminderEntity) {
        if (!GeofenceManager.isEligibleForGeofencing(reminder)) {
            Log.d(TAG, "Reminder ${reminder.id} is not eligible for geofencing (enabled=${reminder.isEnabled}, completed=${reminder.isCompleted}, type=${reminder.reminderType}). Ensuring any old geofence is removed.")
            removeGeofence(reminder.id)
            return
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot register geofence for reminder ${reminder.id}: ACCESS_FINE_LOCATION permission not granted.")
            return
        }

        val requestId = GeofenceManager.getGeofenceRequestId(reminder.id)
        val radiusMeters = reminder.radius.toFloat().coerceAtLeast(30f)

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(
                reminder.latitude,
                reminder.longitude,
                radiusMeters
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(5000) // 5 seconds responsiveness
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence registered successfully: id=$requestId, lat=${reminder.latitude}, lng=${reminder.longitude}, radius=${radiusMeters}m")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to register geofence for reminder ${reminder.id}: ${exception.message}", exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering geofence for reminder ${reminder.id}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error registering geofence for reminder ${reminder.id}", e)
        }
    }

    override fun removeGeofence(reminderId: Long) {
        val requestId = GeofenceManager.getGeofenceRequestId(reminderId)
        val idList = listOf(requestId)

        try {
            geofencingClient.removeGeofences(idList)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence removed successfully for reminder ID: $reminderId ($requestId)")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to remove geofence for reminder ID $reminderId: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence for reminder ID $reminderId", e)
        }
    }

    override fun removeAllGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "All geofences removed successfully.")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to remove all geofences: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing all geofences", e)
        }
    }

    companion object {
        private const val TAG = "AndroidGeofenceManager"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 2001
    }
}
