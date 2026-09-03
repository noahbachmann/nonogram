package com.trainpaths.nonogram.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.BUTTON_SHAPE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalTutorialController = staticCompositionLocalOf<TutorialController> {
    error("No TutorialController provided")
}

private val SPOT_PADDING = 6.dp
private val SPOT_CORNER = 10.dp
private val BLOB_MARGIN = 16.dp
private val BLOB_GAP = 12.dp
private val BLOB_MAX_WIDTH = 340.dp

/**
 * Hosts [content] plus the tutorial spotlight overlay drawn above it. The scrim blocks everything
 * except the highlighted target, which stays fully interactive.
 */
@Composable
fun TutorialHost(
    tutorialRepository: TutorialRepository,
    paused: Boolean,
    content: @Composable () -> Unit,
) {
    val seenSteps by tutorialRepository.seenSteps.collectAsState()
    val controller = remember { TutorialController() }

    controller.syncPersistedSeen(seenSteps)
    controller.paused = paused

    SideEffect {
        controller.onSeen = tutorialRepository::markSeen
        controller.onSkipAll = tutorialRepository::markAllSeen
    }

    CompositionLocalProvider(LocalTutorialController provides controller) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { controller.hostBounds = it.boundsInRoot() },
        ) {
            content()

            val step = controller.currentStep
            val bounds = controller.currentBounds
            if (step != null && bounds != null) {
                TutorialOverlay(
                    step = step,
                    target = bounds,
                    onDismiss = { controller.markSeen(step) },
                    onSkipAll = controller::skipAll,
                )
            }
        }
    }
}

@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    target: Rect,
    onDismiss: () -> Unit,
    onSkipAll: () -> Unit,
) {
    val density = LocalDensity.current
    val padding = with(density) { SPOT_PADDING.toPx() }
    val corner = with(density) { SPOT_CORNER.toPx() }
    val scrimColor = Color.Black.copy(alpha = 0.55f)
    val ringColor = MaterialTheme.colorScheme.onPrimary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val spot = Rect(
            left = max(0f, target.left - padding),
            top = max(0f, target.top - padding),
            right = min(width, target.right + padding),
            bottom = min(height, target.bottom + padding),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val hole = RoundRect(spot, CornerRadius(corner, corner))
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(hole)
            }
            drawPath(path = path, color = scrimColor)
            drawRoundRect(
                color = ringColor,
                topLeft = spot.topLeft,
                size = spot.size,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // Everything but the spotlight is inert: a tap on the scrim acknowledges the hint. Nothing
        // is placed over the spotlight itself, so the real control keeps receiving its own gestures.
        Blocker(step, 0f, 0f, width, spot.top, onDismiss)
        Blocker(step, 0f, spot.bottom, width, height - spot.bottom, onDismiss)
        Blocker(step, 0f, spot.top, spot.left, spot.height, onDismiss)
        Blocker(step, spot.right, spot.top, width - spot.right, spot.height, onDismiss)

        BlobPlacement(spot = spot) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 14.dp,
                        end = 12.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = step.text,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onSkipAll) {
                            Text("Skip all tips", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = onDismiss,
                            shape = BUTTON_SHAPE,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text("Got it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Blocker(
    step: TutorialStep,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    onTap: () -> Unit,
) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(with(density) { width.toDp() }, with(density) { height.toDp() })
            .pointerInput(step) { detectTapGestures { onTap() } }
    )
}

/** Places the blob just outside the spotlight, on whichever side has room. */
@Composable
private fun BlobPlacement(spot: Rect, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = BLOB_MARGIN.toPx()
        val gap = BLOB_GAP.toPx()

        val maxBlobWidth = min(BLOB_MAX_WIDTH.roundToPx(), (width - 2 * margin).roundToInt())
        val blob = measurables.first().measure(
            Constraints(maxWidth = maxBlobWidth.coerceAtLeast(0), maxHeight = height)
        )

        val fitsBelow = spot.bottom + gap + blob.height + margin <= height
        val fitsAbove = spot.top - gap - blob.height - margin >= 0f
        val preferBelow = spot.center.y < height / 2f
        val y = when {
            preferBelow && fitsBelow -> spot.bottom + gap
            !preferBelow && fitsAbove -> spot.top - gap - blob.height
            fitsBelow -> spot.bottom + gap
            fitsAbove -> spot.top - gap - blob.height
            else -> height - blob.height - margin
        }
        val maxX = (width - blob.width - margin).coerceAtLeast(margin)
        val x = (spot.center.x - blob.width / 2f).coerceIn(min(margin, maxX), maxX)

        layout(width, height) { blob.place(x.roundToInt(), y.roundToInt()) }
    }
}
