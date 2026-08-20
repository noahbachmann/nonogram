# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A Kotlin Multiplatform nonogram puzzle app targeting Android, Web (JS/Wasm), and iOS (currently commented out). Users
solve nonogram puzzles, track progress, and optionally sync via Google sign-in with Firebase.

## Build & Run

```bash
# Android
./gradlew :androidApp:assembleDebug

# Web (Wasm — faster, modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS — broader compatibility)
./gradlew :webApp:jsBrowserDevelopmentRun

# Web production bundles
./gradlew :webApp:wasmJsBrowserDistribution :webApp:jsBrowserDistribution

# iOS: open iosApp/ in Xcode
```

## Tests

```bash
# Android host tests (uses SQLite JVM driver, no emulator needed)
./gradlew :shared:testAndroidHostTest

# Common tests only
./gradlew :shared:wasmJsTest
./gradlew :shared:jsTest

# iOS
./gradlew :shared:iosSimulatorArm64Test
```

Tests live in `shared/src/commonTest/` (pure-logic tests) and `shared/src/androidHostTest/` (tests that need a
SQLDelight driver — uses `TestDatabaseFactory` with the SQLite JVM driver).

## Documenting changes

Don't over-document via code comments. When a change is architecturally significant (a new subsystem, a cross-cutting
refactor, a platform port), explain the design in a `.md` file under `docs/` instead — see
`docs/web-architecture.md` for the shape this should take. Code comments stay minimal (one line, only for genuinely
non-obvious *why*).

## Architecture

All shared code lives in `shared/src/commonMain/`, with platform-specific code in `shared/src/androidMain/` and
`shared/src/webMain/` (shared by the `jsMain`/`wasmJsMain` source sets — see `docs/web-architecture.md`). Platform apps
(`androidApp/`, `webApp/`, `iosApp/`) are thin shells that initialize Koin DI and host the Compose UI.

### Layers

- **`AppSDK`** — facade over the database. All data access goes through here; every method is `suspend` (SQLDelight
  `generateAsync` is on so the web worker driver can be async). Builds its `Database` lazily behind a mutex on first use
  so the constructor itself stays synchronous for DI.
- **`cache/Database`** — internal class wrapping SQLDelight-generated `NonogramDb`. Maps DB rows to domain types. Not
  accessed directly outside `AppSDK`.
- **`cache/DatabaseFactory`** — `suspend fun createDriver(): SqlDriver`, implemented per platform:
  `AndroidDatabaseFactory`
  (androidMain, `Schema.synchronous()`), `WebDatabaseFactory` (webMain, OPFS worker driver + explicit
  `PRAGMA user_version` migration — see `docs/web-architecture.md`), `TestDatabaseFactory` (androidHostTest, in-memory
  JDBC).
- **`auth/AuthRepository`** — manages auth state (`GUEST` / `SIGNED_IN`), local user ID, and onboarding flag via
  `multiplatform-settings`. Links guest accounts to Firebase UIDs on sign-in. `initialize()`/`linkFirebaseUser()` are
  suspend (call the suspend `AppSDK`). `signOut()` doesn't delete the signed-in user's local row — it creates a new
  guest `User` and repoints `current_user_id` at it, so re-linking the same Firebase UID later restores the original
  row (progress and authored puzzles intact). Onboarding flag and per-UID sync cursors are left untouched.
- **`theme/ThemeRepository`** — holds the selected `ColorTheme`, persisted via `multiplatform-settings` (key
  `color_theme`, stores the enum name). Same shape as `AuthRepository`: reads synchronously in the constructor (no
  `initialize()` needed), exposes a `StateFlow`, writes through on `setTheme`. See **AppTheme** below.
- **`sync/SyncService`** — interface for syncing *both* progress and the shared `nonograms` collection (push/pull/merge;
  `mergeRemoteNonograms` is the shared merge policy). `sync/FirebaseAndroidSyncService` (androidMain)
  implements it with `dev.gitlive:firebase-firestore`; web binds `sync/FirebaseWebSyncService` (webMain), built on
  hand-written Kotlin externals to the Firebase JS SDK in `firebase/` (see `docs/web-architecture.md` for the externals
  pattern and the auth-session gate). Same Firestore shape on both platforms, so data interoperates.
- **`classes/` board + game** — the interactive grid (clues, tiles, pan/zoom, drag-to-draw) is a self-contained Compose
  engine: `Board`/`BoardTransform` (one Canvas for all tiles + a layer-transform pan/zoom model),
  `Game` (win check), `Tile`/`TileState`. Performance-critical and gesture-heavy — see `docs/board-rendering.md`.
