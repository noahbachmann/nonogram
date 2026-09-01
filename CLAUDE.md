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
- **`auth/AuthRepository`** — manages auth state (`GUEST` / `SIGNED_IN`), the **user key**, and the onboarding flag via
  `multiplatform-settings`. `initialize()`/`linkFirebaseUser()`/`signOut()` are suspend (they call the suspend `AppSDK`).
  Onboarding flag and per-UID sync cursors survive sign-out untouched.

  **The user key** (`currentUserUid: StateFlow<String?>`, persisted as `current_user_uid`) is the app's *only* identity:
  the Firebase uid once signed in, `"local:<random>"` while a guest. It keys both `Nonogram.authorUid` and
  `UserProgress.userUid`, so the local tables mirror Firestore's own `users/{uid}/progress/{nonogramId}` layout — see
  `docs/identity.md`. `currentFirebaseUid` is the same value projected to null while a guest, for the calls that must
  reach Firestore. The generator is open to guests, hence the local form; guests never push, so it cannot leak.
  `linkFirebaseUser` migrates the guest's data onto the uid (`reassignAuthor` + `mergeProgressInto`, then drops the
  emptied guest row); a key that is already a real uid is never migrated, so signing into a second account cannot take
  the first one's data. `signOut()` mints a fresh guest key and leaves the signed-in row intact, so signing back in
  restores that account's puzzles and progress.
- **`settings/SettingsRepository`** — holds the app's persisted preferences via `multiplatform-settings`: the selected
  `ColorTheme` (key `color_theme`, stores the enum name) and `showNames` (key `show_all_nonogram_names`, default
  `true` — the Settings screen's "Always show names" switch, read by `MenuViewModel`). Same shape as `AuthRepository`:
  reads synchronously in the constructor (no `initialize()` needed), exposes a `StateFlow` per preference, writes
  through on set. See **AppTheme** below.
- **`sync/SyncService`** — interface for syncing *both* progress and the shared `nonograms` collection (push/pull/merge;
  `mergeRemoteNonograms` is the shared merge policy), plus the publish-review calls (`requestPublish`,
  `fetchModerationGate`, `isAdmin`, `pullPendingReviews`, `decideReview` — see `docs/publish-moderation.md`.
  The review queue is a plain `List<Nonogram>`: the author rides along in `authorUid`, so there is no wrapper type;
  pure streak/ban helpers live in `sync/Moderation.kt`). `sync/FirebaseAndroidSyncService` (androidMain)
  implements it with `dev.gitlive:firebase-firestore`; web binds `sync/FirebaseWebSyncService` (webMain), built on
  hand-written Kotlin externals to the Firebase JS SDK in `firebase/` (see `docs/web-architecture.md` for the externals
  pattern and the auth-session gate). Same Firestore shape on both platforms, so data interoperates.
- **`classes/` board + game** — the interactive grid (clues, tiles, pan/zoom, drag-to-draw) is a self-contained Compose
  engine: `Board`/`BoardTransform` (one Canvas for all tiles + a layer-transform pan/zoom model),
  `Game` (win check), `Tile`/`TileState`. Performance-critical and gesture-heavy — see `docs/board-rendering.md`.
- **Desktop widths** — `MAX_CONTENT_WIDTH = 1000.dp` lives in `AppTheme.kt` alongside the palettes, applied as
  `Modifier.widthIn(max = …)` ahead of any `fillMax*` and centred by the screen root's `horizontalAlignment`. App bars
  stay full-bleed with capped content, the Board is deliberately exempt, and `NonogramGrid` picks its column count from
  the available width — see `docs/responsive-layout.md`.
- **`filter/`** — the menu's combined filter/sorter: a pure `FilterSortState` model plus the
  `FilterMenuButton` dropdown hosted in the `TopAppBar`'s `navigationContent` slot. Rows are data —
  a sortable `FilterAttribute` with checkable values, or a standalone `FilterToggle` (the "Personal"
  own-puzzles switch) — so adding one is a list entry in `NonogramFilters.forUser(authorUid)`, a
  function rather than a constant because ownership is user-scoped. A row's `label` is also its id.
  See `docs/menu-filtering.md`.
- **`tutorial/`** — the first-run hint overlay: `TutorialStep` (an enum whose declaration order is
  priority order, carrying the copy), `TutorialRepository` (one `tutorial_seen_<STEP>` boolean per
  step, device-wide so it survives sign-out), `TutorialController` + `Modifier.tutorialAnchor(step)`
  (an anchor registry, since screens own their own app bars), and `TutorialHost`, a `Box` wrapped
  around the `NavHost` in `App.kt` that draws the scrim, the spotlight hole and the blob. Nothing is
  ever placed over the highlighted control — Compose hit-testing stops at the topmost sibling, so the
  anchor itself listens on `PointerEventPass.Initial` without consuming. See `docs/tutorial-overlay.md`.
- **`classes/Solver`** — line-logic solver; run via `Nonogram.isValid` to check a puzzle is uniquely solvable (gates
  publishing). **User-owned and actively changing — do not document its internals or modify it.**
- **`network/NonogramApi`** — an unused stub (bare Ktor `HttpClient`, no callers). Not a live data path.
- **`screens/GoogleSignInSection`** — `expect`/`actual` composable for the Google sign-in button, both actuals built on
  kmpauth's `GoogleSignInButton` + a `SignInState`. Android uses `rememberGoogleAuthState`, which exchanges the
  credential for a Firebase session through kmpauth's own Firebase backend (auto-registered from `kmpauth-firebase`) and
  hands back a `KMPAuthUser`. Web uses `rememberGoogleSignInState` — credential only — and does the exchange itself via
  `FirebaseWeb.signInWithGoogle`, because the web sync gate reads the Firebase JS SDK's auth state (see
  `docs/web-architecture.md`). Both skip `KMPAuthUserCancelledException` rather than logging a dismissed prompt.
