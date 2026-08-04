package com.trainpaths.nonogram.classes

data class Cell(
    var state: Int = 0,
    val posRowClues: MutableSet<Int> = mutableSetOf(),
    val posColClues: MutableSet<Int> = mutableSetOf(),
    var rowClue: Int = -1,
    var colClue: Int = -1,
)

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
                        drawTile(row, col, index, isInitial = true)
                    }

                    if (r >= ng.width) continue

                    drawCross(row, r)
                    l = r + 1
                }
                continue
            } else if (max > emptyCells && clues.max() > emptyCells) {
                var l = 0
                var r = emptyCells
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        for (col in r until l) {
                            drawTile(row, col, index, isInitial = true)
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
                        drawTile(row, col, index, false, isInitial = true)
                    }

                    if (r >= ng.height) continue

                    drawCross(r, col)
                    l = r + 1
                }
                continue
            } else if (max > emptyCells && clues.max() > emptyCells) {
                var l = 0
                var r = emptyCells
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        for (row in r until l) {
                            drawTile(row, col, index, false, isInitial = true)
                        }
                    }
                    r += clue + 1
                    l++
                }
            }
            computePosCellClues(clues, emptyCells, { row -> solving[row][col] }, false)

            //first partial col check
            if (col in trackingCols) {
                colCheck(col)
            }
        }
        trackingCols.clear()

        //first partial row check
        for (row in trackingRows) {
            rowCheck(row)
        }
        trackingRows.clear()

        return solution
    }

    private fun drawTile(row: Int, col: Int, index: Int?, isRow: Boolean = true, isInitial: Boolean = false) {
        val cell = solving[row][col]
        cell.state = 1
        if (index != null) {
            if (isRow) {
                if (isInitial) {
                    cell.rowClue = index
                } else {
                    cell.posRowClues.add(index)
                }
                trackingCols.add(col)
            } else {
                if (isInitial) {
                    cell.colClue = index
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

        solution[row][col] = 2
    }

    private fun rowCheck(row: Int) {
        val clues: List<Int> = ng.rowClues[row]
        val emptyCells: Int = ng.width - (clues.sum() + clues.size - 1)
        computePosCellClues(clues, emptyCells, { col -> solving[row][col] })

        //left check for constraints
        var left = clues[0]
        var leftOffset = 0
        while (solution[row][leftOffset] == 2) {
            leftOffset++
        }

        //check if perfect distance between constraint and nearest cell
        while (solution[row][left + leftOffset] == 1) {
            drawCross(row, leftOffset++)
            trackingCols.add(leftOffset)
        }

        left += leftOffset
        for (col in leftOffset until left) {
            if (solution[row][col] == 1) {
                var len = 0
                for (c in col until left) {
                    drawTile(row, c, 0)
                    len++
                }
                if (len == clues[0]) {
                    drawCross(row, left)
                    trackingCols.addAll(row until left + 1)
                } else {
                    trackingCols.addAll(row until left)
                }
                break
            }
        }

        //right check for constraints
        var right = ng.width - clues.last()
        var rightOffset = 1

        while (solution[row][right - rightOffset] == 1) {
            val newRight = ng.width - rightOffset

            drawCross(row, newRight)
            trackingCols.add(newRight)

            rightOffset++
        }
        right -= --rightOffset

        val rightEnd = ng.width - rightOffset
        for (col in rightEnd - 1 downTo right) {
            if (solution[row][col] == 1) {
                var len = 1
                for (c in right until col) {
                    drawTile(row, c, clues.size - 1)
                    len++
                }
                if (len == clues.last()) {
                    drawCross(row, right--)
                    trackingCols.addAll(right until ng.width)
                } else {
                    trackingCols.addAll(right until col)
                }
                break
            }
        }
    }

    private fun colCheck(col: Int) {
        val clues: List<Int> = ng.colClues[col]
        val emptyCells: Int = ng.height - (clues.sum() + clues.size - 1)
        computePosCellClues(clues, emptyCells, cellAt = { row -> solving[row][col] }, false)
        //top check
        var top = clues[0]
        var topOffset = 0
        while (solution[topOffset][col] == 2) {
            topOffset++
        }
        while (solution[top + topOffset][col] == 1) {
            drawCross(topOffset++, col)
            trackingRows.add(topOffset)
        }
        top += topOffset
        for (row in topOffset until top) {
            if (solution[row][col] == 1) {
                var len = 0
                for (r in row until top) {
                    drawTile(r, col, 0, false)
                    len++
                }
                if (len == clues[0]) {
                    drawCross(top, col)
                    trackingRows.addAll(row until top + 1)
                } else {
                    trackingRows.addAll(row until top)
                }
                break
            }
            if (solution[row][col] == 2) {
                for (r in topOffset until row) {
                    drawCross(r, col)
                    trackingRows.add(r)
                }
                topOffset = row + 1
                top += topOffset
            }
        }

        //bot check
        var bot = ng.height - clues.last()
        var botOffset = 1

        while (solution[bot - botOffset][col] == 1) {
            val newBot = ng.height - botOffset

            drawCross(newBot, col)
            trackingRows.add(newBot)

            botOffset++
        }
        bot -= --botOffset

        val botEnd = ng.height - botOffset
        for (row in botEnd - 1 downTo bot) {
            if (solution[row][col] == 1) {
                var len = 1
                for (r in bot until row) {
                    drawTile(r, col, clues.size - 1, false)
                    len++
                }
                if (len == clues.last()) {
                    drawCross(bot--, col)
                    trackingRows.addAll(bot until ng.height)
                } else {
                    trackingRows.addAll(bot until row)
                }
                break
            }
        }
    }

    private fun computePosCellClues(clues: List<Int>, emptyCells: Int, cellAt: (Int) -> Cell, isRow: Boolean = true) {
        var left = 0
        for ((index, clue) in clues.withIndex()) {
            for (i in left until left + clue + emptyCells) {
                val cell = cellAt(i)
                if (isRow) {
                    if (cell.rowClue < 0 && cell.state != 2) {
                        cell.posRowClues.add(index)
                    }
                } else {
                    if (cell.colClue < 0 && cell.state != 2) {
                        cell.posColClues.add(index)
                    }
                }
            }
            left += clue + 1
        }
    }

    private fun fillPart(start: Int, emptyCells: Int, index: Int, row: Int, isRow: Boolean = true) {
        var l = start
        var r = emptyCells
        l += if (isRow) {
            ng.rowClues[row][index]
        } else {
            ng.colClues[row][index]
        }
        for (i in l - 1 downTo start) {
            if (isRow) {
                if (solution[row][i] == 2) {
                    l++
                    for (j in i - 1 downTo start) {
                        drawCross(row, j)
                        l++
                    }
                    break
                } else if (solution[row][i] == 1) {
                    for (j in i + 1 until l) {
                        drawTile(row, j, index)
                    }
                }
            } else {
                if (solution[i][row] == 2) {
                    l++
                    for (j in i - 1 downTo start) {
                        drawCross(j, row)
                        l++
                    }
                    break
                } else if (solution[i][row] == 1) {
                    for (j in i + 1 until l) {
                        drawTile(j, row, index, isRow = false)
                    }
                }
            }
        }
        if (l > r && l - r < clue) {
            for (col in r until l) {
                if (isRow) {
                    drawTile(row, col, index, true)
                } else {
                    drawTile(col, row, index, false)
                }
            }
        }
    }
}

