package com.trainpaths.nonogram.classes

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
            val diff = ng.width - max

            if (diff == 0) {
                var l = 0
                for ((index, clue) in clues.withIndex()) {
                    val r = l + clue

                    drawRowTiles(l, r, row, index)

                    if (r >= ng.width) continue
                    solution[row][r] = 2
                    l = r + 1
                }
                continue
            } else if (max > diff && clues.max() > diff) {
                var l = 0
                var r = diff
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        drawRowTiles(r, l, row, index)
                    }
                    r += clue + 1
                    l++
                }
            }
        }

        // first time COLS
        for (col in 0 until ng.width) {
            val clues: List<Int> = ng.colClues[col]
            val max = clues.sum() + clues.size - 1
            val diff = ng.height - max

            if (diff == 0) {
                var l = 0
                for ((index, clue) in clues.withIndex()) {
                    val r = l + clue

                    drawColTiles(l, r, col, index)

                    if (r >= ng.height) continue
                    solution[r][col] = 2
                    l = r + 1
                }
                continue
            } else if (max > diff && clues.max() > diff) {
                var l = 0
                var r = diff
                for ((index, clue) in clues.withIndex()) {
                    l += clue
                    if (l > r && l - r < clue) {
                        drawColTiles(r, l, col, index)
                    }
                    r += clue + 1
                    l++
                }
            }
        }

        for (col in trackingCols) {
            val clues: List<Int> = ng.colClues[col]

            //top check for constraints
            var top = clues[0]
            var topCount = 0
            while (solution[top + topCount][col] == 1) {
                trackingRows.add(topCount)
                solution[topCount++][col] = 2
            }
            top += topCount
            for (row in topCount until top) {
                if (solution[row][col] == 1) {
                    var len = 0
                    for (r in row until top) {
                        solution[r][col] = 1
                        len++
                    }
                    if (len == clues[0]) {
                        solution[top][col] = 2
                        trackingRows.addAll(row until top + 1)
                    } else {
                        trackingRows.addAll(row until top)
                    }
                    break
                }
            }

            //bot check for constraints
            var bot = ng.height - clues.last()
            var botCount = 1

            while (solution[bot - botCount][col] == 1) {
                val newBot = ng.height - botCount

                trackingRows.add(newBot)
                solution[newBot][col] = 2

                botCount++
            }
            bot -= --botCount

            val botEnd = ng.height - botCount
            for (row in bot until botEnd) {
                if (solution[row][col] == 1) {
                    var len = 1
                    for (r in bot until row) {
                        solution[r][col] = 1
                        len++
                    }
                    if (len == clues.last()) {
                        solution[bot--][col] = 2
                        trackingRows.addAll(bot until ng.height)
                    } else {
                        trackingRows.addAll(bot until row)
                    }
                    break
                }
            }
        }
        trackingCols.clear()

        for (row in trackingRows) {
            val clues: List<Int> = ng.rowClues[row]

            //left check for constraints
            var left = clues[0]
            var leftCount = 0
            while (solution[row][left + leftCount] == 1) {
                trackingCols.add(leftCount)
                solution[row][leftCount++] = 2
            }
            left += leftCount
            for (col in leftCount until left) {
                if (solution[row][col] == 1) {
                    var len = 0
                    for (c in col until left) {
                        solution[row][c] = 1
                        len++
                    }
                    if (len == clues[0]) {
                        solution[row][left] = 2
                        trackingCols.addAll(row until left + 1)
                    } else {
                        trackingCols.addAll(row until left)
                    }
                    break
                }
            }

            //right check for constraints
            var right = ng.width - clues.last()
            var rightCount = 1

            while (solution[row][right - rightCount] == 1) {
                val newRight = ng.width - rightCount

                trackingCols.add(newRight)
                solution[row][newRight] = 2

                rightCount++
            }
            right -= --rightCount

            val rightEnd = ng.width - rightCount
            for (col in right until rightEnd) {
                if (solution[row][col] == 1) {
                    var len = 1
                    for (c in right until col) {
                        solution[row][c] = 1
                        len++
                    }
                    if (len == clues.last()) {
                        solution[row][right--] = 2
                        trackingCols.addAll(right until ng.width)
                    } else {
                        trackingCols.addAll(right until col)
                    }
                    break
                }
            }
        }
        trackingRows.clear()

        return solution
    }

    private fun drawRowTiles(l: Int, r: Int, row: Int, index: Int?) {
        for (col in l until r) {
            val cell = solving[row][col]
            cell.state = 1
            if (index != null) {
                cell.posRowClues.add(index)
            }

            solution[row][col] = 1
            trackingCols.add(col)
        }
    }

    private fun drawColTiles(l: Int, r: Int, col: Int, index: Int?) {
        for (row in l until r) {
            val cell = solving[row][col]
            cell.state = 1
            if (index != null) {
                cell.posColClues.add(index)
            }

            solution[row][col] = 1
            trackingRows.add(row)
        }
    }
}