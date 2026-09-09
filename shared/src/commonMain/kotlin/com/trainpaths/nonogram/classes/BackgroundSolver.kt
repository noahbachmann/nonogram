package com.trainpaths.nonogram.classes

/**
 * Runs the [Solver] somewhere that is not the UI thread, and returns its deduction.
 *
 * `Dispatchers.Default` is enough on Android but is the *same* thread as the UI on js/wasmJs, so
 * web routes the work to a worker instead — see `docs/web-architecture.md`. Every caller of the
 * Solver outside tests goes through here.
 */
internal expect suspend fun solveInBackground(nonogram: Nonogram): List<List<Int>>
