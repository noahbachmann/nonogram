# Production Firebase setup → signed release AAB

How to finish wiring the production Firebase project, split the build into `dev`/`prod` environments, turn on
App Check, and get to an uploadable `.aab`.

> **Background only. For what is still to be done, use `docs/prod-release-checklist.md`.** Parts B and E are
> applied and the status notes below ("prod has no Android OAuth client", "App Check does not exist anywhere
> in the repo", the Part A table) are stale. What remains valid here is the reasoning behind each choice.

## The two environments

| | Project | Project number | State today |
|---|---|---|---|
| **prod** | `nonogram-trainpaths` | `482051008739` | Android + web apps registered, Google sign-in on, config in `androidApp/src/prod/` and `webApp/src/prod/`. **No Android OAuth client yet** — no `"client_type": 1` entry, so sign-in fails at runtime until C3. |
| **dev** | `nonogram-ba791` | `83667943466` | Android + web apps registered, Google sign-in on, debug SHA-1 in (its `google-services.json` carries a `"client_type": 1` entry). Still to confirm: Firestore rules and the two composite indexes (A2). |

Both sets of credentials get committed, and the build picks one — Android by product flavor (`dev`/`prod`),
web by the Gradle property `nonogram.env`. Part E is where that lands.

**The rule this repo follows: debug against dev, build for prod.** Development, debugging and day-to-day work
happen on the dev project; the prod project exists to be built and shipped from. That is what the switch is
*for*, and it is why the App Check provider sits on the flavor axis (E5) rather than the build-type axis:

| Variant | Used for |
|---|---|
| `devDebug` | everything — development, debugging, running from the IDE |
| `devRelease` | local R8 smoke test before a release (Part F) |
| `prodRelease` | the artifact uploaded to Play |
| `prodDebug` | **not used.** Available before App Check enforcement, and the one-time setup checks below lean on it; after D4 it cannot reach Firestore, by design |

Four things still have to be fixed along the way:

1. **`nonogram-ba791` may still be missing its Firestore setup.** The Android app is registered and its
   `google-services.json` is in the tree, so the app builds and signs in against dev. What has not been
   confirmed is A2: the security rules and the two composite indexes. Without the indexes the puzzle pulls
   fail at runtime with a "query requires an index" error; without the rules the moderation model is not
   enforced there at all.
2. **Prod has no Android OAuth client.** The current `google-services.json` *does* now carry a
   `"client_type": 3` (web) entry, so `R.string.default_web_client_id` resolves and the build compiles — that
   part is fixed. But there is no `"client_type": 1` entry, which means **no SHA-1 is registered for the
   Android app**. The build succeeds and Google sign-in then fails at runtime (`ApiException: 10`,
   `DEVELOPER_ERROR`). Part C3 is what closes this.
3. **The release build type is signed with the debug key** — `signingConfig = signingConfigs.getByName("debug")`
   in `androidApp/build.gradle.kts`. Play rejects debug-signed bundles. There is no keystore and no
   `keystore.properties`.
4. **Firestore rules are not tracked in this repo, deliberately** — they are maintained in the Firebase console,
   per project. `CLAUDE.md` still claims the file is checked in; that is stale and gets corrected in E8. Those
   rules are what actually enforce publish moderation, so neither project may go live without them — see E7 for
   where to get the last known-good copy.

App Check does not exist anywhere in the repo yet: no dependency, no init, no reCAPTCHA key.

---

## Order of operations

The parts below are grouped by topic, but one dependency forces the sequence: **the Play app signing
certificate does not exist until your first upload**, and App Check's Play Integrity provider needs its
SHA-256. So:

1. **A** — bring `nonogram-ba791` up to a full dev environment (Android app, Firestore + rules + indexes) and
   fill prod's remaining gaps.
2. **B** — generate the upload keystore, wire `signingConfigs`.
3. **C1 / C3** — register debug + upload fingerprints in **both** projects, download both
   `google-services.json`.
4. **E1–E4** — add the env switch, the `dev`/`prod` flavors and the two `google-services.json`, the two
   `FirebaseWebConfig.kt`, and gitignore the keystore. *(App is now buildable on both environments and
   sign-in works.)*
5. **E5–E6** — add the App Check client code (still unenforced, so nothing can break yet).
6. **D2 / D3** — register the web reCAPTCHA key and the Android debug token; verify locally.
7. **F** — build and upload the AAB to internal testing.
8. **C2 → C3** — now grab the app signing SHA-1 + SHA-256 from Play Console and add them to Firebase.
9. **D1** — link the Cloud project in Play Console, register Play Integrity in App Check.
10. **D4** — watch the App Check metrics, then enforce Firestore, then Auth.

---

## Part A — Bring both Firebase projects up to the same shape

Console work. This is one checklist run **twice** — once per project — because dev and prod are independent
Firebase projects that share nothing. Neither is a fresh start:

| Step | `nonogram-trainpaths` (prod) | `nonogram-ba791` (dev) |
|---|---|---|
| A1 project exists | ✅ | ✅ |
| A2 Firestore + rules + indexes | verify (rules and both indexes) | verify — most likely still to do |
| A3 Android app registered | ⚠️ registered, but **no Android OAuth client** — needs C3 | ✅ registered, debug SHA-1 in |
| A4 Web app registered | ✅ | ✅ |
| A5 Google sign-in enabled | ✅ (a web OAuth client exists) | ✅ (a web OAuth client exists) |

Within each project, do the steps in order; later ones depend on IDs produced by earlier ones.

### A1. The project

Both already exist, so there is nothing to create. If you ever do add a third: **Add project** at
<https://console.firebase.google.com>, and skip Google Analytics — the app uses no `measurementId`.

Keep the two straight in the console's project switcher; every following step is silently wrong if done in the
other project.

### A2. Firestore

1. **Build → Firestore Database → Create database** — if the project already has one, skip to step 3 and
   *verify* rather than create.
2. Pick **Production mode** (locked rules; you replace them in step 4) and a **location** — this is permanent.
   `eur3 (europe-west)` is the sane pick for a European audience. The two projects do not have to share a
   location, and dev's does not matter much.
3. Paste your own copy of the rules into **Firestore → Rules → Publish** (E7 says where to get it — they are
   not tracked in this repo). Do not ship with test-mode rules: the moderation model (`admins/{uid}`,
   `publishBanned`, the `publishStatus` transition rules) is enforced *only* there.
4. Create the two composite indexes on `nonograms` under **Firestore → Indexes → Composite → Create index**:
   - `publishStatus` Ascending, `updatedAt` Ascending — the public pull and the review queue
   - `authorUid` Ascending, `updatedAt` Ascending — the owned pull

   Without them `pullPublicNonogramsSince` / `pullOwnNonogramsSince` fail at runtime with a
   "The query requires an index" error carrying a create-link.
5. Seed the admin roster by hand once you have signed in for the first time: create collection `admins`,
   document ID = your Firebase UID, any field (the rules only check `exists()`). Client writes are denied by
   rule, so this must be done in the console. **Your uid differs between the two projects** — the same Google
   account gets a different Firebase uid per project, so this is two separate `admins` documents.

### A3. Register the Android app

**Done for dev.** Prod's app exists but has no OAuth client of its own, so its fingerprint work in C3 is still
outstanding (see blocker 2). Kept here as the reference for how dev's was produced.

**Project settings → General → Your apps → Add app → Android.**

- **Package name:** `com.trainpaths.nonogram` (must match `applicationId` exactly). Both flavors use the same
  applicationId — that is why one package name is registered in two different projects rather than two package
  names in one.
- **Debug signing certificate SHA-1:** leave blank for now — all fingerprints go in at Part C.
- Download `google-services.json` when prompted, but **do not use it yet**; without a registered fingerprint it
  has no `"client_type": 1` entry, so sign-in would still fail. Re-download after C3.

### A4. Register the Web app

Both projects already have one, so this is reference material — skip unless something is missing.

**Project settings → General → Your apps → Add app → Web** (`</>`).

- Nickname `nonogram-web`. **Do not** enable Firebase Hosting from this dialog unless you actually intend to
  host there.
- Copy the config snippet. You need `apiKey`, `authDomain`, `projectId`, `messagingSenderId`, `appId` —
  exactly the five values `FirebaseWeb.initialize` takes. These go into that environment's
  `FirebaseWebConfig.kt` (Part E3).

### A5. Authentication

Google sign-in is already enabled in both projects — each has an auto-created web OAuth client, which is the
proof. Steps 1–2 are reference; **steps 3–5 still need doing**, at least for dev.

1. **Build → Authentication → Get started → Sign-in method → Google → Enable.**
2. Set a **public-facing name** and a **support email**, then Save. Enabling Google here is what creates the
   **Web client (auto created by Google Service)** OAuth client — the one whose ID becomes
   `default_web_client_id` on Android and `googleWebClientId` on web. Nothing works before this step.
3. **Authentication → Settings → Authorized domains**: `localhost` is there by default. Add the domain you will
   serve the web build from.
4. **Google Cloud console → APIs & Services → Credentials** (same project): open the auto-created **Web client**.
   Under **Authorized JavaScript origins** add every origin the web app runs on:
   - `http://localhost:8080` and any other dev-server port — the `js` and `wasmJs` dev servers bind different
     ports, and different ports are different origins with separate indexedDB sessions
     (`docs/web-architecture.md`)
   - your production web origin, `https://…` — prod only; dev only ever runs on localhost

   Under **Authorized redirect URIs** add `https://<project-id>.firebaseapp.com/__/auth/handler`
   (`nonogram-trainpaths` or `nonogram-ba791`, matching the project you are in).
   Copy this client's **Client ID** — that is the value for `GOOGLE_WEB_CLIENT_ID` in the environment's
   `FirebaseWebConfig.kt` (E3). For prod it is the `482051008739-hc89i851oq…` client already in the tree;
   for dev it is `83667943466-r9ptgubthqn…`, recoverable from git history (E3).
5. **Google Cloud console → Google Auth Platform → Audience**: while the consent screen is in *Testing*, only
   listed test users can sign in and everyone sees an "unverified app" screen. The app requests only
   `openid`/`email`/`profile`, which are non-sensitive scopes, so **Publish app** takes effect immediately and
   needs no Google verification. Fill in **Branding** (app name, support email, developer contact) first.
   Only prod needs publishing — dev can stay in *Testing* with your own account as a test user.

---

## Part B — Create the upload keystore and wire release signing

> **Status: the Gradle half is applied.** `androidApp/build.gradle.kts` now reads `keystore.properties` and
> uses a real `release` signing config. The keystore itself and `keystore.properties` are yours to create —
> until they exist, release variants fail at signing while debug builds are unaffected.

Generate the keystore **outside the repo** and back it up. Losing it means losing the ability to update the app
(recoverable only through Play's upload-key reset flow, and only because Play App Signing holds the real
signing key).

```bash
keytool -genkeypair -v \
  -keystore ~/keystores/nonogram-upload.jks \
  -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

On Windows the same command works from PowerShell with the JDK's `keytool` on PATH; put the file somewhere like
`%USERPROFILE%\keystores\nonogram-upload.jks`.

Then create `keystore.properties` at the **project root** (gitignored — see E4):

```properties
storeFile=/absolute/path/to/nonogram-upload.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

And change `androidApp/build.gradle.kts`:

```kotlin
import java.util.Properties

val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        create("release") {
            keystoreProperties.getProperty("storeFile")?.let { storeFile = file(it) }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            // … existing minify/shrink/proguard lines stay …
            signingConfig = signingConfigs.getByName("release")   // was: getByName("debug")
        }
    }
}
```

Guarding on `f.exists()` keeps `:shared` tests and CI (`.github/workflows/tests.yml`, which never provisions a
keystore) working — only the release variants need the file.

Signing configs attach to the **build type**, not the flavor, so this one block covers both `devRelease` and
`prodRelease` once the flavors from E2 exist. That is deliberate: it means an `installDevRelease` smoke test
exercises the same R8 + signing path the uploaded bundle will.

---

## Part C — Fingerprints: which SHA goes where

This is the part that trips everyone up. There are **three** certificates in play and **two** hash algorithms,
and they are not interchangeable.

| Certificate | Where it lives | What it signs | Needed for |
|---|---|---|---|
| **Debug key** | `~/.android/debug.keystore` (alias `androiddebugkey`, password `android`) | every `*Debug` variant | Google sign-in while developing (**SHA-1**) |
| **Upload key** | the `.jks` you just made | the AAB you upload to Play | Google sign-in on locally installed release builds (**SHA-1**); Play verifies your uploads with it |
| **App signing key** | held by Google, generated by Play App Signing | the APKs Play actually delivers to users | Google sign-in **and** App Check in production (**SHA-1 + SHA-256**) |

Rules of thumb:

- **Google Sign-In needs SHA-1.** Firebase creates one OAuth *Android* client per SHA-1 you register.
- **App Check / Play Integrity needs SHA-256** of the certificate the installed app is actually signed with —
  in production that is the **app signing key**, not your upload key.
- Register **both SHA-1 and SHA-256** for every certificate. It costs nothing and avoids a second round trip.

### C1. Debug and upload fingerprints (local)

Gradle prints all of them for every variant — with the flavors from E2 that is four rows (`devDebug`,
`devRelease`, `prodDebug`, `prodRelease`), but only two distinct certificates, since the flavor does not
change the signing config:

```bash
./gradlew :androidApp:signingReport
```

Or directly:

```bash
# debug
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android
# upload
keytool -list -v -alias upload -keystore ~/keystores/nonogram-upload.jks
```

### C2. App signing key fingerprint (Play Console)

**This is not available yet.** The Play Console app exists but no release has been uploaded — with Play App
Signing and a Google-generated key, the app signing certificate is **created at your first upload**. So: build
and upload the AAB first (Part F), then come back here.

Once a bundle has been uploaded to any track (internal testing is enough):

**Play Console → your app → Test and release → Setup → App integrity → Play app signing** tab. Two panels:

- **App signing key certificate** — MD5 / SHA-1 / SHA-256. ← the production one.
- **Upload key certificate** — MD5 / SHA-1 / SHA-256. Should match what `signingReport` printed.

### C3. Register them in Firebase — in **both** projects

Both flavors ship the same `applicationId` and are signed by the same certificates, so the *same* fingerprints
go into `nonogram-trainpaths` **and** `nonogram-ba791`. This is the step that fixes blocker 2.

**Project settings → General → Your apps → Android app → Add fingerprint.** Add, one at a time:

| Fingerprint | prod | dev |
|---|---|---|
| debug SHA-1 | ✅ | ✅ |
| upload SHA-1 + SHA-256 | ✅ | optional — only if you sideload `installDevRelease` and want sign-in there |
| app signing SHA-1 + SHA-256 (after C2) | ✅ | ❌ never — that certificate only signs the Play-delivered prod app |

Then **Download google-services.json** again from each project — prod's to `androidApp/src/prod/`, dev's to
`androidApp/src/dev/` (E2). Verify each now has:

- at least one `"client_type": 3` (web) entry — that is what generates `default_web_client_id`, and prod
  already has it;
- at least one `"client_type": 1` (Android) entry carrying a `certificate_hash` — **this is the new one**, and
  its absence is exactly why sign-in currently fails on Android.

```bash
for e in dev prod; do
  echo "$e:"
  grep -c '"client_type": 3' androidApp/src/$e/google-services.json   # >= 1
  grep -c '"client_type": 1' androidApp/src/$e/google-services.json   # >= 1
done
```

> Adding a fingerprint later is a **server-side** change. You do **not** need to rebuild or re-upload the AAB
> for sign-in to start working — the app never reads the fingerprint list at runtime. Re-downloading
> `google-services.json` only matters when the *web client ID* changes, i.e. the first time.

---

## Part D — App Check

App Check attests that requests come from your genuine app, and is enforced per Firebase product. This app uses
**Cloud Firestore** and **Firebase Authentication**, so those are the two to enforce.

Sequencing rule: **ship clients that send App Check tokens before you turn enforcement on.** Enforcement with no
token-sending client = every request fails.

**All of Part D is prod-only.** Register nothing in `nonogram-ba791` and leave it unenforced: dev's
`recaptchaSiteKey` stays empty, which makes the web client skip App Check init entirely (E6), and a sideloaded
`devRelease` would fail Play Integrity attestation anyway. Dev's protection is that nothing points at it but
your own machine.

### D1. Console — Android (Play Integrity)

1. **Play Console → your app → Test and release → App integrity → Play Integrity API → Link Cloud project** →
   pick the Google Cloud project that backs your prod Firebase project (same project ID).
2. Make sure the **app signing key SHA-256** is registered on the Firebase Android app (C3).
3. **Firebase console → Build → App Check → Apps tab** → your Android app → **Play Integrity** → Register.
4. Optionally set a token TTL (30 min – 7 days; the default 1 hour is fine).

Caveat: the built-in Play Integrity provider **only works for apps distributed by Google Play**. A release APK
sideloaded from your own machine will fail attestation — that is expected, and is what the debug provider in D3
is for.

### D2. Console — Web (reCAPTCHA v3)

1. **Firebase console → App Check → Apps tab** → your Web app → **reCAPTCHA v3** → Register. Firebase either
   creates a site key for you or asks you to paste one from <https://www.google.com/recaptcha/admin> (choose
   **reCAPTCHA v3**, not v2 or Enterprise — v3 is invisible, free, and is what the JS SDK's `ReCaptchaV3Provider`
   expects).
