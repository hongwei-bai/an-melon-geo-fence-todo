package com.melonapp.an_melon_geo_fence_todo.data.repository

import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceDao
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.data.remote.FirestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class LocationRepository(
    private val savedPlaceDao: SavedPlaceDao,
    private val firestoreService: FirestoreService,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val allPlacesFlow: Flow<List<SavedPlaceEntity>> = savedPlaceDao.getAllPlacesFlow()

    suspend fun getAllPlaces(): List<SavedPlaceEntity> = savedPlaceDao.getAllPlaces()

    suspend fun getPlaceById(id: String): SavedPlaceEntity? = savedPlaceDao.getPlaceById(id)

    suspend fun savePlace(
        id: String? = null,
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): SavedPlaceEntity {
        val place = SavedPlaceEntity(
            id = id ?: ("place_" + UUID.randomUUID().toString().take(8)),
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            isSynced = false
        )
        savedPlaceDao.insertPlace(place)

        // Trigger remote sync
        scope.launch {
            val userId = authRepository.getCurrentUserId()
            val success = firestoreService.uploadPlace(userId, place)
            if (success) {
                savedPlaceDao.updateSyncStatus(place.id, true)
            }
        }

        return place
    }

    suspend fun deletePlace(place: SavedPlaceEntity) {
        savedPlaceDao.deletePlace(place)
        scope.launch {
            val userId = authRepository.getCurrentUserId()
            firestoreService.deletePlace(userId, place.id)
        }
    }

    suspend fun syncRemotePlaces(): Int {
        val userId = authRepository.getCurrentUserId()
        val remotePlaces = firestoreService.fetchAllPlaces(userId)
        if (remotePlaces.isNotEmpty()) {
            val entities = remotePlaces.map { it.toEntity() }
            savedPlaceDao.insertPlaces(entities)
        }
        return remotePlaces.size
    }
}
