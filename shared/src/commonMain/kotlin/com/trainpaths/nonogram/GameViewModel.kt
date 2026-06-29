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

    init {
        viewModelScope.launch {
            val loadedNonogram: Nonogram? = withContext(Dispatchers.Default) {
                sdk.seedIfEmpty()
                sdk.getRandomNonogram()
            }
            if (loadedNonogram != null) {
                this@GameViewModel.nonogram = loadedNonogram
                tiles = List(loadedNonogram.height) { List(loadedNonogram.width) { Tile() } }
            }
        }
    }
}