2. In the reCAPTCHA admin, the key's **domain list** must include `localhost` plus your production web domain.
3. Copy the **site key** — it is public and goes into `FirebaseWebConfig.kt`.

### D3. Debug tokens (so local development keeps working)

- **Android: not applicable, by design.** `DebugAppCheckProviderFactory` is bound to the **dev flavor** (E5),
  and the dev project runs no App Check, so its debug secret has nothing to register against. Every build that
  talks to prod uses Play Integrity, which means only the Play-distributed build reaches prod once enforced —
  the debug-on-dev / build-for-prod rule, enforced by the build rather than by discipline. Nothing to do here.
- **Web on localhost** sets `globalThis.FIREBASE_APPCHECK_DEBUG_TOKEN = true`, which makes the SDK print a
  debug token to the browser console. Copy it into
  **App Check → Apps → Web app → ⋮ → Manage debug tokens → Add debug token**. This one *does* apply: the web
  client gates on `RECAPTCHA_SITE_KEY` being non-blank, so a localhost run against the prod config still
  initializes App Check.

Debug tokens bypass attestation entirely — register only your own machines, and revoke them when done.

### D4. Enforcement rollout

1. Ship the clients with App Check initialized (Part E) — Android to internal testing, web to your host.
2. Leave both products **unenforced** and watch **App Check → APIs tab** for a day or two. Each product shows
   the verified / unverified / outdated-client request split.
