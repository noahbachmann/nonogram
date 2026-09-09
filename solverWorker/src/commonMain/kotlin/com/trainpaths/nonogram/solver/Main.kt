@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.solver

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.SolveResponse
import com.trainpaths.nonogram.classes.Solver
import com.trainpaths.nonogram.classes.decodeSolveRequest
import com.trainpaths.nonogram.classes.encode
import org.w3c.dom.DedicatedWorkerGlobalScope
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

private external val self: DedicatedWorkerGlobalScope

/**
 * The whole worker: read a grid, run the Solver, post the deduction back. Nothing is retained
 * between messages, so a failed solve cannot poison the next one.
 */
fun main() {
    self.onmessage = { event ->
        val text = event.data?.toString()
        if (text != null) self.postMessage(solve(text).toJsString())
    }
}

private fun solve(text: String): String {
    val request = try {
        decodeSolveRequest(text)
    } catch (error: Throwable) {
        // No id to answer with, so there is nothing the page can match this to; say so anyway.
        return SolveResponse(id = -1, error = error.message ?: "unreadable request").encode()
    }
    return try {
        val puzzle = Nonogram(id = 0, difficulty = Difficulty.EASY, solution = request.grid)
        SolveResponse(request.id, deduced = Solver(puzzle).solveNonogram()).encode()
    } catch (error: Throwable) {
        SolveResponse(request.id, error = error.message ?: "solve failed").encode()
    }
}
