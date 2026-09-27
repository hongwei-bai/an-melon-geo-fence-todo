package com.melonapp.an_melon_geo_fence_todo.engine

import android.content.Context
import android.content.SharedPreferences

class MotionStateManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun updateActivity(activityName: String) {
        prefs.edit()
            .putString(KEY_ACTIVITY, activityName)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun updateSpeed(speedKmh: Float) {
        prefs.edit()
            .putFloat(KEY_SPEED, speedKmh)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getCurrentActivity(): String {
        return prefs.getString(KEY_ACTIVITY, "UNKNOWN") ?: "UNKNOWN"
    }

    fun getLastSpeedKmh(): Float? {
        val speed = prefs.getFloat(KEY_SPEED, -1f)
        return if (speed >= 0f) speed else null
    }

    fun getLastUpdatedTimestamp(): Long {
        return prefs.getLong(KEY_TIMESTAMP, 0L)
    }

    fun isSnoozed(taskId: String): Boolean {
        val snoozeUntil = prefs.getLong(KEY_SNOOZE_PREFIX + taskId, 0L)
        return System.currentTimeMillis() < snoozeUntil
    }

    fun setSnooze(taskId: String, minutes: Int = 15) {
        val snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_SNOOZE_PREFIX + taskId, snoozeUntil)
            .apply()
    }

    fun clearSnooze(taskId: String) {
        prefs.edit()
            .remove(KEY_SNOOZE_PREFIX + taskId)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "geo_todo_motion_state"
        private const val KEY_ACTIVITY = "detected_activity"
        private const val KEY_SPEED = "last_speed_kmh"
        private const val KEY_TIMESTAMP = "last_updated_time"
        private const val KEY_SNOOZE_PREFIX = "snooze_task_"

        @Volatile
        private var INSTANCE: MotionStateManager? = null

        fun getInstance(context: Context): MotionStateManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MotionStateManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
