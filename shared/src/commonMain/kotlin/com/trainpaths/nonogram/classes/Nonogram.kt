package com.trainpaths.nonogram.classes

class Nonogram(val rowClues: List<List<Int>>, val colClues: List<List<Int>>) {
    val height: Int = rowClues.size
    val width: Int = colClues.size
    val tiles: List<List<Tile>> = List(height) { List(width) { Tile() } }
    fun tileAt(row: Int, col: Int): Tile = tiles[row][col]
}