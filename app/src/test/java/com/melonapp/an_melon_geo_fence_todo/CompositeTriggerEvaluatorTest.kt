package com.melonapp.an_melon_geo_fence_todo

import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity
import com.melonapp.an_melon_geo_fence_todo.engine.CompositeTriggerEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CompositeTriggerEvaluatorTest {

    private fun createBaseTask(
        id: String = "task_test_1",
        title: String = "Test Task",
        placeId: String = "place_1",
        isCompleted: Boolean = false,
        startTime: String? = null,
        endTime: String? = null,
        daysOfWeekCsv: String? = null,
        validFromEpoch: Long? = null,
        validUntilEpoch: Long? = null,
        requiredActivity: String = "ANY",
        minSpeedKmh: Float? = null,
        maxSpeedKmh: Float? = null,
        geoTriggerType: String = "ENTER"
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            placeId = placeId,
            isCompleted = isCompleted,
            startTime = startTime,
            endTime = endTime,
            daysOfWeekCsv = daysOfWeekCsv,
            validFromEpoch = validFromEpoch,
            validUntilEpoch = validUntilEpoch,
            requiredActivity = requiredActivity,
            minSpeedKmh = minSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            geoTriggerType = geoTriggerType,
            isSynced = true
        )
    }

    @Test
    fun testAllConditionsMatch_triggersNotification() {
        val task = createBaseTask(
            requiredActivity = "ANY"
        )

        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ON_FOOT",
            cachedSpeedKmh = 4.0f
        )

        assertTrue("Expected trigger when all conditions match", result.shouldTrigger)
    }

    @Test
    fun testInvalidTimeWindow_doesNotFireNotification() {
        // Active from 09:00 to 17:00
        val task = createBaseTask(
            startTime = "09:00",
            endTime = "17:00"
        )

        // Set simulated time to 21:30 (outside allowed hours)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 30)
        }

        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ON_FOOT",
            currentTimeMillis = cal.timeInMillis
        )

        assertFalse("Notification must NOT fire when current time is outside valid window", result.shouldTrigger)
    }

    @Test
    fun testValidTimeWindow_firesNotification() {
        val task = createBaseTask(
            startTime = "09:00",
            endTime = "17:00"
        )

        // Set simulated time to 14:00 (inside allowed hours)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ON_FOOT",
            currentTimeMillis = cal.timeInMillis
        )

        assertTrue("Notification should fire during valid time window", result.shouldTrigger)
    }

    @Test
    fun testEnteringInCarWhenConditionIsOnFoot_doesNotFireNotification() {
        // Task requires ON_FOOT with max speed 8.0 km/h
        val task = createBaseTask(
            requiredActivity = "ON_FOOT",
            maxSpeedKmh = 8.0f
        )

        // User enters boundary at 50 km/h in a car
        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "IN_VEHICLE",
            cachedSpeedKmh = 50.0f
        )

        assertFalse(
            "Entering a geofenced area in a car when condition is set to ON_FOOT must NOT fire a notification",
            result.shouldTrigger
        )
    }

    @Test
    fun testEnteringOnFootWhenConditionIsOnFoot_firesNotification() {
        val task = createBaseTask(
            requiredActivity = "ON_FOOT",
            maxSpeedKmh = 8.0f
        )

        // User enters boundary walking at 4 km/h
        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ON_FOOT",
            cachedSpeedKmh = 4.0f
        )

        assertTrue(
            "Entering on foot when condition is ON_FOOT should fire a notification",
            result.shouldTrigger
        )
    }

    @Test
    fun testEnteringInCarWhenConditionIsInVehicle_firesNotification() {
        val task = createBaseTask(
            requiredActivity = "IN_VEHICLE",
            minSpeedKmh = 15.0f
        )

        // User driving at 40 km/h
        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "IN_VEHICLE",
            cachedSpeedKmh = 40.0f
        )

        assertTrue(
            "Driving into geofenced area when condition is IN_VEHICLE should fire a notification",
            result.shouldTrigger
        )
    }

    @Test
    fun testWalkingWhenConditionIsInVehicle_doesNotFireNotification() {
        val task = createBaseTask(
            requiredActivity = "IN_VEHICLE"
        )

        // User walking at 3 km/h
        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ON_FOOT",
            cachedSpeedKmh = 3.0f
        )

        assertFalse(
            "Walking into geofenced area when condition is IN_VEHICLE must NOT fire a notification",
            result.shouldTrigger
        )
    }

    @Test
    fun testCompletedTask_doesNotFireNotification() {
        val task = createBaseTask(
            isCompleted = true
        )

        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            location = null,
            cachedActivity = "ANY"
        )

        assertFalse("Completed task should never trigger a notification", result.shouldTrigger)
    }

    @Test
    fun testEnterCondition_firesOnEnter_doesNotFireOnExit() {
        val task = createBaseTask(
            geoTriggerType = "ENTER"
        )

        val enterResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "ENTER",
            location = null,
            cachedActivity = "ANY"
        )
        assertTrue("ENTER task should trigger on ENTER transition", enterResult.shouldTrigger)

        val exitResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "EXIT",
            location = null,
            cachedActivity = "ANY"
        )
        assertFalse("ENTER task must NOT trigger on EXIT transition", exitResult.shouldTrigger)
    }

    @Test
    fun testExitCondition_firesOnExit_doesNotFireOnEnter() {
        val task = createBaseTask(
            geoTriggerType = "EXIT"
        )

        val exitResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "EXIT",
            location = null,
            cachedActivity = "ANY"
        )
        assertTrue("EXIT task should trigger on EXIT transition", exitResult.shouldTrigger)

        val enterResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "ENTER",
            location = null,
            cachedActivity = "ANY"
        )
        assertFalse("EXIT task must NOT trigger on ENTER transition", enterResult.shouldTrigger)
    }

    @Test
    fun testDwellCondition_firesOnDwell() {
        val task = createBaseTask(
            geoTriggerType = "DWELL"
        )

        val dwellResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "DWELL",
            location = null,
            cachedActivity = "ANY"
        )
        assertTrue("DWELL task should trigger on DWELL transition", dwellResult.shouldTrigger)

        val exitResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "EXIT",
            location = null,
            cachedActivity = "ANY"
        )
        assertFalse("DWELL task must NOT trigger on EXIT transition", exitResult.shouldTrigger)
    }

    @Test
    fun testEnterOrExitCondition_firesOnBoth() {
        val task = createBaseTask(
            geoTriggerType = "ENTER_OR_EXIT"
        )

        val enterResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "ENTER",
            location = null,
            cachedActivity = "ANY"
        )
        assertTrue("ENTER_OR_EXIT task should trigger on ENTER", enterResult.shouldTrigger)

        val exitResult = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = "EXIT",
            location = null,
            cachedActivity = "ANY"
        )
        assertTrue("ENTER_OR_EXIT task should trigger on EXIT", exitResult.shouldTrigger)
    }
}
