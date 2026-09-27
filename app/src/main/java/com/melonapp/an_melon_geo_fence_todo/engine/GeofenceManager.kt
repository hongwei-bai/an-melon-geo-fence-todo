package com.melonapp.an_melon_geo_fence_todo.engine

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.receiver.GeofenceBroadcastReceiver
import kotlinx.coroutines.tasks.await

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, GEOFENCE_REQUEST_CODE, intent, flags)
    }

    fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && backgroundLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(
        places: List<SavedPlaceEntity>,
        currentLocation: Location? = null
    ): Boolean {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Cannot register geofences: Missing fine or background location permission.")
            return false
        }

        if (places.isEmpty()) {
            removeAllGeofences()
            return true
        }

        // Android enforces a hard limit of 100 geofences per app.
        // If places exceed 100, prioritize the 100 nearest to the current location.
        val prioritizedPlaces = if (places.size > MAX_GEOFENCES && currentLocation != null) {
            places.sortedBy { place ->
                val placeLocation = Location("").apply {
                    latitude = place.latitude
                    longitude = place.longitude
                }
                currentLocation.distanceTo(placeLocation)
            }.take(MAX_GEOFENCES)
        } else {
            places.take(MAX_GEOFENCES)
        }

        val geofenceList = prioritizedPlaces.map { place ->
            Geofence.Builder()
                .setRequestId(place.id)
                .setCircularRegion(
                    place.latitude,
                    place.longitude,
                    place.radiusMeters.coerceIn(10.0, 1000.0).toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(LOITERING_DELAY_MS)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
            )
            .addGeofences(geofenceList)
            .build()

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.d(TAG, "Successfully registered ${geofenceList.size} geofences.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofences: ${e.message}", e)
            false
        }
    }

    suspend fun removeGeofence(placeId: String): Boolean {
        return try {
            geofencingClient.removeGeofences(listOf(placeId)).await()
            Log.d(TAG, "Successfully removed geofence: $placeId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove geofence $placeId: ${e.message}", e)
            false
        }
    }

    suspend fun removeAllGeofences(): Boolean {
        return try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Log.d(TAG, "Removed all geofences.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove all geofences: ${e.message}", e)
            false
        }
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_REQUEST_CODE = 1001
        private const val MAX_GEOFENCES = 100
        private const val LOITERING_DELAY_MS = 30000 // 30s loitering delay per PRD
    }
}
