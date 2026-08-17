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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.icons.refresh
import kotlin.math.max
import kotlin.math.pow

@Composable
fun Board(
    nonogram: Nonogram,
    tiles: List<List<Tile>>,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    drawMode: DrawMode = DrawMode.TOGGLE,
    state: BoardTransformState = remember(nonogram.width, nonogram.height) { BoardTransformState() },
    onTilesChanged: () -> Unit = {},
    onEdits: (List<TileEdit>) -> Unit = {},
) {
    // Clues key on the nonogram *object*: GenViewModel builds a new Nonogram on every tap, and the
    // clues must recompute so they update live as the user draws.
    val rowClues = remember(nonogram) { nonogram.rowClues }
    val colClues = remember(nonogram) { nonogram.colClues }
    // maxOfOrNull: a zero-size nonogram yields no clue lines at all, and maxOf would throw.
    val maxRowClues = remember(rowClues) { rowClues.maxOfOrNull { it.size } ?: 1 }
    val maxColClues = remember(colClues) { colClues.maxOfOrNull { it.size } ?: 1 }
    // cacheSize 32: the default of 8 thrashes on a board with more than a handful of block labels.
    val labelMeasurer = rememberTextMeasurer(cacheSize = 32)

    // The transform keys on *dimensions*: in GenScreen the nonogram identity changes on every tap
    // while the size does not, and re-fitting the view mid-drawing would snap the board around. The
    // default is remembered here; GameScreen hoists it so the bottom-bar zoom-out button can reset it.
    val currentTiles = rememberUpdatedState(tiles)
    val currentIsEditable = rememberUpdatedState(isEditable)
    val currentDrawMode = rememberUpdatedState(drawMode)
    val currentOnTilesChanged = rememberUpdatedState(onTilesChanged)
    val currentOnEdits = rememberUpdatedState(onEdits)

    // Gutters are sized by the thin CLUE_CELL, not CELL: a 50-wide row can hold 25 clues.
    val gutterW = CLUE_CELL * maxRowClues
    val gutterH = CLUE_CELL * maxColClues
    val gridW = CELL * nonogram.width
    val gridH = CELL * nonogram.height
    val background = MaterialTheme.colorScheme.background
    val separatorColor = Color.DarkGray

    // safeContentPadding sits outside the clip, so the safe area is the viewport and the clip rect.
    BoxWithConstraints(modifier.safeContentPadding()) {
        val density = LocalDensity.current
        val cellPx = with(density) { CELL.toPx() }
        val cluePx = with(density) { CLUE_CELL.toPx() }
        val borderPx = with(density) { TILE_BORDER.toPx() }
        val separatorContentPx = with(density) { BOARD_SEPARATOR.toPx() }

        state.updateGeometry(
            viewportW = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f,
            viewportH = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f,
            cellPx = cellPx,
            cluePx = cluePx,
            tileBorderPx = borderPx,
            separatorContentPx = separatorContentPx,
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
                    detectBoardTaps(
                        onTap = { position ->
                            if (!currentIsEditable.value) return@detectBoardTaps
                            val hit = state.hitTest(position) ?: return@detectBoardTaps
                            val tile = currentTiles.value[hit.row][hit.col]
                            val before = tile.state
                            tile.click(currentDrawMode.value)
                            if (tile.state != before) {
                                currentOnEdits.value(
                                    listOf(TileEdit(hit.row, hit.col, before = before, after = tile.state)),
                                )
                            }
                            currentOnTilesChanged.value()
                        },
                    )
                }
                // Final pass, after every other node has had its say: once no pointer is down the
                // gesture is over, so the next drag re-picks whether it moves the board or a gutter.
                .pointerInput(state) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.none { it.pressed }) state.endGesture()
                        }
                    }
                }
                .pointerInput(state) {
                    detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                        state.applyTransformGesture(centroid, pan, zoom)
                    }
                }
                // Last means innermost on the Main pass. In locked mode this detector gets first
                // refusal and consumes a committed one-pointer stroke before transform sees it.
                .pointerInput(state, isLocked) {
                    if (isLocked) {
                        detectBoardDrawGestures(
                            state = state,
                            tiles = { currentTiles.value },
                            isEditable = { currentIsEditable.value },
                            drawMode = { currentDrawMode.value },
                            onTilesChanged = { currentOnTilesChanged.value() },
                            onEdits = { currentOnEdits.value(it) },
                        )
                    }
                },
            contentAlignment = Alignment.TopStart,
        ) {
            // Each region is measured at its unscaled size (requiredSize beats the viewport
            // constraints), placed at (0,0), and moved into place by its own layer matrix.
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
                drawTiles(currentTiles.value, cellPx, borderPx, state.scale)
            }

            // Block-index labels, pinned to the visible field edge
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawBlockLabels(state, nonogram.width, nonogram.height, cellPx, labelMeasurer)
                    },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val left = state.rowGutterTx
                        clipRect(left = left, right = left + state.rowClueWindowW) {
                            this@drawWithContent.drawContent()
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .oversized(gutterW, gridH)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = state.scale
                            scaleY = state.scale
                            translationX = state.rowGutterTx + state.clueScrollX
                            translationY = state.gridTy
                        },
                ) {
                    for (row in 0 until nonogram.height) {
                        // One tile tall, so clue lines stay flush with the rows they label.
                        RowClueLine(clues = rowClues[row], slots = maxRowClues, gutterW = gutterW)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val top = state.colHeaderTy
                        clipRect(top = top, bottom = top + state.colClueWindowH) {
                            this@drawWithContent.drawContent()
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .oversized(gridW, gutterH)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = state.scale
                            scaleY = state.scale
                            translationX = state.gridTx
                            translationY = state.colHeaderTy + state.clueScrollY
                        },
                ) {
                    for (column in 0 until nonogram.width) {
                        // One tile wide, so clue lines stay flush with the columns they label.
                        ColClueLine(clues = colClues[column], slots = maxColClues, gutterH = gutterH)
                    }
                }
            }

            // Drawn last, over everything. Two jobs:
            //  1. Mask the corner. With both axes panned the row gutter slides under the column
            //     header and would paint row clues into the corner, and vice versa.
            //  2. Draw the left and top edges of the playing field's frame. These are pinned to the
            //     gutters, so they live in viewport px — but their *width* still scales with the
            //     board (state.separatorScreenPx), floored so they never thin away. The transform
            //     reserves this width at the end of each gutter window (see rowClueWindowW), so the
            //     frame abuts the clues instead of painting over them. drawTiles draws the matching
            //     right and bottom edges; drawing all four here would double these two.
            // Reads the transform in the draw phase, and contributes no pointer node.
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val s = state.scale
                        val frame = state.separatorScreenPx
                        val gridLeft = state.rowGutterTx + state.rowGutterWindowW
                        val gridTop = state.colHeaderTy + state.colHeaderWindowH
                        if (gridLeft > 0f && gridTop > 0f) {
                            drawRect(background, Offset.Zero, Size(gridLeft, gridTop))
                        }

                        val gridRight = (state.gridTx + state.gridWpx * s).coerceAtMost(size.width)
                        val gridBottom = (state.gridTy + state.gridHpx * s).coerceAtMost(size.height)

                        if (gridBottom > gridTop) {
                            drawRect(
                                color = separatorColor,
                                topLeft = Offset(gridLeft - frame, gridTop - frame),
                                size = Size(frame, gridBottom - gridTop + frame),
                            )
                        }
                        if (gridRight > gridLeft) {
                            drawRect(
                                color = separatorColor,
                                topLeft = Offset(gridLeft - frame, gridTop - frame),
                                size = Size(gridRight - gridLeft + frame, frame),
                            )
                        }
                    },
            )
        }

        // A sibling of the gesture Box, not a child: Compose commits to the first hit path among
        // overlapping siblings, so pressing a zoom control never starts a pan or drawing stroke.
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
 *
 * [scale] is the layer's scale, and drawing depends on it: this node paints in *content* px and the
 * layer matrix scales the result, so every stroke is a multiple of [lineUnitPx] — proportional to the
 * zoom while it can be, one device pixel once it cannot.
 */
