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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesListScreen(
    locationViewModel: LocationViewModel,
    onNavigateBack: () -> Unit,
    onOpenMapPicker: (placeId: String?) -> Unit
) {
    val savedPlaces by locationViewModel.savedPlaces.collectAsState()
    var placeToQuickEdit by remember { mutableStateOf<SavedPlaceEntity?>(null) }
    var placeToDelete by remember { mutableStateOf<SavedPlaceEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Places Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenMapPicker(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Place")
            }
        }
    ) { innerPadding ->
        if (savedPlaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No saved places yet.\nDrop a pin on the map to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onOpenMapPicker(null) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Place on Map")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedPlaces, key = { it.id }) { place ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMapPicker(place.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Center: %.5f, %.5f".format(place.latitude, place.longitude),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onOpenMapPicker(place.id) }) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = "See on Map",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { placeToQuickEdit = place }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Place",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(onClick = { placeToDelete = place }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Place",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Quick Info Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = { onOpenMapPicker(place.id) },
                                    label = { Text("Radius: ${place.radiusMeters.toInt()}m") }
                                )
                                SuggestionChip(
                                    onClick = { onOpenMapPicker(place.id) },
                                    label = { Text("🗺️ Tap to view on Map") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Edit Dialog
    placeToQuickEdit?.let { place ->
        var editName by remember(place) { mutableStateOf(place.name) }
        var editRadius by remember(place) { mutableFloatStateOf(place.radiusMeters.toFloat()) }

        AlertDialog(
            onDismissRequest = { placeToQuickEdit = null },
            title = { Text("Edit Saved Place") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Place Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Radius / Distance:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${editRadius.toInt()}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Radius Preset Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(10, 25, 50, 100, 200, 500, 1000).forEach { preset ->
                            FilterChip(
                                selected = editRadius.toInt() == preset,
                                onClick = { editRadius = preset.toFloat() },
                                label = { Text("${preset}m") }
                            )
                        }
                    }

                    Slider(
                        value = editRadius,
                        onValueChange = { editRadius = it },
                        valueRange = 10f..1000f
                    )

                    OutlinedButton(
                        onClick = {
                            val targetId = place.id
                            placeToQuickEdit = null
                            onOpenMapPicker(targetId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Location on Map")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        locationViewModel.quickUpdatePlace(
                            place = place,
                            newName = editName,
                            newRadiusMeters = editRadius.toDouble(),
                            onSuccess = { placeToQuickEdit = null }
                        )
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { placeToQuickEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    placeToDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Delete Saved Place") },
            text = {
                Text("Are you sure you want to delete \"${place.name}\"? Any associated smart reminders may need a new location.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        locationViewModel.deletePlace(place)
                        placeToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { placeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
