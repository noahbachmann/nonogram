package com.trainpaths.nonogram.classes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.icons.refresh
import kotlin.math.pow

/**
 * The nonogram board: a viewport onto a content plane of 48.dp cells, pannable and zoomable.
 *
 * Opens fit-and-centred, so a 5x5 lands zoomed in and a 50x50 lands zoomed out. Tap cycles a tile,
 * drag pans, pinch or scroll-wheel zooms, double-tap resets. The clue gutters pin to the left and
 * top edges so the row/column being solved is always labelled.
 */
@Composable
fun Board(
    nonogram: Nonogram,
    tiles: List<List<Tile>>,
    modifier: Modifier = Modifier,
    onTileClick: () -> Unit = {},
) {
    // Clues key on the nonogram *object*: GenViewModel builds a new Nonogram on every tap, and the
    // clues must recompute so they update live as the user draws.
    val rowClues = remember(nonogram) { nonogram.rowClues }
    val colClues = remember(nonogram) { nonogram.colClues }
    // maxOfOrNull: a zero-size nonogram yields no clue lines at all, and maxOf would throw.
    val maxRowClues = remember(rowClues) { rowClues.maxOfOrNull { it.size } ?: 1 }
    val maxColClues = remember(colClues) { colClues.maxOfOrNull { it.size } ?: 1 }

    // The transform keys on *dimensions*: in GenScreen the nonogram identity changes on every tap
    // while the size does not, and re-fitting the view mid-drawing would snap the board around.
    val state = remember(nonogram.width, nonogram.height) { BoardTransformState() }

    val currentTiles = rememberUpdatedState(tiles)
    val currentOnTileClick = rememberUpdatedState(onTileClick)

    // Gutters are sized by the thin CLUE_CELL, not CELL: a 50-wide row can hold 25 clues.
    val gutterW = CLUE_CELL * maxRowClues
    val gutterH = CLUE_CELL * maxColClues
    val gridW = CELL * nonogram.width
    val gridH = CELL * nonogram.height
    val background = MaterialTheme.colorScheme.background
    val separatorColor = MaterialTheme.colorScheme.secondary

    // safeContentPadding sits outside the clip, so the safe area is the viewport and the clip rect.
    BoxWithConstraints(modifier.safeContentPadding()) {
        val density = LocalDensity.current
        val cellPx = with(density) { CELL.toPx() }
        val cluePx = with(density) { CLUE_CELL.toPx() }
        val borderPx = with(density) { TILE_BORDER.toPx() }
        val separatorPx = with(density) { BOARD_SEPARATOR.toPx() }

        state.updateGeometry(
            viewportW = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f,
            viewportH = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f,
            cellPx = cellPx,
            cluePx = cluePx,
            rows = nonogram.height,
            cols = nonogram.width,
            maxRowClues = maxRowClues,
            maxColClues = maxColClues,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                // Order matters. Compose dispatches Main innermost-outward, and the last modifier
                // is the innermost, so the transform detector sees each event first. Once it passes
                // touch slop it consumes, and the tap detector cancels — that *is* the tap-vs-drag
                // threshold.
                .pointerInput(state) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            // Browsers report line/page deltas; clamp so one flick isn't a 100x zoom.
                            val notches = change.scrollDelta.y.coerceIn(-3f, 3f)
                            if (notches == 0f) continue
                            state.zoomBy(SCROLL_ZOOM_PER_NOTCH.pow(-notches), change.position)
                            change.consume()
                        }
                    }
                }
                .pointerInput(state) {
                    // onTap fires immediately, so a double-tap would have already advanced the tile
                    // once. Remember enough to put it back.
                    var lastTile: Tile? = null
                    var lastTileState: TileState? = null
                    detectBoardTaps(
                        onTap = { position ->
                            val hit = state.hitTest(position)
                            if (hit == null) {
                                lastTile = null
                            } else {
                                val tile = currentTiles.value[hit.row][hit.col]
                                lastTile = tile
                                lastTileState = tile.state
                                tile.click()
                                currentOnTileClick.value()
                            }
                        },
                        onDoubleTap = {
                            val tile = lastTile
                            val previous = lastTileState
                            if (tile != null && previous != null) {
                                tile.state = previous
                                currentOnTileClick.value()
                            }
                            lastTile = null
                            state.reset()
                        },
                    )
                }
                .pointerInput(state) {
                    detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                        state.applyTransformGesture(centroid, pan, zoom)
                    }
                },
            contentAlignment = Alignment.TopStart,
        ) {
            // Each region is measured at its unscaled size (requiredSize beats the viewport
            // constraints), placed at (0,0), and moved into place by its own layer matrix. Reading
            // the transform inside the graphicsLayer lambda scopes the snapshot read to the layer,
            // so panning invalidates the layer only — no recomposition, no relayout. Do not inline
            // these into the value form of graphicsLayer.
            Canvas(
                modifier = Modifier
                    .oversized(gridW, gridH)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = state.scale
                        scaleY = state.scale
                        translationX = state.gridTx
                        translationY = state.gridTy
                    },
            ) {
                drawTiles(currentTiles.value, cellPx, borderPx)
            }

            Column(
                modifier = Modifier
                    .oversized(gutterW, gridH)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = state.scale
                        scaleY = state.scale
                        translationX = state.rowGutterTx
                        translationY = state.gridTy
                    },
            ) {
                for (row in 0 until nonogram.height) {
                    // One tile tall, so clue lines stay flush with the rows they label.
                    RowClueLine(clues = rowClues[row], slots = maxRowClues, gutterW = gutterW)
                }
            }

            Row(
                modifier = Modifier
                    .oversized(gridW, gutterH)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = state.scale
                        scaleY = state.scale
                        translationX = state.gridTx
                        translationY = state.colHeaderTy
                    },
            ) {
                for (column in 0 until nonogram.width) {
                    // One tile wide, so clue lines stay flush with the columns they label.
                    ColClueLine(clues = colClues[column], slots = maxColClues, gutterH = gutterH)
                }
            }

            // Drawn last, over everything. Two jobs:
            //  1. Mask the corner. With both axes panned the row gutter slides under the column
            //     header and would paint row clues into the corner, and vice versa.
            //  2. Draw the separators between the gutters and the playing field. Their thickness is
            //     in viewport px, not content px, so zooming out never thins them away.
            // Reads the transform in the draw phase, and contributes no pointer node.
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val s = state.scale
                        val gridLeft = state.rowGutterTx + state.gutterWpx * s
                        val gridTop = state.colHeaderTy + state.gutterHpx * s
                        if (gridLeft > 0f && gridTop > 0f) {
                            drawRect(background, Offset.Zero, Size(gridLeft, gridTop))
                        }

                        val gridRight = (state.gridTx + state.gridWpx * s).coerceAtMost(size.width)
                        val gridBottom = (state.gridTy + state.gridHpx * s).coerceAtMost(size.height)

                        if (gridBottom > gridTop) {
                            drawRect(
                                color = separatorColor,
                                topLeft = Offset(gridLeft - separatorPx, gridTop - separatorPx),
                                size = Size(separatorPx, gridBottom - gridTop + separatorPx),
                            )
                        }
                        if (gridRight > gridLeft) {
                            drawRect(
                                color = separatorColor,
                                topLeft = Offset(gridLeft - separatorPx, gridTop - separatorPx),
                                size = Size(gridRight - gridLeft + separatorPx, separatorPx),
                            )
                        }
                    },
            )
        }

        // A sibling of the gesture Box, not a child: Compose commits to the first hit path among
        // overlapping siblings, so pressing a button never also starts a pan.
        if (maxWidth >= ZOOM_CONTROLS_MIN_WIDTH) {
            ZoomControls(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                onZoomIn = { state.zoomAtCenter(ZOOM_STEP) },
                onZoomOut = { state.zoomAtCenter(1f / ZOOM_STEP) },
                onFit = { state.reset() },
            )
        }
    }
}

