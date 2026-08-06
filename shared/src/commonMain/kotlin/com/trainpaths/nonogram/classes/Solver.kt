package com.trainpaths.nonogram.classes

data class Cell(
    var state: Int = 0,
    val posRowClues: MutableSet<Int> = mutableSetOf(),
    val posColClues: MutableSet<Int> = mutableSetOf(),
) {
    val rowClue: Int
        get() = posRowClues.singleOrNull() ?: -1

    val colClue: Int
        get() = posColClues.singleOrNull() ?: -1
}

class Solver(val ng: Nonogram) {

    val solution: Array<Array<Int>> = Array(ng.height) { Array(ng.width) { 0 } }
    val solving: Array<Array<Cell>> = Array(ng.height) { Array(ng.width) { Cell() } }
    val trackingRows: MutableSet<Int> = mutableSetOf()
    val trackingCols: MutableSet<Int> = mutableSetOf()

    fun solveNonogram(): Array<Array<Int>> {
        // first time ROWS
        for (row in 0 until ng.height) {
            val clues: List<Int> = ng.rowClues[row]
            val max = clues.sum() + clues.size - 1
            val emptyCells = ng.width - max

            if (emptyCells == 0) {
                var l = 0
                for ((index, clue) in clues.withIndex()) {
                    val r = l + clue

                    for (col in l until r) {
                        drawTile(row, col, index, isClear = true)
                    }

                    if (r >= ng.width) continue

                    drawCross(row, r)
                    l = r + 1
                }
            } else if (max > emptyCells && clues.max() > emptyCells) {
                var l = 0
                var r = emptyCells
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        for (col in r until l) {
                            drawTile(row, col, index, isClear = true)
                        }
                    }
                    r += clue + 1
                    l++
                }
            }
            computePosCellClues(clues, emptyCells, { col -> solving[row][col] })
        }

        // first time COLS
        for (col in 0 until ng.width) {
            val clues: List<Int> = ng.colClues[col]
            val max = clues.sum() + clues.size - 1
            val emptyCells = ng.height - max

            if (emptyCells == 0) {
                var l = 0
                for ((index, clue) in clues.withIndex()) {
                    val r = l + clue

                    for (row in l until r) {
                        drawTile(row, col, index, false, isClear = true)
                    }

                    if (r >= ng.height) continue

                    drawCross(r, col)
                    l = r + 1
                }
            } else if (max > emptyCells && clues.max() > emptyCells) {
                var l = 0
                var r = emptyCells
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        for (row in r until l) {
                            drawTile(row, col, index, false, isClear = true)
                        }
                    }
                    r += clue + 1
                    l++
                }
            }
            computePosCellClues(clues, emptyCells, { row -> solving[row][col] }, false)
        }

        while (trackingRows.isNotEmpty() || trackingCols.isNotEmpty()) {

            val colsToProcess = trackingCols.toList()
            trackingCols.clear()

            for (col in colsToProcess) {
                recomputePosCellClues(col, cellAt = { row -> solving[row][col] }, false)
            }

            val rowsToProcess = trackingRows.toList()
            trackingRows.clear()

            for (row in rowsToProcess) {
                recomputePosCellClues(row, cellAt = { col -> solving[row][col] })
            }
        }

        val check = solving.toSolution()
        return check
    }

    private fun computePosCellClues(clues: List<Int>, emptyCells: Int, cellAt: (Int) -> Cell, isRow: Boolean = true) {
        var left = 0
        for ((index, clue) in clues.withIndex()) {
            for (i in left until left + clue + emptyCells) {
                val cell = cellAt(i)
                if (cell.state != 2) {
                    if (isRow) {
                        cell.posRowClues.add(index)
                    } else {
                        cell.posColClues.add(index)
                    }
                }
            }
            left += clue + 1
        }
    }

    private fun recomputePosCellClues(rowIndex: Int, cellAt: (Int) -> Cell, isRow: Boolean = true) {
        var leftOffset = 0
        var index = 0
        if (isRow) {
            val clues = ng.rowClues[rowIndex]
            while (index < ng.width) {
                val cell = cellAt(index)
                if (cell.state == 1) {
                    var count = 1

                    while (index + count < ng.width && cellAt(index + count).state == 1) {
                        count++
                    }

                    if (cell.rowClue > -1) {
                        val clue = clues[cell.rowClue]
                        while (leftOffset > 0 && count + leftOffset > clue) {
                            val checkCell = cellAt(index - leftOffset)
                            checkCell.posRowClues.remove(cell.rowClue)
                            if (checkCell.posRowClues.isEmpty()) {
                                drawCross(rowIndex, index - leftOffset)
                            }
                            leftOffset--
                        }
                        while (count + leftOffset < clue && index + count < ng.width) {
                            val checkCell = cellAt(index + count)
                            if (checkCell.state == 0) {
                                drawTile(rowIndex, index + count, cell.rowClue, isClear = true)
                            }
                            count++
                        }
                        if (count == clue) {
                            if (index + count < ng.width) {
                                drawCross(index + count, rowIndex)
                            }
                            if (index > 0) {
                                drawCross(index - 1, rowIndex)
                            }
                        } else {
                            var missingAmount = clue - count
                            for (i in missingAmount - 1 downTo 0) {
                                val checkCell = cellAt(index + count + i)
                                if (checkCell.rowClue == cell.rowClue && checkCell.state == 1) {
                                    for (j in i - 1 downTo 0) {
                                        drawTile(rowIndex, index + count + j, checkCell.rowClue, isClear = true)
                                    }
                                    count += i + 1
                                    missingAmount -= i + 1
                                    break
                                }
                            }
                            for (cellIndex in 0 until ng.width) {
                                if (count + index + missingAmount > cellIndex && cellIndex >= index - missingAmount) continue
                                val crossCell = cellAt(cellIndex)
                                crossCell.posRowClues.remove(cell.rowClue)
                                if (crossCell.posRowClues.isEmpty()) {
                                    drawCross(rowIndex, cellIndex)
                                }
                            }
                        }
                    } else {
                        val iterator = cell.posRowClues.iterator()

                        while (iterator.hasNext()) {
                            val posIndex = iterator.next()
                            val clue = clues[posIndex]
                            if (count > clue) {
                                iterator.remove()
                                continue
                            }
                            var counter = count
                            while (counter + leftOffset < clue && index + counter < ng.width) {
                                if (cellAt(index + counter).state == 2) {
                                    iterator.remove()
                                    break
                                }
                                counter++
                            }
                        }
                        val clues = cell.posRowClues.map { index ->
                            clues[index]
                        }
                        if (clues.toSet().size == 1 && clues.first() == count) {
                            if (index + count < ng.width) {
                                drawCross(index + count, rowIndex)
                            }
                            if (index > 0) {
                                drawCross(index - 1, rowIndex)
                            }
                        }
                    }
                    leftOffset = 0
                    index += ++count
                    continue
                } else {
                    index++

                    if (cell.state == 0) {
                        leftOffset++
                    } else if (cell.state == 2) {
                        leftOffset = 0
                    }
                }
            }
        } else {
            val clues = ng.colClues[rowIndex]
            while (index < ng.height) {
                val cell = cellAt(index)
                if (cell.state == 1) {
                    var count = 1

                    while (index + count < ng.height && cellAt(index + count).state == 1) {
                        count++
                    }

                    if (cell.colClue > -1) {
                        val clue = clues[cell.colClue]
                        while (count + leftOffset < clue && index + count < ng.height) {
                            val checkCell = cellAt(index + count)
                            if (checkCell.state == 0) {
                                drawTile(index + count, rowIndex, cell.colClue, isRow = false, isClear = true)
                            }
                            count++
                        }
                        if (count == clue) {
                            if (index + count < ng.height) {
                                drawCross(index + count, rowIndex)
                            }
                            if (index > 0) {
                                drawCross(index - 1, rowIndex)
                            }
                        } else {
                            val diff = clue - count
                            for (cellIndex in 0 until ng.height) {
                                if (count + index + diff > cellIndex && cellIndex >= index - diff) continue
                                val crossCell = cellAt(cellIndex)
                                crossCell.posColClues.remove(cell.colClue)
                                if (crossCell.posColClues.isEmpty()) {
                                    drawCross(cellIndex, rowIndex)
                                }
                            }
                        }
                    } else {
                        val iterator = cell.posColClues.iterator()

                        while (iterator.hasNext()) {
                            val posIndex = iterator.next()
                            val clue = clues[posIndex]
                            if (count > clue) {
                                iterator.remove()
                                continue
                            }
                            var counter = count
                            while (counter + leftOffset < clue && index + counter < ng.height) {
                                if (cellAt(index + counter).state == 2) {
                                    iterator.remove()
                                    break
                                }
                                counter++
                            }
                        }
                        val clues = cell.posColClues.map { index ->
                            clues[index]
                        }
                        if (clues.toSet().size == 1 && clues.first() == count) {
                            if (index + count < ng.height) {
                                drawCross(index + count, rowIndex)
                            }
                            if (index > 0) {
                                drawCross(index - 1, rowIndex)
                            }
                        }
                    }
                    leftOffset = 0
                    index += ++count
                    continue
                } else {
                    index++

                    if (cell.state == 0) {
                        leftOffset++
                    } else if (cell.state == 2) {
                        leftOffset = 0
                    }
                }
            }
        }
    }

    private fun drawTile(row: Int, col: Int, index: Int?, isRow: Boolean = true, isClear: Boolean = false) {
        val cell = solving[row][col]
        cell.state = 1
        if (index != null) {
            if (isRow) {
                if (isClear) {
                    cell.posRowClues.clear()
                    cell.posRowClues.add(index)
                } else {
                    cell.posRowClues.add(index)
                }
                trackingCols.add(col)
            } else {
                if (isClear) {
                    cell.posColClues.clear()
                    cell.posColClues.add(index)
                } else {
                    cell.posColClues.add(index)
                }
                trackingRows.add(row)
            }
        }
        solution[row][col] = 1
    }

    private fun drawCross(row: Int, col: Int) {
        val cell = solving[row][col]
        cell.state = 2
        cell.posRowClues.clear()
        cell.posColClues.clear()

        trackingRows.add(row)
        trackingCols.add(col)

        solution[row][col] = 2
    }

    private fun Array<Array<Cell>>.toSolution(): Array<Array<Int>> {
        return Array(size) { row ->
            Array(this[row].size) { col ->
                this[row][col].state
            }
        }
    }
}

