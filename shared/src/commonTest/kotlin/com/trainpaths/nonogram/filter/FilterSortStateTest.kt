package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OWNER_UID = "uid-7"

class FilterSortStateTest {

    private fun nonogram(id: Long, difficulty: Difficulty, authorUid: String = "") =
        Nonogram(id = id, difficulty = difficulty, solution = listOf(listOf(1)), authorUid = authorUid)

    private val easy = nonogram(1, Difficulty.EASY)
    private val hard = nonogram(2, Difficulty.HARD)
    private val medium = nonogram(3, Difficulty.MEDIUM)
    private val easyToo = nonogram(4, Difficulty.EASY)
    private val all = listOf(easy, hard, medium, easyToo)

    private val myHard = nonogram(5, Difficulty.HARD, OWNER_UID)
    private val myEasy = nonogram(6, Difficulty.EASY, OWNER_UID)
    private val mixed = listOf(easy, myHard, hard, myEasy)

    private val entries = NonogramFilters.forUser(OWNER_UID)
    private val difficulty = NonogramFilters.DIFFICULTY.label
    private val personal = NonogramFilters.PERSONAL

    @Test
    fun cycleSort_runsThroughDescAscAndBackToNone() {
        val desc = FilterSortState().cycleSort(difficulty)
        assertEquals(difficulty, desc.sortAttribute)
        assertEquals(SortDirection.DESC, desc.sortDirection)

        val asc = desc.cycleSort(difficulty)
        assertEquals(difficulty, asc.sortAttribute)
        assertEquals(SortDirection.ASC, asc.sortDirection)

        val cleared = asc.cycleSort(difficulty)
        assertNull(cleared.sortAttribute)
        assertNull(cleared.sortDirection)
    }

    @Test
    fun cycleSort_onAnotherAttribute_takesOverTheSort() {
        val state = FilterSortState().cycleSort(difficulty).cycleSort("Size")

        assertEquals("Size", state.sortAttribute)
        assertEquals(SortDirection.DESC, state.sortDirection)
    }

    @Test
    fun defaultState_keepsEveryPuzzleInOriginalOrder() {
        assertEquals(all, FilterSortState().applyTo(all, entries))
    }

    @Test
    fun uncheckingOneDifficulty_removesOnlyThosePuzzles() {
        val state = FilterSortState().toggle(Difficulty.EASY.label)

        assertTrue(!state.isChecked(Difficulty.EASY.label))
        assertEquals(listOf(hard, medium), state.applyTo(all, entries))
    }

    @Test
    fun uncheckingEveryDifficulty_yieldsNothing() {
        val state = Difficulty.entries.fold(FilterSortState()) { acc, d -> acc.toggle(d.label) }

        assertEquals(emptyList(), state.applyTo(all, entries))
    }

    @Test
    fun toggle_isReversible() {
        val state = FilterSortState().toggle(Difficulty.HARD.label).toggle(Difficulty.HARD.label)

        assertEquals(FilterSortState(), state)
        assertEquals(all, state.applyTo(all, entries))
    }

    @Test
    fun ascendingSort_ordersEasyFirst_andKeepsTiesStable() {
        val state = FilterSortState(difficulty, SortDirection.ASC)

        assertEquals(listOf(easy, easyToo, medium, hard), state.applyTo(all, entries))
    }

    @Test
    fun descendingSort_isTheReverseOrderOfDifficulty() {
        val state = FilterSortState(difficulty, SortDirection.DESC)

        assertEquals(listOf(hard, medium, easy, easyToo), state.applyTo(all, entries))
    }

    @Test
    fun sortAndFilter_composeInOnePass() {
        val state = FilterSortState(difficulty, SortDirection.ASC).toggle(Difficulty.MEDIUM.label)

        assertEquals(listOf(easy, easyToo, hard), state.applyTo(all, entries))
    }

    @Test
    fun unknownSortAttribute_leavesOrderUntouched() {
        val state = FilterSortState("nope", SortDirection.ASC)

        assertEquals(all, state.applyTo(all, entries))
    }

    @Test
    fun personalChecked_byDefault_keepsYourOwnPuzzles() {
        assertEquals(mixed, FilterSortState().applyTo(mixed, entries))
    }

    @Test
    fun uncheckingPersonal_removesOnlyYourOwnPuzzles() {
        val state = FilterSortState().toggle(personal)

        assertEquals(listOf(easy, hard), state.applyTo(mixed, entries))
    }

    @Test
    fun personal_appliesToTheOwnerOnly() {
        val someoneElse = NonogramFilters.forUser("uid-8")
        val state = FilterSortState().toggle(personal)

        assertEquals(mixed, state.applyTo(mixed, someoneElse))
    }

    @Test
    fun uncheckingPersonalAndADifficulty_appliesBothConstraints() {
        val state = FilterSortState().toggle(personal).toggle(Difficulty.HARD.label)

        assertEquals(listOf(easy), state.applyTo(mixed, entries))
    }

    @Test
    fun sortsBy_isTrueOnlyForAnAttributeInTheEntries() {
        assertEquals(false, FilterSortState().sortsBy(entries))
        assertEquals(false, FilterSortState(personal, SortDirection.ASC).sortsBy(entries))
        assertEquals(false, FilterSortState("nope", SortDirection.ASC).sortsBy(entries))
        assertEquals(
            true,
            FilterSortState(NonogramFilters.DIFFICULTY.label, SortDirection.ASC).sortsBy(entries),
        )
    }

    @Test
    fun sortingByPersonal_isANoOp_becauseItIsNotAnAttribute() {
        val state = FilterSortState(personal, SortDirection.ASC)

        assertEquals(mixed, state.applyTo(mixed, entries))
    }
}
