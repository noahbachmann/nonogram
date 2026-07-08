package com.trainpaths.nonogram.classes

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Size of a tile in content space. Zoom is a transform, not a resize. */
val CELL = 48.dp

/**
 * Extent of one clue cell along the gutter's own axis — the width of a row-clue cell, the height of
 * a col-clue cell. Much thinner than [CELL]: a row of width W can hold up to `ceil(W / 2)` clues, so
 * at 48.dp the gutter would grow to half the grid and squeeze the board off screen.
 */
val CLUE_CELL = 20.dp

/** Black grid line between tiles. */
val TILE_BORDER = 1.dp

/** Breathing room around a clue number inside its cell. */
val CLUE_PADDING = 1.dp

/** Divider between the clue gutters and the playing field. Screen-space, so zoom never hides it. */
val BOARD_SEPARATOR = 4.dp

/** Zoom ceiling, as a multiple of the larger of [BoardTransformState.fitScale] and 1x. */
private const val MAX_ZOOM_MULTIPLE = 3f

/** Zoom factor applied per press of the +/- buttons. */
const val ZOOM_STEP = 1.5f

/** Multiplicative zoom per scroll-wheel notch. */
const val SCROLL_ZOOM_PER_NOTCH = 1.15f

/** Material compact -> medium breakpoint. Below this the zoom buttons would cover the board. */
val ZOOM_CONTROLS_MIN_WIDTH = 600.dp

data class TileCoord(val row: Int, val col: Int)

/**
 * Pan/zoom transform for [Board], mapping a content plane larger than the viewport onto it.
 *
 * Content coordinates have their origin at the top-left of the (empty) corner cell, so the clue
 * gutters occupy `[0, gutterWpx] x [0, gutterHpx]` and the tile grid starts at
 * `(gutterWpx, gutterHpx)`. The map to viewport pixels is `v = p * scale + offset`.
 *
 * Nothing exposed here is read during composition — only inside `graphicsLayer {}` lambdas,
 * `drawBehind`, and gesture coroutines — so writes invalidate layers and draws but never trigger
 * recomposition. That is what keeps pan/zoom smooth on a 2500-tile board.
 */
@Stable
class BoardTransformState {

    var viewportW by mutableFloatStateOf(0f); private set
    var viewportH by mutableFloatStateOf(0f); private set
    var cellPx by mutableFloatStateOf(0f); private set
    var gutterWpx by mutableFloatStateOf(0f); private set
    var gutterHpx by mutableFloatStateOf(0f); private set
    var gridWpx by mutableFloatStateOf(0f); private set
    var gridHpx by mutableFloatStateOf(0f); private set

    private var rows = 0
    private var cols = 0
    private var initialised = false

    /** Set once the user zooms or pans by hand; until then the board re-fits as the gutters grow. */
    private var userAdjusted = false

    val contentW: Float get() = gutterWpx + gridWpx
    val contentH: Float get() = gutterHpx + gridHpx

    var scale by mutableFloatStateOf(1f); private set
    var offsetX by mutableFloatStateOf(0f); private set
    var offsetY by mutableFloatStateOf(0f); private set

    /** Scale at which the whole board — grid plus both gutters — is exactly inscribed. */
    val fitScale: Float
        get() = if (contentW <= 0f || contentH <= 0f || viewportW <= 0f || viewportH <= 0f) 1f
        else min(viewportW / contentW, viewportH / contentH)

    val minScale: Float get() = fitScale
    val maxScale: Float get() = max(fitScale, 1f) * MAX_ZOOM_MULTIPLE

    /** Viewport placement of the tile grid. */
    val gridTx: Float get() = gutterWpx * scale + offsetX
    val gridTy: Float get() = gutterHpx * scale + offsetY

    /**
     * Viewport placement of the pinned gutters. `max(0f, ...)` is sticky-header behaviour: the
     * gutter sits at its natural position while its axis is fully visible (otherwise a gap would
     * open between the gutter and the grid it labels), and hard-pins to the edge the moment the
     * content starts to leave the viewport.
     */
    val rowGutterTx: Float get() = max(0f, offsetX)
    val colHeaderTy: Float get() = max(0f, offsetY)

    /**
     * Pushes geometry in from composition. Idempotent; nothing written here is read during
     * composition, so this is safe to call from a composable and — unlike `onSizeChanged` — it
     * produces a correct *first* frame with no fit-scale flash.
     *
     * The first usable geometry fits and centres the board. Later changes (window resize, or a clue
     * gutter growing as the user draws in GenScreen) re-clamp but deliberately preserve the user's
     * zoom.
     */
    fun updateGeometry(
        viewportW: Float,
        viewportH: Float,
        cellPx: Float,
        cluePx: Float,
        rows: Int,
        cols: Int,
        maxRowClues: Int,
        maxColClues: Int,
    ) {
        val gw = maxRowClues * cluePx
        val gh = maxColClues * cluePx
        val grW = cols * cellPx
        val grH = rows * cellPx
        if (this.viewportW == viewportW && this.viewportH == viewportH &&
            this.gutterWpx == gw && this.gutterHpx == gh &&
            this.gridWpx == grW && this.gridHpx == grH
        ) return

        this.viewportW = viewportW
        this.viewportH = viewportH
        this.cellPx = cellPx
        this.rows = rows
        this.cols = cols
        this.gutterWpx = gw
        this.gutterHpx = gh
        this.gridWpx = grW
        this.gridHpx = grH

        val usable = viewportW > 0f && viewportH > 0f && contentW > 0f && contentH > 0f
        if (!usable) return

        if (!initialised || !userAdjusted) {
            // First layout, or the content grew (a clue gutter widening as the user draws in
            // GenScreen) while the view is still the untouched default. Re-fit rather than let the
            // board silently outgrow the viewport.
            initialised = true
            reset()
        } else {
            apply(scale.coerceIn(minScale, maxScale), offsetX, offsetY)
        }
    }

