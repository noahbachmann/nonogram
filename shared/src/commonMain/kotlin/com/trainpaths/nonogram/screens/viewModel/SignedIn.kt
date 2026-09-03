package com.trainpaths.nonogram.screens.viewModel

/**
 * The app's one shape for "there is no uid":
 * returns it, or reports through [onMissing] and returns null.
 * Default just logs
 */
internal inline fun String?.orMissing(onMissing: () -> Unit = { println("No user found.") }): String? {
    if (this == null) onMissing()
    return this
}
