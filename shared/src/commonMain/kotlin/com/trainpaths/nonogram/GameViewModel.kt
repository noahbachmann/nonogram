package com.trainpaths.nonogram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val sdk: AppSDK,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var nonogram: Nonogram? by mutableStateOf(null)
        private set
    var tiles: List<List<Tile>> by mutableStateOf(emptyList())
        private set

    fun loadNonogram(id: Long) {
        nonogram = null
        tiles = emptyList()
        viewModelScope.launch {
            val loaded: Nonogram? = withContext(Dispatchers.Default) {
                sdk.getNonogramById(id)
            }
            if (loaded != null) {
                val userId = authRepository.currentUserId.value
                val existingProgress = if (userId != null) {
                    withContext(Dispatchers.Default) {
                        sdk.getProgressForUser(userId).find { it.nonogram.id == id }
                    }
                } else null

                nonogram = loaded
                tiles = if (existingProgress?.board != null) {
                    existingProgress.board.map { row ->
                        row.map { value ->
                            Tile().apply {
                                if (value == 1) {
                                    state = TileState.FILLED
                                }
                            }
                        }
                    }
                } else {
                    List(loaded.height) { List(loaded.width) { Tile() } }
                }
            }
        }
    }

    fun saveCurrentProgress() {
        val userId = authRepository.currentUserId.value ?: return
        val nonogramId = nonogram?.id ?: return
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        viewModelScope.launch(Dispatchers.Default) {
            sdk.saveProgress(userId, nonogramId, board)
        }
    }
}
