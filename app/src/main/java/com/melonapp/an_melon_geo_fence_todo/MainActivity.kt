package com.melonapp.an_melon_geo_fence_todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.melonapp.an_melon_geo_fence_todo.engine.NotificationHelper
import com.melonapp.an_melon_geo_fence_todo.ui.components.ActivityRecognitionRationaleDialog
import com.melonapp.an_melon_geo_fence_todo.ui.components.BackgroundLocationRationaleDialog
import com.melonapp.an_melon_geo_fence_todo.ui.navigation.NavRoute
import com.melonapp.an_melon_geo_fence_todo.ui.screens.MapLocationPickerScreen
import com.melonapp.an_melon_geo_fence_todo.ui.screens.PlacesListScreen
import com.melonapp.an_melon_geo_fence_todo.ui.screens.TaskEditScreen
import com.melonapp.an_melon_geo_fence_todo.ui.screens.TaskListScreen
import com.melonapp.an_melon_geo_fence_todo.ui.theme.AnmelongeofencetodoTheme
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.AuthViewModel
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.LocationViewModel
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val stage1PermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true

        if (fineLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkBackgroundLocationPermission()
        }
    }

    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val app = application as GeoTodoApplication
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    app.activityRecognitionManager.registerActivityTransitions()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var showBackgroundRationale by mutableStateOf(false)
    private var showActivityRationale by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestStage1Permissions()

        setContent {
            AnmelongeofencetodoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application as GeoTodoApplication
                    val taskViewModel: TaskViewModel = viewModel(
                        factory = TaskViewModel.Factory(app.taskRepository, NotificationHelper(this))
                    )
                    val locationViewModel: LocationViewModel = viewModel(
                        factory = LocationViewModel.Factory(app.locationRepository)
                    )
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(
                            app.authRepository,
                            app.locationRepository,
                            app.taskRepository
                        )
                    )

                    AppNavigation(
                        taskViewModel = taskViewModel,
                        locationViewModel = locationViewModel,
                        authViewModel = authViewModel,
                        onRequestBackgroundPermission = { checkBackgroundLocationPermission() },
                        onRequestActivityPermission = { requestActivityPermission() }
                    )

                    if (showBackgroundRationale) {
                        BackgroundLocationRationaleDialog(
                            onDismiss = { showBackgroundRationale = false },
                            onConfirmed = { showBackgroundRationale = false }
                        )
                    }

                    if (showActivityRationale) {
                        ActivityRecognitionRationaleDialog(
                            onDismiss = { showActivityRationale = false },
                            onConfirmed = {
                                showActivityRationale = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestStage1Permissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        stage1PermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                showBackgroundRationale = true
            }
        }
    }

    private fun requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasActivity = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasActivity) {
                showActivityRationale = true
            }
        }
    }
}

@Composable
fun AppNavigation(
    taskViewModel: TaskViewModel,
    locationViewModel: LocationViewModel,
    authViewModel: AuthViewModel,
    onRequestBackgroundPermission: () -> Unit,
    onRequestActivityPermission: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.TaskList.route
    ) {
        composable(NavRoute.TaskList.route) {
            TaskListScreen(
                taskViewModel = taskViewModel,
                authViewModel = authViewModel,
                onNavigateToCreateTask = {
                    onRequestBackgroundPermission()
                    onRequestActivityPermission()
                    navController.navigate(NavRoute.TaskEdit.createRoute())
                },
                onNavigateToEditTask = { taskId ->
                    onRequestBackgroundPermission()
                    onRequestActivityPermission()
                    navController.navigate(NavRoute.TaskEdit.createRoute(taskId = taskId))
                },
                onNavigateToMap = {
                    navController.navigate(NavRoute.MapPicker.createRoute())
                },
                onNavigateToPlaces = {
                    navController.navigate(NavRoute.PlacesList.route)
                }
            )
        }

        composable(
            route = NavRoute.TaskEdit.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("preselectedPlaceId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.ifEmpty { null }
            val preselectedPlaceId = backStackEntry.arguments?.getString("preselectedPlaceId")?.ifEmpty { null }

            TaskEditScreen(
                taskId = taskId,
                preselectedPlaceId = preselectedPlaceId,
                taskViewModel = taskViewModel,
                locationViewModel = locationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenMapPicker = {
                    navController.navigate(NavRoute.MapPicker.createRoute())
                }
            )
        }

        composable(
            route = NavRoute.MapPicker.route,
            arguments = listOf(
                navArgument("placeId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")?.ifEmpty { null }
            MapLocationPickerScreen(
                placeId = placeId,
                locationViewModel = locationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLocationSaved = { _ ->
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoute.PlacesList.route) {
            PlacesListScreen(
                locationViewModel = locationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenMapPicker = { placeId ->
                    navController.navigate(NavRoute.MapPicker.createRoute(placeId))
                }
            )
        }
    }
}