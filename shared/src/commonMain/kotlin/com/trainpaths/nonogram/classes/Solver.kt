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
    val solving: Array<Array<Cell>> = Array(ng.height) { Array(ng.width) { Cell() } }
    val trackingRows: MutableSet<Int> = mutableSetOf()
    val trackingCols: MutableSet<Int> = mutableSetOf()
    private val solvedRowClues: List<MutableSet<Int>> =
        List(ng.height) { mutableSetOf() }
    private val solvedColClues: List<MutableSet<Int>> =
        List(ng.width) { mutableSetOf() }

    fun firstRun(isRow: Boolean = true) {
        val height = if (isRow) ng.height else ng.width
        val width = if (isRow) ng.width else ng.height

        for (row in 0 until height) {
            val clues: List<Int> = if (isRow) ng.rowClues[row] else ng.colClues[row]

            if (clues.isEmpty()) {
                for (col in 0 until width) {
                    if (isRow) drawCross(row, col)
                    else drawCross(col, row)
                }
                continue
            }

            val max = clues.sum() + clues.size - 1
            val emptyCells = width - max

            if (emptyCells == 0) {
                var l = 0
                for ((index, clue) in clues.withIndex()) {
                    val r = l + clue

                    for (col in l until r) {
                        drawTile(row, col, index, isRow = isRow, isClear = true)
                    }

                    if (r >= width) continue

                    if (isRow) drawCross(row, r)
                    else drawCross(r, row)
                    l = r + 1
                }
                if (isRow) trackingRows.remove(row)
                else trackingCols.remove(row)
            } else if (max > emptyCells && clues.max() > emptyCells) {
                var l = 0
                var r = emptyCells
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        for (col in r until l) {
                            drawTile(row, col, index, isRow = isRow, isClear = true)
                        }
                    }
                    r += clue + 1
                    l++
                }
            }
            if (isRow) computePosCellClues(clues, emptyCells, { col -> solving[row][col] })
            else computePosCellClues(clues, emptyCells, { col -> solving[col][row] }, isRow = false)
        }
    }

    fun solveNonogram(): Array<Array<Int>> {
        // first time ROWS
        firstRun()
        firstRun(false)

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
        val solvedClues = if (isRow) solvedRowClues[rowIndex] else solvedColClues[rowIndex]
        val emptyCells = rowSize - (clues.sum() + clues.size - 1)

        fun recursive(startIndex: Int, offset: Int = 0, goesBack: Boolean = true): Int {
            val currIndex = if (goesBack) startIndex - offset else startIndex + offset
            val currCell = cellAt(currIndex)
            if (currCell.state == 2) return offset

            val amount = if (currIndex == 0 || currIndex >= rowSize - 1) {
                offset + 1
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
                if (isRow) drawCross(rowIndex, currIndex)
                else drawCross(currIndex, rowIndex)
            }

            return amount
        }

        fun constraintDraw() {
            var left = 0

            for ((index, clue) in clues.withIndex()) {
                val range: MutableSet<Int> = mutableSetOf()
                var count = 0
                for (i in left until left + clue + emptyCells) {
                    val cell = cellAt(i)
                    val posClues = if (isRow) cell.posRowClues else cell.posColClues

                    if (cell.state != 2 && posClues.contains(index)) {
                        range.add(i)
                        count++
                    } else {
                        if (count < clue) {
                            for (j in i - 1 downTo i - count) {
                                val currClues = if (isRow) cellAt(j).posRowClues else cellAt(j).posColClues
                                currClues.remove(index)
                                if (currClues.isEmpty()) {
                                    if (isRow) drawCross(rowIndex, j)
                                    else drawCross(j, rowIndex)
                                }
                                range.remove(j)
                            }
                        }
                        count = 0
                    }
                }
                val min = range.min()
                val max = range.max() + 1

                val diff = max - min

                if (diff >= clue && diff < clue * 2) {
                    if (diff == clue) {
                        solvedClues.add(index)
                    }

                    val r = min + clue
                    val l = max - clue

                    for (i in l until r) {
                        drawTile(rowIndex, i, index, isRow, isClear = true)
                    }
                }

                left += clue + 1
            }
        }

        constraintDraw()

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
                        solvedClues.add(clueIndex)
                        if (index + count < rowSize) {
                            if (isRow) drawCross(rowIndex, index + count)
                            else drawCross(index + count, rowIndex)
                        }
                        if (index > 0) {
                            if (isRow) drawCross(rowIndex, index - 1)
                            else drawCross(index - 1, rowIndex)
                        }
                    } else {
                        var missingAmount = clue - count
                        for (i in missingAmount - 1 downTo 0) {
                            val checkIndex = index + count + i
                            if (checkIndex >= rowSize) continue
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


                    val filteredClues = posClues.map { index ->
                        clues[index]
                    }

                    if (filteredClues.toSet().size == 1 && filteredClues.first() == count) {
                        if (posClues.size == 1) {
                            solvedClues.add(posClues.first())
                        }
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
                if (clues.size == solvedClues.size) {
                    if (isRow) drawCross(rowIndex, index)
                    else drawCross(index, rowIndex)
                }
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
                    while (r < rowSize && cellAt(r).state == 2) r++
                }
                index = r
            }
        }

    }

    private fun drawTile(row: Int, col: Int, index: Int?, isRow: Boolean = true, isClear: Boolean = false) {
        val cell = if (isRow) solving[row][col] else solving[col][row]
        val posClues = if (isRow) cell.posRowClues else cell.posColClues

        if (index != null) {
            posClues.add(index)
            if (isClear) posClues.clear()
            posClues.add(index)
            if (cell.state == 1) return

            if (isRow) trackingCols.add(col)
            else trackingRows.add(col)
        }
        cell.state = 1
    }

    private fun drawCross(row: Int, col: Int) {
        val cell = solving[row][col]
        cell.posRowClues.clear()
        cell.posColClues.clear()
        if (cell.state == 2) return

        cell.state = 2
        trackingRows.add(row)
        trackingCols.add(col)
    }

    private fun Array<Array<Cell>>.toSolution(): Array<Array<Int>> {
        return Array(size) { row ->
            Array(this[row].size) { col ->
                val state = this[row][col].state
                if (state > 1) 0 else state
            }
        }
    }
}
