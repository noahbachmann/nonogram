# Web architecture (js + wasmJs)

## Targets

`shared` and `webApp` both build `js` and `wasmJs`. Kotlin's default source set hierarchy creates a shared
`webMain` (dependsOn'd by both `jsMain` and `wasmJsMain`), so almost all web-specific code lives in `webMain`
and only the two lines that construct the platform `Worker` live in `jsMain`/`wasmJsMain`. **wasmJs is the
primary deployment target** (faster startup, smaller output); `js` stays as a fallback for older browsers.

## Data layer: suspend all the way down

SQLDelight's web worker driver is asynchronous, so `generateAsync.set(true)` is on for the whole schema. This
made `Database`, `AppSDK`, `AuthRepository.initialize()/linkFirebaseUser()`, and `AppInitializer.initializeAuth()`
suspend functions. `AppSDK` builds its `Database` lazily behind a mutex on first use, so the constructor itself
stays synchronous (Koin still does `single { AppSDK(get()) }` without change) and the actual driver/schema
creation happens off the first suspend call.

Each platform owns schema creation differently:

| Platform | Driver | Who creates/migrates the schema |
|---|---|---|
| Android | `AndroidSqliteDriver(NonogramDb.Schema.synchronous(), ...)` | the driver, internally, via its create/upgrade callbacks (unchanged behavior) |
| Test (JVM) | `JdbcSqliteDriver` (in-memory) | `TestDatabaseFactory` calls `Schema.synchronous().create(driver)` explicitly |
| Web | `WebWorkerDriver` (OPFS-backed) | `WebDatabaseFactory` reads `PRAGMA user_version`, then `awaitCreate`/`awaitMigrate` explicitly, then writes the new `user_version` |

Web needs explicit version tracking because OPFS storage persists across page loads and app deploys — unlike
sql.js in-memory setups, a returning user's on-disk schema may be older than the current `NonogramDb.Schema.version`.

Because `AuthRepository.initialize()` is now async, `MenuViewModel`'s `init { loadAll() }` could otherwise read
`currentUserId` before it's set. Both `MainActivity` (Android) and `main.kt` (web) gate the app behind
`LoadingScreen()` until `AuthViewModel.authState != INITIALIZING`, so ViewModels are only constructed once
auth has resolved.

## Persistence: OPFS via a custom worker

SQLDelight's documented web worker setup uses `sql.js`, which is in-memory only — a reload wipes all data.
Instead, `webApp/src/webMain/resources/sqlite.worker.js` is a small custom worker (per SQLDelight's
[custom worker protocol](https://github.com/sqldelight/sqldelight/blob/master/docs/js_sqlite/custom_worker.md))
built on the official `@sqlite.org/sqlite-wasm` package, using the `opfs-sahpool` VFS:

- No COOP/COEP headers required (unlike the plain OPFS VFS), so the Gradle dev server needs no special config.
- Requires a secure context (`https://` or `localhost`) — both the dev server and any real deployment satisfy this.
- The pool holds an **exclusive lock**: a second tab open at the same time gets no database access. Acceptable
  for v1; would need a `SharedWorker` or a leader-election scheme to fix.
- `webApp/webpack.config.d/sqlite-wasm.js` copies `sqlite3.js`/`sqlite3.wasm` from the npm package next to the
  compiled bundle so the worker's `importScripts("sqlite3.js")` resolves.

`WebDatabaseFactory` (`shared/src/webMain/.../cache/WebDatabaseFactory.kt`) drives the worker through the same
`WebWorkerDriver` SQLDelight uses for its own sql.js reference worker — only the worker script differs.

## Guest-only v1 and the Firebase seams

Neither `kmpauth-firebase` nor `dev.gitlive:firebase-firestore` publish a `wasmJs` variant (firestore doesn't
even publish `js`+sign-in together in a way that works without gitlive on wasm). Rather than block the whole
web port on that, v1 ships **guest-only** on web, with two seams ready for a follow-up:

- **`sync/SyncService`** — interface with the five sync methods `AuthViewModel`/`GameViewModel` already called.
  `sync/FirestoreSyncService` (androidMain) implements it with gitlive+Firestore, unchanged behavior. Web binds
  `sync/NoOpSyncService` (webMain) — unreachable in guest mode, but keeps DI symmetric.
- **`screens/GoogleSignInSection`** — `expect`/`actual` composable. Android's actual is the original
  `GoogleButtonUiContainerFirebase` + `GoogleSignInButton` block moved out of `LoginScreen`. Web's actual
  renders nothing; `LoginScreen` on web only shows "Continue as Guest".

See the GitHub issue for the milestone-2 design (kmpauth-google web ID token + hand-written Kotlin externals
to the Firebase JS SDK, implementing `FirebaseWebSyncService : SyncService` and filling in the web actual of
`GoogleSignInSection` — avoiding gitlive-firebase entirely so both `js` and `wasmJs` get sign-in + sync).

## Dead code note

`network/NonogramApi.kt` is never instantiated anywhere in the app — no web Ktor engine (`ktor-client-js`) was
added because there's currently nothing that needs it.
