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

/**
 * Nominal width of the thinnest line on the board — the grid line between two tiles.
 *
 * Every other line is a multiple of it, so the hierarchy hairline < block line < separator is stated
 * once and holds at every zoom. See [lineUnitPx].
 */
val TILE_BORDER = 1.dp

/** Rows and columns per block: the interval at which a heavy grid line is drawn. */
const val BLOCK_SIZE = 5

/** Weight of the heavy grid line drawn every [BLOCK_SIZE] rows and columns, in [lineUnitPx] units. */
const val BLOCK_LINE_UNITS = 2f

/** Weight of the divider between the clue gutters and the playing field, in [lineUnitPx] units. */
const val SEPARATOR_UNITS = 4f

/** No line ever renders thinner than this, or it antialiases into nothing. */
const val LINE_MIN_DEVICE_PX = 1f

/** Breathing room around a clue number inside its cell. */
val CLUE_PADDING = 1.dp

/** Content-space width the divider reserves between the clue gutters and the playing field. */
val BOARD_SEPARATOR = TILE_BORDER * SEPARATOR_UNITS

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

/**
 * Width of one line unit, in *content* px, at a given layer [scale].
 *
 * A line width that is constant is wrong at some zoom, and wrong in opposite directions depending on
 * the space the constant lives in. Held constant in screen px it is a slab next to a zoomed-out tile
 * and a hairline next to a zoomed-in one. Held constant in content px it scales correctly but falls
 * below one device pixel once `scale < 1 / tileBorderPx` and antialiases away — unevenly, since
 * whether a line lands on a pixel boundary depends on its index.
 *
 * So the width is a function of scale with a floor: proportional above the crossover, exactly
 * [LINE_MIN_DEVICE_PX] below it. A stroke of `w` content px renders at `w * scale` device px, which
 * makes `LINE_MIN_DEVICE_PX / scale` precisely the content width of one device pixel.
 */
fun lineUnitPx(scale: Float, tileBorderPx: Float): Float =
    max(tileBorderPx, LINE_MIN_DEVICE_PX / scale)

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

    /** Nominal hairline width. Sets the crossover below which lines stop scaling. See [lineUnitPx]. */
    var tileBorderPx by mutableFloatStateOf(0f); private set

    /** Width the gutter/grid divider reserves in the content plane, so it scales with everything else. */
    var separatorContentPx by mutableFloatStateOf(0f); private set

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

    val contentW: Float get() = visibleGutterWpx + separatorContentPx + gridWpx
    val contentH: Float get() = visibleGutterHpx + separatorContentPx + gridHpx

    /**
     * On-screen width of the divider. Scales with the board like every other line, but never thinner
     * than [SEPARATOR_UNITS] device px — the same floor the grid lines get, so the 1:2:4 hierarchy
     * survives all the way down to fit on a 50x50.
     */
    val separatorScreenPx: Float
        get() = SEPARATOR_UNITS * max(tileBorderPx * scale, LINE_MIN_DEVICE_PX)

    /**
     * On-screen extent of each gutter, *including* the separator that terminates it.
     *
     * Two bounds. It is never wider than the gutter itself, and never narrower than the distance to
     * the grid's actual left edge — the latter is what stops the `GUTTER_MAX_FRACTION` clamp from
     * opening a strip of background between the clues and the grid they label. While the gutter is
     * unpinned, `gridTx - rowGutterTx` *is* the gutter's own width, so the clamp cannot bite. It
     * takes over only once the board is panned far enough that the grid slides underneath, and there
     * narrowing the window reveals grid rather than a gap. The two branches meet continuously.
     */
    val rowGutterWindowW: Float
        get() = min(
            (visibleGutterWpx + separatorContentPx) * scale,
            max(viewportW * GUTTER_MAX_FRACTION, gridTx - rowGutterTx),
        )
    val colHeaderWindowH: Float
        get() = min(
            (visibleGutterHpx + separatorContentPx) * scale,
            max(viewportH * GUTTER_MAX_FRACTION, gridTy - colHeaderTy),
        )

    /**
     * The part of the window the clues may actually paint into: the gutter window less the divider.
     * The divider is *reserved*, not overlaid — at fit on a large board the whole gutter is only a few
     * pixels wide, and a divider drawn on top of it would erase the clues entirely. Below the
     * crossover the drawn width outruns the reserved `separatorContentPx * scale` by at most
     * [SEPARATOR_UNITS] px, which this clip absorbs.
     */
    val rowClueWindowW: Float get() = max(0f, rowGutterWindowW - separatorScreenPx)
    val colClueWindowH: Float get() = max(0f, colHeaderWindowH - separatorScreenPx)

    /** Most-negative scroll: the gutter's far end (the clues nearest the grid) flush with the window. */
    private val minClueScrollX: Float get() = min(0f, rowClueWindowW - gutterWpx * scale)
    private val minClueScrollY: Float get() = min(0f, colClueWindowH - gutterHpx * scale)

    /** Scale at which the whole board — grid, both gutters, and the separator — is exactly inscribed. */
    val fitScale: Float
        get() = if (contentW <= 0f || contentH <= 0f || viewportW <= 0f || viewportH <= 0f) 1f
        else min(viewportW / contentW, viewportH / contentH)

    val minScale: Float get() = fitScale
    val maxScale: Float get() = max(fitScale, 1f) * MAX_ZOOM_MULTIPLE

    /** Viewport placement of the tile grid: after the *capped* gutter and its separator. */
    val gridTx: Float get() = (visibleGutterWpx + separatorContentPx) * scale + offsetX
    val gridTy: Float get() = (visibleGutterHpx + separatorContentPx) * scale + offsetY

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
        tileBorderPx: Float,
        separatorContentPx: Float,
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
            this.tileBorderPx == tileBorderPx && this.separatorContentPx == separatorContentPx
        ) return

        this.viewportW = viewportW
        this.viewportH = viewportH
        this.cellPx = cellPx
        this.tileBorderPx = tileBorderPx
        this.separatorContentPx = separatorContentPx
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
