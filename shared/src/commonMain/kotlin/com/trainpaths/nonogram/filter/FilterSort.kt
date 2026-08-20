package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Nonogram

enum class SortDirection { DESC, ASC }

/** One row of the filter dropdown. Its [label] is shown to the user and identifies it in state. */
sealed interface FilterEntry {
    val label: String
}

/** One checkable value under an attribute, e.g. "Easy" under "Difficulty". */
data class FilterOption(
    val label: String,
    val matches: (Nonogram) -> Boolean,
)

/** A sortable attribute together with the values it can be filtered by. */
data class FilterAttribute(
    override val label: String,
    val options: List<FilterOption>,
    val ascending: Comparator<Nonogram>,
) : FilterEntry

/**
 * A standalone checkable row with no sub-options and no sorting. Its checkbox reads inverted
 * compared to a [FilterOption]: while unchecked, everything [matches] accepts is hidden.
 */
data class FilterToggle(
    override val label: String,
    val matches: (Nonogram) -> Boolean,
) : FilterEntry

/**
 * Unchecked (rather than checked) labels are stored so that the default [FilterSortState] means
 * "everything checked" and options added later default to checked. Options and toggles share this
 * one namespace, so their labels must not collide.
 */
data class FilterSortState(
    val sortAttribute: String? = null,
    val sortDirection: SortDirection? = null,
    val uncheckedLabels: Set<String> = emptySet(),
) {

    fun isChecked(label: String): Boolean = label !in uncheckedLabels

    /** Cycles the sort through none -> DESC -> ASC -> none, clearing any other attribute's sort. */
    fun cycleSort(attribute: String): FilterSortState = when {
        sortAttribute != attribute ->
            copy(sortAttribute = attribute, sortDirection = SortDirection.DESC)

        sortDirection == SortDirection.DESC -> copy(sortDirection = SortDirection.ASC)
        else -> copy(sortAttribute = null, sortDirection = null)
    }

    fun toggle(label: String): FilterSortState = copy(
        uncheckedLabels = if (label in uncheckedLabels) {
            uncheckedLabels - label
        } else {
            uncheckedLabels + label
        },
    )

    fun applyTo(nonograms: List<Nonogram>, entries: List<FilterEntry>): List<Nonogram> {
        val filtered = nonograms.filter { nonogram ->
            entries.all { entry ->
                when (entry) {
                    is FilterAttribute ->
                        entry.options.any { isChecked(it.label) && it.matches(nonogram) }

                    is FilterToggle -> isChecked(entry.label) || !entry.matches(nonogram)
                }
            }
        }
        val attribute = entries.filterIsInstance<FilterAttribute>()
            .firstOrNull { it.label == sortAttribute } ?: return filtered
        return filtered.sortedWith(
            if (sortDirection == SortDirection.ASC) attribute.ascending else attribute.ascending.reversed()
        )
    }
}
