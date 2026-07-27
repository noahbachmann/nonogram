package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun publicationStatus(isPublic: Boolean, isValid: Boolean, isSignedIn: Boolean): Boolean =
    isPublic && isValid && isSignedIn

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

    var nonogram by mutableStateOf(Nonogram(0, Difficulty.EASY, emptyList()))
        private set

    var myNonograms by mutableStateOf<List<Nonogram>>(emptyList())
        private set

    var isLoadingMine by mutableStateOf(true)
        private set

    /** True when the current puzzle has edits not yet written to the database. */
    var isDirty by mutableStateOf(false)
        private set

    /** True while the current puzzle is being persisted (and, eventually, validated). */
    var isSaving by mutableStateOf(false)
        private set

    /** A user-facing message when the latest save could not be completed. */
    var saveError by mutableStateOf<String?>(null)
        private set

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

    fun setNonogram(h: Int, w: Int) {
        height = h
        width = w
        nonogram = Nonogram(0, Difficulty.EASY, emptyList())
        tiles = List(h) { List(w) { Tile() } }
        updateNonogram()
        isDirty = false
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
        updateNonogram()
    }

    fun loadForEdit(existing: Nonogram) {
        nonogram = existing
        height = existing.solution.size
        width = existing.solution.firstOrNull()?.size ?: 0
        tiles = existing.solution.map { row ->
            row.map { cell -> Tile().apply { if (cell == 1) state = TileState.FILLED } }
        }
        isDirty = false
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
    }

    fun setPublic(isPublic: Boolean) {
        val isPublicAllowed = publicationStatus(
            isPublic = isPublic,
            isValid = nonogram.isValid,
            isSignedIn = authRepository.authState.value == AuthState.SIGNED_IN,
        )
        if (nonogram.isPublic != isPublicAllowed) {
            nonogram = nonogram.copy(isPublic = isPublicAllowed)
            isDirty = true
        }
    }

    fun onSave(onDone: () -> Unit = {}) {
        if (isSaving) return
        val userId = authRepository.currentUserId.value ?: return
        val nonogramId = nonogram.id
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        isSaving = true
        saveError = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    // Invalid puzzles must never be persisted as public, including legacy data.
                    val puzzleToSave = nonogram.copy(
                        solution = board,
                        isPublic = publicationStatus(
                            isPublic = nonogram.isPublic,
                            isValid = nonogram.isValid,
                            isSignedIn = authRepository.authState.value == AuthState.SIGNED_IN,
                        ),
                    )
                    val savedId = if (nonogramId != 0L) {
                        sdk.updateNonogram(nonogramId, puzzleToSave)
                    } else {
                        sdk.addNonogram(
                            difficulty = puzzleToSave.difficulty.toString(),
                            solution = board,
                            authorId = userId,
                            isValid = puzzleToSave.isValid,
                            isPublic = puzzleToSave.isPublic,
                        )
                    }
                    // Re-fetch so the UI and pushed copy carry validator output and updatedAt.
                    val saved = sdk.getNonogramById(savedId)
                    if (saved != null) {
                        nonogram = saved
                        val firebaseUid = sdk.getUserById(userId)?.firebaseUid
                        if (firebaseUid != null) {
                            syncService.pushNonogram(firebaseUid, saved)
                        }
                    }
                }
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