/**
 * Lays the content out at its true [width] x [height] while reporting a size that fits the incoming
 * constraints.
 *
 * `Box` sizes itself to `max(constraints.minWidth, largestChild.width)` and never coerces back down
 * to `maxWidth`, so a bare `requiredSize` child would drag the parent out to the full content size —
 * defeating `clipToBounds`, displacing the parent under any centring alignment, and shifting the
 * pointer-input coordinate space out from under `hitTest`.
 */
private fun Modifier.oversized(width: Dp, height: Dp): Modifier =
    this.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, height)

/**
 * One clue line — a single white block spanning the whole gutter, with the numbers spaced across it.
 * The seam belongs between *lines*, not between individual numbers.
 *
 * Numbers occupy equal weighted slots so they stay column-aligned across lines; short lines are
 * padded with empty slots at the start, which right-aligns row clues against the grid.
 */
@Composable
private fun RowClueLine(clues: List<Int>, slots: Int, gutterW: Dp) {
    Row(
        modifier = Modifier
            .requiredSize(gutterW, CELL)
            .padding(CLUE_PADDING)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(slots - clues.size) { Spacer(Modifier.weight(1f)) }
        for (clue in clues) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                ClueText(clue)
            }
        }
    }
}

@Composable
private fun ColClueLine(clues: List<Int>, slots: Int, gutterH: Dp) {
    Column(
        modifier = Modifier
            .requiredSize(CELL, gutterH)
            .padding(CLUE_PADDING)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(slots - clues.size) { Spacer(Modifier.weight(1f)) }
        for (clue in clues) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ClueText(clue)
            }
        }
    }
}

