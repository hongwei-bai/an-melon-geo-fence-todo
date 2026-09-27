package com.melonapp.an_melon_geo_fence_todo.engine

import android.location.Location
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CompositeTriggerEvaluator {

    data class EvaluationResult(
        val shouldTrigger: Boolean,
        val reason: String
    )

    fun evaluate(
        task: TaskEntity,
        isGeofenceTripped: Boolean,
        transition: String = "ENTER",
        location: Location?,
        cachedActivity: String,
        cachedSpeedKmh: Float? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): EvaluationResult {
        // 1. Task Completion check
        if (task.isCompleted) {
            return EvaluationResult(false, "Task is already completed.")
        }

        // 2. Spatial check
        if (!isGeofenceTripped) {
            return EvaluationResult(false, "Geofence is not tripped.")
        }

        // 2b. Geofence Transition Type matching check
        val transitionMatches = when (task.geoTriggerType.uppercase()) {
            "ENTER" -> transition.uppercase() == "ENTER"
            "EXIT" -> transition.uppercase() == "EXIT"
            "DWELL" -> transition.uppercase() == "DWELL" || transition.uppercase() == "ENTER"
            "ENTER_OR_EXIT" -> transition.uppercase() == "ENTER" || transition.uppercase() == "EXIT"
            else -> transition.uppercase() == "ENTER"
        }
        if (!transitionMatches) {
            return EvaluationResult(
                false,
                "Geofence transition '$transition' does not match task requirement '${task.geoTriggerType}'."
            )
        }

        // 3. Temporal Window evaluation
        val temporalResult = evaluateTemporal(task, currentTimeMillis)
        if (!temporalResult.first) {
            return EvaluationResult(false, temporalResult.second)
        }

        // 4. Motion & Speed evaluation
        val motionResult = evaluateMotion(task, location, cachedActivity, cachedSpeedKmh)
        if (!motionResult.first) {
            return EvaluationResult(false, motionResult.second)
        }

        return EvaluationResult(
            true,
            "All conditions met: Spatial, Temporal (${temporalResult.second}), and Motion (${motionResult.second})."
        )
    }

    private fun evaluateTemporal(task: TaskEntity, currentTimeMillis: Long): Pair<Boolean, String> {
        // Absolute validFrom / validUntil checks
        if (task.validFromEpoch != null && currentTimeMillis < task.validFromEpoch) {
            return Pair(false, "Current time is before task validFrom epoch.")
        }
        if (task.validUntilEpoch != null && currentTimeMillis > task.validUntilEpoch) {
            return Pair(false, "Current time is after task validUntil epoch.")
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }

        // Days of week check
        if (!task.daysOfWeekCsv.isNullOrBlank()) {
            val allowedDays = task.daysOfWeekCsv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }

            if (allowedDays.isNotEmpty()) {
                // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
                // Standard ISO mapping: Monday=1 .. Sunday=7
                val calDay = calendar.get(Calendar.DAY_OF_WEEK)
                val isoDay = when (calDay) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> calDay
                }

                // Match against either ISO (1..7) or Calendar (1..7)
                val dayMatches = allowedDays.contains(isoDay) || allowedDays.contains(calDay)
                if (!dayMatches) {
                    return Pair(false, "Current day ($isoDay) is not within allowed days of week: ${task.daysOfWeekCsv}.")
                }
            }
        }

        // Daily Time Range check (startTime, endTime in "HH:mm")
        if (!task.startTime.isNullOrBlank() && !task.endTime.isNullOrBlank()) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentLocalTimeStr = timeFormat.format(Date(currentTimeMillis))

            val currentMinutes = parseTimeToMinutes(currentLocalTimeStr)
            val startMinutes = parseTimeToMinutes(task.startTime)
            val endMinutes = parseTimeToMinutes(task.endTime)

            if (startMinutes != null && endMinutes != null && currentMinutes != null) {
                val inRange = if (startMinutes <= endMinutes) {
                    // Standard intra-day range, e.g. 08:00 to 18:00
                    currentMinutes in startMinutes..endMinutes
                } else {
                    // Overnight range, e.g. 22:00 to 06:00
                    currentMinutes >= startMinutes || currentMinutes <= endMinutes
                }

                if (!inRange) {
                    return Pair(
                        false,
                        "Current time ($currentLocalTimeStr) is outside allowed range (${task.startTime} - ${task.endTime})."
                    )
                }
            }
        }

        return Pair(true, "Temporal constraints satisfied")
    }

    private fun evaluateMotion(
        task: TaskEntity,
        location: Location?,
        cachedActivity: String,
        cachedSpeedKmh: Float?
    ): Pair<Boolean, String> {
        val required = task.requiredActivity.uppercase()

        // Calculate current speed in km/h if available from GPS
        val currentSpeedKmh: Float? = if (location != null && location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            cachedSpeedKmh
        }

        // 1. Speed Threshold Checks
        if (currentSpeedKmh != null) {
            task.minSpeedKmh?.let { min ->
                if (currentSpeedKmh < min) {
                    return Pair(false, "Speed $currentSpeedKmh km/h is below required minimum $min km/h.")
                }
            }
            task.maxSpeedKmh?.let { max ->
                if (currentSpeedKmh > max) {
                    return Pair(false, "Speed $currentSpeedKmh km/h is above required maximum $max km/h.")
                }
            }
        }

        // 2. Activity Classification Checks
        if (required == "ANY") {
            return Pair(true, "Activity ANY satisfied")
        }

        // Kinematic inference: if ground speed is clearly high (> 20 km/h), user is in a vehicle
        val isKinematicallyInVehicle = currentSpeedKmh != null && currentSpeedKmh > 20.0f
        val isKinematicallyStationary = currentSpeedKmh != null && currentSpeedKmh < 1.0f

        val effectiveActivity = when {
            isKinematicallyInVehicle -> "IN_VEHICLE"
            isKinematicallyStationary && cachedActivity == "UNKNOWN" -> "STILL"
            else -> cachedActivity.uppercase()
        }

        when (required) {
            "IN_VEHICLE" -> {
                if (effectiveActivity != "IN_VEHICLE" && !isKinematicallyInVehicle) {
                    return Pair(false, "Required IN_VEHICLE, but current activity is $effectiveActivity.")
                }
            }
            "ON_FOOT" -> {
                // If user is in vehicle or going at driving speed, ON_FOOT fails
                if (isKinematicallyInVehicle || effectiveActivity == "IN_VEHICLE") {
                    return Pair(false, "Required ON_FOOT, but user is in a vehicle ($effectiveActivity, speed: $currentSpeedKmh km/h).")
                }
                if (effectiveActivity != "ON_FOOT" && effectiveActivity != "WALKING" && effectiveActivity != "RUNNING" && effectiveActivity != "UNKNOWN") {
                    return Pair(false, "Required ON_FOOT, but current activity is $effectiveActivity.")
                }
            }
            "STILL" -> {
                if (isKinematicallyInVehicle || (currentSpeedKmh != null && currentSpeedKmh > 3.0f)) {
                    return Pair(false, "Required STILL, but user is moving at $currentSpeedKmh km/h.")
                }
                if (effectiveActivity != "STILL" && effectiveActivity != "UNKNOWN") {
                    return Pair(false, "Required STILL, but current activity is $effectiveActivity.")
                }
            }
        }

        return Pair(true, "Motion requirement $required matched with activity $effectiveActivity")
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }
}
