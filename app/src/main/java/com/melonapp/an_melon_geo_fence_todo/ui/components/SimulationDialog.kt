package com.melonapp.an_melon_geo_fence_todo.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskWithPlace

@Composable
fun SimulationDialog(
    taskWithPlace: TaskWithPlace,
    onDismiss: () -> Unit,
    onSimulate: (simulatedActivity: String, simulatedSpeedKmh: Float?, simulatedTransition: String) -> Unit
) {
    val task = taskWithPlace.task
    val place = taskWithPlace.place

    val activityOptions = listOf("ON_FOOT", "IN_VEHICLE", "STILL", "UNKNOWN")
    var selectedActivity by remember { mutableStateOf("ON_FOOT") }
    var speedSliderValue by remember { mutableFloatStateOf(4.0f) }
    var selectedTransition by remember {
        mutableStateOf(
            when (task.geoTriggerType) {
                "EXIT" -> "EXIT"
                "DWELL" -> "DWELL"
                else -> "ENTER"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simulate Geofence Trigger")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Testing Task: \"${task.title}\"\nLocation: ${place.name} (${place.radiusMeters.toInt()}m)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Required Task Constraints:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "• Trigger Condition: ${task.geoTriggerType}\n" +
                            "• Activity: ${task.requiredActivity}\n" +
                            "• Speed: ${task.minSpeedKmh ?: 0} - ${task.maxSpeedKmh ?: "∞"} km/h\n" +
                            "• Hours: ${task.startTime ?: "Any"} - ${task.endTime ?: "Any"}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simulated Geofence Transition:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        "ENTER" to "🚪 Enter",
                        "EXIT" to "🏃 Exit",
                        "DWELL" to "⏳ Dwell"
                    ).forEach { (trans, label) ->
                        FilterChip(
                            selected = (selectedTransition == trans),
                            onClick = { selectedTransition = trans },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simulated User Motion State:",
                    style = MaterialTheme.typography.labelMedium
                )

                Column(modifier = Modifier.selectableGroup()) {
                    activityOptions.forEach { activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .selectable(
                                    selected = (activity == selectedActivity),
                                    onClick = {
                                        selectedActivity = activity
                                        if (activity == "IN_VEHICLE" && speedSliderValue < 30f) {
                                            speedSliderValue = 45f
                                        } else if (activity == "ON_FOOT" && speedSliderValue > 10f) {
                                            speedSliderValue = 4f
                                        } else if (activity == "STILL") {
                                            speedSliderValue = 0f
                                        }
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (activity == selectedActivity),
                                onClick = null
                            )
                            Text(
                                text = when (activity) {
                                    "ON_FOOT" -> "Walking / On Foot"
                                    "IN_VEHICLE" -> "Driving (In Vehicle)"
                                    "STILL" -> "Stationary (Still)"
                                    else -> "Unknown"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulated GPS Speed:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${speedSliderValue.toInt()} km/h",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Slider(
                    value = speedSliderValue,
                    onValueChange = { speedSliderValue = it },
                    valueRange = 0f..120f,
                    steps = 23
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSimulate(selectedActivity, speedSliderValue, selectedTransition)
                    onDismiss()
                }
            ) {
                Text("Evaluate & Trigger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