3. When the verified share is essentially 100 %, click **Enforce** on **Cloud Firestore** first, then
   **Firebase Authentication**. Auth last, because a mistake there locks out sign-in on every platform at once.
4. Enforcement takes effect within minutes and is reversible.

---

## Part E — Repo changes

> **Status: applied.** E1–E6 and E8 are in the working tree already, unverified by a build. Both
> `google-services.json` files are in place. One thing is still open because it needs console work: prod's
> `RECAPTCHA_SITE_KEY`, blank in `webApp/src/prod/…/FirebaseWebConfig.kt` until D2. Read the sections below as
> the record of *why* each change looks the way it does.

Both environments' credentials live in the repo; the build picks one. The two platforms use different
mechanisms, because Android already has a variant system and the web target does not:

| Platform | Selected by | Dev | Prod |
|---|---|---|---|
| Android | build variant | `:androidApp:assembleDevDebug` | `:androidApp:bundleProdRelease` |
| Web | Gradle property `nonogram.env` | default | `-Pnonogram.env=prod` |

### E1. The environment switch

Add the committed default to `gradle.properties`:

```properties
# Firebase environment for the web build: dev | prod. Android switches by product flavor instead.
nonogram.env=dev
```

So a bare `:webApp:wasmJsBrowserDevelopmentRun` always hits dev, and prod is opt-in per invocation. Only the
web build reads this — Android ignores it entirely.

