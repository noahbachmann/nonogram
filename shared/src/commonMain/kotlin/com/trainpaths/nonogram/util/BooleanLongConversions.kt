package com.trainpaths.nonogram.util

internal fun Boolean.toLong(): Long = if (this) 1L else 0L

internal fun Long.toBoolean(): Boolean = this != 0L