- **`filter/`** — the menu's combined filter/sorter: a pure `FilterSortState` model plus the
  `FilterMenuButton` dropdown hosted in the `TopAppBar`'s `navigationContent` slot. Rows are data —
  a sortable `FilterAttribute` with checkable values, or a standalone `FilterToggle` (the "Personal"
  own-puzzles switch) — so adding one is a list entry in `NonogramFilters.forUser(userId)`, a
  function rather than a constant because ownership is user-scoped. A row's `label` is also its id.
  See `docs/menu-filtering.md`.
- **`classes/Solver`** — line-logic solver; run via `Nonogram.isValid` to check a puzzle is uniquely solvable (gates
  publishing). **User-owned and actively changing — do not document its internals or modify it.**
- **`network/NonogramApi`** — an unused stub (bare Ktor `HttpClient`, no callers). Not a live data path.
- **`screens/GoogleSignInSection`** — `expect`/`actual` composable for the Google sign-in button; Android wires
  `kmpauth-firebase`, web wires kmpauth's `GoogleButtonUiContainer` + `FirebaseWeb.signInWithGoogle` (note: the web flow
  yields an access token, not an ID token — see `docs/web-architecture.md`).
- **ViewModels** (`screens/viewModel/`) — Compose state holders using `mutableStateOf`. `GameViewModel` manages the tile
  board and save/sync. `GenViewModel` drives the generator (draw/resize/save + validation). `MenuViewModel` holds the
  nonogram list and progress preview map. `AuthViewModel` orchestrates login flow and **all remote sync** —
  `syncAll` pulls progress + public + owned nonograms in one pass (separate public/owned cursors read via
  `AuthRepository`), `retryOwnNonograms` re-runs just the owned stream for the generator's retry button.
  All depend on the suspend `AppSDK`/`SyncService` from inside `viewModelScope.launch`.

  **When sync runs.** `syncAll` fires once from `AppContent`'s app-start `LaunchedEffect`, and after that
  only when the user pull-to-refreshes the menu (`MenuScreen`'s
  `PullToRefreshBox` → `MenuViewModel.refresh`). Entering `MenuRoute` does **not** sync — it calls
  `MenuViewModel.reload()`, a silent local-DB re-read with no spinner, so puzzles just authored in the
  generator still appear. `MenuViewModel` therefore has two flags: `isLoading` (full-screen spinner, cold
  start and sign-in/sign-out only, via `loadAll()`) and `isRefreshing` (the pull-to-refresh indicator).

### Navigation

Type-safe navigation via `navigation-compose` with `@Serializable` route objects in `navigation/Routes.kt`. Routes:
`LoginRoute`, `MenuRoute`, `GameRoute(nonogramId)`, `SettingsRoute`, plus the generator routes `GenListRoute`,
`GenConfRoute(editing)`, `GeneratorRoute` (the board editor), plus dialog routes (`PlayDialogRoute`, `WinDialogRoute`,
`LeaveDialogRoute`).

**Generator flow.** `GenConfRoute` is *linked to a specific `GeneratorRoute`* via its `editing` flag:

- **New:** `GenList` → `+New` (`startNew()`) → `GenConf(editing=false)` → *Generate* (`setNonogram`) → `GenScreen`.
  GenConf's
  "Done" navigates forward to `GeneratorRoute` (`popUpTo(GenListRoute)`); back cancels to the list.
- **Edit:** `GenList` → card (`loadForEdit`) → `GenScreen`. The top-left wrench opens `GenConf(editing=true)` pre-filled
  with the current dims; *Save* applies `resizeNonogram` (preserves overlapping cells, keeps the puzzle id) **and
  persists via
  `onSave`**, *Back* discards — both `popBackStack()` to the same `GenScreen`.

