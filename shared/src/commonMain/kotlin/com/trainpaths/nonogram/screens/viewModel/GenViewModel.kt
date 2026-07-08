package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel

class GenViewModel() : ViewModel() {

    var height: Int = 0
    var width: Int = 0

    var currentBoard: List<List<Int>> = emptyList()

    fun setSize(h: Int, w: Int) {
        height = h
        width = w
    }
}