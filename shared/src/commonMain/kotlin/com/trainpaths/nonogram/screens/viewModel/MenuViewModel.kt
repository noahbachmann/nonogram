package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuViewModel(private val sdk: AppSDK, private val authRepository: AuthRepository) : ViewModel() {

    var nonograms: List<Nonogram> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    private var progressMap: Map<Long, List<List<Int>>> by mutableStateOf(emptyMap())

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
            val userId = authRepository.currentUserId.value
            if (userId != null) {
                val allProgress = withContext(Dispatchers.Default) {
                    sdk.getProgressForUser(userId)
                }
                progressMap = allProgress
                    .filter { it.board != null }
                    .associate { it.nonogram.id to it.board!! }
            }
            isLoading = false
        }
    }

    fun getProgress(id: Long, height: Int, width: Int): List<List<Int>> {
        return progressMap[id] ?: List(height) { List(width) { 0 } }
    }
}
