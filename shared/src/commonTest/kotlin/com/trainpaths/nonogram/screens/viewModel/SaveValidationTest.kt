package com.trainpaths.nonogram.screens.viewModel

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
}
