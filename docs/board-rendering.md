# Board rendering (pan / zoom / draw)

The interactive grid — clues, tiles, pan, zoom, and drawing — is a self-contained engine in
`shared/src/commonMain/.../classes/`:

- **`Board.kt`** — the composable + all drawing.
- **`BoardTransform.kt`** — `BoardTransformState` (the pan/zoom model), gesture detectors, and every
  layout constant.
- **`Game.kt`** — thin wrapper: hosts `Board` and fires `onWin` when tiles match the solution.
- **`Tile.kt`** — `Tile` (a Compose `mutableStateOf` cell), `TileState` (NONE → FILLED → CROSSED → …)
  and `DrawMode` (which state an edit writes).

Used by both `GameScreen` (playing) and `GenScreen` (drawing). This doc exists so I don't have to
re-read ~1000 lines to remember how it fits together — the source has the fine-grained *why* in
comments; this is the map.

## The core performance idea: draw, don't recompose

A naïve grid is one Box per tile — 2500 layout nodes for a 50×50, re-laid-out on every pan. Instead:

- **All tiles are one `Canvas`** (`drawTiles`). Reading `tile.state` inside the draw lambda registers a
  *draw-scope* dependency, so filling a tile invalidates only that one node's **draw** — no
  recomposition, no relayout.
- **Pan/zoom is a layer transform, not a resize.** Each region (grid, row gutter, col header) is
  measured once at its true unscaled size (`Modifier.oversized` = `wrapContentSize(unbounded) +
  requiredSize`) and moved by its own `graphicsLayer { scale/translation = state.… }`. Reading the
  transform *inside* the `graphicsLayer`/`drawBehind`/`clipRect` lambda scopes the snapshot read to
  the layer, so a pan invalidates layers/draws but **never triggers recomposition**.

> Consequence: nothing exposed by `BoardTransformState` is read during composition — only inside
> `graphicsLayer{}`, draw lambdas, and gesture coroutines. Keep it that way.

## Layout of the `Board` composable

Inside a `BoxWithConstraints(...safeContentPadding())`, `state.updateGeometry(...)` is pushed the
viewport + cell/clue pixel sizes each frame (idempotent; produces a correct *first* frame with no
fit-scale flash — unlike `onSizeChanged`). Then a `clipToBounds` Box stacks, in draw order:

1. **Grid Canvas** — `drawTiles`: white bg, filled cells (full black rects), crossed cells (two
   lines), then gridlines on top (every `BLOCK_SIZE`th line heavy), then the field's right/bottom edge.
2. **Block-label overlay** (`Spacer` + `drawBehind`, `drawBlockLabels`) — the every-`BLOCK_SIZE`th
   line's index number, pinned to the *visible* field edge rather than the field's own edge (see
   "Line metrics" below). Its position in the stack — after the grid Canvas, before the gutters and
   the frame overlay — is what makes a label sliding under a pinned gutter get occluded, for free,
   by draw order; no extra culling math needed.
3. **Row-clue gutter** (`Column` of `RowClueLine`) — clipped to a horizontal window (`rowClueWindowW`),
   scrolls inside it via `clueScrollX`.
4. **Col-clue header** (`Row` of `ColClueLine`) — clipped to a vertical window, scrolls via `clueScrollY`.
5. **Frame overlay** (`Spacer` + `drawBehind`) — masks the corner where gutters would overlap when
   both axes are panned, and paints the field's *left/top* divider edges (the grid Canvas paints the
   matching right/bottom, so the field ends framed on all four sides).
6. **`ZoomControls`** (+/−/fit) — only when `maxWidth >= ZOOM_CONTROLS_MIN_WIDTH` (600.dp); a *sibling*
   of the gesture Box so pressing a button never starts a pan/stroke.

Clue gutters use the thin `CLUE_CELL` (20.dp) along their own axis, not `CELL` (40.dp), because a row
of width W holds up to `ceil(W/2)` clues — at full cell size the gutter would eat half the screen.

## `BoardTransformState`

Maps a content plane (origin at the top-left of the empty corner cell) to the viewport:
`v = p * scale + offset`. Everything is `mutableFloatStateOf` with `private set`.

- **Fit / re-fit gate.** `fitScale` inscribes the whole board (grid + both gutters + separator).
  `reset()` fits, centres, and clears `userAdjusted`. `updateGeometry` re-fits **only while
  `!userAdjusted`** — so as the clue gutter grows while you draw in GenScreen the board keeps fitting,
  but once you pan/zoom by hand your zoom is preserved. Any pan/zoom sets `userAdjusted = true`.
- **Gutter windowing.** Gutters are "sticky headers": pinned at `max(0f, offset)` and clipped to a
  window capped at `GUTTER_MAX_FRACTION` (0.35) of the viewport; clues past the cap are reached by
  scrolling the gutter (`clueScroll{X,Y}`, always ≤ 0). `visibleGutterWpx` caps the reserved gutter at
  the grid's own width so it can't shove the grid off-screen.
