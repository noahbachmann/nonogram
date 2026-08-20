package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Nonogram

enum class SortDirection { DESC, ASC }

/** One checkable value under an attribute, e.g. "Easy" under "Difficulty". */
data class FilterOption(
    val id: String,
    val label: String,
    val matches: (Nonogram) -> Boolean,
)

/** A sortable attribute together with the values it can be filtered by. */
data class FilterAttribute(
    val id: String,
    val label: String,
    val options: List<FilterOption>,
    val ascending: Comparator<Nonogram>,
)

/**
 * Unchecked (rather than checked) ids are stored so that the default [FilterSortState] means
 * "everything checked" and options added later default to checked.
 */
data class FilterSortState(
    val sortAttributeId: String? = null,
    val sortDirection: SortDirection? = null,
    val uncheckedOptionIds: Set<String> = emptySet(),
)

/** Cycles the sort through none -> DESC -> ASC -> none, clearing any other attribute's sort. */
fun FilterSortState.cycleSort(attributeId: String): FilterSortState = when {
    sortAttributeId != attributeId ->
        copy(sortAttributeId = attributeId, sortDirection = SortDirection.DESC)

    sortDirection == SortDirection.DESC -> copy(sortDirection = SortDirection.ASC)
    else -> copy(sortAttributeId = null, sortDirection = null)
}

fun FilterSortState.toggle(optionId: String): FilterSortState = copy(
    uncheckedOptionIds = if (optionId in uncheckedOptionIds) {
        uncheckedOptionIds - optionId
    } else {
        uncheckedOptionIds + optionId
    },
)

fun FilterSortState.isChecked(optionId: String): Boolean = optionId !in uncheckedOptionIds

fun FilterSortState.applyTo(
    nonograms: List<Nonogram>,
    attributes: List<FilterAttribute>,
): List<Nonogram> {
    val filtered = nonograms.filter { nonogram ->
        attributes.all { attribute ->
            attribute.options.any { isChecked(it.id) && it.matches(nonogram) }
        }
    }
    val attribute = attributes.firstOrNull { it.id == sortAttributeId } ?: return filtered
    return filtered.sortedWith(
        if (sortDirection == SortDirection.ASC) attribute.ascending else attribute.ascending.reversed()
    )
}
