package com.trainpaths.nonogram.tutorial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TutorialStepTest {

    @Test
    fun picksTheFirstStepInDeclarationOrder() {
        val onScreen = setOf(
            TutorialStep.MENU_SETTINGS,
            TutorialStep.MENU_FILTER,
            TutorialStep.MENU_PLAY,
        )
        assertEquals(TutorialStep.MENU_PLAY, nextStep(seen = emptySet(), registered = onScreen))
    }

    @Test
    fun skipsSeenStepsAndChainsToTheNext() {
        val onScreen = setOf(
            TutorialStep.MENU_PLAY,
            TutorialStep.MENU_FILTER,
            TutorialStep.MENU_SETTINGS,
        )
        assertEquals(
            TutorialStep.MENU_FILTER,
            nextStep(seen = setOf(TutorialStep.MENU_PLAY), registered = onScreen),
        )
    }

    @Test
    fun ignoresStepsThatAreNotOnScreen() {
        assertEquals(
            TutorialStep.SETTINGS_THEME,
            nextStep(seen = emptySet(), registered = setOf(TutorialStep.SETTINGS_THEME)),
        )
    }

    @Test
    fun nothingOnScreen_returnsNull() {
        assertNull(nextStep(seen = emptySet(), registered = emptySet()))
    }

    @Test
    fun everythingSeen_returnsNull() {
        assertNull(
            nextStep(
                seen = TutorialStep.entries.toSet(),
                registered = setOf(TutorialStep.GEN_SAVE),
            )
        )
    }
}
