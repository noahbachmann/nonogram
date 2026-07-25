package com.trainpaths.nonogram.screens.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals

class PublicationStatusTest {

    @Test
    fun validSignedInPuzzleCanBePublic() {
        assertEquals(1L, publicationStatus(isPublic = true, isValid = true, isSignedIn = true))
    }

    @Test
    fun invalidPuzzleIsAlwaysPrivate() {
        assertEquals(0L, publicationStatus(isPublic = true, isValid = false, isSignedIn = true))
    }

    @Test
    fun guestPuzzleIsAlwaysPrivate() {
        assertEquals(0L, publicationStatus(isPublic = true, isValid = true, isSignedIn = false))
    }

    @Test
    fun privatizingIsAlwaysAllowed() {
        assertEquals(0L, publicationStatus(isPublic = false, isValid = true, isSignedIn = true))
    }
}
