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

Don't over-document via code comments. When a change is architecturally significant (a new subsystem, a
cross-cutting refactor, a platform port), explain the design in a `.md` file under `docs/` instead — see
`docs/web-architecture.md` for the shape this should take. Code comments stay minimal (one line, only for
genuinely non-obvious *why*).

## Architecture

All shared code lives in `shared/src/commonMain/`, with platform-specific code in `shared/src/androidMain/` and
`shared/src/webMain/` (shared by the `jsMain`/`wasmJsMain` source sets — see `docs/web-architecture.md`).
Platform apps (`androidApp/`, `webApp/`, `iosApp/`) are thin shells that initialize Koin DI and host the Compose UI.

### Layers

- **`AppSDK`** — facade over the database. All data access goes through here; every method is `suspend` (SQLDelight
  `generateAsync` is on so the web worker driver can be async). Builds its `Database` lazily behind a mutex on first
  use so the constructor itself stays synchronous for DI.
- **`cache/Database`** — internal class wrapping SQLDelight-generated `NonogramDb`. Maps DB rows to domain types. Not
  accessed directly outside `AppSDK`.
- **`cache/DatabaseFactory`** — `suspend fun createDriver(): SqlDriver`, implemented per platform: `AndroidDatabaseFactory`
  (androidMain, `Schema.synchronous()`), `WebDatabaseFactory` (webMain, OPFS worker driver + explicit
  `PRAGMA user_version` migration — see `docs/web-architecture.md`), `TestDatabaseFactory` (androidHostTest, in-memory JDBC).
- **`auth/AuthRepository`** — manages auth state (`GUEST` / `SIGNED_IN`), local user ID, and onboarding flag via
  `multiplatform-settings`. Links guest accounts to Firebase UIDs on sign-in. `initialize()`/`linkFirebaseUser()` are
  suspend (call the suspend `AppSDK`).
- **`sync/SyncService`** — interface for progress sync (push/pull/merge). `sync/FirestoreSyncService` (androidMain)
  implements it with `dev.gitlive:firebase-firestore`; web binds `sync/NoOpSyncService` (guest-only web v1 — see
  `docs/web-architecture.md` for the milestone-2 plan to add web sign-in + sync).
- **`screens/GoogleSignInSection`** — `expect`/`actual` composable for the Google sign-in button; Android wires
  `kmpauth-firebase`, web renders nothing in v1.
- **ViewModels** (`screens/viewModel/`) — Compose state holders using `mutableStateOf`. `GameViewModel` manages the tile
  board and save/sync. `MenuViewModel` holds the nonogram list and progress preview map. `AuthViewModel` orchestrates
  login flow and sync-on-start. All depend on the suspend `AppSDK`/`SyncService` from inside `viewModelScope.launch`.

### Navigation

Type-safe navigation via `navigation-compose` with `@Serializable` route objects in `navigation/Routes.kt`. Routes:
`LoginRoute`, `MenuRoute`, `GameRoute(nonogramId)`, `SettingsRoute`, plus the generator routes `GenListRoute`,
`GenConfRoute(editing)`, `GeneratorRoute` (the board editor), plus dialog routes (`PlayDialogRoute`, `WinDialogRoute`,
`LeaveDialogRoute`).

**Generator flow.** `GenConfRoute` is *linked to a specific `GeneratorRoute`* via its `editing` flag:

- **New:** `GenList` → `+New` (`startNew()`) → `GenConf(editing=false)` → *Generate* (`setNonogram`) → `GenScreen`. GenConf's
  "Done" navigates forward to `GeneratorRoute` (`popUpTo(GenListRoute)`); back cancels to the list.
- **Edit:** `GenList` → card (`loadForEdit`) → `GenScreen`. The top-left wrench opens `GenConf(editing=true)` pre-filled with
  the current dims; *Save* applies `resizeNonogram` (preserves overlapping cells, keeps the puzzle id) **and persists via
  `onSave`**, *Back* discards — both `popBackStack()` to the same `GenScreen`.

