# Release checklist — what is left to do

Short form of `docs/prod-firebase-setup.md` (which explains *why* each piece looks the way it does; its status
notes are stale). This file is the actionable list. **prod = `nonogram-trainpaths`, dev = `nonogram-ba791`.**

## Already done — do not redo

Verified in the working tree:

- Upload keystore + `keystore.properties` exist; `release` build type signs with them, not the debug key.
- `dev`/`prod` flavors; `androidApp/src/{dev,prod}/google-services.json` in place, module-root copy gone.
  Both files carry a web (`client_type: 3`) and an Android (`client_type: 1`) client with a `certificate_hash`.
- Android App Check: `AppCheckSetup.kt` per flavor, `installAppCheck()` first in `MainApplication.onCreate`,
  providers scoped `devImplementation` / `prodImplementation`.
- Web App Check: externals + `AppCheckOptions.kt`, initialized inside `FirebaseWeb.initialize`, skipped when
  `RECAPTCHA_SITE_KEY` is blank. Prod's key is filled in; dev's is blank on purpose.
- Web env switch: `nonogram.env=dev` in `gradle.properties`, `webApp/src/{dev,prod}/…/FirebaseWebConfig.kt`.
- `.gitignore`: `google-services.json` tracked, `keystore.properties` / `*.jks` / `*.keystore` ignored.
- `CLAUDE.md` and `docs/web-architecture.md` updated.
- CI runs only `:shared` tasks — no keystore needed, nothing to change there.

Not verifiable from the repo, so it is in Part A below: the Firebase console state.

---

## Part A — Verify (console + one local command)

**A1. Which fingerprint is registered where.** The two projects have *different* certificate hashes registered:

| Project | registered hash |
|---|---|
| dev  | `5d591c10e64d57a731a262ab187686fd0f9a512c` |
| prod | `c3604877aba2baf87cfef7d91bd837743f9c01f1` |

Run:

```bash
./gradlew :androidApp:signingReport
```

Note the **debug** SHA-1 and the **release/upload** SHA-1, then check each project has what it needs
(Firebase → Project settings → General → Your apps → Android app):

- dev — debug SHA-1. Required; this is what makes `devDebug` sign-in work.
- prod — upload SHA-1 **and** SHA-256. Add the SHA-256 if only the SHA-1 is there.
- prod — debug SHA-1 is *optional*. Without it `prodDebug` sign-in fails, which is intended (debug against dev).

A hash that matches neither certificate means it came from a machine or keystore you no longer use — remove it.

**A2. Firestore rules published — in both projects.** Firebase → Firestore → Rules.
Last known-good copy: `git show fix/admin-panel:firestore.rules`. Must contain the `admins/{uid}`,
`users/{uid}` and `nonograms/{nonogramId}` blocks, including the unauthenticated read of `APPROVED` docs.
Test-mode or locked defaults both break the app in ways that look like bugs.

**A3. Composite indexes — in both projects.** Firestore → Indexes → Composite. Two on `nonograms`, status
**Enabled**:

- `publishStatus` ↑, `updatedAt` ↑
- `authorUid` ↑, `updatedAt` ↑

Missing → puzzle pulls fail at runtime with "The query requires an index".

**A4. Admin document — in both projects, separately.** Collection `admins`, document ID = your Firebase uid in
*that* project (the same Google account gets a different uid per project). Any field; the rules only check
`exists()`. The Admin entry in Settings is the proof.

**A5. Prod OAuth consent screen published.** Google Cloud console (prod project) → Google Auth Platform →
Audience. While it says *Testing*, only listed test users can sign in and everyone sees an "unverified app"
screen. The app uses only non-sensitive scopes, so **Publish app** takes effect immediately — no review.
Dev can stay in Testing.

**A6. Web App Check registered.** Firebase (prod) → Build → App Check → Apps → web app should show
**reCAPTCHA v3** registered. In <https://www.google.com/recaptcha/admin> the key's domain list must include
`localhost`. Register nothing in dev.

**A7. Prod web OAuth client origins.** Google Cloud console (prod) → APIs & Services → Credentials → the
auto-created **Web client** → Authorized JavaScript origins: add the dev-server ports you use
(`http://localhost:8080` and the other one — js and wasmJs bind different ports, and different ports are
different origins). Needed for `-Pnonogram.env=prod` runs on localhost.

