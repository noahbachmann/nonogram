package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilterSortStateTest {

    private fun nonogram(id: Long, difficulty: Difficulty) =
        Nonogram(id = id, difficulty = difficulty, solution = listOf(listOf(1)))

    private val easy = nonogram(1, Difficulty.EASY)
    private val hard = nonogram(2, Difficulty.HARD)
    private val medium = nonogram(3, Difficulty.MEDIUM)
    private val easyToo = nonogram(4, Difficulty.EASY)
    private val all = listOf(easy, hard, medium, easyToo)

    private val attributes = NonogramFilters.ALL
    private val difficultyId = NonogramFilters.DIFFICULTY.id

    @Test
    fun cycleSort_runsThroughDescAscAndBackToNone() {
        val desc = FilterSortState().cycleSort(difficultyId)
        assertEquals(difficultyId, desc.sortAttributeId)
        assertEquals(SortDirection.DESC, desc.sortDirection)

        val asc = desc.cycleSort(difficultyId)
        assertEquals(difficultyId, asc.sortAttributeId)
        assertEquals(SortDirection.ASC, asc.sortDirection)

        val cleared = asc.cycleSort(difficultyId)
        assertNull(cleared.sortAttributeId)
        assertNull(cleared.sortDirection)
    }

    @Test
    fun cycleSort_onAnotherAttribute_takesOverTheSort() {
        val state = FilterSortState().cycleSort(difficultyId).cycleSort("size")

        assertEquals("size", state.sortAttributeId)
        assertEquals(SortDirection.DESC, state.sortDirection)
    }

    @Test
    fun defaultState_keepsEveryPuzzleInOriginalOrder() {
        assertEquals(all, FilterSortState().applyTo(all, attributes))
    }

    @Test
    fun uncheckingOneDifficulty_removesOnlyThosePuzzles() {
        val state = FilterSortState().toggle(Difficulty.EASY.name)

        assertTrue(!state.isChecked(Difficulty.EASY.name))
        assertEquals(listOf(hard, medium), state.applyTo(all, attributes))
    }

    @Test
    fun uncheckingEveryDifficulty_yieldsNothing() {
        val state = Difficulty.entries.fold(FilterSortState()) { acc, d -> acc.toggle(d.name) }

        assertEquals(emptyList(), state.applyTo(all, attributes))
    }

    @Test
    fun toggle_isReversible() {
        val state = FilterSortState().toggle(Difficulty.HARD.name).toggle(Difficulty.HARD.name)

        assertEquals(FilterSortState(), state)
        assertEquals(all, state.applyTo(all, attributes))
    }

    @Test
    fun ascendingSort_ordersEasyFirst_andKeepsTiesStable() {
        val state = FilterSortState(difficultyId, SortDirection.ASC)

        assertEquals(listOf(easy, easyToo, medium, hard), state.applyTo(all, attributes))
    }

    @Test
    fun descendingSort_isTheReverseOrderOfDifficulty() {
        val state = FilterSortState(difficultyId, SortDirection.DESC)

        assertEquals(listOf(hard, medium, easy, easyToo), state.applyTo(all, attributes))
    }

    @Test
    fun sortAndFilter_composeInOnePass() {
        val state = FilterSortState(difficultyId, SortDirection.ASC).toggle(Difficulty.MEDIUM.name)

        assertEquals(listOf(easy, easyToo, hard), state.applyTo(all, attributes))
    }

    @Test
    fun unknownSortAttribute_leavesOrderUntouched() {
        val state = FilterSortState("nope", SortDirection.ASC)

        assertEquals(all, state.applyTo(all, attributes))
    }
}
