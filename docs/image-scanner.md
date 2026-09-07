# Image scanner

Image → black/white grid → the generator board, editable, saved through the normal path (Solver validation, duplicate
check, publish request).

## Flow

`GenListScreen` **Scan image** → `GenScanRoute` → `GenScanScreen` → **Generate** →
`GenViewModel.loadScanned(grid, name)` → `GeneratorRoute` with `popUpTo(GenListRoute)`.

Two things that will bite:

- **Never route a scan through `GenConfScreen`.** Its `!editing` branch calls `setNonogram`, which rebuilds `tiles` and
  wipes the grid. (The wrench inside `GenScreen` opens `GenConfRoute(editing =
  true)`, which resizes non-destructively — that path is fine.)
- **`loadScanned` leaves `isDirty = true`**, unlike `loadForEdit`. `GenScreen.attemptLeave` keys off it, so an unsaved
  scan gets the confirm dialog instead of vanishing.

## Pipeline

1. `ByteArray.decodeToLumaMap()` — `decodeToImageBitmap()`, then `readPixels` a band at a time into one reused buffer
   (`BAND_BUDGET` ints), accumulating into a `LumaMap` of at most `WORKING_SIDE`
   (256) on its longest side. Alpha is composited over **white**; without that a transparent PNG scans as a fully filled
   grid.
2. `LumaMap.otsuThreshold()` — the initial slider value.
3. `defaultDimensions(width, height)` — aspect-preserving, longest side `DEFAULT_SCAN_SIDE`.
4. `LumaMap.toGrid(ScanOptions)` — averages the source pixels under each cell, fills when the average is below
   `threshold`, `invert` flips.

No cropping: the whole image is always used, framing is the user's job.

`toGrid` clamps its sides via `clampToGridSide()`, so the output always satisfies `isWellFormedGrid()`
whatever the caller asked for — a ragged or out-of-range grid makes `Nonogram.colClues` throw.

Reducing to 256px once is what keeps the slider cheap: every recompute after the first runs over ≤65 KB. Matters because
on web `Dispatchers.Default` **is** the main thread.

## Files

|                                      |                                                                                  |
|--------------------------------------|----------------------------------------------------------------------------------|
| `scan/LumaMap.kt`                    | `LumaMap`, `LumaAccumulator` (the box-downsample), `lumaOverWhite`, `scaledSize` |
| `scan/ImageDecode.kt`                | `decodeToLumaMap` — the only part tests can't reach (needs a real image + Skia)  |
| `scan/ImageToGrid.kt`                | `ScanOptions`, `otsuThreshold`, `defaultDimensions`, `clampToGridSide`, `toGrid` |
| `screens/ImagePicker.kt`             | `expect @Composable rememberImagePicker(onPicked, onError): () -> Unit`          |
| `screens/viewModel/ScanViewModel.kt` | tuning state + `previewGrid`; `MAX_IMAGE_BYTES` = 20 MB                          |
| `screens/GenScanScreen.kt`           | the screen; `GridPreview` is a plain `Canvas`, not `Board`                       |
| `commonTest/scan/ImageToGridTest.kt` | everything except the decode                                                     |

Decoding is **not** platform-specific: `decodeToImageBitmap` and `readPixels` are both in the common API surface of
`compose.ui:ui-graphics`. Only acquiring the bytes needs `expect`/`actual`.

## Picker actuals

- **android**: `ActivityResultContracts.PickVisualMedia` + `ContentResolver` on `Dispatchers.IO`. No manifest permission
  needed. Required adding `activity-compose` to `shared`'s androidMain — it was only on `androidApp`.
- **web**: one actual in `webMain`, **no js/wasmJs split**. The split `createDbWorkerDriver` needs does not apply: the
  stdlib's webMain view no longer ships `org.w3c`, so `kotlinx-browser` (now declared explicitly) is the sole DOM
  provider for both targets and `FileReader.result` is one unified `JsAny?`. A detached `<input type="file">`,
  `addEventListener("change", …)` rather than
  `onchange` (that one's type does differ across targets).

A dismissed picker is silent — no pick, no error — like the canceled sign-in in
`GoogleSignInSection`.

## Shared with the generator

`SizeField` (rows/columns input, digit filter at the call site, `SIDE_RANGE_HINT` supporting text)
lives in `GenConfScreen.kt`; same package, so no import. `AppButton` is in `AppTheme.kt`.