- **Zoom** is clamped to `[fitScale, max(fitScale,1)*MAX_ZOOM_MULTIPLE]` (3×), anchored so the content
  point under the cursor/centroid stays fixed (`zoomBy`).
- **Hit testing.** `hitTest(v)` → `TileCoord?` (rejects gutters/corner via `regionAt`, and any tile
  occluded by a pinned gutter). `hitTestSegment(from,to)` samples a whole pointer segment at half a
  rendered cell so a fast stroke can't skip a tile.

## Gesture routing — the part that's easy to break

Four stacked `pointerInput` nodes on the gesture Box. Compose dispatches the **Main** pass
innermost-outward, so the *last* modifier sees each event *first*. Order (top → bottom in source):

1. **Scroll-wheel zoom** (Main) — desktop/web; clamps notches so one flick isn't a 100× zoom.
2. **`detectBoardTaps`** — fires `onTap` immediately on UP (no double-tap delay, so tapping a tile
   isn't laggy). Never consumes; **cancels itself** the moment another detector consumes (i.e. once a
   drag passes touch slop). That self-cancel *is* the "tap toggles vs drag pans" threshold — there is
   no explicit distance check of our own.
3. **End-of-gesture reset** (Final pass) — when no pointer is down, `endGesture()` so the next drag
   re-picks its region.
4. **`detectTransformGestures`** — pan/zoom. A one-finger drag starting in a gutter scrolls *that
   gutter* on its own axis and pans the board on the other; a pinch always transforms the board and
   pins the rest of the gesture to that (`activeRegion`).
5. **`detectBoardDrawGestures`** (only when `isLocked`) — innermost, so in locked mode it gets first
   refusal. Commits a one-pointer stroke only after touch slop (a second finger before that hands off
   to pinch); once committed it consumes every change so the transform detector can't also pan.
   `TileStroke` picks its target state once from the start tile (`mode.apply(startTile.state)`) and
   visits each cell at most once, so crossing back over a stroke doesn't re-toggle.

**Lock mode** (`isLocked`): `true` → one-finger drag *draws*; `false` → one-finger drag *pans*. Pinch
zoom and tap-to-edit work in both. Toggled from `BottomToolBar` (the lock/unlock button).

**Draw mode** (`DrawMode`, cycled by the `BottomToolBar` tool button): what an edit *writes*.
`TOGGLE` is the historical behaviour — advance the cell by `TileState.next()`. `FILL` / `CROSS` /
`ERASE` write that one state and are idempotent, so re-tapping or re-crossing a cell never undoes
it. Both mutation paths — the tap in `Board` and `TileStroke.begin` — resolve through
`DrawMode.apply(current)`, which is the single answer to "what does this edit write?". The mode
reaches the long-lived gesture coroutines as a lambda (`drawMode: () -> DrawMode`) read at stroke
commit, and is *not* a `pointerInput` key: changing tools mid-board must not tear down and restart
the detectors. It is per-screen composable state, like `isLocked`, and resets to `TOGGLE`.

## Line metrics

`lineUnitPx(scale, tileBorderPx) = max(tileBorderPx, LINE_MIN_DEVICE_PX / scale)` — a stroke width
that scales with zoom but never drops below one device pixel (or it antialiases into nothing). Every
line is a multiple of this unit, giving a stable **1 : 2 : 4** hierarchy: hairline gridline
(`TILE_BORDER`) < block line every `BLOCK_SIZE`=5 (`BLOCK_LINE_UNITS`) < gutter/field separator
(`SEPARATOR_UNITS`). The separator width is *reserved* in the content plane (`separatorContentPx`) so
it scales with everything and the clip leaves room for it rather than painting over the clues.

Each heavy block line also carries its index, so counting blocks off a clue doesn't mean counting
lines from the edge by hand. The number belongs to the *line*, not to the last cell in it: it tracks
its line on the cross axis (right-aligned against a vertical line, sitting above a horizontal one) but
pins to the *visible* right/bottom edge of the field on the other axis (`drawBlockLabels`), not the
field's own edge — so it stays legible under any pan, even once the last row/column has scrolled off
screen. That pinning is why the labels are a separate overlay (`Board.kt`, drawn in viewport px)
rather than living inside `drawTiles`, which paints in content px under the grid's own
`graphicsLayer` and would drag the numbers along with a pan same as the tiles. Font size is a fraction
of `CELL` (`BLOCK_LABEL_FONT_FRACTION`), so it scales with the board like everything else, but a
numeral can't be widened the way a line's stroke can: below `BLOCK_LABEL_MIN_DEVICE_PX` rendered
height the labels are dropped outright rather than floored.
