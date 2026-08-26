package com.trainpaths.nonogram.tutorial

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

private data class Anchor(val owner: Any, val boundsInRoot: Rect)

/**
 * Tracks where every on-screen tutorial target is and decides which hint is due. Pure UI state, so
 * it lives in [TutorialHost] rather than in a ViewModel; only the seen-set is persisted.
 */
@Stable
class TutorialController {

    private val anchors = mutableStateMapOf<TutorialStep, Anchor>()

    var hostBounds: Rect by mutableStateOf(Rect.Zero)
    var paused: Boolean by mutableStateOf(false)

    private var persistedSeen: Set<TutorialStep> by mutableStateOf(emptySet())

    // Dismissals are applied here first so a hint closes on the click that dismissed it, rather than
    // waiting for the persisted set to make the round trip back through the repository's StateFlow.
    private var dismissed: Set<TutorialStep> by mutableStateOf(emptySet())

    private val seenSteps: Set<TutorialStep> by derivedStateOf { persistedSeen + dismissed }

    /** Set by [TutorialHost]; writes through to the persisted seen-set. */
    var onSeen: (TutorialStep) -> Unit = {}

    /** Set by [TutorialHost]; persists "skip all tips". */
    var onSkipAll: () -> Unit = {}

    fun syncPersistedSeen(value: Set<TutorialStep>) {
        if (value == persistedSeen) return
        persistedSeen = value
        if (value.isEmpty()) dismissed = emptySet()
    }

    private val visibleSteps: Set<TutorialStep> by derivedStateOf {
        val host = hostBounds
        if (host.isEmpty) {
            emptySet()
        } else {
            anchors.entries
                .filter { (_, anchor) -> !anchor.boundsInRoot.isEmpty && anchor.boundsInRoot.overlaps(host) }
                .map { it.key }
                .toSet()
        }
    }

    val currentStep: TutorialStep? by derivedStateOf {
        if (paused) null else nextStep(seenSteps, visibleSteps)
    }

    /** The current target's bounds relative to the host, or null when no hint is due. */
    val currentBounds: Rect? by derivedStateOf {
        currentStep?.let { anchors[it] }?.boundsInRoot?.translate(-hostBounds.topLeft)
    }

    fun register(step: TutorialStep, owner: Any, boundsInRoot: Rect) {
        val existing = anchors[step]
        // onGloballyPositioned fires on every layout pass; only write when something actually moved.
        if (existing != null && existing.owner === owner && existing.boundsInRoot == boundsInRoot) return
        anchors[step] = Anchor(owner, boundsInRoot)
    }

    fun unregister(step: TutorialStep, owner: Any) {
        if (anchors[step]?.owner === owner) anchors.remove(step)
    }

    fun markSeen(step: TutorialStep) {
        dismissed += step
        onSeen(step)
    }

    fun skipAll() {
        dismissed = TutorialStep.entries.toSet()
        onSkipAll()
    }
}
