package com.trainpaths.nonogram.classes

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BoardInteractionTest {

    @Test
    fun strokeFromNonePaintsEveryCellFilled() {
        assertStrokeResult(
            startingState = TileState.NONE,
            expectedState = TileState.FILLED,
        )
    }

    @Test
    fun strokeFromFilledPaintsEveryCellCrossed() {
        assertStrokeResult(
            startingState = TileState.FILLED,
            expectedState = TileState.CROSSED,
        )
    }

    @Test
    fun strokeFromCrossedClearsEveryCell() {
        assertStrokeResult(
            startingState = TileState.CROSSED,
            expectedState = TileState.NONE,
        )
    }

    @Test
    fun strokeOnlyProcessesEachCoordinateOnce() {
        val tiles = listOf(listOf(tile(TileState.NONE), tile(TileState.CROSSED)))
        val stroke = assertNotNull(TileStroke.begin(tiles, TileCoord(0, 0)))

        assertTrue(stroke.paint(listOf(TileCoord(0, 0), TileCoord(0, 1))))
        assertFalse(stroke.paint(listOf(TileCoord(0, 1), TileCoord(0, 0))))
        assertEquals(listOf(TileState.FILLED, TileState.FILLED), tiles.single().map { it.state })
    }

    @Test
    fun sparsePointerSegmentIncludesInterveningCells() {
        val state = BoardTransformState().apply {
            updateGeometry(
                viewportW = 50f,
                viewportH = 10f,
                cellPx = 10f,
                cluePx = 0f,
                tileBorderPx = 1f,
                separatorContentPx = 0f,
                rows = 1,
                cols = 5,
                maxRowClues = 0,
                maxColClues = 0,
            )
        }

        val coords = state.hitTestSegment(from = Offset(1f, 5f), to = Offset(49f, 5f))

        assertEquals((0 until 5).map { TileCoord(row = 0, col = it) }, coords)
    }

    private fun assertStrokeResult(startingState: TileState, expectedState: TileState) {
        val tiles = listOf(
            listOf(
                tile(startingState),
                tile(TileState.NONE),
                tile(TileState.FILLED),
                tile(TileState.CROSSED),
            ),
        )
        val stroke = assertNotNull(TileStroke.begin(tiles, TileCoord(0, 0)))

        assertTrue(stroke.paint(tiles.single().indices.map { TileCoord(row = 0, col = it) }))
        assertEquals(List(tiles.single().size) { expectedState }, tiles.single().map { it.state })
    }

    private fun tile(state: TileState) = Tile().apply { this.state = state }
}
