package com.trainpaths.nonogram.classes

/**
 * Which clues of one line the player has *certainly drawn*
 * It only reads what the player has explicitly marked, so it can never hand them a
 * deduction they had not made themselves.
 *
 * Returns a [Long] because a line holds at most `ceil(MAX_NONOGRAM_SIDE / 2)` = 30 clues.
 */
fun solvedClueMask(clues: List<Int>, cells: List<TileState>): Long {
    val k = clues.size
    val n = cells.size
    if (k == 0 || n == 0) return 0L

    val prefix = IntArray(k + 1)
    for (i in 0 until k) prefix[i + 1] = prefix[i] + clues[i]
    fun minLen(a: Int, b: Int): Int = if (b <= a) 0 else prefix[b] - prefix[a] + (b - a) - 1

    var mask = 0L
    var c = 0
    var start = 0
    while (start < n) {
        if (cells[start] != TileState.FILLED) {
            start++
            continue
        }
        var end = start
        while (end < n && cells[end] == TileState.FILLED) end++

        val isSealed = (start == 0 || cells[start - 1] == TileState.CROSSED) &&
                (end == n || cells[end] == TileState.CROSSED)
        val len = end - start

        while (c < k) {
            if (c > 0 && minLen(0, c) > start - 1) {
                c = k
                break
            }
            val matches = if (isSealed) clues[c] == len else clues[c] >= len
            val fitsRight = c + 1 >= k || minLen(c + 1, k) <= n - end - 1
            if (matches && fitsRight) break
            c++
        }
        if (c == k) break

        if (isSealed) mask = mask or (1L shl c)
        c++
        start = end
    }
    return mask
}
