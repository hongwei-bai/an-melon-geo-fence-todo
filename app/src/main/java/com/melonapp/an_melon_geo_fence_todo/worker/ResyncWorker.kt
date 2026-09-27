package com.melonapp.an_melon_geo_fence_todo.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melonapp.an_melon_geo_fence_todo.GeoTodoApplication

class ResyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic/reboot cloud resync via WorkManager.")
        return try {
            val app = applicationContext as GeoTodoApplication
            val placesSynced = app.locationRepository.syncRemotePlaces()
            val tasksSynced = app.taskRepository.syncRemoteTasks()

            Log.i(TAG, "ResyncWorker finished: $placesSynced places and $tasksSynced tasks synced.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ResyncWorker encountered an error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ResyncWorker"
        const val WORK_NAME = "periodic_cloud_resync_work"
    }
}
