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
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun canSaveNonogram(isSaving: Boolean, isDirty: Boolean, nonogramId: Long): Boolean =
    !isSaving && (isDirty || nonogramId == 0L)

internal fun publicationStatus(isPublic: Boolean, isValid: Boolean?, isSignedIn: Boolean): Boolean =
    isPublic && isValid == true && isSignedIn

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

    /** Whether the editor has a new or changed puzzle that can currently be persisted. */
    val canSave: Boolean
        get() = canSaveNonogram(
            isSaving = isSaving,
            isDirty = isDirty,
            nonogramId = nonogram.id,
        )

    val authState = authRepository.authState

    fun loadMyNonograms() {
        isLoadingMine = true
        viewModelScope.launch {
            val userId = authRepository.currentUserId.value
            myNonograms = if (userId == null) {
                emptyList()
            } else {
                withContext(Dispatchers.Default) { sdk.getNonogramsByAuthor(userId) }
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

    fun onSave(
        requestedPublic: Boolean = nonogram.isPublic,
        onDone: () -> Unit = {},
    ) {
        if (isSaving) return
        val userId = authRepository.currentUserId.value ?: return
        val nonogramId = nonogram.id
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        nonogram = nonogram.copy(solution = board)
        val isSignedIn = authRepository.authState.value == AuthState.SIGNED_IN
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

                nonogram.isPublic = publicationStatus(
                    isPublic = requestedPublic,
                    isValid = validation.isValid,
                    isSignedIn = isSignedIn,
                )

                val savedNonogram = withContext(Dispatchers.Default) {
                    val savedId = if (nonogramId != 0L) {
                        sdk.updateNonogram(nonogramId, nonogram)
                    } else {
                        sdk.addNonogram(
                            difficulty = nonogram.difficulty.toString(),
                            solution = nonogram.solution,
                            authorId = userId,
                            isPublic = nonogram.isPublic,
                            name = nonogram.name,
                        )
                    }
                    // Re-fetch so the UI and pushed copy carry the freshly stamped updatedAt.
                    val persistedNonogram = sdk.getNonogramById(savedId)
                    if (persistedNonogram != null) {
                        val firebaseUid = sdk.getUserById(userId)?.firebaseUid
                        if (firebaseUid != null) {
                            syncService.pushNonogram(firebaseUid, persistedNonogram)
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
}
