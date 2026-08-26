package com.trainpaths.nonogram.tutorial

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Marks this composable as the target of [step]: reports its bounds to the [TutorialHost] so the
 * spotlight can find it, and — while its hint is showing — treats a press on it as acknowledgement.
 * A null [step] is a no-op, so call sites can decide per-usage whether a shared widget is a target.
 */
fun Modifier.tutorialAnchor(step: TutorialStep?): Modifier = if (step == null) this else composed {
    val controller = LocalTutorialController.current
    val owner = remember { Any() }

    DisposableEffect(controller, step, owner) {
        onDispose { controller.unregister(step, owner) }
    }

    val isCurrent = controller.currentStep == step
    Modifier
        .onGloballyPositioned { controller.register(step, owner, it.boundsInRoot()) }
        .then(
            if (!isCurrent) {
                Modifier
            } else {
                Modifier.pointerInput(step) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        controller.markSeen(step)
                    }
                }
            }
        )
}
