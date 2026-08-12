package com.trainpaths.nonogram.screens.viewModel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun unvalidatedPuzzleIsAlwaysPrivate() {
        assertEquals(false, publicationStatus(isPublic = true, isValid = null, isSignedIn = true))
    }

    @Test
    fun guestPuzzleIsAlwaysPrivate() {
        assertEquals(false, publicationStatus(isPublic = true, isValid = true, isSignedIn = false))
    }

    @Test
    fun privatizingIsAlwaysAllowed() {
        assertEquals(false, publicationStatus(isPublic = false, isValid = true, isSignedIn = true))
    }

    @Test
    fun validationFailureBecomesNonBlockingUnavailableResult() {
        val result = validationForSave { error("Solver failed") }

        assertEquals(null, result.isValid)
        assertEquals(ValidationState.UNAVAILABLE, result.state)
        assertEquals("Solver failed", result.error)
    }

    @Test
    fun cancellationStillCancelsSaving() {
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            validationForSave { throw kotlinx.coroutines.CancellationException("Cancelled") }
        }
    }
}
