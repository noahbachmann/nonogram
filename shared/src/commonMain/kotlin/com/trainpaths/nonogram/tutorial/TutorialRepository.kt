package com.trainpaths.nonogram.tutorial

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_SEEN_PREFIX = "tutorial_seen_"

/**
 * Which tutorial hints the user has already acknowledged. Device-wide and not keyed by uid, so the
 * tips survive sign-out the way [com.trainpaths.nonogram.auth.AuthRepository]'s onboarding flag does.
 */
class TutorialRepository(private val settings: Settings) {

    private val _seenSteps = MutableStateFlow(
        TutorialStep.entries
            .filter { settings.getBoolean(KEY_SEEN_PREFIX + it.name, false) }
            .toSet()
    )
    val seenSteps: StateFlow<Set<TutorialStep>> = _seenSteps.asStateFlow()

    fun markSeen(step: TutorialStep) {
        if (step in _seenSteps.value) return
        _seenSteps.value += step
        settings.putBoolean(KEY_SEEN_PREFIX + step.name, true)
    }

    fun markAllSeen() {
        if (_seenSteps.value.size == TutorialStep.entries.size) return
        _seenSteps.value = TutorialStep.entries.toSet()
        TutorialStep.entries.forEach { settings.putBoolean(KEY_SEEN_PREFIX + it.name, true) }
    }

    fun resetAll() {
        if (_seenSteps.value.isEmpty()) return
        _seenSteps.value = emptySet()
        TutorialStep.entries.forEach { settings.remove(KEY_SEEN_PREFIX + it.name) }
    }
}
