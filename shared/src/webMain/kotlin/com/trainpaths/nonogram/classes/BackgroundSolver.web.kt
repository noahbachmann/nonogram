@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.classes

import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

/** Built by `:solverWorker` and copied next to the app bundle by `webApp/build.gradle.kts`. */
private const val SOLVER_WORKER_SCRIPT = "solverWorker.js"

internal actual suspend fun solveInBackground(nonogram: Nonogram): List<List<Int>> =
    SolverWorker.solve(nonogram.solution) ?: Solver(nonogram).solveNonogram()

/**
 * The page's end of `:solverWorker`.
 *
 * Every failure — the script missing, the worker erroring, a grid it could not read — falls back to
 * solving inline. That is the behaviour this replaced, so a broken worker costs responsiveness and
 * nothing else.
 */
private object SolverWorker {

    private var worker: Worker? = null
    private var unavailable = false
    private var nextId = 0
    private val pending = mutableMapOf<Int, CompletableDeferred<SolveResponse>>()

    /**
     * Null when the worker could not be used; the caller then solves inline.
     *
     * Deliberately unbounded: a long solve is the case this exists for, and abandoning it would only
     * hand the same work back to the UI thread. A worker that actually dies fires `error` instead.
     */
    suspend fun solve(grid: List<List<Int>>): List<List<Int>>? {
        val worker = worker() ?: return null
        val id = nextId++
        val reply = CompletableDeferred<SolveResponse>()
        pending[id] = reply
        return try {
            worker.postMessage(SolveRequest(id, grid).encode().toJsString())
            reply.await().deduced
        } catch (error: Throwable) {
            println("SolverWorker: solve failed, falling back to the main thread: ${error.message}")
            null
        } finally {
            pending.remove(id)
        }
    }

    private fun worker(): Worker? {
        if (unavailable) return null
        worker?.let { return it }
        return try {
            Worker(SOLVER_WORKER_SCRIPT).also { created ->
                created.onmessage = ::onMessage
                created.addEventListener("error") { _: Event -> onWorkerLost("worker script error") }
                created.addEventListener("messageerror") { _: Event -> onWorkerLost("undeliverable message") }
                worker = created
            }
        } catch (error: Throwable) {
            println("SolverWorker: unavailable, solving on the main thread: ${error.message}")
            unavailable = true
            null
        }
    }

    private fun onMessage(event: MessageEvent) {
        val text = event.data?.toString() ?: return
        val response = try {
            decodeSolveResponse(text)
        } catch (error: Throwable) {
            println("SolverWorker: unreadable reply: ${error.message}")
            return
        }
        pending.remove(response.id)?.complete(response)
    }

    /** A worker that died takes its in-flight requests with it; release them to the inline path. */
    private fun onWorkerLost(reason: String) {
        println("SolverWorker: $reason, solving on the main thread from here on")
        unavailable = true
        worker?.terminate()
        worker = null
        val waiting = pending.values.toList()
        pending.clear()
        waiting.forEach { it.complete(SolveResponse(id = -1, error = reason)) }
    }
}
