package com.melonapp.an_melon_geo_fence_todo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskWithPlace
import com.melonapp.an_melon_geo_fence_todo.ui.components.SimulationDialog
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.AuthViewModel
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.TaskFilter
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    taskViewModel: TaskViewModel,
    authViewModel: AuthViewModel,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToEditTask: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToPlaces: () -> Unit
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val currentFilter by taskViewModel.filter.collectAsState()
    val isSyncing by authViewModel.isSyncing.collectAsState()
    val syncMessage by authViewModel.syncMessage.collectAsState()
    val simulationMessage by taskViewModel.simulationMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var taskToSimulate by remember { mutableStateOf<TaskWithPlace?>(null) }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearSyncMessage()
        }
    }

    LaunchedEffect(simulationMessage) {
        simulationMessage?.let {
            snackbarHostState.showSnackbar(it)
            taskViewModel.clearSimulationMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GeoTodo Smart Reminders")
                        Text(
                            text = "Cloud: ${authViewModel.getUserId().take(10)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.syncDataNow() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync Data")
                        }
                    }
                    IconButton(onClick = onNavigateToPlaces) {
                        Icon(Icons.Default.Map, contentDescription = "Saved Places")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == TaskFilter.ALL,
                    onClick = { taskViewModel.setFilter(TaskFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.PENDING,
                    onClick = { taskViewModel.setFilter(TaskFilter.PENDING) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.COMPLETED,
                    onClick = { taskViewModel.setFilter(TaskFilter.COMPLETED) },
                    label = { Text("Completed") }
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No tasks found for selected filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onNavigateToCreateTask) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create a Smart Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.task.id }) { taskWithPlace ->
                        TaskCard(
                            taskWithPlace = taskWithPlace,
                            onToggleComplete = { taskViewModel.toggleTaskCompleted(taskWithPlace.task) },
                            onDelete = { taskViewModel.deleteTask(taskWithPlace.task) },
                            onEdit = { onNavigateToEditTask(taskWithPlace.task.id) },
                            onSimulateTrigger = { taskToSimulate = taskWithPlace }
                        )
                    }
                }
            }
        }
    }

    taskToSimulate?.let { item ->
        SimulationDialog(
            taskWithPlace = item,
            onDismiss = { taskToSimulate = null },
            onSimulate = { simulatedActivity, simulatedSpeedKmh, simulatedTransition ->
                taskViewModel.simulateGeofenceEvent(item, simulatedActivity, simulatedSpeedKmh, simulatedTransition)
            }
        )
    }
}

@Composable
fun TaskCard(
    taskWithPlace: TaskWithPlace,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSimulateTrigger: () -> Unit
) {
    val task = taskWithPlace.task
    val place = taskWithPlace.place

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .clickable { onEdit() }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "📍 ${place.name} (${place.radiusMeters.toInt()}m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Trigger Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Geo Condition Badge
                val geoLabel = when (task.geoTriggerType) {
                    "EXIT" -> "🏃 Exit / Leaving"
                    "DWELL" -> "⏳ Staying inside (~30s)"
                    "ENTER_OR_EXIT" -> "🔄 Enter or Exit"
                    else -> "🚪 Enter / Arriving"
                }
                SuggestionChip(
                    onClick = onEdit,
                    label = { Text(geoLabel) }
                )

                // Temporal Badge
                if (!task.startTime.isNullOrEmpty() && !task.endTime.isNullOrEmpty()) {
                    SuggestionChip(
                        onClick = onEdit,
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("${task.startTime}-${task.endTime}") }
                    )
                }

                // Days Badge
                if (!task.daysOfWeekCsv.isNullOrEmpty()) {
                    val days = task.daysOfWeekCsv.split(",")
                    val labelText = if (days.size == 7) "Everyday" else "${days.size} days/wk"
                    SuggestionChip(
                        onClick = {},
                        label = { Text(labelText) }
                    )
                }

                // Motion Badge
                val motionLabel = when (task.requiredActivity.uppercase()) {
                    "ON_FOOT" -> "Walking (On Foot)"
                    "IN_VEHICLE" -> "Driving (In Vehicle)"
                    "STILL" -> "Stationary"
                    else -> "Any Motion"
                }
                val motionIcon = when (task.requiredActivity.uppercase()) {
                    "IN_VEHICLE" -> Icons.Default.DirectionsCar
                    else -> Icons.AutoMirrored.Filled.DirectionsWalk
                }

                SuggestionChip(
                    onClick = {},
                    icon = { Icon(motionIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(motionLabel) }
                )

                // Speed bounds badge
                if (task.maxSpeedKmh != null) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("≤ ${task.maxSpeedKmh.toInt()} km/h") }
                    )
                }
            }

            // Quick Simulate Button
            if (!task.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onSimulateTrigger,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Geofence Wake-up", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
