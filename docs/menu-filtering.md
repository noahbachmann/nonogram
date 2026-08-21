# Menu filtering & sorting

The puzzle menu's top-left button opens a dropdown that both **filters** and **sorts** the grid.
Difficulty and "Personal" are the entries today; the subsystem exists so that adding more is data,
not code.

## Shape

Everything lives in `shared/src/commonMain/kotlin/com/trainpaths/nonogram/filter/`:

- **`FilterSort.kt`** — the pure model. `FilterEntry` is the sealed supertype of the two row
  shapes:
  - `FilterAttribute` — a sortable heading (a `label` plus an `ascending: Comparator<Nonogram>`)
    with `FilterOption`s beneath it. A `FilterOption` is one checkable value (a `label` and a
    `matches: (Nonogram) -> Boolean` predicate).
  - `FilterToggle` — a standalone checkable row: no sub-options, no chevron, no sorting.

  `FilterSortState` is the user's selection — an immutable data class whose own methods
  (`cycleSort`, `toggle`, `isChecked`, `applyTo`) return the next state. No Compose, no ViewModel —
  unit-tested in `shared/src/commonTest/.../filter/FilterSortStateTest.kt`.
- **`NonogramFilters.kt`** — the registry. `NonogramFilters.forUser(userId)` is the list the UI
  renders; it is a function rather than a constant because the "Personal" toggle needs to know who
  you are.
- **`FilterMenu.kt`** — `FilterMenuButton`, the app-bar button plus its dropdown panel.

**The label is the id.** There is no separate identifier: `FilterSortState` stores labels, so a
label has to be unique across every option and toggle. State is in-memory only, so renaming one
costs nothing beyond resetting that row's checkbox for the session.

## Three decisions worth knowing

**Unchecked labels are stored, not checked ones.** `FilterSortState.uncheckedLabels` defaults to
`emptySet()`, which means "everything checked". A new `FilterOption` therefore starts checked with
no migration. It also makes the agreed empty-filter behaviour fall out of the `applyTo` predicate
(`attribute.options.any { isChecked(it.label) && it.matches(n) }`) without a special case: uncheck
every value of an attribute and nothing matches, so the grid is empty.

**A toggle's checkbox reads inverted.** An attribute's option *admits* the puzzles it matches; a
toggle *stops excluding* them (`isChecked(entry.label) || !entry.matches(n)`). That is what makes
"Personal checked" mean "no constraint" rather than "only mine" — the default state shows
everything, exactly as it does for difficulty.

**Edits are drafted, then applied on dismiss.** `FilterMenuButton` keeps a local `draft` copy seeded
from the applied state when the menu opens; `onDismissRequest` is what calls `onApply`. That is the
"re-filter when the dropdown is exited" behaviour — the grid never churns while the menu is open.
Material 3's `DropdownMenu` supplies the outside-tap and back dismissal.

**The button's highlight is animated to catch up with the menu.** While open the button wears the
menu's own `outline` colour, rounded on top and square along the bottom to meet the menu's squared
top-left corner. `DropdownMenu` hardcodes its enter/exit transition internally and exposes no way to
change it, so the highlight would otherwise appear a beat before the menu did; instead it fades on
the same timings (`MENU_ENTER_MS`/`MENU_ENTER_DELAY_MS`/`MENU_EXIT_MS`, copied from Material's
constants). Adjust those if the highlight ever leads or lags the menu.

Sorting is a single nullable `sortAttribute`, so "only one chevron active at a time" is structural
rather than enforced. The chevron cycles none → `DESC` → `ASC` → none, drawn from the one
`icons/ChevronDown.kt` vector rotated 180° for ascending and dimmed to alpha 0.4 when inactive.
`applyTo` resolves it against `filterIsInstance<FilterAttribute>()`, so a stale label pointing at a
toggle simply sorts nothing. It sorts with `sortedWith`, which is stable, so puzzles that tie keep
their database order.

## Own puzzles

Ownership is `Nonogram.isOwned(userId)`, which excludes `authorId == 0L` — seeded puzzles and
puzzles pulled from another author both carry 0 (see `sync/SyncService.kt` and its two platform
implementations). Guests count: a guest has a local user id and can author puzzles, so the Personal
row is meaningful before sign-in and is always shown.

Your puzzles are pinned to the top of the grid **only while nothing is sorted**. That is
`MenuViewModel`'s private `ownFirst()`, applied *after* `applyTo` rather than inside it, and skipped
when `FilterSortState.sortsBy(entries)` reports an active sort — sorting by difficulty is meant to
order own and other puzzles together, so own-first would fight it. `sortsBy` resolves the label the
same way `applyTo` does, so a stale label pointing at a toggle counts as no sort and own-first still
applies. No extra knob is needed on `FilterToggle`.

`NonogramCard` also tints the offset box behind the card by ownership — `beaten → tertiary`, else
`own → onSecondary`, else `onPrimary`. `isOwn` defaults to `false`, so `GenListScreen` (where every
card is yours anyway) is unaffected.

## Adding an entry

Append to the list `NonogramFilters.forUser` returns: a `FilterAttribute` for something sortable
with several values, a `FilterToggle` for a single show/hide switch. Nothing in `FilterMenu`,
`MenuViewModel`, or `MenuScreen` changes.

## Wiring

`MenuViewModel` owns the applied `filterSort` (in memory only — it resets on restart, and there is
deliberately no `Settings` key) and exposes `visibleNonograms`, a `derivedStateOf` over the
unfiltered `nonograms` list. Because `AuthRepository.currentUserId` is a `StateFlow` that
`derivedStateOf` cannot observe, `reload()` mirrors it into a Compose-state `userId`, which
`filterEntries` and `visibleNonograms` derive from and which `MenuScreen` reads directly for
`nonogram.isOwned(viewModel.userId)` — so the entries, the pinning, and the card tint all refresh on
sign-in/sign-out, both of which run `loadAll()`. `MenuScreen` passes
`FilterMenuButton` through the `TopAppBar`'s `navigationContent` slot — an optional caller-supplied
composable that takes precedence over the default back button.
