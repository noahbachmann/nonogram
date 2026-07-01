package com.trainpaths.nonogram

import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.trainpaths.nonogram.auth.AuthRepository

object AppInitializer {
    fun onApplicationStart(googleWebClientId: String) {
        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = googleWebClientId))
    }

    fun initializeAuth(authRepository: AuthRepository) {
        authRepository.initialize()
    }
}