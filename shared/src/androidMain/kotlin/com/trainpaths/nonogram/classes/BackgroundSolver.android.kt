package com.trainpaths.nonogram.classes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun solveInBackground(nonogram: Nonogram): List<List<Int>> =
    withContext(Dispatchers.Default) { Solver(nonogram).solveNonogram() }
