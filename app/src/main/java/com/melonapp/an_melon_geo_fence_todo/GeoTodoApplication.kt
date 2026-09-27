package com.melonapp.an_melon_geo_fence_todo

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.melonapp.an_melon_geo_fence_todo.data.local.AppDatabase
import com.melonapp.an_melon_geo_fence_todo.data.remote.FirestoreService
import com.melonapp.an_melon_geo_fence_todo.data.repository.AuthRepository
import com.melonapp.an_melon_geo_fence_todo.data.repository.LocationRepository
import com.melonapp.an_melon_geo_fence_todo.data.repository.TaskRepository
import com.melonapp.an_melon_geo_fence_todo.engine.ActivityRecognitionManager
import com.melonapp.an_melon_geo_fence_todo.engine.GeofenceManager
import com.melonapp.an_melon_geo_fence_todo.engine.MotionStateManager
import com.melonapp.an_melon_geo_fence_todo.worker.ResyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GeoTodoApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var firestoreService: FirestoreService
        private set
    lateinit var locationRepository: LocationRepository
        private set
    lateinit var taskRepository: TaskRepository
        private set
    lateinit var geofenceManager: GeofenceManager
        private set
    lateinit var activityRecognitionManager: ActivityRecognitionManager
        private set
    lateinit var motionStateManager: MotionStateManager
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        initFirebaseSafely()
        initMapsSafely()

        database = AppDatabase.getInstance(this)
        authRepository = AuthRepository(this)
        firestoreService = FirestoreService()
        geofenceManager = GeofenceManager(this)
        activityRecognitionManager = ActivityRecognitionManager(this)
        motionStateManager = MotionStateManager.getInstance(this)

        locationRepository = LocationRepository(
            savedPlaceDao = database.savedPlaceDao(),
            firestoreService = firestoreService,
            authRepository = authRepository
        )

        taskRepository = TaskRepository(
            taskDao = database.taskDao(),
            savedPlaceDao = database.savedPlaceDao(),
            firestoreService = firestoreService,
            authRepository = authRepository,
            geofenceManager = geofenceManager
        )

        applicationScope.launch {
            // Sign in anonymously if not already signed in to namespace Firestore data
            try {
                authRepository.signInAnonymously()
                // Initial cold recovery / resync
                locationRepository.syncRemotePlaces()
                taskRepository.syncRemoteTasks()
            } catch (e: Exception) {
                Log.w(TAG, "Background initial sync note: ${e.message}")
            }

            // Register activity recognition transitions if permission already granted
            if (activityRecognitionManager.hasActivityRecognitionPermission()) {
                activityRecognitionManager.registerActivityTransitions()
            }
        }

        schedulePeriodicResync()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = try {
                    FirebaseOptions.fromResource(this)
                } catch (e: Exception) {
                    null
                }
                if (options != null) {
                    FirebaseApp.initializeApp(this, options)
                    Log.i(TAG, "Firebase initialized from google-services resources.")
                } else {
                    val fallbackOptions = FirebaseOptions.Builder()
                        .setApplicationId("1:277520564194:android:d2ad577ca06f2aa36d03bb")
                        .setApiKey("AIzaSyBuVk_ZrdTVxqy_qYDUCx6l299daD_iHr8")
                        .setProjectId("melon-home-private-apps")
                        .setStorageBucket("melon-home-private-apps.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, fallbackOptions)
                    Log.i(TAG, "Firebase initialized with project credentials.")
                }
            } else {
                Log.i(TAG, "Firebase auto-initialized by FirebaseInitProvider.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp initialization handled: ${e.message}")
        }
    }

    private fun initMapsSafely() {
        try {
            MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
                Log.i(TAG, "Google Maps initialized with renderer: $renderer")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MapsInitializer error: ${e.message}")
        }
    }

    private fun schedulePeriodicResync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<ResyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ResyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    companion object {
        private const val TAG = "GeoTodoApp"
        lateinit var INSTANCE: GeoTodoApplication
            private set
    }
}