**Save-state guard.** `GenViewModel.isDirty` tracks unsaved edits (set on tile edit/`resize`, cleared on load/save). Leaving
`GenScreen` toward the list — via the swap button *or* system/predictive back (`NavigationBackHandler` +
`rememberNavigationEventState` from `androidx.navigationevent:navigationevent-compose`, the modern non-deprecated back
API — NOT the deprecated `androidx.compose.ui.backhandler.BackHandler`) — routes through `attemptLeave`:
if dirty it shows `GenSaveConfirmDialog` (Save / Don't save / cancel-by-dismiss) before navigating; otherwise it goes
straight to `GenListRoute`. The board's bottom **Save** button is an explicit save-and-exit.

`NonogramAppBar` navigation icon: GENERATOR mode shows the `build` wrench (used as the "config" affordance in `GenScreen`);
pass `backArrow = true` to force a plain back arrow (used in `GenConf`).

### DI (Koin)

- `di/AppModule.kt` — common singletons: `AppSDK`, `Settings`, `AuthRepository`, plus all ViewModel registrations via
  `viewModelOf` (koin-core-viewmodel, shared across platforms).
- `di/AndroidModule.kt` — platform bindings: `DatabaseFactory` → `AndroidDatabaseFactory`, `SyncService` →
  `FirestoreSyncService`.
- `di/WebModule.kt` (webMain) — platform bindings: `DatabaseFactory` → `WebDatabaseFactory`, `SyncService` →
  `NoOpSyncService`.

### Data Model

- **`Nonogram`** — `id`, `difficulty` (enum: EASY/MEDIUM/HARD/HARDCORE), `solution` (List<List<Int>> stored as JSON),
  `authorId`, `valid`, `status`. Computes `rowClues`/`colClues` on the fly.
- **`Tile`** — mutable Compose state. Cycles: NONE → FILLED → CROSSED → NONE.
- Board state is serialized as `List<List<Int>>` (0/1) for persistence and sync.

### SQLDelight

Schema: `shared/src/commonMain/sqldelight/com/trainpaths/nonogram/cache/database.sq`
Migrations: numbered `.sqm` files in subdirectories (e.g., `1.sqm`, `2/2.sqm`)
Database name: `NonogramDb`, package: `com.trainpaths.nonogram.cache`

Tables: `NonogramData`, `User`, `UserProgress` (composite PK: userId + nonogramId).

`generateAsync` is enabled, so all generated query/transaction code is `suspend`. Sync drivers (Android, JVM tests)
adapt via `NonogramDb.Schema.synchronous()`; the web worker driver uses the async API directly. See
`docs/web-architecture.md` for the per-platform schema init/migration story.

### AppTheme

Defined in `AppTheme.kt`. Material 3 `lightColorScheme`, dark-teal palette.

- `primary` `#153D36` dark teal — background, TopAppBar, main surface
- `onPrimary` white — text/icons on primary
- `secondary` `#C2EFFF` light blue — accents, focused borders, button fills
- `tertiary` `#FFD700` gold — highlights (beat badges, win borders)
- `background` `#153D36` — same as primary, full-screen bg
- `onBackground` white — text on background

Use `MaterialTheme.colorScheme.*`, never hardcode hex. Interactive elements pair `secondary` container + `primary`
content. Text on main background uses `onPrimary`.

### Firebase / Auth

- Google sign-in via `kmpauth` (`io.github.mirzemehdi:kmpauth-google/firebase`) — `kmpauth-firebase` is Android-only
  (no web target published), so it's an androidMain-only dependency; `kmpauth-google`/`kmpauth-uihelper` are
  commonMain (both publish js+wasmJs).
- `AppInitializer.onApplicationStart()` sets up `GoogleAuthProvider` with a web client ID (Android only; web has no
  Google client id needed in guest-only v1).
- Firestore path: `users/{firebaseUid}/progress/{nonogramId}` (Android only — `dev.gitlive:firebase-firestore` has
  no wasmJs variant; androidMain-only dependency, isolated behind `sync/SyncService`).

## Current State

The generator is implemented end-to-end: `GenListScreen` lists the signed-in user's puzzles, `GenConfScreen` sets the
grid size, `GenScreen` is the tile-drawing board, all driven by the shared `GenViewModel`. Users can create new puzzles
and edit existing ones (with non-destructive resize). See **Navigation → Generator flow** above. Difficulty is still
hardcoded to `EASY` in `GenViewModel` (no selector yet), and puzzles aren't validated for a unique solution.

Web (js + wasmJs) is implemented as a guest-only port with persistent OPFS storage — no Google sign-in or Firestore
sync yet (Firebase libraries don't support wasmJs; see `docs/web-architecture.md` and the tracked GitHub issue for
the sign-in/sync follow-up).
