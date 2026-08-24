# Web architecture (js + wasmJs)

## Targets

`shared` and `webApp` both build `js` and `wasmJs`. Kotlin's default source set hierarchy creates a shared
`webMain` (dependsOn'd by both `jsMain` and `wasmJsMain`), so almost all web-specific code lives in `webMain`
and only the two lines that construct the platform `Worker` live in `jsMain`/`wasmJsMain`. **wasmJs is the primary
deployment target** (faster startup, smaller output); `js` stays as a fallback for older browsers.

## Data layer: suspend all the way down

SQLDelight's web worker driver is asynchronous, so `generateAsync.set(true)` is on for the whole schema. This made
`Database`, `AppSDK`, `AuthRepository.initialize()/linkFirebaseUser()`, and `AppInitializer.initializeAuth()`
suspend functions. `AppSDK` builds its `Database` lazily behind a mutex on first use, so the constructor itself stays
synchronous (Koin still does `single { AppSDK(get()) }` without change) and the actual driver/schema creation happens
off the first suspend call.

Each platform owns schema creation differently:

| Platform   | Driver                                                      | Who creates/migrates the schema                                                                                                    |
|------------|-------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Android    | `AndroidSqliteDriver(NonogramDb.Schema.synchronous(), ...)` | the driver, internally, via its create/upgrade callbacks (unchanged behavior)                                                      |
| Test (JVM) | `JdbcSqliteDriver` (in-memory)                              | `TestDatabaseFactory` calls `Schema.synchronous().create(driver)` explicitly                                                       |
| Web        | `WebWorkerDriver` (OPFS-backed)                             | `WebDatabaseFactory` reads `PRAGMA user_version`, then `awaitCreate`/`awaitMigrate` explicitly, then writes the new `user_version` |

Web needs explicit version tracking because OPFS storage persists across page loads and app deploys — unlike sql.js
in-memory setups, a returning user's on-disk schema may be older than the current `NonogramDb.Schema.version`.

Because `AuthRepository.initialize()` is now async, `MenuViewModel`'s `init { loadAll() }` could otherwise read
`currentUserUid` before it's set. Both `MainActivity` (Android) and `main.kt` (web) gate the app behind
`LoadingScreen()` until `AuthViewModel.authState != INITIALIZING`, so ViewModels are only constructed once auth has
resolved.

## Persistence: OPFS via a custom worker

SQLDelight's documented web worker setup uses `sql.js`, which is in-memory only — a reload wipes all data. Instead,
`webApp/src/webMain/resources/sqlite.worker.js` is a small custom worker (per SQLDelight's
[custom worker protocol](https://github.com/sqldelight/sqldelight/blob/master/docs/js_sqlite/custom_worker.md))
built on the official `@sqlite.org/sqlite-wasm` package, using the `opfs-sahpool` VFS:

- No COOP/COEP headers required (unlike the plain OPFS VFS), so the Gradle dev server needs no special config.
- Requires a secure context (`https://` or `localhost`) — both the dev server and any real deployment satisfy this.
- The pool holds an **exclusive lock**: a second tab open at the same time gets no database access. Acceptable for v1;
  would need a `SharedWorker` or a leader-election scheme to fix.
- `webApp/webpack.config.d/sqlite-wasm.js` copies `sqlite3.js`/`sqlite3.wasm` from the npm package next to the compiled
  bundle so the worker's `importScripts("sqlite3.js")` resolves.

`WebDatabaseFactory` (`shared/src/webMain/.../cache/WebDatabaseFactory.kt`) drives the worker through the same
`WebWorkerDriver` SQLDelight uses for its own sql.js reference worker — only the worker script differs.

## Web sign-in + sync (milestone 2)

Neither `kmpauth-firebase` nor `dev.gitlive:firebase-firestore` publish a `wasmJs` variant, so web sign-in/sync is built
without gitlive: `kmpauth-google` (publishes js+wasmJs, commonMain dep) obtains the Google token in the browser, and
**hand-written Kotlin externals to the Firebase JS SDK** (`npm("firebase", ...)` in `shared`'s
`webMain`) do the credential exchange and Firestore I/O. Both v1 seams are now filled:

- **`sync/SyncService`** — web binds `sync/FirebaseWebSyncService` (webMain), which mirrors the androidMain
  `FirebaseAndroidSyncService` method-for-method against the same Firestore shape, so Android and web sync interoperate.
  Two collections: progress (`users/{uid}/progress/{nonogramId}`, fields `boardState: String?` +
  `updatedAt: number`) and the shared `nonograms/{id}` puzzle collection (own + public).
- **`screens/GoogleSignInSection`** — the web actual renders kmpauth's `GoogleButtonUiContainer` +
  `GoogleSignInButton`, exchanges the Google token via `FirebaseWeb.signInWithGoogle`, and feeds the resulting Firebase
  `uid`/`displayName` into the unchanged common login flow.

### The externals pattern (first in this repo)

All bindings live once in `shared/src/webMain/kotlin/.../firebase/` and compile for **both** js and wasmJs (supported
since Kotlin 2.2.20). The rules that make that work:

- Only `JsAny`-family types in external signatures; every `external interface` extends `JsAny`. No `Long`
  (Firestore numbers are doubles — `updatedAt` crosses the boundary as `Double`, `.toLong()` on read).
- `@file:JsModule("firebase/auth")` etc. at file level; `@OptIn(ExperimentalWasmJsInterop::class)` per file.
- `@JsModule`-only externals can't link under UMD, so **both `shared` and `webApp` set `js { useEsModules() }`**
  (wasmJs is ESM anyway; webpack bundles either).
- `js(...)` is unavailable in a shared source set. Plain JS objects (Firebase config, Firestore write payloads)
  are built via a global `external object JSON { fun parse(...) }` + kotlinx-serialization `buildJsonObject`.
- `Promise<T : JsAny?>.await()` comes from kotlinx-coroutines ≥ 1.11, which ships it in its shared web fragment.
- Statics like `GoogleAuthProvider.credential(...)` are bound as an `external object`.
- `QuerySnapshot` is consumed via `.empty` / `.forEach(callback)` instead of `.docs`, sidestepping the js-vs-wasm
  `JsArray` API divergence.

`firebase/FirebaseWeb.kt` is the facade: everything outside the `firebase` package (sync service, sign-in UI,
`webApp/main.kt`) talks only to it, so if shared externals ever regress the bindings can move per-target without
touching callers.

### Auth/session details

- **kmpauth token caveat:** kmpauth-google's web implementation uses the GIS *token client* (implicit flow), so
  `GoogleUser.idToken` is usually empty in the browser and `accessToken` is populated. The sign-in actual passes both
  (blank-filtered) to `GoogleAuthProvider.credential(idToken?, accessToken?)` — Firebase accepts either. kmpauth injects
  the GIS script itself; `index.html` needs no change.
- **Session restore gate:** on page reload the app trusts the local SQL `User.firebaseUid`, but the Firebase JS session
  restores asynchronously from indexedDB. Every Firestore op in `FirebaseWebSyncService` first awaits
  `auth.authStateReady()` and verifies the live uid matches — otherwise it logs and no-ops instead of hitting a
  guaranteed permission-denied.
- **Config:** `webApp/.../FirebaseWebConfig.kt` holds committed constants (Firebase web config + the Google web OAuth
  client id). These are public-by-design — they ship in every JS bundle; security comes from Firestore rules and the
  OAuth **Authorized JavaScript origins** allowlist (each dev/prod origin must be listed there; note js and wasmJs dev
  servers on different ports are different origins with separate indexedDB sessions).
  `main.kt` calls `FirebaseWeb.initialize(...)` and `AppInitializer.onApplicationStart(clientId)` before Koin.

## Dead code note

`network/NonogramApi.kt` is never instantiated anywhere in the app — no web Ktor engine (`ktor-client-js`) was added
because there's currently nothing that needs it.
