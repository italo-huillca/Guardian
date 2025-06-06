package com.midam.guardian.data.auth

import android.content.Context

object AuthManager {
    @Volatile
    private var INSTANCE: AuthRepository? = null
    
    fun getInstance(context: Context): AuthRepository {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
    
    fun signOut() {
        INSTANCE?.signOut()
    }
} 