private fun DrawScope.drawTiles(tiles: List<List<Tile>>, cellPx: Float, borderPx: Float, scale: Float) {
    val rows = tiles.size
    val cols = tiles.firstOrNull()?.size ?: 0
    if (rows == 0 || cols == 0 || scale <= 0f) return

    val width = cols * cellPx
    val height = rows * cellPx
    val unit = lineUnitPx(scale, borderPx)
    val thin = unit
    val thick = unit * BLOCK_LINE_UNITS
    val frame = unit * SEPARATOR_UNITS
    val crossInset = cellPx * 0.22f
    val crossStroke = max(cellPx / 14f, LINE_MIN_DEVICE_PX / scale)

    drawRect(Color.White, Offset.Zero, Size(width, height))

    for (row in tiles.indices) {
        val top = row * cellPx
        for (column in tiles[row].indices) {
            val left = column * cellPx
            when (tiles[row][column].state) {
                TileState.FILLED -> drawRect(Color.Black, Offset(left, top), Size(cellPx, cellPx))

                TileState.CROSSED -> {
                    val near = crossInset
                    val far = cellPx - crossInset
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

                TileState.NONE -> {}
            }
        }
    }

    // Gridlines last, over the tiles. Every fifth is heavy with number
    for (column in 1 until cols) {
        val w = if (column % BLOCK_SIZE == 0) thick else thin
        val x = column * cellPx
        drawLine(Color.Gray, Offset(x, 0f), Offset(x, height), strokeWidth = w)
    }
    for (row in 1 until rows) {
        val w = if (row % BLOCK_SIZE == 0) thick else thin
        val y = row * cellPx
        drawLine(Color.Gray, Offset(0f, y), Offset(width, y), strokeWidth = w)
    }

    // The playing field's right and bottom edges
    drawLine(Color.DarkGray, Offset(width, 0f), Offset(width, height), strokeWidth = frame)
    drawLine(Color.DarkGray, Offset(0f, height), Offset(width, height), strokeWidth = frame)
}

/**
 * Draws the every-[BLOCK_SIZE]th line's index number, pinned to the *visible* right/bottom edge of
 * the field rather than the field's own edge — so the ruler stays legible no matter how far the
 * board is panned, including once the last row/column has scrolled off-screen.
 */
private fun DrawScope.drawBlockLabels(
    state: BoardTransformState,
    cols: Int,
    rows: Int,
    cellPx: Float,
    labelMeasurer: TextMeasurer,
) {
    val s = state.scale
    if (s <= 0f || cols == 0 || rows == 0) return

    val cell = cellPx * s
    val fontPx = cellPx * BLOCK_LABEL_FONT_FRACTION * s
    if (fontPx < BLOCK_LABEL_MIN_DEVICE_PX) return
    val inset = cellPx * BLOCK_LABEL_INSET_FRACTION * s

    val labelStyle = TextStyle(
        color = Color.Gray,
        fontSize = fontPx.toSp(),
        lineHeight = fontPx.toSp(),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

    val anchorX = (state.gridTx + state.gridWpx * s).coerceAtMost(size.width).coerceAtLeast(state.gridTx)
    val anchorY = (state.gridTy + state.gridHpx * s).coerceAtMost(size.height).coerceAtLeast(state.gridTy)

    for (column in 1 until cols) {
        if (column % BLOCK_SIZE != 0) continue
        val x = state.gridTx + column * cell
        if (x < 0f || x > size.width) continue // line itself is off-screen; nothing to label
        val label = labelMeasurer.measure(column.toString(), labelStyle)
        drawText(label, topLeft = Offset(x - inset - label.size.width, anchorY - inset - label.size.height))
    }
    for (row in 1 until rows) {
        if (row % BLOCK_SIZE != 0) continue
        val y = state.gridTy + row * cell
        if (y < 0f || y > size.height) continue
        val label = labelMeasurer.measure(row.toString(), labelStyle)
        drawText(label, topLeft = Offset(anchorX - inset - label.size.width, y - inset - label.size.height))
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
