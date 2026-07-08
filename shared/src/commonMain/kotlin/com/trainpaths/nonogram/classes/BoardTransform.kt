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
val CELL = 40.dp

/**
 * Extent of one clue cell along the gutter's own axis — the width of a row-clue cell, the height of
 * a col-clue cell. Much thinner than [CELL]: a row of width W can hold up to `ceil(W / 2)` clues, so
 * at 48.dp the gutter would grow to half the grid and squeeze the board off-screen.
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

/** A clue gutter may never occupy more than this fraction of the viewport along its own axis. */
private const val GUTTER_MAX_FRACTION = 0.4f

data class TileCoord(val row: Int, val col: Int)

/** Which part of the board a viewport position falls in. Drives gesture routing. */
enum class BoardRegion { GRID, ROW_GUTTER, COL_HEADER, CORNER }

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

    /** Thickness of the gutter/grid divider. Screen-space, so zooming out never thins it away. */
    var separatorPx by mutableFloatStateOf(0f); private set

    private var rows = 0
    private var cols = 0
    private var initialised = false

    /** Set once the user zooms or pans by hand; until then the board re-fits as the gutters grow. */
    private var userAdjusted = false

    /** Region the in-flight drag started in, so crossing a boundary mid-drag doesn't switch targets. */
    private var activeRegion: BoardRegion? = null

    var scale by mutableFloatStateOf(1f); private set
    var offsetX by mutableFloatStateOf(0f); private set
    var offsetY by mutableFloatStateOf(0f); private set

    /** Scroll of each gutter within its own window, in viewport px. Always <= 0. */
    var clueScrollX by mutableFloatStateOf(0f); private set
    var clueScrollY by mutableFloatStateOf(0f); private set

    /**
     * Content-space extent the gutters reserve, capped at the grid's own extent.
     *
     * A clue-dense board wants `gutterWpx` up to `ceil(width / 2) * CLUE_CELL`, which would shove the
     * grid off-screen. Capping at `gridWpx` bounds the gutter to half the content plane, and since
     * the content maps into the viewport at [fitScale], that is at most half the viewport — the
     * requirement — whenever the board is not panned. Clues beyond the cap are reached by scrolling
     * the gutter ([clueScrollX]).
     */
    val visibleGutterWpx: Float get() = min(gutterWpx, gridWpx)
    val visibleGutterHpx: Float get() = min(gutterHpx, gridHpx)

    val contentW: Float get() = visibleGutterWpx + gridWpx
    val contentH: Float get() = visibleGutterHpx + gridHpx

    /**
     * On-screen extent of each gutter, *including* the separator that terminates it. The
     * `viewport * GUTTER_MAX_FRACTION` clamp only bites once the gutter is pinned (i.e. the board is
     * panned or zoomed past fit) — and there the grid already extends underneath it, so narrowing the
     * window reveals grid rather than opening a gap.
     */
    val rowGutterWindowW: Float
        get() = min(visibleGutterWpx * scale + separatorPx, viewportW * GUTTER_MAX_FRACTION)
    val colHeaderWindowH: Float
        get() = min(visibleGutterHpx * scale + separatorPx, viewportH * GUTTER_MAX_FRACTION)

    /**
     * The part of the window the clues may actually paint into: the gutter window less the separator.
     * The separator is *reserved*, not overlaid — at fit on a large board the whole gutter is only a
     * few pixels wide, and a screen-space divider drawn on top of it would erase the clues entirely.
     */
    val rowClueWindowW: Float get() = max(0f, rowGutterWindowW - separatorPx)
    val colClueWindowH: Float get() = max(0f, colHeaderWindowH - separatorPx)

    /** Most-negative scroll: the gutter's far end (the clues nearest the grid) flush with the window. */
    private val minClueScrollX: Float get() = min(0f, rowClueWindowW - gutterWpx * scale)
    private val minClueScrollY: Float get() = min(0f, colClueWindowH - gutterHpx * scale)

    /** Viewport left over for the content plane once the separator has taken its screen-space bite. */
    private val usableW: Float get() = max(0f, viewportW - separatorPx)
    private val usableH: Float get() = max(0f, viewportH - separatorPx)

    /** Scale at which the whole board — grid, both gutters, and the separator — is exactly inscribed. */
    val fitScale: Float
        get() = if (contentW <= 0f || contentH <= 0f || usableW <= 0f || usableH <= 0f) 1f
        else min(usableW / contentW, usableH / contentH)

    val minScale: Float get() = fitScale
    val maxScale: Float get() = max(fitScale, 1f) * MAX_ZOOM_MULTIPLE

    /** Viewport placement of the tile grid: after the *capped* gutter and its separator. */
    val gridTx: Float get() = visibleGutterWpx * scale + separatorPx + offsetX
    val gridTy: Float get() = visibleGutterHpx * scale + separatorPx + offsetY

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
        separatorPx: Float,
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
            this.gridWpx == grW && this.gridHpx == grH &&
            this.separatorPx == separatorPx
        ) return

        this.viewportW = viewportW
        this.viewportH = viewportH
        this.cellPx = cellPx
        this.separatorPx = separatorPx
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
        val w = contentW * s + separatorPx
        return if (w <= viewportW) (viewportW - w) / 2f else x.coerceIn(viewportW - w, 0f)
    }

    private fun clampY(y: Float, s: Float): Float {
        val h = contentH * s + separatorPx
        return if (h <= viewportH) (viewportH - h) / 2f else y.coerceIn(viewportH - h, 0f)
    }

    private fun apply(s: Float, x: Float, y: Float) {
        scale = s
        offsetX = clampX(x, s)
        offsetY = clampY(y, s)
        // The scroll range depends on scale, so re-clamp whenever the transform moves.
        clueScrollX = clueScrollX.coerceIn(minClueScrollX, 0f)
        clueScrollY = clueScrollY.coerceIn(minClueScrollY, 0f)
    }

    /** Fit and centre, and hand control of re-fitting back to [updateGeometry]. */
    fun reset() {
        userAdjusted = false
        apply(fitScale, 0f, 0f)
        // Show the clues nearest the grid — the ones you read first.
        clueScrollX = minClueScrollX
        clueScrollY = minClueScrollY
    }

    /** Scrolls the row-clue gutter horizontally within its window. */
    fun panClueX(dx: Float) {
        clueScrollX = (clueScrollX + dx).coerceIn(minClueScrollX, 0f)
    }

    /** Scrolls the column-clue header vertically within its window. */
    fun panClueY(dy: Float) {
        clueScrollY = (clueScrollY + dy).coerceIn(minClueScrollY, 0f)
    }

    private fun panBoard(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) return
        userAdjusted = true
        apply(scale, offsetX + dx, offsetY + dy)
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

    /**
     * Pan, then zoom about the pinch centroid.
     *
     * A pure drag starting inside a gutter scrolls *that gutter* along its own axis, and pans the
     * board along the other — the gutter tracks the grid on its cross-axis, so this stays coherent.
     * A pinch always transforms the board, wherever it starts.
     */
    fun applyTransformGesture(centroid: Offset, pan: Offset, zoom: Float) {
        val old = scale
        val new = (old * zoom).coerceIn(minScale, maxScale)

        if (new == old) {
            val region = activeRegion ?: regionAt(centroid).also { activeRegion = it }
            when (region) {
                BoardRegion.ROW_GUTTER -> {
                    panClueX(pan.x)
                    panBoard(0f, pan.y)
                }

                BoardRegion.COL_HEADER -> {
                    panClueY(pan.y)
                    panBoard(pan.x, 0f)
                }

                BoardRegion.CORNER -> {
                    panClueX(pan.x)
                    panClueY(pan.y)
                }

                BoardRegion.GRID -> panBoard(pan.x, pan.y)
            }
            return
        }

        // A pinch always transforms the board, and pins the rest of the gesture to doing so.
        activeRegion = BoardRegion.GRID
        userAdjusted = true
        val k = new / old
        val px = offsetX + pan.x
        val py = offsetY + pan.y
        apply(new, centroid.x - (centroid.x - px) * k, centroid.y - (centroid.y - py) * k)
    }

    /** Called when every pointer has lifted, so the next drag re-picks its region. */
    fun endGesture() {
        activeRegion = null
    }

    /** Classifies a viewport position against the *windowed* gutters. */
    fun regionAt(v: Offset): BoardRegion {
        val inRowGutter = v.x < rowGutterTx + rowGutterWindowW
        val inColHeader = v.y < colHeaderTy + colHeaderWindowH
        return when {
            inRowGutter && inColHeader -> BoardRegion.CORNER
            inRowGutter -> BoardRegion.ROW_GUTTER
            inColHeader -> BoardRegion.COL_HEADER
            else -> BoardRegion.GRID
        }
    }

    /** Maps a viewport position to a tile, or null for gutters, corner, or empty space. */
    fun hitTest(v: Offset): TileCoord? {
        val s = scale
        if (s <= 0f || cellPx <= 0f || rows == 0 || cols == 0) return null

        // Reject the frozen chrome, including any tile occluded by a pinned gutter.
        if (regionAt(v) != BoardRegion.GRID) return null

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
                val isDouble = dt in doubleTapMinTime..<doubleTapTimeout &&
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
