package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * `viewModelScope.launch` that cannot take the process down: an uncaught throwable inside a plain
 * launch reaches the platform's default handler and kills the app on Android. Cancellation is
 * always rethrown, so a cleared ViewModel still tears its coroutines down normally.
 */
internal fun ViewModel.launchGuarded(
    context: CoroutineContext = EmptyCoroutineContext,
    onError: (Throwable) -> Unit = { println("ViewModel: background task failed: ${it.message}") },
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch(context) {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        onError(error)
    }
}
