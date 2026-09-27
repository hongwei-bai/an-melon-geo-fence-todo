package com.melonapp.an_melon_geo_fence_todo.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val prefs = context.getSharedPreferences("geo_todo_auth_prefs", Context.MODE_PRIVATE)

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
        _currentUser.value = auth?.currentUser
    }

    fun getCurrentUserId(): String {
        val user = _currentUser.value
        if (user != null) {
            return user.uid
        }
        // Fallback local UUID stored in SharedPreferences if Firebase is unconfigured or offline
        var localUid = prefs.getString("local_user_id", null)
        if (localUid == null) {
            localUid = "anon_" + java.util.UUID.randomUUID().toString().take(12)
            prefs.edit().putString("local_user_id", localUid).apply()
        }
        return localUid
    }

    val isAnonymous: Boolean
        get() = _currentUser.value?.isAnonymous ?: true

    suspend fun signInAnonymously(): Boolean {
        val firebaseAuth = auth ?: return false
        return try {
            if (firebaseAuth.currentUser == null) {
                firebaseAuth.signInAnonymously().await()
            }
            _currentUser.value = firebaseAuth.currentUser
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in anonymously: ${e.message}", e)
            false
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
