package com.melonapp.an_melon_geo_fence_todo.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FirestoreService {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not initialized or unavailable: ${e.message}")
            null
        }
    }

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun getCurrentIsoTime(): String = isoDateFormat.format(Date())

    suspend fun uploadPlace(userId: String, place: SavedPlaceEntity): Boolean {
        val firestore = db ?: return false
        return try {
            val placeModel = FirestoreSavedPlace.fromEntity(place, getCurrentIsoTime())
            firestore.collection("users")
                .document(userId)
                .collection("saved_places")
                .document(place.id)
                .set(placeModel)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading place ${place.id} to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deletePlace(userId: String, placeId: String): Boolean {
        val firestore = db ?: return false
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("saved_places")
                .document(placeId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting place $placeId from Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun uploadTask(userId: String, task: TaskEntity): Boolean {
        val firestore = db ?: return false
        return try {
            val taskModel = FirestoreTask.fromEntity(task, getCurrentIsoTime())
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(task.id)
                .set(taskModel)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading task ${task.id} to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Boolean {
        val firestore = db ?: return false
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task $taskId from Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun fetchAllPlaces(userId: String): List<FirestoreSavedPlace> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("saved_places")
                .get()
                .await()
            snapshot.toObjects(FirestoreSavedPlace::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchAllTasks(userId: String): List<FirestoreTask> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .get()
                .await()
            snapshot.toObjects(FirestoreTask::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "FirestoreService"
    }
}
