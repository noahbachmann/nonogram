package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.BoardHistory
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileEdit
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GameViewModel(
    private val sdk: AppSDK,
    private val authRepository: AuthRepository,
    private val syncService: SyncService,
) : ViewModel() {

    var nonogram: Nonogram? by mutableStateOf(null)
        private set
    var tiles: List<List<Tile>> by mutableStateOf(emptyList())
        private set

    val history = BoardHistory()

    val currentNonogramId: Long?
        get() = nonogram?.id

    val currentBoardAsInts: List<List<Int>>
        get() = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }

    fun loadNonogram(id: Long) {
        nonogram = null
        tiles = emptyList()
        viewModelScope.launch {
            val loaded: Nonogram? = withContext(Dispatchers.Default) {
                sdk.getNonogramById(id)
            }
            if (loaded != null) {
                val userUid = authRepository.currentUserUid.value
                val existingProgress: List<List<Int>>? = if (userUid != null) {
                    withContext(Dispatchers.Default) {
                        sdk.getSingleProgress(userUid, id)
                            ?.boardState
                            ?.let { Json.decodeFromString<List<List<Int>>>(it) }
                    }
                } else null

                nonogram = loaded
                tiles = existingProgress?.map { row ->
                    row.map { value ->
                        Tile().apply {
                            if (value == 1) {
                                state = TileState.FILLED
                            }
                        }
                    }
                }
                    ?: List(loaded.height) { List(loaded.width) { Tile() } }
                history.reset(tiles)
            }
        }
    }

    fun saveCurrentProgress(win: Boolean = false) {
        val userUid = authRepository.currentUserUid.value ?: return
        val nonogramId = nonogram?.id ?: return
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        viewModelScope.launch(Dispatchers.Default) {
            if (win) {
                sdk.saveProgressAfterWin(userUid, nonogramId)
            } else {
                sdk.saveProgress(userUid, nonogramId, board)
            }

            val firebaseUid = authRepository.currentFirebaseUid ?: return@launch
            val progress = sdk.getSingleProgress(userUid, nonogramId) ?: return@launch
            syncService.pushProgress(firebaseUid, nonogramId, progress.boardState, progress.updatedAt)
        }
    }

    fun resetBoard() {
        val edits = mutableListOf<TileEdit>()
        for ((rowIndex, row) in tiles.withIndex()) {
            for ((colIndex, tile) in row.withIndex()) {
                if (tile.state != TileState.NONE) {
                    edits.add(TileEdit(rowIndex, colIndex, before = tile.state, after = TileState.NONE))
                    tile.state = TileState.NONE
                }
            }
        }
        history.record(edits)
    }
}
