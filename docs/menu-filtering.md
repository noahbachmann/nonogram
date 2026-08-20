# Menu filtering & sorting

The puzzle menu's top-left button opens a dropdown that both **filters** and **sorts** the grid.
Difficulty is the only attribute today; the subsystem exists so that adding more is data, not code.

## Shape

Everything lives in `shared/src/commonMain/kotlin/com/trainpaths/nonogram/filter/`:

- **`FilterSort.kt`** — the pure model. `FilterAttribute` is a sortable heading (a `label` plus an
  `ascending: Comparator<Nonogram>`); `FilterOption` is one checkable value beneath it (a stable
  `id`, a `label`, and a `matches: (Nonogram) -> Boolean` predicate). `FilterSortState` is the
  user's selection, mutated only through the pure extensions `cycleSort`, `toggle`, and read by
  `isChecked` / `applyTo`. No Compose, no ViewModel — unit-tested in
  `shared/src/commonTest/.../filter/FilterSortStateTest.kt`.
- **`NonogramFilters.kt`** — the registry. `NonogramFilters.ALL` is the list the UI renders.
- **`FilterMenu.kt`** — `FilterMenuButton`, the app-bar button plus its `DropdownMenu`.

## Two decisions worth knowing

**Unchecked ids are stored, not checked ones.** `FilterSortState.uncheckedOptionIds` defaults to
`emptySet()`, which means "everything checked". A new `FilterOption` therefore starts checked with
no migration. It also makes the agreed empty-filter behaviour fall out of the `applyTo` predicate
(`attribute.options.any { isChecked(it.id) && it.matches(n) }`) without a special case: uncheck
every value of an attribute and nothing matches, so the grid is empty.

**Edits are drafted, then applied on dismiss.** `FilterMenuButton` keeps a local `draft` copy seeded
from the applied state when the menu opens; `onDismissRequest` is what calls `onApply`. That is the
"re-filter when the dropdown is exited" behaviour — the grid never churns while the menu is open.
Material 3's `DropdownMenu` supplies the outside-tap and back dismissal.

Sorting is a single nullable `sortAttributeId`, so "only one chevron active at a time" is structural
rather than enforced. The chevron cycles none → `DESC` → `ASC` → none, drawn from the one
`icons/ChevronDown.kt` vector rotated 180° for ascending and dimmed to alpha 0.4 when inactive.
`applyTo` sorts with `sortedWith`, which is stable, so puzzles that tie keep their database order.

## Adding an attribute

Append one `FilterAttribute` to `NonogramFilters.ALL`. Nothing in `FilterMenu`, `MenuViewModel`, or
`MenuScreen` changes.

## Wiring

`MenuViewModel` owns the applied `filterSort` (in memory only — it resets on restart, and there is
deliberately no `Settings` key) and exposes `visibleNonograms`, a `derivedStateOf` over the
unfiltered `nonograms` list. `reload()` is untouched, so sync and filtering stay independent.
`MenuScreen` passes `FilterMenuButton` through the `TopAppBar`'s `navigationContent` slot — an
optional caller-supplied composable that takes precedence over the default back button.
