package com.melonapp.an_melon_geo_fence_todo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.melonapp.an_melon_geo_fence_todo.engine.MotionStateManager

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) {
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val motionStateManager = MotionStateManager.getInstance(context)

        for (event in result.transitionEvents) {
            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                val activityName = when (event.activityType) {
                    DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                    DetectedActivity.ON_FOOT -> "ON_FOOT"
                    DetectedActivity.WALKING -> "WALKING"
                    DetectedActivity.RUNNING -> "RUNNING"
                    DetectedActivity.STILL -> "STILL"
                    else -> "UNKNOWN"
                }

                Log.d(TAG, "Activity Transition Enter: $activityName")
                motionStateManager.updateActivity(activityName)
            }
        }
    }

    companion object {
        private const val TAG = "ActivityTransReceiver"
    }
}
