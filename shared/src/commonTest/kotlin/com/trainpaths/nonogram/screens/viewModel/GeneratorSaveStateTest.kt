package com.trainpaths.nonogram.screens.viewModel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratorSaveStateTest {

    @Test
    fun newPuzzleCanBeSavedWithoutPriorEdits() {
        assertTrue(canSaveNonogram(isSaving = false, isDirty = false, nonogramId = 0L))
    }

    @Test
    fun cleanPersistedPuzzleHasNothingToSave() {
        assertFalse(canSaveNonogram(isSaving = false, isDirty = false, nonogramId = 42L))
    }

    @Test
    fun dirtyPersistedPuzzleCanBeSaved() {
        assertTrue(canSaveNonogram(isSaving = false, isDirty = true, nonogramId = 42L))
    }

    @Test
    fun saveIsDisabledWhilePersistenceIsRunning() {
        assertFalse(canSaveNonogram(isSaving = true, isDirty = true, nonogramId = 42L))
        assertFalse(canSaveNonogram(isSaving = true, isDirty = false, nonogramId = 0L))
    }
}
