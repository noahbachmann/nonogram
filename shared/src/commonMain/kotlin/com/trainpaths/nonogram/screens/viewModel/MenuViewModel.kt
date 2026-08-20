package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.filter.FilterSortState
import com.trainpaths.nonogram.filter.NonogramFilters
import com.trainpaths.nonogram.filter.applyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuViewModel(private val sdk: AppSDK, private val authRepository: AuthRepository) : ViewModel() {

    var nonograms: List<Nonogram> by mutableStateOf(emptyList())
        private set
    var isLoading: Boolean by mutableStateOf(true)
        private set
    var isRefreshing: Boolean by mutableStateOf(false)
        private set
    var filterSort: FilterSortState by mutableStateOf(FilterSortState())
        private set
    val visibleNonograms: List<Nonogram> by derivedStateOf {
        filterSort.applyTo(nonograms, NonogramFilters.ALL)
    }
    private var progressMap: Map<Long, List<List<Int>>> by mutableStateOf(emptyMap())
    private var beatMap: Map<Long, Long> by mutableStateOf(emptyMap())

    init {
        reload(true)
    }

    fun reload(loadAll: Boolean = false) {
        isLoading = loadAll
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
                beatMap = allProgress
                    .filter { it.beat > 0 }
                    .associate { it.nonogram.id to it.beat }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    fun applyFilterSort(state: FilterSortState) {
        filterSort = state
    }

    fun updateSingleProgress(nonogramId: Long, board: List<List<Int>>) {
        progressMap = progressMap + (nonogramId to board)
    }

    fun clearProgress(nonogramId: Long) {
        progressMap = progressMap - nonogramId
    }

    fun getProgress(id: Long, height: Int, width: Int): List<List<Int>> {
        return progressMap[id] ?: List(height) { List(width) { 0 } }
    }

    fun getBeatCount(id: Long): Long = beatMap[id] ?: 0

    fun incrementBeatCount(nonogramId: Long) {
        beatMap = beatMap + (nonogramId to (beatMap[nonogramId] ?: 0) + 1)
    }

    fun startRefresh() {
        isRefreshing = true
    }
}

