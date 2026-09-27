package com.melonapp.an_melon_geo_fence_todo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.LocationViewModel
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String? = null,
    preselectedPlaceId: String? = null,
    taskViewModel: TaskViewModel,
    locationViewModel: LocationViewModel,
    onNavigateBack: () -> Unit,
    onOpenMapPicker: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedPlaces by locationViewModel.savedPlaces.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var selectedPlaceId by remember {
        mutableStateOf(preselectedPlaceId?.ifEmpty { null } ?: savedPlaces.firstOrNull()?.id ?: "")
    }
    var geoTriggerType by remember { mutableStateOf("ENTER") }

    // Temporal Gate States
    var enableTimeGate by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("18:00") }

    val daysOfWeek = remember {
        mutableStateListOf(1, 2, 3, 4, 5) // Default Weekdays (Mon-Fri)
    }

    // Kinematic / Motion States
    var selectedActivity by remember { mutableStateOf("ANY") }
    var enableSpeedLimit by remember { mutableStateOf(false) }
    var maxSpeedKmhStr by remember { mutableStateOf("8.0") }
    var minSpeedKmhStr by remember { mutableStateOf("") }

    var placesDropdownExpanded by remember { mutableStateOf(false) }

    // Load existing task data if editing
    LaunchedEffect(taskId) {
        if (!taskId.isNullOrEmpty()) {
            val taskWithPlace = taskViewModel.getTaskWithPlaceById(taskId)
            if (taskWithPlace != null) {
                val task = taskWithPlace.task
                taskTitle = task.title
                selectedPlaceId = task.placeId
                geoTriggerType = task.geoTriggerType

                val hasTime = !task.startTime.isNullOrEmpty() && !task.endTime.isNullOrEmpty()
                enableTimeGate = hasTime
                if (hasTime) {
                    startTime = task.startTime ?: "08:00"
                    endTime = task.endTime ?: "18:00"
                }

                if (!task.daysOfWeekCsv.isNullOrEmpty()) {
                    daysOfWeek.clear()
                    daysOfWeek.addAll(
                        task.daysOfWeekCsv.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                    )
                }

                selectedActivity = task.requiredActivity
                val hasSpeed = (task.minSpeedKmh != null || task.maxSpeedKmh != null)
                enableSpeedLimit = hasSpeed
                minSpeedKmhStr = task.minSpeedKmh?.toString() ?: ""
                maxSpeedKmhStr = task.maxSpeedKmh?.toString() ?: ""
            }
        }
    }

    LaunchedEffect(savedPlaces) {
        if (selectedPlaceId.isEmpty() && savedPlaces.isNotEmpty()) {
            selectedPlaceId = preselectedPlaceId?.ifEmpty { null } ?: savedPlaces.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId.isNullOrEmpty()) "Create Smart Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Description / Title") },
                placeholder = { Text("e.g., Buy oat milk and coffee beans") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            // Section 1: Spatial Condition (Geofence Place)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Spatial Condition (Geofence Perimeter)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (savedPlaces.isEmpty()) {
                        Text(
                            text = "No saved locations yet. Tap below to pick on the map.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = placesDropdownExpanded,
                            onExpandedChange = { placesDropdownExpanded = !placesDropdownExpanded }
                        ) {
                            val currentPlace = savedPlaces.find { it.id == selectedPlaceId }
                            OutlinedTextField(
                                value = currentPlace?.let { "${it.name} (${it.radiusMeters.toInt()}m)" }
                                    ?: "Select a place",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Associated Place") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = placesDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )

                            ExposedDropdownMenu(
                                expanded = placesDropdownExpanded,
                                onDismissRequest = { placesDropdownExpanded = false }
                            ) {
                                savedPlaces.forEach { place ->
                                    DropdownMenuItem(
                                        text = { Text("${place.name} (${place.radiusMeters.toInt()}m)") },
                                        onClick = {
                                            selectedPlaceId = place.id
                                            placesDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenMapPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Drop Pin / Create New Place on Map")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trigger When:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    val triggerOptions = listOf(
                        "ENTER" to ("🚪 Enter (Arrival)" to "Alert as soon as you enter this boundary"),
                        "EXIT" to ("🏃 Exit (Departure)" to "Alert when leaving the boundary (e.g. remember keys)"),
                        "DWELL" to ("⏳ Stay inside (Dwell)" to "Alert after remaining inside boundary (~30s)"),
                        "ENTER_OR_EXIT" to ("🔄 Enter or Exit" to "Alert both on arrival and when leaving")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        triggerOptions.forEach { (type, optionPair) ->
                            val (label, _) = optionPair
                            FilterChip(
                                selected = (geoTriggerType == type),
                                onClick = { geoTriggerType = type },
                                label = { Text(label) }
                            )
                        }
                    }

                    val conditionHint = triggerOptions.firstOrNull { it.first == geoTriggerType }?.second?.second ?: ""
                    Text(
                        text = conditionHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 2: Temporal Gate (Time/Date Window)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Temporal Gate (Time / Days)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Switch(
                            checked = enableTimeGate,
                            onCheckedChange = { enableTimeGate = it }
                        )
                    }

                    if (enableTimeGate) {
                        Text(
                            text = "Active Daily Hours:",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("From (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("To (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active Days of Week:",
                            style = MaterialTheme.typography.labelMedium
                        )

                        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            dayNames.forEachIndexed { index, name ->
                                val dayNum = index + 1 // 1=Mon .. 7=Sun
                                val isSelected = daysOfWeek.contains(dayNum)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            daysOfWeek.remove(dayNum)
                                        } else {
                                            daysOfWeek.add(dayNum)
                                        }
                                    },
                                    label = { Text(name) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Trigger any time when entering the boundary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 3: Kinematic / Motion State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Kinematic / Motion State",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Required Activity Filter:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    val activities = listOf(
                        "ANY" to "Any",
                        "ON_FOOT" to "Walking (On Foot)",
                        "IN_VEHICLE" to "Driving (In Vehicle)",
                        "STILL" to "Stationary (Still)"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        activities.forEach { (key, label) ->
                            FilterChip(
                                selected = (selectedActivity == key),
                                onClick = { selectedActivity = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed Boundary Thresholds",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Switch(
                            checked = enableSpeedLimit,
                            onCheckedChange = { enableSpeedLimit = it }
                        )
                    }

                    if (enableSpeedLimit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = minSpeedKmhStr,
                                onValueChange = { minSpeedKmhStr = it },
                                label = { Text("Min km/h") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = maxSpeedKmhStr,
                                onValueChange = { maxSpeedKmhStr = it },
                                label = { Text("Max km/h") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    if (taskTitle.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a task title.")
                        }
                        return@Button
                    }
                    if (selectedPlaceId.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select or create a location.")
                        }
                        return@Button
                    }

                    val daysCsv = if (enableTimeGate && daysOfWeek.isNotEmpty()) {
                        daysOfWeek.joinToString(",")
                    } else null

                    val minSpeed = if (enableSpeedLimit) minSpeedKmhStr.toFloatOrNull() else null
                    val maxSpeed = if (enableSpeedLimit) maxSpeedKmhStr.toFloatOrNull() else null

                    taskViewModel.saveTask(
                        id = taskId,
                        title = taskTitle.trim(),
                        placeId = selectedPlaceId,
                        startTime = if (enableTimeGate) startTime else null,
                        endTime = if (enableTimeGate) endTime else null,
                        daysOfWeekCsv = daysCsv,
                        requiredActivity = selectedActivity,
                        minSpeedKmh = minSpeed,
                        maxSpeedKmh = maxSpeed,
                        geoTriggerType = geoTriggerType
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (taskId.isNullOrEmpty()) "Save Smart Reminder" else "Update Reminder")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