Both environments are fully populated, so either default builds. `nonogram.env` stays `dev` because that is
the variant you develop in.

### E2. Android — two `google-services.json`, selected by flavor

`androidApp/build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "env"
    productFlavors {
        create("dev") { dimension = "env" }
        create("prod") { dimension = "env" }
    }
}
```

`applicationId` stays in `defaultConfig`, unsuffixed, so both flavors install as `com.trainpaths.nonogram` and
replace each other on a device. `dev` sorts first and is therefore the IDE's default variant. `:shared`
declares no flavors, so no `missingDimensionStrategy` is needed.

Then move the files. The one in the tree today is **prod** (`nonogram-trainpaths`), and it is currently
untracked because `.gitignore` still excludes it — so this is a plain `mv`, not a `git mv`:

```bash
mkdir -p androidApp/src/dev androidApp/src/prod
mv androidApp/google-services.json androidApp/src/prod/google-services.json
# dev's download from nonogram-ba791  →  androidApp/src/dev/google-services.json
```

Both are in place. Prod's still needs re-downloading after C3 — the copy that was moved predates its Android
OAuth client, so it has no `"client_type": 1` entry and sign-in will fail on device until then. Dev's already
has one.

The google-services plugin resolves the file per variant, searching in order — for `devDebug`:
`src/devDebug/`, `src/dev/`, `src/debug/`, then the module root. `src/<flavor>/` is the hook being used here.
**The module-root copy must actually be gone**, not merely stale: it is the last fallback and would be picked
up silently for any variant whose flavor directory is missing.

