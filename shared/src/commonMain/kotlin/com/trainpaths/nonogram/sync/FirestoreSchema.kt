package com.trainpaths.nonogram.sync

/**
 * The Firestore wire format in one place. Platforms [SyncService] implementations write and
 * read the same documents.
 *
 * The web externals (`firebase/JsInterop.kt`) name the fields a fourth time, as property names:
 * external declarations cannot reference a constant, so those are checked by eye against [Fields].
 */
internal object Paths {
    const val USERS = "users"
    const val PROGRESS = "progress"
    const val NONOGRAMS = "nonograms"
    const val ADMINS = "admins"

    fun progress(firebaseUid: String) = "$USERS/$firebaseUid/$PROGRESS"

    fun progressDoc(firebaseUid: String, nonogramId: Long) = "${progress(firebaseUid)}/$nonogramId"

    fun user(firebaseUid: String) = "$USERS/$firebaseUid"

    fun nonogram(nonogramId: Long) = "$NONOGRAMS/$nonogramId"

    fun admin(firebaseUid: String) = "$ADMINS/$firebaseUid"
}

internal object Fields {
    const val BOARD_STATE = "boardState"
    const val UPDATED_AT = "updatedAt"

    const val DIFFICULTY = "difficulty"
    const val SOLUTION = "solution"
    const val NAME = "name"
    const val AUTHOR_UID = "authorUid"
    const val PUBLISH_STATUS = "publishStatus"

    const val DENIAL_STREAK = "denialStreak"
    const val PUBLISH_BANNED = "publishBanned"
}
