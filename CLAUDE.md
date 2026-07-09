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

## Architecture

All shared code lives in `shared/src/commonMain/`. Platform apps (`androidApp/`, `webApp/`, `iosApp/`) are thin shells
that initialize Koin DI and host the Compose UI.

### Layers

- **`AppSDK`** — facade over the database. All data access goes through here. ViewModels and sync service depend on it.
- **`cache/Database`** — internal class wrapping SQLDelight-generated `NonogramDb`. Maps DB rows to domain types. Not
  accessed directly outside `AppSDK`.
- **`auth/AuthRepository`** — manages auth state (`GUEST` / `SIGNED_IN`), local user ID, and onboarding flag via
  `multiplatform-settings`. Links guest accounts to Firebase UIDs on sign-in.
- **`sync/FirestoreSyncService`** — bidirectional Firestore sync for user progress. Uses
  `dev.gitlive:firebase-firestore` (KMP wrapper). Merge strategy: last-write-wins by `updatedAt` timestamp.
- **ViewModels** (`screens/viewModel/`) — Compose state holders using `mutableStateOf`. `GameViewModel` manages the tile
  board and save/sync. `MenuViewModel` holds the nonogram list and progress preview map. `AuthViewModel` orchestrates
  login flow and sync-on-start.

### Navigation

Type-safe navigation via `navigation-compose` with `@Serializable` route objects in `navigation/Routes.kt`. Routes:
`LoginRoute`, `MenuRoute`, `GameRoute(nonogramId)`, `SettingsRoute`, plus the generator routes `GenListRoute`,
`GenConfRoute(editing)`, `GeneratorRoute` (the board editor), plus dialog routes (`PlayDialogRoute`, `WinDialogRoute`,
`LeaveDialogRoute`).

**Generator flow.** `GenConfRoute` is *linked to a specific `GeneratorRoute`* via its `editing` flag:

- **New:** `GenList` → `+New` (`startNew()`) → `GenConf(editing=false)` → *Generate* (`setNonogram`) → `GenScreen`. GenConf's
  "Done" navigates forward to `GeneratorRoute` (`popUpTo(GenListRoute)`); back cancels to the list.
- **Edit:** `GenList` → card (`loadForEdit`) → `GenScreen`. The top-left wrench opens `GenConf(editing=true)` pre-filled with
  the current dims; *Save* (`resizeNonogram`, preserves overlapping cells, keeps the puzzle id) or *Back* both
  `popBackStack()` to the same `GenScreen`.

`NonogramAppBar` navigation icon: GENERATOR mode shows the `build` wrench (used as the "config" affordance in `GenScreen`);
pass `backArrow = true` to force a plain back arrow (used in `GenConf`).

### DI (Koin)

- `di/AppModule.kt` — common singletons: `AppSDK`, `Settings`, `AuthRepository`, `FirestoreSyncService`
- `di/AndroidModule.kt` — platform bindings: `DatabaseFactory` implementation, ViewModel registration via `viewModelOf`

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

- Google sign-in via `kmpauth` (`io.github.mirzemehdi:kmpauth-google/firebase`)
- `AppInitializer.onApplicationStart()` sets up `GoogleAuthProvider` with a web client ID
- Firestore path: `users/{firebaseUid}/progress/{nonogramId}`

## Current State

The generator is implemented end-to-end: `GenListScreen` lists the signed-in user's puzzles, `GenConfScreen` sets the
grid size, `GenScreen` is the tile-drawing board, all driven by the shared `GenViewModel`. Users can create new puzzles
and edit existing ones (with non-destructive resize). See **Navigation → Generator flow** above. Difficulty is still
hardcoded to `EASY` in `GenViewModel` (no selector yet), and puzzles aren't validated for a unique solution.