Sanity check both:

```bash
for e in dev prod; do
  grep -o '"project_id": "[^"]*"' androidApp/src/$e/google-services.json
done
# expect: nonogram-ba791 for dev, nonogram-trainpaths for prod
```

(The `client_type` check that matters is in C3 — it is about fingerprints, not placement.)

Then clear the stale generated resources from the pre-flavor layout:

```bash
./gradlew :androidApp:clean
```

### E3. Web — one `FirebaseWebConfig.kt` per environment

The web target has no flavors, so the environment picks a **source directory** instead. Two ordinary Kotlin
files, each declaring the same object, and one line of Gradle to add the selected one:

```
webApp/src/dev/kotlin/com/trainpaths/nonogram/FirebaseWebConfig.kt   ← nonogram-ba791
webApp/src/prod/kotlin/com/trainpaths/nonogram/FirebaseWebConfig.kt  ← nonogram-trainpaths
```

```kotlin
// webApp/build.gradle.kts
val nonogramEnv = providers.gradleProperty("nonogram.env").getOrElse("dev")

kotlin {
    sourceSets {
        webMain {
            kotlin.srcDir("src/$nonogramEnv/kotlin")
            dependencies { /* … unchanged … */ }
        }
    }
}
```

(`webMain.dependencies { … }` becomes `webMain { dependencies { … } }`, since the source set now needs
configuring for more than its dependencies.)

The old `webApp/src/webMain/kotlin/.../FirebaseWebConfig.kt` is deleted — it is replaced by these two, not by
anything generated. `main.kt` is untouched: same package, same object, same constant names, so only one of the
two is ever on the compile path and the caller cannot tell which.

Both sets of values already exist, so neither file needs the console:

- **prod** — the six constants that were in `FirebaseWebConfig.kt` in the working tree.
- **dev** — the six the same file held *before* the switch to prod, still in git history:

  ```bash
  # while the switch to prod is still uncommitted:
  git show HEAD:webApp/src/webMain/kotlin/com/trainpaths/nonogram/FirebaseWebConfig.kt
  # once it is committed, the old values are one commit further back:
  git log -p -- webApp/src/webMain/kotlin/com/trainpaths/nonogram/FirebaseWebConfig.kt
  ```

  That prints the `nonogram-ba791` / `83667943466` values — `API_KEY` `AIzaSyD4VXmYnQ…`, `APP_ID`
  `1:83667943466:web:7c152e7ffce14591c94353`, `GOOGLE_WEB_CLIENT_ID` `83667943466-r9ptgubthqn…`.

Both files also declare `RECAPTCHA_SITE_KEY`, empty for now: prod gets its value at D2, dev never does. E6
skips App Check init when it is blank, which is what keeps dev runnable without a reCAPTCHA key.

`GOOGLE_WEB_CLIENT_ID` must be byte-identical to the `default_web_client_id` that the **same environment's**
`google-services.json` generates — same OAuth web client, or the two platforms mint different Firebase users
within that environment. Prod already satisfies this (`482051008739-hc89i851oq…` appears in both the current
`google-services.json` and the current `FirebaseWebConfig.kt`); dev will once its Android app is registered
in A3, since `nonogram-ba791`'s web client already exists.

Two consequences of this shape, both mild:

- **The two files are kept in step by hand.** Adding a constant means adding it twice; miss one and the build
  fails for that environment with an unresolved reference — noisy, but at compile time, which is the good
  failure.
- **The IDE only indexes the selected directory.** The other file greys out as "not in a source set"; switch
  `nonogram.env` and re-sync to work on it. The same is already true of the Android flavors.

### E4. Gitignore

`.gitignore` currently ignores `google-services.json` and nothing signing-related — both are backwards for this
layout. **Remove** the `google-services.json` line: both environment files are now tracked on purpose. Firebase
client config (`apiKey`, `appId`, the OAuth web client ID, the reCAPTCHA site key) is public-by-design — it
ships in every APK and JS bundle, and the security boundary is Firestore rules + App Check, not secrecy. The
same goes for the two `FirebaseWebConfig.kt`, which have always been committed.

