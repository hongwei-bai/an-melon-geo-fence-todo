package com.melonapp.an_melon_geo_fence_todo.engine

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.melonapp.an_melon_geo_fence_todo.receiver.ActivityTransitionReceiver
import kotlinx.coroutines.tasks.await

class ActivityRecognitionManager(private val context: Context) {

    private val activityClient: ActivityRecognitionClient =
        ActivityRecognition.getClient(context)

    private val transitionPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, TRANSITION_REQUEST_CODE, intent, flags)
    }

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun registerActivityTransitions(): Boolean {
        if (!hasActivityRecognitionPermission()) {
            Log.w(TAG, "Missing ACTIVITY_RECOGNITION permission.")
            return false
        }

        val transitions = mutableListOf<ActivityTransition>()
        val activityTypes = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.STILL
        )

        for (activityType in activityTypes) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)

        return try {
            activityClient.requestActivityTransitionUpdates(request, transitionPendingIntent).await()
            Log.d(TAG, "Successfully registered activity transition updates.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register activity transition updates: ${e.message}", e)
            false
        }
    }

    suspend fun unregisterActivityTransitions(): Boolean {
        return try {
            activityClient.removeActivityTransitionUpdates(transitionPendingIntent).await()
            Log.d(TAG, "Unregistered activity transitions.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister activity transitions: ${e.message}", e)
            false
        }
    }

    companion object {
        private const val TAG = "ActivityRecognition"
        private const val TRANSITION_REQUEST_CODE = 2001
    }
}