- **ViewModels** (`screens/viewModel/`) — Compose state holders using `mutableStateOf`. `GameViewModel` manages the tile
  board and save/sync. `GenViewModel` drives the generator (draw/resize/save + validation). `MenuViewModel` holds the
  nonogram list and progress preview map; it loads via `AppSDK.getVisibleNonograms(uid)` — approved puzzles plus
  whatever the current user key owns — so a signed-out user stops seeing the previous account's puzzles (the rows
  stay in the DB, they are just filtered out). `AuthViewModel` orchestrates login flow and **all remote sync** —
  `syncAll` pulls progress + public + owned nonograms in one pass (separate public/owned cursors read via
  `AuthRepository`) and then refreshes the admin flag and publish ban (`isAdmin` / `publishBanned` StateFlows),
  `retryOwnNonograms` re-runs just the owned stream for the generator's retry button. The **public** stream runs
  first, before the `currentFirebaseUid ?: return@launch` gate, so guests pull public puzzles too — approved
  puzzles are readable unauthenticated (see `firestore.rules`), while progress, owned puzzles and the
  admin/moderation reads all need a session and stay behind the gate. `AdminViewModel` drives the
  admin review queue (one pending request at a time, buffered a batch at a time).
  All depend on the suspend `AppSDK`/`SyncService` from inside `viewModelScope.launch`.

  **When sync runs.** `syncAll` fires once from `AppContent`'s app-start `LaunchedEffect`, and after that
  only when the user pull-to-refreshes the menu (`MenuScreen`'s
  `PullToRefreshBox` → `MenuViewModel.refresh`). Entering `MenuRoute` does **not** sync — it calls
  `MenuViewModel.reload()`, a silent local-DB re-read with no spinner, so puzzles just authored in the
  generator still appear. `MenuViewModel` therefore has two flags: `isLoading` (full-screen spinner, cold
  start and sign-in/sign-out only, via `loadAll()`) and `isRefreshing` (the pull-to-refresh indicator).

### Navigation

Type-safe navigation via `navigation-compose` with `@Serializable` route objects in `navigation/Routes.kt`. Routes:
`LoginRoute`, `MenuRoute`, `GameRoute(nonogramId)`, `SettingsRoute`, `AdminRoute`, plus the generator routes `GenListRoute`,
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