@Composable
private fun ClueText(value: Int) {
    Text(
        text = value.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Black,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/**
 * Draws the whole grid into one node.
 *
 * Reading `tile.state` here registers a *draw-scope* dependency, so filling a tile invalidates the
 * draw of this single node — no recomposition, no relayout. That is far cheaper than the 2500
 * layout nodes a per-tile Box grid would need merely to exist.
 */
private fun DrawScope.drawTiles(tiles: List<List<Tile>>, cellPx: Float, borderPx: Float) {
    val rows = tiles.size
    val cols = tiles.firstOrNull()?.size ?: 0
    val inner = cellPx - 2f * borderPx
    val innerSize = Size(inner, inner)
    val crossInset = inner * 0.2f
    val crossStroke = inner / 12f

    // One black field behind the tiles: every inset tile leaves a black gridline around itself, and
    // filled tiles merge into it.
    drawRect(Color.Black, Offset.Zero, Size(cols * cellPx, rows * cellPx))

    for (row in tiles.indices) {
        val top = row * cellPx + borderPx
        for (column in tiles[row].indices) {
            val left = column * cellPx + borderPx
            val state = tiles[row][column].state
            if (state == TileState.FILLED) continue // already black
            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = innerSize,
            )
            if (state == TileState.CROSSED) {
                val near = crossInset
                val far = inner - crossInset
                drawLine(
                    color = Color.Black,
                    start = Offset(left + near, top + near),
                    end = Offset(left + far, top + far),
                    strokeWidth = crossStroke,
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(left + far, top + near),
                    end = Offset(left + near, top + far),
                    strokeWidth = crossStroke,
                )
            }
        }
    }
}

@Composable
private fun ZoomControls(
    modifier: Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
) {
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.primary,
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledIconButton(onClick = onZoomIn, colors = colors) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
        }
        FilledIconButton(onClick = onZoomOut, colors = colors) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        FilledIconButton(onClick = onFit, colors = colors) {
            Icon(imageVector = refresh, contentDescription = "Fit board to screen")
        }
    }
}
