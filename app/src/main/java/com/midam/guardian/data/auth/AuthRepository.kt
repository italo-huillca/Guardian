package com.midam.guardian.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepository(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
    }
    
    init {
        // Verificar si hay una sesión guardada
        val isLoggedIn = sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val currentFirebaseUser = auth.currentUser
        
        if (isLoggedIn && currentFirebaseUser != null) {
            _isAuthenticated.value = true
            _currentUser.value = currentFirebaseUser
        } else {
            // Limpiar preferencias si no hay usuario actual en Firebase
            clearAuthState()
        }
        
        // Listener para cambios en el estado de autenticación de Firebase
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null && _isAuthenticated.value) {
                // El usuario se deslogueó externamente, limpiar estado local
                clearAuthState()
            }
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            suspendCancellableCoroutine { continuation ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user
                            if (user != null) {
                                // Guardar estado de autenticación
                                sharedPrefs.edit()
                                    .putBoolean(KEY_IS_LOGGED_IN, true)
                                    .putString(KEY_USER_EMAIL, email)
                                    .apply()
                                
                                _isAuthenticated.value = true
                                _currentUser.value = user
                                continuation.resume(Result.success(Unit))
                            } else {
                                continuation.resume(Result.failure(Exception("Usuario no encontrado")))
                            }
                        } else {
                            val exception = task.exception ?: Exception("Error desconocido")
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        clearAuthState()
    }
    
    private fun clearAuthState() {
        sharedPrefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_EMAIL)
            .apply()
        
        _isAuthenticated.value = false
        _currentUser.value = null
    }
    
    fun getUserEmail(): String? {
        return sharedPrefs.getString(KEY_USER_EMAIL, null)
    }
} 