**Save-state guard.** `GenViewModel.isDirty` tracks unsaved edits (set on tile edit/`resize`, cleared on load/save).
Leaving
`GenScreen` toward the list — via the swap button *or* system/predictive back (`NavigationBackHandler` +
`rememberNavigationEventState` from `androidx.navigationevent:navigationevent-compose`, the modern non-deprecated back
API — NOT the deprecated `androidx.compose.ui.backhandler.BackHandler`) — routes through `attemptLeave`:
if dirty it shows `GenSaveConfirmDialog` (Save / Don't save / cancel-by-dismiss) before navigating; otherwise it goes
straight to `GenListRoute`. The board's bottom app-bar Save icon saves in place and is enabled only for a new or dirty
puzzle; the leave dialog's Save action remains save-and-exit.

`TopAppBar` navigation icon: GENERATOR mode shows the `build` wrench (used as the "config" affordance in `GenScreen`);
pass `backArrow = true` to force a plain back arrow (used in `GenConf`).

`navigation/BottomToolBar.kt` is the board's bottom bar (GameScreen + GenScreen): a **lock/unlock** toggle (locked =
one-finger drag draws, unlocked = drag pans — see `docs/board-rendering.md`), a **draw-mode** button cycling
`DrawMode` (Toggle → Fill → Cross → Erase; Toggle keeps the classic `TileState.next()` cycle, the other three write that
state idempotently), an optional **reset-zoom** button, and (GenScreen) the **Save** icon (enabled only for a new or
dirty puzzle). Icons come from the hand-built `icons/` package of `ImageVector`s.

### DI (Koin)

- `di/AppModule.kt` — common singletons: `AppSDK`, `Settings`, `AuthRepository`, `ThemeRepository`, plus all ViewModel
  registrations via `viewModelOf` (koin-core-viewmodel, shared across platforms).
- `di/AndroidModule.kt` — platform bindings: `DatabaseFactory` → `AndroidDatabaseFactory`, `SyncService` →
  `FirebaseAndroidSyncService`.
- `di/WebModule.kt` (webMain) — platform bindings: `DatabaseFactory` → `WebDatabaseFactory`, `SyncService` →
  `FirebaseWebSyncService`.

### Data Model

- **`Nonogram`** — `id`, `difficulty` (enum: EASY/MEDIUM/HARD/HARDCORE), `solution` (List<List<Int>> stored as JSON),
  `name: String?`, `authorId`, `isPublic: Boolean`, `updatedAt`. Computes `rowClues`/`colClues` on the fly, and
  `isValid` lazily via the `Solver`. Note: `isPublic` is backed by the DB column named `status` (0/1). Name helpers live
  alongside: `MAX_NONOGRAM_NAME_LENGTH` (30), `normalizeNonogramName()`, `UNNAMED_NONOGRAM_TITLE`.
- **`Tile`** — mutable Compose state. Cycles: NONE → FILLED → CROSSED → NONE.
- Board state is serialized as `List<List<Int>>` (0/1) for persistence and sync.

### SQLDelight

Schema: `shared/src/commonMain/sqldelight/com/trainpaths/nonogram/cache/database.sq`
Migrations: numbered `.sqm` files in the same dir — currently `1.sqm`–`4.sqm` (UserProgress.beat;
NonogramData.authorId + status; NonogramData.updatedAt; NonogramData.name). Database name: `NonogramDb`, package:
`com.trainpaths.nonogram.cache`

Tables: `NonogramData`, `User`, `UserProgress` (composite PK: userId + nonogramId).

`generateAsync` is enabled, so all generated query/transaction code is `suspend`. Sync drivers (Android, JVM tests)
adapt via `NonogramDb.Schema.synchronous()`; the web worker driver uses the async API directly. See
`docs/web-architecture.md` for the per-platform schema init/migration story.

### AppTheme

Defined in `AppTheme.kt`. `ColorTheme` is an enum of up to 5 Material 3 `lightColorScheme`s, built by the private
`colorScheme(...)` factory. `ColorTheme.DEFAULT = FOREST` reproduces the original single hardcoded scheme. Current
entries: `FOREST`, `MIDNIGHT`, `PLUM` (dark), `PAPER`, `FROST` (light).

Per-theme roles (vary):

- `primary` — background, TopAppBar, main surface
- `onPrimary` — the accent; content on primary (e.g. app-bar icons/text, card accent blocks)
- `secondary` / `onSecondary` — BottomToolBar container/content, unfocused text-field state
- `outline` — the near-white/white card panel color (`NonogramGrid`/`GenListScreen` cards)
- `onBackground` — text drawn directly on `background` (white on dark themes, near-black on light)
- `background` — always set equal to `primary`, never passed independently

Frozen roles (identical across every theme — difficulty semantics, always drawn on white cards):

- `tertiary` `#D7B400` gold — MEDIUM difficulty, beat badges
- `onTertiary` `#6DB85C` green — EASY difficulty
- `tertiaryFixed` `#CE0C0C` red — HARD difficulty

