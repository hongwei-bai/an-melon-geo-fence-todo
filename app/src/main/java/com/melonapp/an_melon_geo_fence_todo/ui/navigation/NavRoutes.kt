package com.melonapp.an_melon_geo_fence_todo.ui.navigation

sealed class NavRoute(val route: String) {
    data object TaskList : NavRoute("task_list")
    data object TaskEdit : NavRoute("task_edit?taskId={taskId}&preselectedPlaceId={preselectedPlaceId}") {
        fun createRoute(taskId: String? = null, preselectedPlaceId: String? = null): String {
            val taskParam = taskId ?: ""
            val placeParam = preselectedPlaceId ?: ""
            return "task_edit?taskId=$taskParam&preselectedPlaceId=$placeParam"
        }
    }
    data object MapPicker : NavRoute("map_picker?placeId={placeId}") {
        fun createRoute(placeId: String? = null): String {
            val param = placeId ?: ""
            return "map_picker?placeId=$param"
        }
    }
    data object PlacesList : NavRoute("places_list")
}
