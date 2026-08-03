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

                    drawCross(row, r)
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

                    drawCross(r, col)
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
                drawCross(topCount++, col)
                trackingRows.add(topCount)
            }
            top += topCount
            for (row in topCount until top) {
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
            }

            //bot check for constraints
            var bot = ng.height - clues.last()
            var botCount = 1

            while (solution[bot - botCount][col] == 1) {
                val newBot = ng.height - botCount

                drawCross(newBot, col)
                trackingRows.add(newBot)

                botCount++
            }
            bot -= --botCount

            val botEnd = ng.height - botCount
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
        trackingCols.clear()

        for (row in trackingRows) {
            val clues: List<Int> = ng.rowClues[row]

            //left check for constraints
            var left = clues[0]
            var leftCount = 0
            while (solution[row][left + leftCount] == 1) {
                drawCross(row, leftCount++)
                trackingCols.add(leftCount)
            }
            left += leftCount
            for (col in leftCount until left) {
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
            var rightCount = 1

            while (solution[row][right - rightCount] == 1) {
                val newRight = ng.width - rightCount

                drawCross(row, newRight)
                trackingCols.add(newRight)

                rightCount++
            }
            right -= --rightCount

            val rightEnd = ng.width - rightCount
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
        trackingRows.clear()

        return solution
    }

    private fun drawRowTiles(l: Int, r: Int, row: Int, index: Int?) {
        for (col in l until r) {
            drawTile(row, col, index)
        }
    }

    private fun drawColTiles(l: Int, r: Int, col: Int, index: Int?) {
        for (row in l until r) {
            drawTile(row, col, index, false)
        }
    }

    private fun drawTile(row: Int, col: Int, index: Int?, isRow: Boolean = true) {
        val cell = solving[row][col]
        cell.state = 1
        if (index != null) {
            if (isRow) {
                cell.posRowClues.add(index)
                trackingCols.add(col)
            } else {
                cell.posColClues.add(index)
                trackingRows.add(row)
            }
        }
        solution[row][col] = 1
    }

    private fun drawCross(row: Int, col: Int) {
        val cell = solving[row][col]
        cell.state = 2

        solution[row][col] = 2
    }
}