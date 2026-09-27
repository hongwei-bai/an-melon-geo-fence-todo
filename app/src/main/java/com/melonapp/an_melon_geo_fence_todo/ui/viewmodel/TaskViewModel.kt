package com.melonapp.an_melon_geo_fence_todo.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskWithPlace
import com.melonapp.an_melon_geo_fence_todo.data.repository.TaskRepository
import com.melonapp.an_melon_geo_fence_todo.engine.CompositeTriggerEvaluator
import com.melonapp.an_melon_geo_fence_todo.engine.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _simulationMessage = MutableStateFlow<String?>(null)
    val simulationMessage: StateFlow<String?> = _simulationMessage.asStateFlow()

    val tasks: StateFlow<List<TaskWithPlace>> = combine(
        taskRepository.allTasksWithPlaceFlow,
        _filter
    ) { allTasks, currentFilter ->
        when (currentFilter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.PENDING -> allTasks.filter { !it.task.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.task.isCompleted }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setFilter(newFilter: TaskFilter) {
        _filter.value = newFilter
    }

    fun clearSimulationMessage() {
        _simulationMessage.value = null
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    suspend fun getTaskWithPlaceById(id: String): TaskWithPlace? {
        return taskRepository.getTaskWithPlaceById(id)
    }

    fun saveTask(
        id: String? = null,
        title: String,
        placeId: String,
        startTime: String? = null,
        endTime: String? = null,
        daysOfWeekCsv: String? = null,
        validFromEpoch: Long? = null,
        validUntilEpoch: Long? = null,
        requiredActivity: String = "ANY",
        minSpeedKmh: Float? = null,
        maxSpeedKmh: Float? = null,
        geoTriggerType: String = "ENTER"
    ) {
        viewModelScope.launch {
            taskRepository.saveTask(
                id = id,
                title = title,
                placeId = placeId,
                startTime = startTime,
                endTime = endTime,
                daysOfWeekCsv = daysOfWeekCsv,
                validFromEpoch = validFromEpoch,
                validUntilEpoch = validUntilEpoch,
                requiredActivity = requiredActivity,
                minSpeedKmh = minSpeedKmh,
                maxSpeedKmh = maxSpeedKmh,
                geoTriggerType = geoTriggerType
            )
        }
    }

    fun simulateGeofenceEvent(
        taskWithPlace: TaskWithPlace,
        simulatedActivity: String,
        simulatedSpeedKmh: Float?,
        simulatedTransition: String = "ENTER"
    ) {
        val task = taskWithPlace.task
        val place = taskWithPlace.place

        val simulatedLocation = Location("simulated").apply {
            latitude = place.latitude
            longitude = place.longitude
            if (simulatedSpeedKmh != null) {
                speed = simulatedSpeedKmh / 3.6f
            }
        }

        val result = CompositeTriggerEvaluator.evaluate(
            task = task,
            isGeofenceTripped = true,
            transition = simulatedTransition,
            location = simulatedLocation,
            cachedActivity = simulatedActivity,
            cachedSpeedKmh = simulatedSpeedKmh
        )

        if (result.shouldTrigger) {
            notificationHelper.dispatchTaskNotification(task, place)
            _simulationMessage.value = "✅ Notification Dispatched!\n${result.reason}"
        } else {
            _simulationMessage.value = "⛔ Silently Discarded (Condition Not Met):\n${result.reason}"
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val notificationHelper: NotificationHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(taskRepository, notificationHelper) as T
        }
    }
}
