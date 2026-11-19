package com.example.lifegame.data

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SaveState {
    data object Idle : SaveState
    data class Pending(val revision: Long) : SaveState
    data class Durable(val revision: Long) : SaveState
    data class Failed(val revision: Long) : SaveState
}

/**
 * Observes durability separately from SharedPreferences' immediate in-memory apply().
 *
 * The supplied fence performs an EMPTY editor.commit() on an IO thread. Android documents that
 * commit waits for outstanding apply writes; it also persists the latest in-memory generation if
 * an earlier disk write failed. No old snapshot is replayed, so a delayed fence cannot undo a newer
 * save or clear. The executor is process-scoped, not a thread per screen or store instance.
 */
class ProgressPersistence(
    private val commitLatest: () -> Boolean,
    private val executor: Executor = writer,
) {
    private val lock = Any()
    private var revision = 0L
    private val mutableState = MutableStateFlow<SaveState>(SaveState.Idle)
    val state: StateFlow<SaveState> = mutableState.asStateFlow()

    fun confirmChanges(): CompletableFuture<Boolean> {
        val receipt = CompletableFuture<Boolean>()
        synchronized(lock) {
            val requested = ++revision
            mutableState.value = SaveState.Pending(requested)
            try {
                executor.execute {
                    val success = try { commitLatest() } catch (_: Exception) { false }
                    synchronized(lock) {
                        // A stale completion must never mark a newer pending write as durable.
                        if (revision == requested) mutableState.value = if (success) {
                            SaveState.Durable(requested)
                        } else SaveState.Failed(requested)
                    }
                    receipt.complete(success)
                }
            } catch (_: Exception) {
                mutableState.value = SaveState.Failed(requested)
                receipt.complete(false)
            }
        }
        return receipt
    }

    fun reportFailure() {
        synchronized(lock) { mutableState.value = SaveState.Failed(++revision) }
    }

    companion object {
        private val writer = Executors.newSingleThreadExecutor { task ->
            Thread(task, "LifeGame-save").apply { isDaemon = true }
        }
    }
}
