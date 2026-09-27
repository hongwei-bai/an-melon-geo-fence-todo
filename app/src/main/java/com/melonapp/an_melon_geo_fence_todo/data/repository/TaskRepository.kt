package com.melonapp.an_melon_geo_fence_todo.data.repository

import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceDao
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskDao
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskWithPlace
import com.melonapp.an_melon_geo_fence_todo.data.remote.FirestoreService
import com.melonapp.an_melon_geo_fence_todo.engine.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val savedPlaceDao: SavedPlaceDao,
    private val firestoreService: FirestoreService,
    private val authRepository: AuthRepository,
    private val geofenceManager: GeofenceManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val allTasksWithPlaceFlow: Flow<List<TaskWithPlace>> = taskDao.getAllTasksWithPlaceFlow()

    suspend fun getActiveTasksWithPlace(): List<TaskWithPlace> =
        taskDao.getActiveTasksWithPlace()

    suspend fun getTaskWithPlaceById(id: String): TaskWithPlace? =
        taskDao.getTaskWithPlaceById(id)

    suspend fun saveTask(
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
    ): TaskEntity {
        val existingTask = id?.let { taskDao.getTaskWithPlaceById(it)?.task }
        val task = TaskEntity(
            id = id ?: ("task_" + UUID.randomUUID().toString().take(8)),
            title = title,
            placeId = placeId,
            isCompleted = existingTask?.isCompleted ?: false,
            startTime = startTime,
            endTime = endTime,
            daysOfWeekCsv = daysOfWeekCsv,
            validFromEpoch = validFromEpoch,
            validUntilEpoch = validUntilEpoch,
            requiredActivity = requiredActivity,
            minSpeedKmh = minSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            geoTriggerType = geoTriggerType,
            isSynced = false
        )
        taskDao.insertTask(task)

        // Sync to Firestore
        scope.launch {
            val userId = authRepository.getCurrentUserId()
            val success = firestoreService.uploadTask(userId, task)
            if (success) {
                taskDao.updateSyncStatus(task.id, true)
            }
        }

        // Re-register active geofences
        refreshGeofences()

        return task
    }

    suspend fun setTaskCompleted(taskId: String, isCompleted: Boolean) {
        taskDao.setTaskCompleted(taskId, isCompleted, isSynced = false)

        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            scope.launch {
                val userId = authRepository.getCurrentUserId()
                val success = firestoreService.uploadTask(userId, task)
                if (success) {
                    taskDao.updateSyncStatus(taskId, true)
                }
            }
        }

        refreshGeofences()
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        scope.launch {
            val userId = authRepository.getCurrentUserId()
            firestoreService.deleteTask(userId, task.id)
        }
        refreshGeofences()
    }

    suspend fun deleteTaskById(taskId: String) {
        taskDao.deleteTaskById(taskId)
        scope.launch {
            val userId = authRepository.getCurrentUserId()
            firestoreService.deleteTask(userId, taskId)
        }
        refreshGeofences()
    }

    suspend fun refreshGeofences() {
        val activeTasks = taskDao.getActiveTasksWithPlace()
        val activePlaceIds = activeTasks.map { it.place.id }.distinct()
        val activePlaces = activePlaceIds.mapNotNull { savedPlaceDao.getPlaceById(it) }
        geofenceManager.registerGeofences(activePlaces)
    }

    suspend fun syncRemoteTasks(): Int {
        val userId = authRepository.getCurrentUserId()
        val remoteTasks = firestoreService.fetchAllTasks(userId)
        if (remoteTasks.isNotEmpty()) {
            val entities = remoteTasks.map { it.toEntity() }
            taskDao.insertTasks(entities)
            refreshGeofences()
        }
        return remoteTasks.size
    }
}
