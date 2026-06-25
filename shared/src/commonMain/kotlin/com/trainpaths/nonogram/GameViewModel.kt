package com.trainpaths.nonogram

import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.classes.Nonogram

class GameViewModel : ViewModel() {
    var nonogram: Nonogram = Nonogram(
        rowClues = listOf(listOf(1, 3), listOf(3), listOf(5), listOf(3), listOf(1)),
        colClues = listOf(listOf(1, 1), listOf(3, 1, 2), listOf(5), listOf(3), listOf(1)),
    )
}