- `di/AppModule.kt` — common singletons: `AppSDK`, `Settings`, `AuthRepository`, `SettingsRepository`, plus all ViewModel
  registrations via `viewModelOf` (koin-core-viewmodel, shared across platforms).
- `di/AndroidModule.kt` — platform bindings: `DatabaseFactory` → `AndroidDatabaseFactory`, `SyncService` →
  `FirebaseAndroidSyncService`.
- `di/WebModule.kt` (webMain) — platform bindings: `DatabaseFactory` → `WebDatabaseFactory`, `SyncService` →
  `FirebaseWebSyncService`.

### Data Model

- **`Nonogram`** — `id`, `difficulty` (enum: EASY/MEDIUM/HARD/HARDCORE), `solution` (List<List<Int>> stored as JSON),
  `name: String?`, `authorUid` (the user key — see `auth/AuthRepository`; the same string as the Firestore
  `authorUid` field, so no local↔remote translation is needed), `updatedAt`, `publishState` (enum: NONE/PENDING/APPROVED/UNLISTED/DENIED, stored as its
  ordinal in the DB column `status`, as its name in the Firestore field `publishStatus`). Visibility is derived, not stored:
  `isPublic get() = publishState == APPROVED`, and the author's on/off switch moves an approved puzzle between
  APPROVED and UNLISTED. Computes `rowClues`/`colClues` on the fly, and `isValid` lazily via the `Solver`.
  Name helpers live alongside: `MAX_NONOGRAM_NAME_LENGTH` (30), `normalizeNonogramName()`, `UNNAMED_NONOGRAM_TITLE`.
  Ownership is `isOwned(uid)`, which never matches the blank `authorUid` seeded puzzles carry.
- **`Tile`** — mutable Compose state. Cycles: NONE → FILLED → CROSSED → NONE.
- Board state is serialized as `List<List<Int>>` (0/1) for persistence and sync.

### SQLDelight

Schema: `shared/src/commonMain/sqldelight/com/trainpaths/nonogram/cache/database.sq`
Migrations: numbered `.sqm` files in the same dir — currently `1.sqm`–`5.sqm` (UserProgress.beat;
NonogramData.authorId + status; NonogramData.updatedAt; NonogramData.name; NonogramData.authorId → authorUid, done by
dropping and recreating the table — local puzzles are wiped and re-pulled, which is also why `AuthRepository` bumped its
sync-cursor key prefixes to `_v2_`). Database name: `NonogramDb`, package:
`com.trainpaths.nonogram.cache`

Tables: `NonogramData`, `User` (`uid` TEXT PK → display name), `UserProgress` (composite PK: userUid + nonogramId).

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
call site must supply one. The active theme lives in `settings/SettingsRepository`/`SettingsViewModel` and is
threaded as a required `App(..., settingsViewModel)` param.

`App()` itself owns the single `AppTheme` call site and the auth-init gate: it reads `settingsViewModel.theme` and
`authViewModel.authState`, wraps everything in `AppTheme(theme)`, and renders `LoadingScreen()` while
`authState == INITIALIZING` instead of the real content (`AppContent`, a private composable with the NavHost). The
two platform entry points (`MainActivity.kt`, `webApp/main.kt`) therefore just call `App(...)` unconditionally — no
`if`/`else`, no `AppTheme` call of their own. `menuViewModelFactory`/`genViewModelFactory` are passed as
`@Composable () -> VM` (matching the existing `gameViewModelFactory` idiom), **not** resolved instances — this is
deliberate, not simplifiable: it keeps `MenuViewModel`/`GenViewModel` construction inside the non-`INITIALIZING`
branch, since `MenuViewModel.init { loadAll() }` needs `AuthRepository.currentUserUid` to already be set.

Use `MaterialTheme.colorScheme.*`, never hardcode hex — outside `AppTheme.kt` itself, where each theme's palette is
defined.

### Firebase / Auth

