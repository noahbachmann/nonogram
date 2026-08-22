package com.trainpaths.nonogram.screens.viewModel

import com.trainpaths.nonogram.classes.PublishStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PublicationStatusTest {

    @Test
    fun approvedPuzzleStaysPublicWhenLeftOn() {
        assertEquals(
            PublishStatus.APPROVED,
            visibilityAfterToggle(PublishStatus.APPROVED, requestedPublic = true),
        )
    }

    @Test
    fun approvedPuzzleCanBeTakenDown() {
        assertEquals(
            PublishStatus.UNLISTED,
            visibilityAfterToggle(PublishStatus.APPROVED, requestedPublic = false),
        )
    }

    @Test
    fun unlistedPuzzleGoesBackUpWithoutANewReview() {
        assertEquals(
            PublishStatus.APPROVED,
            visibilityAfterToggle(PublishStatus.UNLISTED, requestedPublic = true),
        )
    }

    @Test
    fun unreviewedPuzzleCannotPublishItself() {
        assertEquals(
            PublishStatus.NONE,
            visibilityAfterToggle(PublishStatus.NONE, requestedPublic = true),
        )
    }

    @Test
    fun pendingRequestIsNotAffectedByTheToggle() {
        assertEquals(
            PublishStatus.PENDING,
            visibilityAfterToggle(PublishStatus.PENDING, requestedPublic = true),
        )
    }

    @Test
    fun deniedPuzzleCannotPublishItself() {
        assertEquals(
            PublishStatus.DENIED,
            visibilityAfterToggle(PublishStatus.DENIED, requestedPublic = true),
        )
    }

    @Test
    fun editingAPublicPuzzleNeedsConfirmation() {
        assertEquals(true, needsPublicEditConfirmation(isPublic = true, changesContent = true))
    }

    @Test
    fun savingAPublicPuzzleUnchangedNeedsNoConfirmation() {
        assertEquals(false, needsPublicEditConfirmation(isPublic = true, changesContent = false))
    }

    @Test
    fun editingAPrivatePuzzleNeedsNoConfirmation() {
        assertEquals(false, needsPublicEditConfirmation(isPublic = false, changesContent = true))
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
