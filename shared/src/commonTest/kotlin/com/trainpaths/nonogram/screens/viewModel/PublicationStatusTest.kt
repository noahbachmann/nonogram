package com.trainpaths.nonogram.screens.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals

class PublicationStatusTest {

    @Test
    fun validSignedInPuzzleCanBePublic() {
        assertEquals(true, publicationStatus(isPublic = true, isValid = true, isSignedIn = true))
    }

    @Test
    fun invalidPuzzleIsAlwaysPrivate() {
        assertEquals(false, publicationStatus(isPublic = true, isValid = false, isSignedIn = true))
    }

    @Test
    fun guestPuzzleIsAlwaysPrivate() {
        assertEquals(false, publicationStatus(isPublic = true, isValid = true, isSignedIn = false))
    }

    @Test
    fun privatizingIsAlwaysAllowed() {
        assertEquals(false, publicationStatus(isPublic = false, isValid = true, isSignedIn = true))
    }
}
