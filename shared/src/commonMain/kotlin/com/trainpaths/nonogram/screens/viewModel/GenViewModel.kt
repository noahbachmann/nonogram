package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState
import com.trainpaths.nonogram.sync.FirestoreSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenViewModel(
    private val sdk: AppSDK,
    private val authRepository: AuthRepository,
    private val syncService: FirestoreSyncService,
) : ViewModel() {

    var height: Int = 0
        private set
    var width: Int = 0
        private set

    var tiles by mutableStateOf<List<List<Tile>>>(emptyList())
        private set

    var nonogram by mutableStateOf(Nonogram(0, Difficulty.EASY, emptyList()))
        private set

    fun setNonogram(h: Int, w: Int) {
        height = h
        width = w
        tiles = List(h) { List(w) { Tile() } }
        updateNonogram()
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
        nonogram.solution = solution
    }

    fun onSave() {
        val userId = authRepository.currentUserId.value ?: return
        val nonogramId = nonogram.id
        val board = tiles.map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
        viewModelScope.launch(Dispatchers.Default) {
            if (nonogramId != 0) {
                sdk.updateNonogram(nonogramId, nonogram)
            } else {
                val newId = sdk.addNonogram(nonogram.difficulty.toString(), board, userId, 0)
                nonogram = sdk.getNonogramById(newId) ?: Nonogram(0, Difficulty.EASY, board)
            }
        }
    }
}
