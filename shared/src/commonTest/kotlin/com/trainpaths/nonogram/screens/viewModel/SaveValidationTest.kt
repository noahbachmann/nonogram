package com.trainpaths.nonogram.screens.viewModel

import com.trainpaths.nonogram.classes.PublishStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaveValidationTest {

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

    @Test
    fun contentChangeReplacesEveryStatusWithTheFreshVerdict() {
        for (status in PublishStatus.entries) {
            assertEquals(
                expected = PublishStatus.VALID,
                actual = status.afterSave(verdict = true, contentChanged = true),
                message = "solvable edit of a $status puzzle",
            )
            assertEquals(
                expected = PublishStatus.NONE,
                actual = status.afterSave(verdict = false, contentChanged = true),
                message = "unsolvable edit of a $status puzzle",
            )
        }
    }

    @Test
    fun unchangedSaveLeavesAReviewStatusAlone() {
        val reviewerOwned =
            listOf(PublishStatus.PENDING, PublishStatus.DENIED, PublishStatus.UNLISTED, PublishStatus.APPROVED)

        for (status in reviewerOwned) {
            for (verdict in listOf(true, false, null)) {
                assertEquals(
                    expected = status,
                    actual = status.afterSave(verdict, contentChanged = false),
                    message = "unchanged save of a $status puzzle with verdict $verdict",
                )
            }
        }
    }

    @Test
    fun unchangedSaveStillCorrectsAnUnpublishedStatus() {
        assertEquals(PublishStatus.VALID, PublishStatus.NONE.afterSave(true, contentChanged = false))
        assertEquals(PublishStatus.NONE, PublishStatus.VALID.afterSave(false, contentChanged = false))
    }

    @Test
    fun anUnavailableVerdictNeverClaimsValid() {
        assertEquals(PublishStatus.NONE, PublishStatus.VALID.afterSave(null, contentChanged = false))
        assertEquals(PublishStatus.NONE, PublishStatus.APPROVED.afterSave(null, contentChanged = true))
    }
}
