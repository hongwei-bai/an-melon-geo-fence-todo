package com.melonapp.an_melon_geo_fence_todo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SavedPlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["placeId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val placeId: String,
    val isCompleted: Boolean,
    val startTime: String?,
    val endTime: String?,
    val daysOfWeekCsv: String?,
    val validFromEpoch: Long?,
    val validUntilEpoch: Long?,
    val requiredActivity: String, // "ANY", "IN_VEHICLE", "ON_FOOT", "STILL"
    val minSpeedKmh: Float?,
    val maxSpeedKmh: Float?,
    val geoTriggerType: String = "ENTER", // "ENTER", "EXIT", "DWELL", "ENTER_OR_EXIT"
    val isSynced: Boolean = true
)
