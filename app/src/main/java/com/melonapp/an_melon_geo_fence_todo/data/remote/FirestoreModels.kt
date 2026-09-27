package com.melonapp.an_melon_geo_fence_todo.data.remote

import com.google.firebase.firestore.PropertyName
import com.melonapp.an_melon_geo_fence_todo.data.local.SavedPlaceEntity
import com.melonapp.an_melon_geo_fence_todo.data.local.TaskEntity

data class FirestoreCoordinates(
    @get:PropertyName("_latitude") @set:PropertyName("_latitude") var latitude: Double = 0.0,
    @get:PropertyName("_longitude") @set:PropertyName("_longitude") var longitude: Double = 0.0
)

data class FirestoreSavedPlace(
    var id: String = "",
    var name: String = "",
    var coordinates: FirestoreCoordinates = FirestoreCoordinates(),
    var radiusMeters: Double = 100.0,
    var updatedAt: String = ""
) {
    fun toEntity(): SavedPlaceEntity {
        return SavedPlaceEntity(
            id = id,
            name = name,
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            radiusMeters = radiusMeters,
            isSynced = true
        )
    }

    companion object {
        fun fromEntity(entity: SavedPlaceEntity, updatedAt: String): FirestoreSavedPlace {
            return FirestoreSavedPlace(
                id = entity.id,
                name = entity.name,
                coordinates = FirestoreCoordinates(entity.latitude, entity.longitude),
                radiusMeters = entity.radiusMeters,
                updatedAt = updatedAt
            )
        }
    }
}

data class TemporalRule(
    var hasTimeConstraint: Boolean = false,
    var startTime: String? = null,
    var endTime: String? = null,
    var daysOfWeek: List<Int> = emptyList(),
    var validFrom: Long? = null,
    var validUntil: Long? = null
)

data class SpatialRule(
    var transitionType: String = "ENTER",
    var loiteringDelaySeconds: Int = 0
)

data class MotionRule(
    var requiredActivity: String = "ANY",
    var minSpeedKmh: Float? = null,
    var maxSpeedKmh: Float? = null
)

data class TriggerRules(
    var temporal: TemporalRule = TemporalRule(),
    var spatial: SpatialRule = SpatialRule(),
    var motion: MotionRule = MotionRule()
)

data class FirestoreTask(
    var id: String = "",
    var title: String = "",
    var placeId: String = "",
    var isCompleted: Boolean = false,
    var triggerRules: TriggerRules = TriggerRules(),
    var createdAt: String = ""
) {
    fun toEntity(): TaskEntity {
        val daysCsv = if (triggerRules.temporal.daysOfWeek.isNotEmpty()) {
            triggerRules.temporal.daysOfWeek.joinToString(",")
        } else null

        return TaskEntity(
            id = id,
            title = title,
            placeId = placeId,
            isCompleted = isCompleted,
            startTime = triggerRules.temporal.startTime,
            endTime = triggerRules.temporal.endTime,
            daysOfWeekCsv = daysCsv,
            validFromEpoch = triggerRules.temporal.validFrom,
            validUntilEpoch = triggerRules.temporal.validUntil,
            requiredActivity = triggerRules.motion.requiredActivity,
            minSpeedKmh = triggerRules.motion.minSpeedKmh,
            maxSpeedKmh = triggerRules.motion.maxSpeedKmh,
            geoTriggerType = triggerRules.spatial.transitionType.ifEmpty { "ENTER" },
            isSynced = true
        )
    }

    companion object {
        fun fromEntity(entity: TaskEntity, createdAt: String): FirestoreTask {
            val days = entity.daysOfWeekCsv?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()

            return FirestoreTask(
                id = entity.id,
                title = entity.title,
                placeId = entity.placeId,
                isCompleted = entity.isCompleted,
                triggerRules = TriggerRules(
                    temporal = TemporalRule(
                        hasTimeConstraint = !entity.startTime.isNullOrEmpty() || !entity.endTime.isNullOrEmpty() || days.isNotEmpty(),
                        startTime = entity.startTime,
                        endTime = entity.endTime,
                        daysOfWeek = days,
                        validFrom = entity.validFromEpoch,
                        validUntil = entity.validUntilEpoch
                    ),
                    spatial = SpatialRule(
                        transitionType = entity.geoTriggerType,
                        loiteringDelaySeconds = 0
                    ),
                    motion = MotionRule(
                        requiredActivity = entity.requiredActivity,
                        minSpeedKmh = entity.minSpeedKmh,
                        maxSpeedKmh = entity.maxSpeedKmh
                    )
                ),
                createdAt = createdAt
            )
        }
    }
}
