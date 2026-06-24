package com.trainpaths.nonogram

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform