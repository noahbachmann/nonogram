package com.trainpaths.nonogram.filter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.trainpaths.nonogram.icons.arrow_drop_down
import com.trainpaths.nonogram.icons.filter

private val ICON_SIZE = 24.dp
private val CORNER = 8.dp
private val BUTTON_SHAPE = RoundedCornerShape(topStart = CORNER, topEnd = CORNER)
private val MENU_SHAPE = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = CORNER,
    bottomEnd = CORNER,
    bottomStart = CORNER,
)
private val ENTRY_HEIGHT = 40.dp
private val ITEM_HEIGHT = 32.dp
private val MENU_PADDING = 8.dp

@Composable
private fun menuItemColors() = MenuDefaults.itemColors(
    textColor = MaterialTheme.colorScheme.secondary,
    leadingIconColor = MaterialTheme.colorScheme.secondary,
    trailingIconColor = MaterialTheme.colorScheme.secondary,
)

/**
 * Top-bar button opening the filter/sort dropdown. Edits are collected in a draft and handed to
 * [onApply] only when the menu is dismissed, so the grid re-filters in one step on exit.
 *
 * The panel is a bare [Popup] rather than Material's `DropdownMenu` because that one's enter/exit
 * transition is hardcoded internally: the menu faded in after the button had already lit up.
 */
@Composable
fun FilterMenuButton(
    entries: List<FilterEntry>,
    state: FilterSortState,
    onApply: (FilterSortState) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(state) }

    fun dismiss() {
        expanded = false
        onApply(draft)
    }

    Box {
        IconButton(
            onClick = {
                draft = state
                expanded = true
            },
            modifier = if (expanded) {
                Modifier.background(MaterialTheme.colorScheme.outline, BUTTON_SHAPE)
            } else {
                Modifier
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (expanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            ),
        ) {
            Icon(
                imageVector = filter,
                contentDescription = "Filter and sort",
                modifier = Modifier.size(32.dp),
            )
        }
        if (expanded) {
            Popup(
                popupPositionProvider = remember { BelowAnchorStart() },
                onDismissRequest = { dismiss() },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MENU_SHAPE,
                    color = MaterialTheme.colorScheme.outline,
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = MENU_PADDING),
                    ) {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            if (entry is FilterAttribute) {
                                SortItem(
                                    attribute = entry,
                                    state = draft,
                                    onClick = { draft = draft.cycleSort(entry.label) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                )
                                entry.options.forEach { option ->
                                    CheckItem(
                                        label = option.label,
                                        checked = draft.isChecked(option.label),
                                        onClick = { draft = draft.toggle(option.label) },
                                    )
                                }
                            } else {
                                CheckItem(
                                    label = entry.label,
                                    checked = draft.isChecked(entry.label),
                                    onClick = { draft = draft.toggle(entry.label) },
                                    isEntry = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Puts the panel directly under the button, start-aligned, flipping above it if it would not fit. */
private class BelowAnchorStart : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val y = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) {
            anchorBounds.bottom
        } else {
            anchorBounds.top - popupContentSize.height
        }
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

@Composable
private fun SortItem(
    attribute: FilterAttribute,
    state: FilterSortState,
    onClick: () -> Unit,
) {
    val selected = state.sortAttribute == attribute.label
    val rotation by animateFloatAsState(
        targetValue = if (selected && state.sortDirection == SortDirection.ASC) 180f else 0f,
        label = "chevron",
    )
    DropdownMenuItem(
        modifier = Modifier.height(ENTRY_HEIGHT),
        text = {
            Text(
                attribute.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = onClick,
        trailingIcon = {
            Icon(
                imageVector = arrow_drop_down,
                contentDescription = sortDescription(attribute.label, selected, state.sortDirection),
                modifier = Modifier
                    .size(32.dp)
                    .alpha(if (selected) 1f else 0.4f)
                    .rotate(rotation),
            )
        },
        colors = menuItemColors(),
    )
}

/**
 * A checkbox row — used both for an attribute's values and, with [isEntry], for a standalone
 * [FilterToggle], which sits at attribute level and so is styled like one.
 */
@Composable
private fun CheckItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    isEntry: Boolean = false,
) {
    DropdownMenuItem(
        modifier = Modifier.height(if (isEntry) ENTRY_HEIGHT else ITEM_HEIGHT),
        text = {
            Text(
                label,
                style = if (isEntry) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            )
        },
        onClick = onClick,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(ICON_SIZE),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                ),
            )
        },
        trailingIcon = { Spacer(Modifier.size(ICON_SIZE)) },
        colors = menuItemColors(),
    )
}

private fun sortDescription(label: String, selected: Boolean, direction: SortDirection?): String =
    when {
        !selected -> "Sort by $label"
        direction == SortDirection.ASC -> "$label, ascending"
        else -> "$label, descending"
    }