To be explicit, since it is the usual source of doubt: `google-services.json` holds client *identifiers*
(project number, app ID, API key, OAuth client ID), all of which ship inside every APK and are extractable by
anyone holding one. They are not credentials, and hiding them protects nothing — Firestore rules and App Check
are what enforce access. Restricting the API key in the Google Cloud console is still worth doing, but that is
hardening an identifier, not keeping a secret.

**Add** the signing material, which is the only genuinely secret thing in this document:

```
keystore.properties
*.jks
*.keystore
```

Because the ignore rule has been in place all along, prod's `google-services.json` has never been committed.
Once the line is gone it is a normal untracked file — check with `git check-ignore -v <path>` (exit 1 = not
ignored) before assuming it will be picked up.

### E5. App Check client code — Android

Version catalog (`gradle/libs.versions.toml`), beside the existing BOM-managed Firebase entries (no versions —
the BOM supplies them):

```toml
firebase-appcheck-playintegrity = { module = "com.google.firebase:firebase-appcheck-playintegrity" }
firebase-appcheck-debug         = { module = "com.google.firebase:firebase-appcheck-debug" }
```

`androidApp/build.gradle.kts` — scoped **per flavor**, so each artifact is only on the classpath of the
environment that uses it:

```kotlin
"prodImplementation"(libs.firebase.appcheck.playintegrity)
"devImplementation"(libs.firebase.appcheck.debug)
```

Two mechanical notes. The `dependencies { }` block has to sit **after** `android { }`, because the flavor
configurations do not exist until the flavors are declared; and they need the quoted string form, since Gradle
generates no type-safe accessor for flavor-derived configurations.

Each provider class therefore exists in only one flavor, so the *call site* must be flavor-specific too — a
runtime `if` in `src/main` would not compile against a class that is absent:

`androidApp/src/dev/kotlin/com/trainpaths/nonogram/AppCheckSetup.kt`

```kotlin
package com.trainpaths.nonogram

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
```

`androidApp/src/prod/kotlin/com/trainpaths/nonogram/AppCheckSetup.kt`

```kotlin
package com.trainpaths.nonogram

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
```

