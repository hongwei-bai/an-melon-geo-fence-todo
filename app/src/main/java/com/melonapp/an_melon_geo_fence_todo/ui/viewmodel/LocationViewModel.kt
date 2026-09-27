package com.melonapp.an_melon_geo_fence_todo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = locationRepository.allPlacesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _selectedLatitude = MutableStateFlow(37.7749) // Default San Francisco
    val selectedLatitude: StateFlow<Double> = _selectedLatitude.asStateFlow()

    private val _selectedLongitude = MutableStateFlow(-122.4194)
    val selectedLongitude: StateFlow<Double> = _selectedLongitude.asStateFlow()

    private val _radiusMeters = MutableStateFlow(200.0) // 50m <= r <= 1000m per PRD
    val radiusMeters: StateFlow<Double> = _radiusMeters.asStateFlow()

    private val _placeName = MutableStateFlow("")
    val placeName: StateFlow<String> = _placeName.asStateFlow()

    private val _editingPlaceId = MutableStateFlow<String?>(null)
    val editingPlaceId: StateFlow<String?> = _editingPlaceId.asStateFlow()

    fun updateSelectedLocation(lat: Double, lng: Double) {
        _selectedLatitude.value = lat
        _selectedLongitude.value = lng
    }

    fun updateRadius(radius: Double) {
        _radiusMeters.value = radius.coerceIn(10.0, 1000.0)
    }

    fun updatePlaceName(name: String) {
        _placeName.value = name
    }

    fun loadPlaceForEdit(placeId: String?) {
        _editingPlaceId.value = placeId
        if (placeId.isNullOrEmpty()) {
            _placeName.value = ""
            return
        }
        viewModelScope.launch {
            val place = locationRepository.getPlaceById(placeId)
            if (place != null) {
                _placeName.value = place.name
                _selectedLatitude.value = place.latitude
                _selectedLongitude.value = place.longitude
                _radiusMeters.value = place.radiusMeters
            }
        }
    }

    suspend fun getPlaceById(id: String): SavedPlaceEntity? {
        return locationRepository.getPlaceById(id)
    }

    fun saveCurrentPlace(
        customName: String? = null,
        onSuccess: (SavedPlaceEntity) -> Unit
    ) {
        val nameToSave = (customName ?: _placeName.value).trim().ifEmpty { "Saved Place" }
        viewModelScope.launch {
            val place = locationRepository.savePlace(
                id = _editingPlaceId.value,
                name = nameToSave,
                latitude = _selectedLatitude.value,
                longitude = _selectedLongitude.value,
                radiusMeters = _radiusMeters.value
            )
            _placeName.value = ""
            _editingPlaceId.value = null
            onSuccess(place)
        }
    }

    fun quickUpdatePlace(
        place: SavedPlaceEntity,
        newName: String,
        newRadiusMeters: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            locationRepository.savePlace(
                id = place.id,
                name = newName.trim().ifEmpty { place.name },
                latitude = place.latitude,
                longitude = place.longitude,
                radiusMeters = newRadiusMeters.coerceIn(10.0, 1000.0)
            )
            onSuccess()
        }
    }

    fun deletePlace(place: SavedPlaceEntity) {
        viewModelScope.launch {
            locationRepository.deletePlace(place)
        }
    }

    class Factory(
        private val locationRepository: LocationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LocationViewModel(locationRepository) as T
        }
    }
}
