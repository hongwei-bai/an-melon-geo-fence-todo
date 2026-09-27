package com.melonapp.an_melon_geo_fence_todo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.melonapp.an_melon_geo_fence_todo.data.local.AppDatabase
import com.melonapp.an_melon_geo_fence_todo.data.remote.FirestoreService
import com.melonapp.an_melon_geo_fence_todo.data.repository.AuthRepository
import com.melonapp.an_melon_geo_fence_todo.engine.MotionStateManager
import com.melonapp.an_melon_geo_fence_todo.engine.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val action = intent.action ?: return

        val notificationHelper = NotificationHelper(context)
        val motionStateManager = MotionStateManager.getInstance(context)

        if (notificationId != -1) {
            notificationHelper.cancelNotification(notificationId)
        }

        when (action) {
            ACTION_MARK_COMPLETE -> {
                Log.d(TAG, "Mark Complete requested for task: $taskId")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = AppDatabase.getInstance(context)
                        val taskDao = database.taskDao()
                        taskDao.setTaskCompleted(taskId, true)

                        val updatedTask = taskDao.getTaskById(taskId)
                        if (updatedTask != null) {
                            val authRepository = AuthRepository(context)
                            val firestoreService = FirestoreService()
                            val userId = authRepository.getCurrentUserId()
                            firestoreService.uploadTask(userId, updatedTask)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error marking task complete: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Snooze 15m requested for task: $taskId")
                motionStateManager.setSnooze(taskId, minutes = 15)
            }
        }
    }

    companion object {
        const val ACTION_MARK_COMPLETE = "com.melonapp.an_melon_geo_fence_todo.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "com.melonapp.an_melon_geo_fence_todo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val TAG = "NotificationAction"
    }
}
