package com.trainpaths.nonogram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuViewModel(private val sdk: AppSDK) : ViewModel() {

    var nonograms: List<Nonogram> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    init {
        loadAll()
    }

    fun loadAll() {
        isLoading = true
        viewModelScope.launch {
            nonograms = withContext(Dispatchers.Default) {
                sdk.seedIfEmpty()
                sdk.getAllNonograms()
            }
            isLoading = false
        }
    }
}
