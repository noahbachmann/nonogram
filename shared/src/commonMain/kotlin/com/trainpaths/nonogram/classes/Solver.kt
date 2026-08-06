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
                        drawTile(col, row, index, false, isClear = true)
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
                            drawTile(col, row, index, false, isClear = true)
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
                recomputePosCellClues(col, cellAt = { row -> solving[row][col] }, ng.height, false)
            }

            val rowsToProcess = trackingRows.toList()
            trackingRows.clear()

            for (row in rowsToProcess) {
                recomputePosCellClues(row, cellAt = { col -> solving[row][col] }, ng.width)
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

    private fun recomputePosCellClues(
        rowIndex: Int,
        cellAt: (Int) -> Cell,
        rowSize: Int,
        isRow: Boolean = true
    ) {
        val clues = if (isRow) ng.rowClues[rowIndex] else ng.colClues[rowIndex]

        fun recursive(startIndex: Int, offset: Int = 0, goesBack: Boolean = true): Int {
            val currIndex = if (goesBack) startIndex - offset else startIndex + offset
            val currCell = cellAt(currIndex)
            if (currCell.state == 2) return offset

            val amount = if (currIndex == 0 || currIndex >= rowSize - 1) {
                if (goesBack) startIndex + 1 else currIndex - startIndex
            } else {
                recursive(
                    startIndex,
                    offset + 1,
                    goesBack
                )
            }

            val currClues = if (isRow) currCell.posRowClues else currCell.posColClues

            currClues.removeAll { clueIndex ->
                clues[clueIndex] > amount
            }
            if (currClues.isEmpty()) {
                currCell.state = 2
            }

            return amount
        }

        var leftOffset = 0
        var index = 0

        while (index < rowSize) {
            val cell = cellAt(index)
            if (cell.state == 1) {
                var count = 1

                while (index + count < rowSize && cellAt(index + count).state == 1) {
                    count++
                }

                val clueIndex: Int = if (isRow) cell.rowClue else cell.colClue

                if (clueIndex > -1) {
                    val clue = clues[clueIndex]
                    while (leftOffset > 0 && count + leftOffset > clue) {
                        val checkCellClues =
                            if (isRow) cellAt(index - leftOffset).posRowClues else cellAt(index - leftOffset).posColClues
                        checkCellClues.remove(clueIndex)

                        if (checkCellClues.isEmpty()) {
                            if (isRow) drawCross(rowIndex, index - leftOffset)
                            else drawCross(index - leftOffset, rowIndex)
                        }
                        leftOffset--
                    }
                    while (count + leftOffset < clue && index + count < rowSize) {
                        val checkCell = cellAt(index + count)
                        if (checkCell.state == 0) {
                            drawTile(rowIndex, index + count, clueIndex, isClear = true, isRow = isRow)
                        }
                        count++
                    }
                    if (count == clue) {
                        if (index + count < rowSize) {
                            if (isRow) drawCross(rowIndex, index + count)
                            else drawCross(index + count, rowIndex)
                        }
                        if (index > 0) {
                            if (isRow) drawCross(rowIndex, index - 1)
                            else drawCross(index - count, rowIndex)
                        }
                    } else {
                        var missingAmount = clue - count
                        for (i in missingAmount - 1 downTo 0) {
                            val checkCell = cellAt(index + count + i)

                            val checkCellClue = if (isRow) checkCell.rowClue else checkCell.colClue

                            if (checkCellClue == clueIndex && checkCell.state == 1) {
                                for (j in i - 1 downTo 0) {
                                    drawTile(rowIndex, index + count + j, checkCellClue, isClear = true, isRow = isRow)
                                }
                                count += i + 1
                                missingAmount -= i + 1
                                break
                            }
                        }
                        for (cellIndex in 0 until rowSize) {
                            if (count + index + missingAmount > cellIndex && cellIndex >= index - missingAmount) continue

                            val crossCellClues =
                                if (isRow) cellAt(cellIndex).posRowClues else cellAt(cellIndex).posColClues
                            crossCellClues.remove(clueIndex)

                            if (crossCellClues.isEmpty()) {
                                if (isRow) drawCross(rowIndex, cellIndex)
                                else drawCross(cellIndex, rowIndex)
                            }
                        }
                    }
                } else {
                    val posClues = if (isRow) cell.posRowClues else cell.posColClues

                    val iterator = posClues.iterator()

                    while (iterator.hasNext()) {
                        val posIndex = iterator.next()
                        val clue = clues[posIndex]
                        if (count > clue) {
                            iterator.remove()
                            continue
                        }
                        var counter = count
                        while (counter + leftOffset < clue && index + counter < rowSize) {
                            if (cellAt(index + counter).state == 2) {
                                iterator.remove()
                                break
                            }
                            counter++
                        }
                    }
                    val clues = posClues.map { index ->
                        clues[index]
                    }
                    if (clues.toSet().size == 1 && clues.first() == count) {
                        if (index + count < rowSize) {
                            if (isRow) drawCross(rowIndex, index + count)
                            else drawCross(index + count, rowIndex)
                        }
                        if (index > 0) {
                            if (isRow) drawCross(rowIndex, index - 1)
                            else drawCross(index - 1, rowIndex)
                        }
                    }
                }
                leftOffset = 0
                index += ++count
                continue
            } else if (cell.state == 0) {
                leftOffset++
                index++
            } else if (cell.state == 2) {
                val l = index - 1
                if (l > -1) {
                    recursive(l)
                }
                var r = index + 1
                if (r < rowSize) {
                    recursive(r, goesBack = false)
                    while (cellAt(r).state == 2) r++
                }
                index = r
            }
        }
    }

    private fun drawTile(row: Int, col: Int, index: Int?, isRow: Boolean = true, isClear: Boolean = false) {
        val cell = if (isRow) solving[row][col] else solving[col][row]
        cell.state = 1
        if (index != null) {
            if (isRow) {
                if (isClear) {
                    cell.posRowClues.clear()
                }
                cell.posRowClues.add(index)
                trackingCols.add(col)
                solution[row][col] = 1
            } else {
                if (isClear) {
                    cell.posColClues.clear()
                }
                cell.posColClues.add(index)
                trackingRows.add(col)
                solution[col][row] = 1
            }
        }

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

