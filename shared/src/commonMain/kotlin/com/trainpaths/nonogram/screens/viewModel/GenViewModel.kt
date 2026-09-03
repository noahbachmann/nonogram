package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.BoardHistory
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.MAX_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.MIN_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.classes.toInts
import com.trainpaths.nonogram.sync.SyncService
import com.trainpaths.nonogram.sync.syncPublicNonograms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

internal data class SaveValidationResult(
    val isValid: Boolean?,
    val state: ValidationState,
    val error: String? = null,
)

/** Validation is advisory for saving; failure only prevents publishing the puzzle. */
internal inline fun validationForSave(validate: () -> Boolean): SaveValidationResult =
    try {
        val isValid = validate()
        SaveValidationResult(
            isValid = isValid,
            state = if (isValid) ValidationState.VALID else ValidationState.INVALID,
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        SaveValidationResult(
            isValid = null,
            state = ValidationState.UNAVAILABLE,
            error = error.message ?: "Unable to validate this nonogram.",
        )
    }

enum class ValidationState {
    UNCHECKED,
    CHECKING,
    VALID,
    INVALID,
    UNAVAILABLE,
}

class GenViewModel(
    private val sdk: AppSDK,
    private val authRepository: AuthRepository,
    private val syncService: SyncService,
) : ViewModel() {

    companion object {
        private const val DEFAULT_SIZE = 5
    }

    var tiles by mutableStateOf<List<List<Tile>>>(emptyList())
        private set

    val height: Int get() = tiles.size
    val width: Int get() = tiles.firstOrNull()?.size ?: 0

    val history = BoardHistory(onApply = { updateNonogram() })

    var nonogram by mutableStateOf(Nonogram(0, Difficulty.EASY, emptyList()))
        private set

    var myNonograms by mutableStateOf<List<Nonogram>>(emptyList())
        private set

    var isLoadingMine by mutableStateOf(true)
        private set

    /** True when the current puzzle has edits not yet written to the database. */
    var isDirty by mutableStateOf(false)
        private set

    var validationState by mutableStateOf(ValidationState.UNCHECKED)
        private set

    /** True while the current puzzle is being validated and persisted. */
    var isSaving by mutableStateOf(false)
        private set

    /** A user-facing message when the latest save could not be completed. */
    var saveError by mutableStateOf<String?>(null)
        private set

    /** A non-blocking warning when the puzzle could not be validated before it was saved. */
    var validationError by mutableStateOf<String?>(null)
        private set

    /** True while a publish request is in flight. */
    var isRequestingPublish by mutableStateOf(false)
        private set

    /** A user-facing message when the latest publish request could not be filed. */
    var publishError by mutableStateOf<String?>(null)
        private set

    /** Whether the editor has a new or changed puzzle that can currently be persisted. */
    val canSave: Boolean
        get() = !isSaving && (isDirty || nonogram.id == 0L)

    /** Editing a live public puzzle takes it private and back into review, so the author confirms first. */
    fun needsPublicEditConfirmation(changesContent: Boolean = isDirty): Boolean =
        nonogram.isPublic && changesContent

    /**
     * The author's visibility switch, which only an already-approved puzzle has: it moves between
     * `APPROVED` and `UNLISTED` and never costs a new review. Every other state ignores it, because
     * publication is the admin's call — see `docs/publish-moderation.md`. Staged in memory until
     * the next save, and never an edit: [isDirty] means a *content* change.
     */
    fun setPublic(requested: Boolean) {
        nonogram = nonogram.copy(
            publishStatus = when (nonogram.publishStatus) {
                PublishStatus.APPROVED, PublishStatus.UNLISTED ->
                    if (requested) PublishStatus.APPROVED else PublishStatus.UNLISTED

                else -> return
            }
        )
    }

    val authState = authRepository.authState

    fun loadMyNonograms() {
        isLoadingMine = true
        launchGuarded(onError = { println("Generator: loading own nonograms failed: ${it.message}") }) {
            try {
                val authorUid = authRepository.currentUserUid.value.orMissing()
                myNonograms =
                    if (authorUid == null) emptyList() else sdk.getNonogramsByAuthor(authorUid)
            } finally {
                isLoadingMine = false
            }
        }
    }

    fun startNew() {
        setNonogram(DEFAULT_SIZE, DEFAULT_SIZE)
    }

    private fun Int.clampToGridSide(): Int = coerceIn(MIN_NONOGRAM_SIDE, MAX_NONOGRAM_SIDE)

    fun setNonogram(h: Int, w: Int, name: String? = null) {
        nonogram = Nonogram(0, Difficulty.EASY, emptyList(), name = name)
        tiles = List(h.clampToGridSide()) { List(w.clampToGridSide()) { Tile() } }
        history.reset(tiles)
        updateNonogram()
        isDirty = false
    }

    fun updateName(name: String?) {
        if (nonogram.name == name) return
        nonogram = nonogram.copy(name = name)
        isDirty = true
        saveError = null
        validationError = null
    }

    /**
     * Resizes the current puzzle to [h] x [w], each clamped to the allowed sides, while preserving
     * the existing drawing where the old and new grids overlap. Keeps [nonogram] (and its id) so
     * an edit still updates the same row.
     */
    fun resizeNonogram(h: Int, w: Int) {
        val rows = h.clampToGridSide()
        val cols = w.clampToGridSide()
        if (rows == height && cols == width) return
        val old = tiles
        tiles = List(rows) { r ->
            List(cols) { c ->
                val previous = old.getOrNull(r)?.getOrNull(c)
                Tile().apply { if (previous != null) state = previous.state }
            }
        }
        history.reset(tiles)
        updateNonogram()
    }

    fun loadForEdit(existing: Nonogram) {
        nonogram = existing
        tiles = existing.solution.map { row ->
            row.map { cell -> Tile().apply { if (cell == 1) state = TileState.FILLED } }
        }
        history.reset(tiles)
        isDirty = false
        validationState = ValidationState.UNCHECKED
        saveError = null
        validationError = null
        publishError = null
    }

    fun updateNonogram() {
        nonogram = nonogram.copy(solution = tiles.toInts())
        isDirty = true
        validationState = ValidationState.UNCHECKED
        saveError = null
        validationError = null
    }

    fun onSave(onDone: () -> Unit = {}) {
        if (isSaving) return
        val authorUid = authRepository.currentUserUid.value
            .orMissing { saveError = "Sign in again to continue." } ?: return
        val nonogramId = nonogram.id
        nonogram = nonogram.copy(solution = tiles.toInts())
        // Any content change revokes approval, so the reviewer's verdict always matches the puzzle.
        val contentChanged = isDirty
        isSaving = true
        validationState = ValidationState.CHECKING
        saveError = null
        validationError = null
        launchGuarded {
            try {
                val validation = withContext(Dispatchers.Default) {
                    validationForSave { nonogram.isValid }
                }
                validationState = validation.state
                validationError = validation.error

                if (contentChanged) nonogram = nonogram.copy(publishStatus = PublishStatus.NONE)

                val savedNonogram = persistAndPush(nonogramId, authorUid, contentChanged)
                if (savedNonogram != null) nonogram = savedNonogram
                isDirty = false
                onDone()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                saveError = error.message ?: "Unable to save this nonogram."
            } finally {
                isSaving = false
            }
        }
    }

    /**
     * Files a publish request for the saved puzzle. A grid another puzzle already publishes is
     * refused outright, so the duplicate never reaches Firestore; otherwise the local row moves to
     * `PENDING` first so the button responds immediately, and a rejection by the Firestore rules
     * (a banned author) rolls it back.
     */
    fun requestPublish() {
        if (isSaving || isRequestingPublish) return
        val nonogramId = nonogram.id
        if (nonogramId == 0L) return
        val firebaseUid = authRepository.currentFirebaseUid
            .orMissing { publishError = "Sign in to publish." } ?: return
        isRequestingPublish = true
        publishError = null
        launchGuarded {
            try {
                syncService.syncPublicNonograms(authRepository, firebaseUid)
                val saved = sdk.getNonogramById(nonogramId)
                if (saved == null) {
                    publishError = "Could not send this publish request."
                    return@launchGuarded
                }
                val conflict = sdk.hasPublishConflict(saved.solution, nonogramId, firebaseUid)
                if (conflict) {
                    publishError = "This puzzle already exists."
                    return@launchGuarded
                }
                val result = fileRequest(nonogramId, firebaseUid, saved)
                if (result != null) nonogram = result
                if (nonogram.publishStatus != PublishStatus.PENDING) {
                    publishError = "Could not send this publish request."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                publishError = error.message ?: "Could not send this publish request."
            } finally {
                isRequestingPublish = false
            }
        }
    }

    /**
     * Writes the current puzzle — inserting when [nonogramId] is 0 — and pushes the row back out.
     * Returns it re-read, so the caller's copy carries the `updatedAt` to the database stamped.
     */
    private suspend fun persistAndPush(
        nonogramId: Long,
        authorUid: String,
        contentChanged: Boolean,
    ): Nonogram? {
        val savedId = if (nonogramId != 0L) {
            sdk.updateNonogram(nonogramId, nonogram)
        } else {
            sdk.addNonogram(
                difficulty = nonogram.difficulty.toString(),
                solution = nonogram.solution,
                authorUid = authorUid,
                name = nonogram.name,
                publishStatus = nonogram.publishStatus,
            )
        }
        // Re-fetch so the UI and pushed copy carry the freshly stamped updatedAt.
        val persisted = sdk.getNonogramById(savedId)
        val firebaseUid = authRepository.currentFirebaseUid.orMissing()
        if (persisted != null && firebaseUid != null) {
            syncService.pushNonogram(firebaseUid, persisted, contentChanged)
        }
        return persisted
    }

    /**
     * Moves [saved] to `PENDING` locally before pushing the request, so the button responds without
     * waiting on the network, and puts it back to `NONE` if Firestore refuses it. Returns the row as
     * it now stands, or null if it went missing under us.
     */
    private suspend fun fileRequest(
        nonogramId: Long,
        firebaseUid: String,
        saved: Nonogram,
    ): Nonogram? {
        sdk.updateNonogram(nonogramId, saved.copy(publishStatus = PublishStatus.PENDING))
        val pending = sdk.getNonogramById(nonogramId) ?: return null
        if (syncService.requestPublish(firebaseUid, pending)) return pending
        sdk.updateNonogram(nonogramId, pending.copy(publishStatus = PublishStatus.NONE))
        return sdk.getNonogramById(nonogramId)
    }
}
