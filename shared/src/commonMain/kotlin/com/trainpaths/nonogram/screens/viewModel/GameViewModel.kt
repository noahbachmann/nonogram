package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.BoardHistory
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileEdit
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.classes.toInts
import com.trainpaths.nonogram.classes.toSolutionOrNull
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers

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
        get() = tiles.toInts()

    fun loadNonogram(id: Long) {
        nonogram = null
        tiles = emptyList()
        launchGuarded(onError = { println("Game: loading nonogram $id failed: ${it.message}") }) {
            val loaded: Nonogram? = sdk.getNonogramById(id)
            if (loaded != null) {
                val userUid = authRepository.currentUserUid.value
                val existingProgress: List<List<Int>>? = if (userUid != null) {
                    sdk.getSingleProgress(userUid, id)
                        ?.boardState
                        ?.toSolutionOrNull()
                        ?.takeIf { it.size == loaded.height && it.all { row -> row.size == loaded.width } }
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
        val userUid = authRepository.currentUserUid.value.orMissing() ?: return
        val nonogramId = nonogram?.id ?: return
        val board = currentBoardAsInts
        launchGuarded(
            Dispatchers.Default,
            onError = { println("Game: saving progress for $nonogramId failed: ${it.message}") },
        ) {
            if (win) {
                sdk.saveProgressAfterWin(userUid, nonogramId)
            } else {
                sdk.saveProgress(userUid, nonogramId, board)
            }

            val firebaseUid = authRepository.currentFirebaseUid.orMissing() ?: return@launchGuarded
            val progress = sdk.getSingleProgress(userUid, nonogramId) ?: return@launchGuarded
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