`surface` and `error` are never overridden (M3 light defaults) — don't put per-theme content on `surface`, use
`outline` instead. `AppTheme(theme: ColorTheme, content: ...)` requires the theme explicitly (no default), so every
call site must supply one. The active theme lives in `theme/ThemeRepository`/`ThemeViewModel` and is threaded as a
required `App(..., themeViewModel)` param.

`App()` itself owns the single `AppTheme` call site and the auth-init gate: it reads `themeViewModel.theme` and
`authViewModel.authState`, wraps everything in `AppTheme(theme)`, and renders `LoadingScreen()` while
`authState == INITIALIZING` instead of the real content (`AppContent`, a private composable with the NavHost). The
two platform entry points (`MainActivity.kt`, `webApp/main.kt`) therefore just call `App(...)` unconditionally — no
`if`/`else`, no `AppTheme` call of their own. `menuViewModelFactory`/`genViewModelFactory` are passed as
`@Composable () -> VM` (matching the existing `gameViewModelFactory` idiom), **not** resolved instances — this is
deliberate, not simplifiable: it keeps `MenuViewModel`/`GenViewModel` construction inside the non-`INITIALIZING`
branch, since `MenuViewModel.init { loadAll() }` needs `AuthRepository.currentUserId` to already be set.

Use `MaterialTheme.colorScheme.*`, never hardcode hex — outside `AppTheme.kt` itself, where each theme's palette is
defined.

### Firebase / Auth

- Google sign-in via `kmpauth` (`io.github.mirzemehdi:kmpauth-google/firebase`) — `kmpauth-firebase` is Android-only (no
  web target published), so it's an androidMain-only dependency; `kmpauth-google`/`kmpauth-uihelper` are commonMain
  (both publish js+wasmJs).
- `AppInitializer.onApplicationStart()` sets up `GoogleAuthProvider` with a web client ID. Android passes
  `R.string.default_web_client_id` (generated from `androidApp/google-services.json`); web passes
  `FirebaseWebConfig.GOOGLE_WEB_CLIENT_ID` (committed constants in `webApp` — Firebase web config is public-by-design).
- Firestore paths: `users/{firebaseUid}/progress/{nonogramId}` (progress) and `nonograms/{id}` (puzzles — own + public
  per the `status` column). Puzzles are pulled incrementally by `AuthViewModel.syncAll` in **two independent
  streams** — public and owned — each with its own `updatedAt` cursor persisted via `AuthRepository`
  (`getLast{Public,Owned}NonogramSyncTimestamp`); merge policy is `sync/SyncService.kt` `mergeRemoteNonograms` (remote
  newer → upsert; local newer & locally authored → push back). On both platforms — Android via
  `dev.gitlive:firebase-firestore` (androidMain), web via hand-written Firebase JS SDK externals (webMain), both
  isolated behind `sync/SyncService`. The `nonograms` queries need security rules (read: public or own; write: own)
  and two composite indexes — `(status, updatedAt)`, `(authorUid, updatedAt)` — set up in the Firebase console.
- `auth/PlatformAuth.kt` declares `expect suspend fun firebaseSignOut()`, ending the platform Firebase session —
  `dev.gitlive.firebase.auth.auth.signOut()` on Android, `FirebaseWeb.signOut()` (a new `firebase/auth` `signOut`
  external) on web. `AuthViewModel.signOut()` calls it before `AuthRepository.signOut()`, swallowing failures so local
  sign-out still proceeds if the platform call errors.

## Current State

The generator is implemented end-to-end: `GenListScreen` lists the signed-in user's puzzles, `GenConfScreen` sets the
grid size, `GenScreen` is the tile-drawing board, all driven by the shared `GenViewModel`. Users can create new puzzles
and edit existing ones (with non-destructive resize). See **Navigation → Generator flow** above. On save, `GenViewModel`
runs `Nonogram.isValid` (the `Solver`) to check the puzzle is uniquely solvable — validation is *advisory* (the puzzle
still saves if it fails or the check throws) and only gates whether it may be published as public. Difficulty is still
hardcoded to `EASY` in `GenViewModel` (no selector yet).

Web (js + wasmJs) has persistent OPFS storage plus Google sign-in and Firestore sync via hand-written Firebase JS SDK
externals in `shared/src/webMain` (no gitlive — it doesn't publish wasmJs; see `docs/web-architecture.md` for the
externals pattern, the kmpauth access-token caveat, and the auth-session restore gate).
