package com.trainpaths.nonogram

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.google.google
import com.trainpaths.nonogram.auth.AuthRepository

object AppInitializer {
    fun onApplicationStart(googleWebClientId: String) {
        KMPAuth.initialize {
            google(serverId = googleWebClientId)
        }
    }

    suspend fun initializeAuth(authRepository: AuthRepository) {
        authRepository.initialize()
    }
}