- Google sign-in via `kmpauth` (`io.github.mirzemehdi:kmpauth-google/firebase`) — `kmpauth-firebase` stays an
  androidMain-only dependency: its wasmJs actual cannot do browser-flow sign-in (gitlive has no wasm target), and
  `webMain` is shared by js+wasmJs, so web exchanges the credential itself. `kmpauth-google`/`kmpauth-uihelper` are
  commonMain (both publish js+wasmJs).
- `AppInitializer.onApplicationStart()` calls `KMPAuth.initialize { google(serverId = …) }` with a web client ID.
  Android passes `R.string.default_web_client_id` (generated from `androidApp/google-services.json`); web passes
  `FirebaseWebConfig.GOOGLE_WEB_CLIENT_ID` (committed constants in `webApp` — Firebase web config is public-by-design).
- Firestore paths: `users/{firebaseUid}/progress/{nonogramId}` (progress), `nonograms/{id}` (puzzles — own + public
  per their `publishStatus` field), `users/{firebaseUid}` (`denialStreak` /
  `publishBanned`) and `admins/{firebaseUid}` (admin roster). Puzzles are pulled incrementally by `AuthViewModel.syncAll` in **two independent
  streams** — public and owned — each with its own `updatedAt` cursor persisted via `AuthRepository`
  (`getLast{Public,Owned}NonogramSyncTimestamp`). The owned cursor is per-uid; the **public** cursor is
  device-wide (no uid suffix), since every user on the device — guests included — sees the same approved set.
  `pullPublicNonogramsSince` therefore takes a nullable uid: null is a guest's unauthenticated pull, and
  `mergeRemoteNonograms` (`sync/SyncService.kt`) then merges without ever pushing back. Merge policy is remote
  newer → upsert; local newer & locally authored → push back. On both platforms — Android via
  `dev.gitlive:firebase-firestore` (androidMain), web via hand-written Firebase JS SDK externals (webMain), both
  isolated behind `sync/SyncService`; the web impl gates every call on `sessionMatches` *except* the public pull,
  which must work signed out. Security rules are checked in at `firestore.rules` — they are what actually enforces
  publish moderation, and the `nonograms` read rule deliberately allows unauthenticated reads of `APPROVED` docs.
  Two composite indexes are needed on `nonograms` — `(publishStatus, updatedAt)` (public pull + review queue) and
  `(authorUid, updatedAt)` (owned pull) — but they are configured in the Firebase console, not checked in.
- `auth/PlatformAuth.kt` declares `expect suspend fun firebaseSignOut()`, ending the platform Firebase session —
  `dev.gitlive.firebase.auth.auth.signOut()` on Android, `FirebaseWeb.signOut()` (a new `firebase/auth` `signOut`
  external) on web. `AuthViewModel.signOut()` calls it before `AuthRepository.signOut()`, swallowing failures so local
  sign-out still proceeds if the platform call errors.

## Current State

The generator is implemented end-to-end: `GenListScreen` lists the signed-in user's puzzles, `GenConfScreen` sets the
grid size, `GenScreen` is the tile-drawing board, all driven by the shared `GenViewModel`. Users can create new puzzles
and edit existing ones (with non-destructive resize). See **Navigation → Generator flow** above. On save, `GenViewModel`
runs `Nonogram.isValid` (the `Solver`) to check the puzzle is uniquely solvable — validation is *advisory* (the puzzle
still saves if it fails or the check throws) and only gates whether the author may *request* publication. Publishing
itself is admin-moderated: the generator's config screen offers a "Request publish" button, an admin accepts or denies
in `AdminScreen` (reachable from Settings), and five denials in a row ban a user from requesting. Editing a puzzle
that is currently public un-publishes it, so every save path first confirms via `PublicEditConfirmDialog`. See
`docs/publish-moderation.md`. Difficulty is still
hardcoded to `EASY` in `GenViewModel` (no selector yet).

Web (js + wasmJs) has persistent OPFS storage plus Google sign-in and Firestore sync via hand-written Firebase JS SDK
externals in `shared/src/webMain` (no gitlive — it doesn't publish wasmJs; see `docs/web-architecture.md` for the
externals pattern, the kmpauth One Tap / token-client caveat, and the auth-session restore gate).