(`FirebaseAppCheck.getInstance()` rather than the `Firebase.appCheck` KTX accessor — the KTX artifacts were
folded into the main SDKs and the accessor's import path has moved between versions.)

Call it first thing in `androidApp/src/main/kotlin/com/trainpaths/nonogram/MainApplication.kt`, **before**
`startKoin` — Koin constructs `FirebaseAndroidSyncService`, which touches `Firebase.firestore`, and App Check
must be installed before the first Firestore call:

```kotlin
override fun onCreate() {
    super.onCreate()
    installAppCheck()

    val koinApp = startKoin { … }        // unchanged
    …
}
```

The default `FirebaseApp` is already initialized by `FirebaseInitProvider` before `Application.onCreate`, so no
explicit `FirebaseApp.initializeApp(this)` is needed. No new ProGuard rules — the Firebase SDKs ship their own
consumer rules and `proguard-rules.pro` already has `-dontwarn com.google.firebase.**`.

**The provider is tied to the Firebase project, not to debuggability.** That is deliberate twice over: it keeps
`androidApp/src/` down to the two flavor directories, and it encodes the debug-on-dev / build-for-prod rule
from the top of this document into the build itself. Two things follow from it:

- **`devRelease` carries the debug provider** into a minified, signed build. It exists only as the local R8
  smoke test (Part F); only `prodRelease` goes to Play. The reason to keep it local is that it points at the
  dev project and does not attest — not that it leaks anything. The debug secret is generated per installation
  at first run and kept in that device's SharedPreferences, so the APK contains no token.
- **`prodDebug` stops working once App Check is enforced — which is the point.** Play Integrity vouches for an
  install that came *from Google Play*, signed with the Play app-signing key; an APK you build and push over
  USB did not, so it never obtains a valid token. Before enforcement that costs nothing (Firestore logs the
  request as unverified and serves it), which is what makes the one-time setup checks below possible. After
  D4, Firestore rejects it and a locally built `prodDebug` can no longer read or write. Debug against
  `devDebug`; reach prod through the Play-installed build.

  Should you ever need the old behaviour back — a locally built app talking to enforced prod — it is a
  reversal, not a rescue: recreate `androidApp/src/debug/kotlin/.../AppCheckSetup.kt` with the debug provider,
  move `firebase-appcheck-debug` back to `debugImplementation`, and the build type decides again. Enforcement
  is also a console toggle, reversible in minutes, if the need is one-off.

### E6. App Check client code — Web

`firebase/app-check` is part of the `npm("firebase", "12.13.0")` dependency already declared in
`shared/build.gradle.kts`; nothing new to add. Both `shared` and `webApp` already set `js { useEsModules() }`,
which is what makes `@JsModule`-only externals link.

New file `shared/src/webMain/kotlin/com/trainpaths/nonogram/firebase/FirebaseAppCheckExternals.kt`, following
the per-module externals pattern in `docs/web-architecture.md`:

```kotlin
@file:JsModule("firebase/app-check")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule

internal external interface AppCheck : JsAny

// external classes are implicitly JsAny in wasmJs — do not write an explicit supertype
internal external class ReCaptchaV3Provider(siteKey: String)

internal external fun initializeAppCheck(app: FirebaseApp, options: JsAny): AppCheck
```

**The one real obstacle:** the options argument is `{ provider: <a JS object>, isTokenAutoRefreshEnabled: true }`.
Every other JS object in this codebase is built with `JSON.parse(buildJsonObject { … }.toString())` because — as
`docs/web-architecture.md` records — `js(...)` is unavailable in a shared source set. That trick cannot carry
`provider`: it is a live `ReCaptchaV3Provider`, not JSON.

It gets you most of the way, though. Build the JSON-able half the usual way, declare the object shape as an
external interface with **one settable property**, and set `provider` on it — no `js(...)`, so it all stays in
`webMain` and neither `jsMain` nor `wasmJsMain` needs a file:

`shared/src/webMain/.../firebase/AppCheckOptions.kt`

```kotlin
// Not a firebase/app-check export — just the object shape initializeAppCheck expects, so it stays out of
// the @JsModule externals file.
internal external interface AppCheckOptions : JsAny {
    var provider: ReCaptchaV3Provider
}

internal fun appCheckOptions(provider: ReCaptchaV3Provider): AppCheckOptions {
    val options = JSON.parse(
        buildJsonObject { put("isTokenAutoRefreshEnabled", true) }.toString()
    )!! as AppCheckOptions
    options.provider = provider
    return options
}
```

The two mechanisms this leans on — an unchecked cast from `JsAny` to an external interface, and a `var`
property setter on one — are ordinary interop on both targets, but they are the parts to look at first if the
web build complains. Fallbacks, in order of preference: a one-function `expect`/`actual` with a
`js("({ provider: provider, isTokenAutoRefreshEnabled: true })")` actual per target (the Kotlin/JS one needs
`.unsafeCast<JsAny>()`, wasmJs does not), or a global helper in
`webApp/src/webMain/resources/index.html`
(`window.appCheckOptions = p => ({ provider: p, isTokenAutoRefreshEnabled: true })`) bound from `webMain` as a
plain `internal external fun` — the same shape as the global `external object JSON`.

**The localhost check and the debug-token flag need no `js(...)` either.** `location` and `globalThis` are
ordinary global bindings, so plain externals in the shared `firebase/JsInterop.kt` reach them on both targets —
same trick as the `external object JSON` already there:

```kotlin
internal external interface JsLocation : JsAny { val hostname: String }
internal external val location: JsLocation

internal external interface GlobalScope : JsAny { var FIREBASE_APPCHECK_DEBUG_TOKEN: Boolean }
internal external val globalThis: GlobalScope
```

Writing through `globalThis` rather than assigning a bare global name matters: the bundles are ESM, so ESM
strict mode makes `FIREBASE_APPCHECK_DEBUG_TOKEN = true` on an undeclared name a `ReferenceError`.

Then wire it into the facade. `FirebaseWeb.initialize` currently discards the `FirebaseApp` it creates; App Check
must be initialized on that instance and should run **before** `getAuth`/`getFirestore`, so fold it into the
existing call rather than adding a second entry point — that keeps the ordering un-losable and keeps
`FirebaseWeb` the single facade:

```kotlin
fun initialize(
    apiKey: String,
    authDomain: String,
    projectId: String,
    messagingSenderId: String,
    appId: String,
    recaptchaSiteKey: String,
) {
    val options = JSON.parse(…)!!            // unchanged
    val app = initializeApp(options)

    if (recaptchaSiteKey.isNotBlank()) {
        if (location.hostname == "localhost" || location.hostname == "127.0.0.1") {
            globalThis.FIREBASE_APPCHECK_DEBUG_TOKEN = true
        }
        initializeAppCheck(app, appCheckOptions(ReCaptchaV3Provider(recaptchaSiteKey)))
    }

    auth = getAuth(app)
    firestore = getFirestore(app)
}
```

Gating the debug-token flag on `localhost`/`127.0.0.1` keeps it off the production origin while letting both dev
servers (js and wasmJs, different ports) print a token you can register. Gating the whole block on a non-blank
key is what lets the **dev** environment build and run before it has a reCAPTCHA key of its own — an empty
`RECAPTCHA_SITE_KEY` in `src/dev` simply means no App Check on dev.

`RECAPTCHA_SITE_KEY` is the seventh constant both `FirebaseWebConfig.kt` files declare (E3), and
`webApp/src/webMain/.../main.kt` passes it in the existing `FirebaseWeb.initialize(...)` call.

### E7. Firestore rules stay out of the repo

This is a deliberate decision, not an oversight — the rules are maintained in the Firebase console, per
project, and are not tracked here. Two consequences to hold on to:

- **Dev and prod are separate rule sets.** A rule fixed in one is not fixed in the other; changing the
  moderation model means editing it twice, and there is nothing in CI that will notice if they drift.
  `nonogram-ba791` in particular has probably never had these rules published — check it explicitly rather
  than assuming, since test-mode or locked defaults both misbehave in ways that look like app bugs.
- **Keep a copy somewhere outside the repo.** The last version that was checked in is still recoverable from
  git history:

  ```bash
  git show fix/admin-panel:firestore.rules > ~/keystores/nonogram-firestore.rules
  ```

  Verify it has the `admins/{uid}`, `users/{uid}` and `nonograms/{nonogramId}` blocks with the `publishStatus`
  transition logic before pasting it into either project. That file is what enforces publish moderation and
  the unauthenticated read of `APPROVED` documents; test-mode rules will silently break the moderation model.

For the same reason there is no `firebase.json` / `.firebaserc` / `firestore.indexes.json` here, so rules and
indexes are console-only hand edits (A2.3, A2.4).

### E8. Documentation

`CLAUDE.md` claims `firestore.rules` is checked in — correct it to say the rules are console-managed per
project and why (E7). It also says nothing about App Check, release signing, R8, or the dev/prod split; add
the split in particular, since `assembleDevDebug` / `-Pnonogram.env=prod` are not guessable from the code.
Add a line to `docs/web-architecture.md`'s externals section noting the `js(...)` escape hatch App Check
forced, and a note that `FirebaseWebConfig` comes from `webApp/src/<env>`, selected by `nonogram.env`.

---

## Part F — Build and upload the AAB

```bash
./gradlew :androidApp:clean :androidApp:bundleProdRelease
```

Output: `androidApp/build/outputs/bundle/prodRelease/androidApp-prod-release.aab`.

Before uploading, smoke-test a **release** build on a device, because R8 is on (`isMinifyEnabled = true`,
`isShrinkResources = true`) and this configuration has never been exercised. Do it against **dev** first — same
build type, same signing config, same R8 pass, but it cannot write junk into the prod Firestore:

```bash
./gradlew :androidApp:installDevRelease
# then, once that is clean:
./gradlew :androidApp:installProdRelease
```

`devRelease` is a local smoke test only — it bundles the App Check **debug** provider and points at the dev
project (E5), so it is not a shippable artifact. The bundle that goes to Play is `prodRelease`.

Watch for kotlinx.serialization failures (`Nonogram.solution` JSON), SQLDelight, and Firestore document mapping.
The existing `androidApp/proguard-rules.pro` covers serialization, SQLDelight, enums and coroutines.

Then in Play Console: **Test and release → Testing → Internal testing → Create new release → upload the AAB**.
Play App Signing is confirmed on this first upload — accept the Google-generated key. Then go back to **C2** and
register the app signing fingerprints.

`versionCode = 1` / `versionName = "1.0"` in `androidApp/build.gradle.kts`: every subsequent upload needs a
higher `versionCode`.

Internal testing only needs the app entry plus testers; the **Dashboard → App content** items (privacy policy
URL, Data safety — declare the Google account / email collected via sign-in, content rating, target audience,
ads declaration) are required before a *production* track release, not before this first upload. Getting them
out of the way now avoids a second review round trip later.

---

## Verification

Steps 1–4 are **one-time setup checks**, run before App Check enforcement (D4). They are the only place
`prodDebug` appears: once enforced, prod is reached through the Play build and debugging happens on dev.

1. `./gradlew :androidApp:clean :androidApp:assembleDevDebug :androidApp:assembleProdDebug` — proves
   `default_web_client_id` resolves from *both* `google-services.json` files.
2. Confirm the flavors actually differ, rather than one file silently serving both variants:

   ```bash
   grep -r default_web_client_id androidApp/build/generated/res/processDevDebugGoogleServices/
   grep -r default_web_client_id androidApp/build/generated/res/processProdDebugGoogleServices/
   ```

   Expect `83667943466-…` for dev and `482051008739-…` for prod, each matching its environment's
   `GOOGLE_WEB_CLIENT_ID` in `webApp/src/<env>`. Identical values mean one file is serving both variants
   — check that the module-root `google-services.json` is really gone.
3. `devDebug` on device — the variant you will actually live in. Google sign-in **completes** (this is the
   check that blocker 2 is fixed: it fails with `ApiException: 10` until the debug SHA-1 is registered in
   `nonogram-ba791`), a puzzle syncs, and `admins/<your-uid>` makes the Admin entry appear in Settings.
4. `prodDebug` on device, **once**, to prove the prod project is wired: the same three things, against
   `nonogram-trainpaths` — a different uid, a different admin document, a different set of puzzles. This works
   only until D4; after enforcement, repeat it with the Play-installed build instead.
5. Firebase console → App Check → APIs (prod only — dev has no App Check): the web app's requests show as
   **verified** once its debug token is registered. Android will show unverified until a Play-distributed
   build runs, which is expected — Play Integrity cannot attest a sideloaded APK.
6. Web: `./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pnonogram.env=prod` — sign in, confirm no App Check
   errors in the browser console, and that the same account shows the same puzzles as the prod Android build
   from step 4. That is what proves both platforms are on one project.
7. Then re-run without the flag (`:webApp:wasmJsBrowserDevelopmentRun`) and confirm the browser is talking to
   `nonogram-ba791` — the network tab's Firestore requests carry the project ID, and a puzzle authored in dev
   must *not* appear in the prod build. That is what proves the split works and prod is not the accidental
   default.
8. `./gradlew :androidApp:bundleProdRelease` produces a signed `.aab`; `jarsigner -verify -verbose -certs` (or
   the Play Console upload itself) confirms it is signed with the upload key, not the debug key.
9. After enforcement: sign out and use the app as a guest — public puzzles must still load, since guests read
   `APPROVED` docs unauthenticated and App Check is independent of auth.
