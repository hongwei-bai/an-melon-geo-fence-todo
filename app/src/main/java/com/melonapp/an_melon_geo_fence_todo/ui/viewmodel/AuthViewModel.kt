package com.melonapp.an_melon_geo_fence_todo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.melonapp.an_melon_geo_fence_todo.data.repository.AuthRepository
import com.melonapp.an_melon_geo_fence_todo.data.repository.LocationRepository
import com.melonapp.an_melon_geo_fence_todo.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun getUserId(): String = authRepository.getCurrentUserId()

    fun isAnonymous(): Boolean = authRepository.isAnonymous

    fun syncDataNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val placesCount = locationRepository.syncRemotePlaces()
                val tasksCount = taskRepository.syncRemoteTasks()
                _syncMessage.value = "Synced $placesCount places and $tasksCount tasks with cloud."
            } catch (e: Exception) {
                _syncMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val locationRepository: LocationRepository,
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository, locationRepository, taskRepository) as T
        }
    }
}
