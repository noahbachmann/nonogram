package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.CancellationException

private const val REVIEW_BATCH_SIZE = 20
private const val SIGN_IN_REQUIRED_TO_REVIEW = "Sign in again to review requests."

class AdminViewModel(
    private val authRepository: AuthRepository,
    private val syncService: SyncService,
) : ViewModel() {

    private var queue by mutableStateOf<List<Nonogram>>(emptyList())

    val current: Nonogram? get() = queue.firstOrNull()

    var isLoading by mutableStateOf(true)
        private set

    var isDeciding by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        isLoading = true
        error = null
        launchGuarded {
            try {
                val firebaseUid = authRepository.currentFirebaseUid
                    .orMissing { error = SIGN_IN_REQUIRED_TO_REVIEW } ?: return@launchGuarded
                queue = syncService.pullPendingReviews(firebaseUid, REVIEW_BATCH_SIZE)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                error = failure.message ?: "Could not load pending requests."
            } finally {
                isLoading = false
            }
        }
    }

    fun accept() = decide(approve = true)

    fun deny() = decide(approve = false)

    private fun decide(approve: Boolean) {
        if (isDeciding) return
        val nonogram = current ?: return
        isDeciding = true
        error = null
        launchGuarded {
            try {
                val firebaseUid = authRepository.currentFirebaseUid
                    .orMissing { error = SIGN_IN_REQUIRED_TO_REVIEW } ?: return@launchGuarded

                val decided = syncService.decideReview(firebaseUid, nonogram, approve)
                if (!decided) {
                    error = "The decision could not be saved."
                    return@launchGuarded
                }
                queue = queue.drop(1)
                if (queue.isEmpty()) refresh()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                error = failure.message ?: "The decision could not be saved."
            } finally {
                isDeciding = false
            }
        }
    }
}
