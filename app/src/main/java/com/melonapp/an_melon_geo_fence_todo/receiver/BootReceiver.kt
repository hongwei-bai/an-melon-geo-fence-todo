package com.melonapp.an_melon_geo_fence_todo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.melonapp.an_melon_geo_fence_todo.data.local.AppDatabase
import com.melonapp.an_melon_geo_fence_todo.engine.ActivityRecognitionManager
import com.melonapp.an_melon_geo_fence_todo.engine.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val isBoot = action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!isBoot) return

        Log.d(TAG, "Device rebooted or package replaced ($action). Restoring active geofences and motion monitoring.")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val taskDao = database.taskDao()
                val savedPlaceDao = database.savedPlaceDao()

                // Query for all active tasks with geofences
                val activeTasks = taskDao.getActiveTasksWithPlace()
                val activePlaceIds = activeTasks.map { it.place.id }.distinct()
                val activePlaces = activePlaceIds.mapNotNull { savedPlaceDao.getPlaceById(it) }

                Log.d(TAG, "Restoring ${activePlaces.size} geofenced places for ${activeTasks.size} active tasks.")

                val geofenceManager = GeofenceManager(context)
                geofenceManager.registerGeofences(activePlaces)

                val activityRecognitionManager = ActivityRecognitionManager(context)
                activityRecognitionManager.registerActivityTransitions()

                Log.i(TAG, "Cold recovery / reboot restore completed successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring geofences after reboot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
