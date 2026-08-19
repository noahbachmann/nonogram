package com.trainpaths.nonogram.auth

import com.trainpaths.nonogram.firebase.FirebaseWeb

actual suspend fun firebaseSignOut() {
    FirebaseWeb.signOut()
}
