package com.trainpaths.nonogram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(private val sdk: AppSDK) : ViewModel() {

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
                nonogram = loaded
                tiles = List(loaded.height) { List(loaded.width) { Tile() } }
            }
        }
    }
}