**A8. Keystore backup.** The `.jks` and its passwords, somewhere off this machine. Losing them means the app can
only be updated through Play's upload-key reset flow.

---

## Part B — Do, in this order

**B1. Register the web App Check debug token.**

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pnonogram.env=prod
```

On `localhost` the client sets `FIREBASE_APPCHECK_DEBUG_TOKEN`, so the SDK prints a token in the browser
console. Copy it into Firebase → App Check → Apps → web app → ⋮ → **Manage debug tokens** → Add.
Do this before B8, or localhost web stops working the moment enforcement is on.

**B2. R8 smoke test.** Release is minified and shrunk and has never been run.

```bash
./gradlew :androidApp:installDevRelease   # against dev first
./gradlew :androidApp:installProdRelease  # then prod
```

Watch for kotlinx.serialization, SQLDelight and Firestore mapping failures.

**B3. Build the bundle.**

```bash
./gradlew :androidApp:clean :androidApp:bundleProdRelease
```

Output: `androidApp/build/outputs/bundle/prodRelease/androidApp-prod-release.aab`.
Confirm the signer is the upload key, not the debug key:

```bash
jarsigner -verify -verbose -certs androidApp/build/outputs/bundle/prodRelease/androidApp-prod-release.aab | head -30
```

**B4. Upload to internal testing.** Play Console → Test and release → Testing → Internal testing → Create new
release → upload the AAB. Accept the Google-generated Play App Signing key. This upload is what *creates* the
app signing certificate — B5 is impossible before it.

**B5. Register the app signing fingerprints in prod Firebase.** Play Console → Test and release → Setup →
App integrity → **Play app signing** tab → **App signing key certificate** → copy SHA-1 and SHA-256 →
Firebase (prod) → Android app → Add fingerprint, both.

Prod only. Never add these to dev — that certificate only signs the Play-delivered prod app.
This is a server-side change: no rebuild, no re-upload, no re-download of `google-services.json`.

**B6. Play Integrity.**

1. Play Console → Test and release → App integrity → Play Integrity API → **Link Cloud project** → the Google
   Cloud project behind `nonogram-trainpaths`.
2. Firebase (prod) → App Check → Apps → Android app → **Play Integrity** → Register.

Default token TTL (1 h) is fine.

**B7. Install from the internal-testing track** on a device (not a sideloaded APK — Play Integrity cannot
attest one). Sign in, sync a puzzle, open Settings and confirm the Admin entry. Then watch
Firebase → App Check → **APIs** for a day or so: Firestore and Auth should trend to ~100 % verified.

**B8. Enforce — prod only.** App Check → APIs → **Cloud Firestore** → Enforce. Once that is stable,
**Firebase Authentication** → Enforce. Auth last: a mistake there locks out sign-in everywhere at once.
Reversible in minutes.

**B9. Post-enforcement checks.**

- Signed out, public puzzles still load (guests read `APPROVED` unauthenticated; App Check is independent of auth).
- `devDebug` unaffected — dev has no App Check.
- `prodDebug` / sideloaded `prodRelease` can no longer reach Firestore. Expected. Debug on dev.

**B10. Before any *production*-track release** (not needed for internal testing): Play Console → Dashboard →
App content — privacy policy URL, Data safety (declare the Google account / email collected at sign-in),
content rating, target audience, ads declaration. Every upload after the first needs a higher `versionCode` in
`androidApp/build.gradle.kts`.

---

## Part C — Later, when the web build gets a domain

All three must be in place before that origin can sign in or pass App Check:

1. Firebase (prod) → Authentication → Settings → **Authorized domains** → add the domain.
2. Google Cloud → Credentials → prod **Web client** → Authorized JavaScript origins: add `https://<domain>`;
   Authorized redirect URIs: `https://nonogram-trainpaths.firebaseapp.com/__/auth/handler`.
3. reCAPTCHA admin → the prod site key's domain list → add the domain.

Then build with `./gradlew :webApp:wasmJsBrowserDistribution :webApp:jsBrowserDistribution -Pnonogram.env=prod`.
