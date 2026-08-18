# CI runner image (self-hosted)

`.github/workflows/tests.yml` runs on `runs-on: self-hosted` — an ephemeral-looking but actually
long-lived Docker container (`docker compose`, `restart: unless-stopped`, no volume for
`~/.gradle`). This doc is about the *image*, which lives outside this repo (in the runner's own
`Dockerfile`/`docker-compose.yml`); the workflow itself only assumes the image provides what's
listed below.

## Why this matters

The container's writable layer persists across jobs (a restart reuses it), so job #1 after a
container start is cold and every job after it is warm — this is the real explanation for CI runs
varying between ~3 and ~10 minutes; it isn't PR vs. `main`. Baking the cold-start cost into the
image once, at build time, turns every job into a warm job.

The image as of writing (`debian:bookworm-slim` + `chromium`, `nodejs`, `npm`, `git`, `curl`) has
**no JDK and no Android SDK**, so `actions/setup-java` and `android-actions/setup-android` in the
workflow re-download them on every cold container.

## What to add to the runner image

1. **Amazon Corretto 21**, with `JAVA_HOME` set. `gradle/gradle-daemon-jvm.properties` in this repo
   pins the Gradle daemon JVM to `toolchainVendor=AMAZON` / `toolchainVersion=21`; without a local
   match Gradle downloads that exact JDK from foojay.io on every cold run, on top of whatever
   `actions/setup-java` installs. Once Corretto 21 is baked in, the workflow's `setup-java` step can
   be dropped entirely.

2. **Android SDK** — `cmdline-tools`, `platform-tools`, `platforms;android-36`, and matching
   `build-tools` (see `android-compileSdk`/`android-targetSdk` in `gradle/libs.versions.toml`), with
   `ANDROID_HOME`/`ANDROID_SDK_ROOT` set and licenses pre-accepted. `:shared` uses
   `com.android.kotlin.multiplatform.library`, which needs `ANDROID_HOME` at *configuration* time —
   so even the `js`/`wasmJs` test tasks fail without it, not just the Android one. Once baked in,
   drop the workflow's `setup-android` step too.

3. **Warm `~/.gradle`** — copy the wrapper and build scripts into the image and run something like:
   ```dockerfile
   COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts /tmp/warm/
   COPY gradle /tmp/warm/gradle
   COPY */build.gradle.kts /tmp/warm/...   # each module
   RUN cd /tmp/warm && ./gradlew --version && \
       ./gradlew :shared:testAndroidHostTest :shared:jsBrowserTest :shared:wasmJsBrowserTest --dry-run
   ```
   This downloads the Gradle distribution, the dependency graph into `~/.gradle/caches/modules-2`,
   and node/yarn packages into the image layer, so a genuinely fresh container is already warm.
   Re-run this step (or accept some staleness) when dependencies change meaningfully.

4. **Scale runners if you want the workflow's job matrix to actually parallelize.** The workflow
   (`.github/workflows/tests.yml`) now runs the three test tasks as a 3-way matrix. A single
   `runner` container executes one job at a time, so with only one runner the matrix jobs queue and
   run *sequentially* — worse than before, since each pays setup cost independently. Either:
   - scale the compose service to ≥3 replicas (each needs a distinct `RUNNER_NAME`), each with the
     warm image above so no shared-cache coordination is needed, or
   - fall back to a single job running all three Gradle tasks with `--continue`, as before, if you'd
     rather not run multiple runner containers.

## Why the workflow uses `cache-disabled: true` on setup-gradle

`gradle/actions/setup-gradle@v6` normally manages a save/restore cache for `~/.gradle` using
GitHub's cache service. On this runner `~/.gradle` already persists locally between jobs — that's
*why* you saw "Gradle State Caching – Skipped" (the action detected a non-empty Gradle User Home
and refused to overwrite it, per
[its docs](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md#overwriting-an-existing-gradle-user-home)).
Explicitly disabling the action's cache makes that intentional instead of accidental, and avoids
paying for a ~1 GB round trip to GitHub's cache service that would be slower than the warm local
directory. If the runner setup changes (e.g. truly ephemeral containers with a shared network
cache), reconsider this.
