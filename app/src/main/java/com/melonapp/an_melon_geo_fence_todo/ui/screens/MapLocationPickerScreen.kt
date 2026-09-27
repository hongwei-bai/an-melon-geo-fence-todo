package com.melonapp.an_melon_geo_fence_todo.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.melonapp.an_melon_geo_fence_todo.ui.viewmodel.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    placeId: String? = null,
    locationViewModel: LocationViewModel,
    onNavigateBack: () -> Unit,
    onLocationSaved: (placeId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedLat by locationViewModel.selectedLatitude.collectAsState()
    val selectedLng by locationViewModel.selectedLongitude.collectAsState()
    val radiusMeters by locationViewModel.radiusMeters.collectAsState()
    val placeName by locationViewModel.placeName.collectAsState()

    val initialPosition = remember { LatLng(selectedLat, selectedLng) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }

    // Bottom card expand state (default collapsed per user requirement)
    var isBottomCardExpanded by remember { mutableStateOf(false) }

    // Name prompt dialog state
    var showNamePromptDialog by remember { mutableStateOf(false) }
    var promptedNameInput by remember { mutableStateOf("") }

    // Load place if editing
    LaunchedEffect(placeId) {
        if (!placeId.isNullOrEmpty()) {
            locationViewModel.loadPlaceForEdit(placeId)
        } else {
            locationViewModel.loadPlaceForEdit(null)
        }
    }

    // When coordinates are loaded from an existing place, animate camera to that position
    LaunchedEffect(selectedLat, selectedLng) {
        val target = LatLng(selectedLat, selectedLng)
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16f))
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        keyboardController?.hide()
        isSearching = true
        searchResults = emptyList()

        scope.launch {
            val results = searchAddress(context, trimmed)
            isSearching = false
            if (results.isNotEmpty()) {
                searchResults = results
                val topMatch = results.first()
                val targetLatLng = LatLng(topMatch.latitude, topMatch.longitude)
                locationViewModel.updateSelectedLocation(topMatch.latitude, topMatch.longitude)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(targetLatLng, 16f))

                // If no place name is entered, suggest the search term or address
                if (placeName.isBlank()) {
                    val suggested = topMatch.featureName ?: topMatch.thoroughfare ?: trimmed
                    locationViewModel.updatePlaceName(suggested)
                }
            } else {
                snackbarHostState.showSnackbar("No locations found for \"$trimmed\".")
            }
        }
    }

    fun handleSave() {
        if (placeName.isNotBlank()) {
            locationViewModel.saveCurrentPlace { savedPlace ->
                onLocationSaved(savedPlace.id)
            }
        } else {
            // Prompt name input dialog when no name is provided yet
            scope.launch {
                val reverseName = reverseGeocodeAddress(context, selectedLat, selectedLng)
                promptedNameInput = reverseName ?: ""
                showNamePromptDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (placeId.isNullOrEmpty()) "Choose Geofence Location" else "Edit Geofence Place") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val currentLatLng = LatLng(selectedLat, selectedLng)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true,
                        myLocationButtonEnabled = false
                    ),
                    onMapClick = { latLng ->
                        locationViewModel.updateSelectedLocation(latLng.latitude, latLng.longitude)
                        searchResults = emptyList()
                    }
                ) {
                    // Focal Coordinate Marker
                    Marker(
                        state = MarkerState(position = currentLatLng),
                        title = placeName.ifEmpty { "Selected Location" },
                        snippet = "Radius: ${radiusMeters.toInt()}m"
                    )

                    // Perimeter Boundary Circle
                    Circle(
                        center = currentLatLng,
                        radius = radiusMeters,
                        fillColor = Color(0x334285F4),
                        strokeColor = Color(0xFF4285F4),
                        strokeWidth = 3f
                    )
                }

                // Top Search Bar (Google Maps style)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search address, place, or landmark...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    }

                    // Search Suggestions Dropdown List
                    if (searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                searchResults.take(4).forEachIndexed { index, address ->
                                    val displayName = address.getAddressLine(0)
                                        ?: "${address.featureName ?: ""}, ${address.locality ?: ""}"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val latLng = LatLng(address.latitude, address.longitude)
                                                locationViewModel.updateSelectedLocation(
                                                    address.latitude,
                                                    address.longitude
                                                )
                                                scope.launch {
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                                    )
                                                }
                                                if (placeName.isBlank()) {
                                                    locationViewModel.updatePlaceName(
                                                        address.featureName ?: address.locality ?: displayName
                                                    )
                                                }
                                                searchResults = emptyList()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2
                                        )
                                    }
                                    if (index < searchResults.take(4).size - 1) {
                                        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Location FAB
                FloatingActionButton(
                    onClick = {
                        @SuppressLint("MissingPermission")
                        scope.launch {
                            try {
                                val fusedLocationClient =
                                    LocationServices.getFusedLocationProviderClient(context)
                                val location: Location? = fusedLocationClient.lastLocation.await()
                                if (location != null) {
                                    val userLatLng = LatLng(location.latitude, location.longitude)
                                    locationViewModel.updateSelectedLocation(
                                        location.latitude,
                                        location.longitude
                                    )
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                                    )
                                } else {
                                    snackbarHostState.showSnackbar("Unable to fetch current location fix.")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Location error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }

            // Bottom Expandable Dialog / Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Bar / Expand-Collapse Toggle Handle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBottomCardExpanded = !isBottomCardExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (placeName.isNotBlank()) placeName else "Geofence Perimeter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${radiusMeters.toInt()}m)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { isBottomCardExpanded = !isBottomCardExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isBottomCardExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isBottomCardExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    // COLLAPSED VIEW: Only Radius Slider & Save Button
                    if (!isBottomCardExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Slider(
                                value = radiusMeters.toFloat(),
                                onValueChange = { locationViewModel.updateRadius(it.toDouble()) },
                                valueRange = 10f..1000f,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { handleSave() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (placeId.isNullOrEmpty()) "Save" else "Update")
                            }
                        }
                    }

                    // EXPANDED VIEW: Full Details (Name, Center Coordinates, Presets, Slider, Save)
                    AnimatedVisibility(
                        visible = isBottomCardExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = placeName,
                                onValueChange = { locationViewModel.updatePlaceName(it) },
                                label = { Text("Location Name (e.g. Home, Office, Costco)") },
                                placeholder = { Text("Enter a friendly place alias") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                text = "Center: %.5f, %.5f".format(selectedLat, selectedLng),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Geofence Radius:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${radiusMeters.toInt()} meters",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Quick Preset Radius Chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                listOf(10, 25, 50, 100, 200, 500, 1000).forEach { preset ->
                                    FilterChip(
                                        selected = radiusMeters.toInt() == preset,
                                        onClick = { locationViewModel.updateRadius(preset.toDouble()) },
                                        label = { Text("${preset}m") }
                                    )
                                }
                            }

                            Slider(
                                value = radiusMeters.toFloat(),
                                onValueChange = { locationViewModel.updateRadius(it.toDouble()) },
                                valueRange = 10f..1000f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isBottomCardExpanded = false }) {
                                    Text("Collapse")
                                }

                                Button(
                                    onClick = { handleSave() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (placeId.isNullOrEmpty()) "Save Place" else "Update Place")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Name Prompt Dialog (Triggered when user clicks Save without entering a name)
    if (showNamePromptDialog) {
        val suggestedChips = listOf("Home", "Work", "Gym", "Supermarket", "School", "Park")

        AlertDialog(
            onDismissRequest = { showNamePromptDialog = false },
            title = { Text("Name this Location") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter a name for this geofence place so you can identify it easily in your smart reminders:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = promptedNameInput,
                        onValueChange = { promptedNameInput = it },
                        label = { Text("Place Name") },
                        placeholder = { Text("e.g., Home, Costco, Office") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Quick Suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        suggestedChips.forEach { chipName ->
                            SuggestionChip(
                                onClick = { promptedNameInput = chipName },
                                label = { Text(chipName) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = promptedNameInput.trim().ifEmpty { "Saved Place" }
                        showNamePromptDialog = false
                        locationViewModel.saveCurrentPlace(customName = finalName) { savedPlace ->
                            onLocationSaved(savedPlace.id)
                        }
                    }
                ) {
                    Text("Save Place")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNamePromptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Geocoding helper using Android Geocoder with coroutines
private suspend fun searchAddress(context: Context, query: String): List<Address> = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        cont.resume(emptyList())
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, 5) ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Reverse Geocoding helper to suggest a place/street name for given GPS coordinates
private suspend fun reverseGeocodeAddress(context: Context, latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<List<Address>> { cont ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        cont.resume(emptyList())
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
        }
        val first = addresses.firstOrNull()
        first?.featureName ?: first?.thoroughfare ?: first?.locality ?: first?.getAddressLine(0)
    } catch (e: Exception) {
        null
    }
}
