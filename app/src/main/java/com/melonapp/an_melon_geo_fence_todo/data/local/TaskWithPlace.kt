package com.melonapp.an_melon_geo_fence_todo.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithPlace(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "placeId",
        entityColumn = "id"
    )
    val place: SavedPlaceEntity
)