    /** An axis that fits entirely is force-centred rather than draggable. */
    private fun clampX(x: Float, s: Float): Float {
        val w = contentW * s
        return if (w <= viewportW) (viewportW - w) / 2f else x.coerceIn(viewportW - w, 0f)
    }

    private fun clampY(y: Float, s: Float): Float {
        val h = contentH * s
        return if (h <= viewportH) (viewportH - h) / 2f else y.coerceIn(viewportH - h, 0f)
    }

    private fun apply(s: Float, x: Float, y: Float) {
        scale = s
        offsetX = clampX(x, s)
        offsetY = clampY(y, s)
    }

    /** Fit and centre, and hand control of re-fitting back to [updateGeometry]. */
    fun reset() {
        userAdjusted = false
        apply(fitScale, 0f, 0f)
    }

    /** Zoom about [anchor], keeping the content point under it fixed. */
    fun zoomBy(factor: Float, anchor: Offset) {
        val old = scale
        val new = (old * factor).coerceIn(minScale, maxScale)
        if (new == old) return
        userAdjusted = true
        val k = new / old
        apply(new, anchor.x - (anchor.x - offsetX) * k, anchor.y - (anchor.y - offsetY) * k)
    }

    fun zoomAtCenter(factor: Float) = zoomBy(factor, Offset(viewportW / 2f, viewportH / 2f))

    /** Pan, then zoom about the pinch centroid. */
    fun applyTransformGesture(centroid: Offset, pan: Offset, zoom: Float) {
        val old = scale
        val new = (old * zoom).coerceIn(minScale, maxScale)
        if (new != old || pan != Offset.Zero) userAdjusted = true
        val k = new / old
        val px = offsetX + pan.x
        val py = offsetY + pan.y
        apply(new, centroid.x - (centroid.x - px) * k, centroid.y - (centroid.y - py) * k)
    }

    /** Maps a viewport-local position to a tile, or null for gutters, corner, or empty space. */
    fun hitTest(v: Offset): TileCoord? {
        val s = scale
        if (s <= 0f || cellPx <= 0f || rows == 0 || cols == 0) return null

        // Reject the frozen chrome: the corner, both gutters, and any tile occluded by a pinned
        // gutter. When an axis is fully visible these reduce to "before the grid starts".
        if (v.x < rowGutterTx + gutterWpx * s) return null
        if (v.y < colHeaderTy + gutterHpx * s) return null

        val gx = (v.x - gridTx) / s
        val gy = (v.y - gridTy) / s
        if (gx < 0f || gy < 0f || gx >= gridWpx || gy >= gridHpx) return null

        val col = floor(gx / cellPx).toInt()
        val row = floor(gy / cellPx).toInt()
        if (row !in 0 until rows || col !in 0 until cols) return null
        return TileCoord(row, col)
    }
}

/**
 * Tap detector for the board.
 *
 * Unlike `detectTapGestures`, [onTap] fires immediately on UP rather than being deferred by the
 * ~300ms double-tap timeout — filling a tile must not feel laggy. If a second qualifying tap lands
 * inside the window, [onDoubleTap] fires *instead of* a second [onTap], and the caller is expected
 * to undo the first tap's side effect.
 *
 * Never consumes, so a sibling `detectTransformGestures` is unaffected. Cancels its pending tap
 * whenever that detector consumes — i.e. once the drag passes touch slop — which is what makes
 * "drag pans, tap toggles" work with no explicit threshold of our own.
 */
suspend fun PointerInputScope.detectBoardTaps(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
    val doubleTapMinTime = viewConfiguration.doubleTapMinTimeMillis
    val never = Long.MIN_VALUE / 4
    var lastUpTime = never
    var lastUpPos = Offset.Zero

    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Main)
            var cancelled = false
            var up: PointerInputChange? = null

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.changes.size > 1) cancelled = true // second finger: a pinch, not a tap
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    cancelled = true
                    break
                }
                if (change.isConsumed) cancelled = true // the transform detector took over
                if ((change.position - down.position).getDistance() > slop) cancelled = true
                if (!change.pressed) {
                    up = change
                    break
                }
            }

            val u = up
            if (!cancelled && u != null) {
                val dt = u.uptimeMillis - lastUpTime
                val isDouble = dt >= doubleTapMinTime && dt < doubleTapTimeout &&
                        (u.position - lastUpPos).getDistance() <= slop * 2f
                if (isDouble) {
                    lastUpTime = never
                    onDoubleTap(u.position)
                } else {
                    lastUpTime = u.uptimeMillis
                    lastUpPos = u.position
                    onTap(u.position)
                }
            } else {
                lastUpTime = never
            }
        }
    }
}
