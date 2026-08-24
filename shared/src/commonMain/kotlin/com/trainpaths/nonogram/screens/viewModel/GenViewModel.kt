package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.classes.BoardHistory
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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

    var height: Int = 0
        private set
    var width: Int = 0
        private set

    var tiles by mutableStateOf<List<List<Tile>>>(emptyList())
        private set

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
        viewModelScope.launch {
            val authorUid = authRepository.currentAuthorUid.value
            myNonograms = if (authorUid == null) {
                emptyList()
            } else {
                withContext(Dispatchers.Default) { sdk.getNonogramsByAuthor(authorUid) }
            }
            isLoadingMine = false
        }
    }

    fun startNew() {
        setNonogram(DEFAULT_SIZE, DEFAULT_SIZE)
    }

    fun setNonogram(h: Int, w: Int, name: String? = null) {
        height = h
        width = w
        nonogram = Nonogram(0, Difficulty.EASY, emptyList(), name = name)
        tiles = List(h) { List(w) { Tile() } }
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
     * Resizes the current puzzle to [h] x [w] while preserving the existing drawing where the old
     * and new grids overlap. Keeps [nonogram] (and its id) so an edit still updates the same row.
     */
    fun resizeNonogram(h: Int, w: Int) {
        if (h == height && w == width) return
        val old = tiles
        tiles = List(h) { r ->
            List(w) { c ->
                val previous = old.getOrNull(r)?.getOrNull(c)
                Tile().apply { if (previous != null) state = previous.state }
            }
        }
        height = h
        width = w
        history.reset(tiles)
        updateNonogram()
    }

    fun loadForEdit(existing: Nonogram) {
        nonogram = existing
        height = existing.solution.size
        width = existing.solution.firstOrNull()?.size ?: 0
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
        val solution = tiles.map { row ->
            row.map { tile ->
                when (tile.state) {
                    TileState.FILLED -> 1
                    else -> 0
                }
            }
        }
        nonogram = nonogram.copy(solution = solution)
        isDirty = true
        validationState = ValidationState.UNCHECKED
        saveError = null
        validationError = null
    }

    fun onSave(onDone: () -> Unit = {}) {
        if (isSaving) return
        val userId = authRepository.currentUserId.value ?: return
        val authorUid = authRepository.currentAuthorUid.value ?: return
        val nonogramId = nonogram.id
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        nonogram = nonogram.copy(solution = board)
        // Any content change revokes approval, so the reviewer's verdict always matches the puzzle.
        val contentChanged = isDirty
        isSaving = true
        validationState = ValidationState.CHECKING
        saveError = null
        validationError = null
        viewModelScope.launch {
            try {
                val validation = withContext(Dispatchers.Default) {
                    validationForSave { nonogram.isValid }
                }
                validationState = validation.state
                validationError = validation.error

                if (contentChanged) nonogram = nonogram.copy(publishStatus = PublishStatus.NONE)

                val savedNonogram = withContext(Dispatchers.Default) {
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
                    val persistedNonogram = sdk.getNonogramById(savedId)
                    if (persistedNonogram != null) {
                        val firebaseUid = sdk.getUserById(userId)?.firebaseUid
                        if (firebaseUid != null) {
                            syncService.pushNonogram(firebaseUid, persistedNonogram, contentChanged)
                        }
                    }
                    persistedNonogram
                }
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
     * Files a publish request for the saved puzzle. The local row moves to `PENDING` first so the
     * button responds immediately; a rejection by the Firestore rules (a banned author) rolls it back.
     */
    fun requestPublish() {
        if (isSaving || isRequestingPublish) return
        val nonogramId = nonogram.id
        if (nonogramId == 0L) return
        val userId = authRepository.currentUserId.value ?: return
        if (authRepository.authState.value != AuthState.SIGNED_IN) return
        isRequestingPublish = true
        publishError = null
        viewModelScope.launch {
            try {
                val requested = nonogram.copy(publishStatus = PublishStatus.PENDING)
                val result = withContext(Dispatchers.Default) {
                    sdk.updateNonogram(nonogramId, requested)
                    val persisted = sdk.getNonogramById(nonogramId)
                    val firebaseUid = sdk.getUserById(userId)?.firebaseUid
                    val accepted = persisted != null && firebaseUid != null &&
                            syncService.requestPublish(firebaseUid, persisted)
                    if (!accepted && persisted != null) {
                        sdk.updateNonogram(
                            nonogramId,
                            persisted.copy(publishStatus = PublishStatus.NONE),
                        )
                    }
                    sdk.getNonogramById(nonogramId)
                }
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
}
