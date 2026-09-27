package com.melonapp.an_melon_geo_fence_todo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.melonapp.an_melon_geo_fence_todo.data.local.AppDatabase
import com.melonapp.an_melon_geo_fence_todo.engine.CompositeTriggerEvaluator
import com.melonapp.an_melon_geo_fence_todo.engine.MotionStateManager
import com.melonapp.an_melon_geo_fence_todo.engine.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geofencingEvent = GeofencingEvent.fromIntent(intent)
                if (geofencingEvent == null) {
                    Log.w(TAG, "GeofencingEvent is null")
                    return@launch
                }

                if (geofencingEvent.hasError()) {
                    Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode}")
                    return@launch
                }

                val transitionType = geofencingEvent.geofenceTransition
                val isRelevantTransition = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                        transitionType == Geofence.GEOFENCE_TRANSITION_EXIT ||
                        transitionType == Geofence.GEOFENCE_TRANSITION_DWELL

                if (!isRelevantTransition) {
                    Log.d(TAG, "Ignoring transition type: $transitionType")
                    return@launch
                }

                val transitionName = when (transitionType) {
                    Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
                    Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
                    else -> "ENTER"
                }

                val triggeringLocation = geofencingEvent.triggeringLocation
                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

                val motionStateManager = MotionStateManager.getInstance(context)
                val notificationHelper = NotificationHelper(context)
                val database = AppDatabase.getInstance(context)
                val taskDao = database.taskDao()

                // Update speed in motion state manager if GPS speed is available
                if (triggeringLocation != null && triggeringLocation.hasSpeed()) {
                    val speedKmh = triggeringLocation.speed * 3.6f
                    motionStateManager.updateSpeed(speedKmh)
                }

                val cachedActivity = motionStateManager.getCurrentActivity()
                val cachedSpeed = motionStateManager.getLastSpeedKmh()

                for (geofence in triggeringGeofences) {
                    val placeId = geofence.requestId
                    val tasksWithPlace = taskDao.getActiveTasksForPlace(placeId)

                    for (taskWithPlace in tasksWithPlace) {
                        val task = taskWithPlace.task
                        val place = taskWithPlace.place

                        // Check snooze window
                        if (motionStateManager.isSnoozed(task.id)) {
                            Log.d(TAG, "Task ${task.title} is currently snoozed. Discarding trigger.")
                            continue
                        }

                        // Evaluate Composite Trigger Rules:
                        // Notification Triggered <=> Geofence Tripped & Temporal Active & Motion Satisfied
                        val evaluation = CompositeTriggerEvaluator.evaluate(
                            task = task,
                            isGeofenceTripped = true,
                            transition = transitionName,
                            location = triggeringLocation,
                            cachedActivity = cachedActivity,
                            cachedSpeedKmh = cachedSpeed
                        )

                        if (evaluation.shouldTrigger) {
                            Log.i(TAG, "Dispatching notification for task: ${task.title}. ${evaluation.reason}")
                            notificationHelper.dispatchTaskNotification(task, place)
                        } else {
                            Log.d(TAG, "Silently discarding task '${task.title}': ${evaluation.reason}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in GeofenceBroadcastReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
