package com.example.lifegame.data

import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Test

class ProgressPersistenceTest {
    private class ManualExecutor : Executor {
        val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks += command }
        fun runNext() = tasks.removeFirst().run()
    }

    @Test
    fun queuedChangesArePendingUntilTheDiskFenceActuallyCompletes() {
        val executor = ManualExecutor()
        val persistence = ProgressPersistence({ true }, executor)
        val receipt = persistence.confirmChanges()
        assertTrue(persistence.state.value is SaveState.Pending)
        assertFalse(receipt.isDone)
        executor.runNext()
        assertTrue(receipt.get())
        assertEquals(SaveState.Durable(1), persistence.state.value)
    }

    @Test
    fun staleCompletionNeverConfirmsANewerRevision() {
        val executor = ManualExecutor()
        val persistence = ProgressPersistence({ true }, executor)
        val first = persistence.confirmChanges()
        val second = persistence.confirmChanges()
        executor.runNext()
        assertTrue(first.get())
        assertEquals(SaveState.Pending(2), persistence.state.value)
        assertFalse(second.isDone)
        executor.runNext()
        assertEquals(SaveState.Durable(2), persistence.state.value)
    }

    @Test
    fun failedDiskWriteCanBeRetriedWithoutReapplyingGameResults() {
        val executor = ManualExecutor()
        var diskWorks = false
        var latestMemory = "choice-A-applied-once"
        var durable: String? = null
        val persistence = ProgressPersistence({
            if (diskWorks) durable = latestMemory
            diskWorks
        }, executor)
        val failed = persistence.confirmChanges()
        executor.runNext()
        assertFalse(failed.get())
        assertEquals(SaveState.Failed(1), persistence.state.value)
        latestMemory = "choice-A-applied-once-and-animation-consumed"
        diskWorks = true
        persistence.confirmChanges()
        executor.runNext()
        assertEquals(latestMemory, durable)
        assertEquals(SaveState.Durable(2), persistence.state.value)
    }

    @Test
    fun unexpectedWriterFailureIsReportedWithoutClaimingDurability() {
        val executor = ManualExecutor()
        val persistence = ProgressPersistence({ error("disk unavailable") }, executor)
        val receipt = persistence.confirmChanges()
        executor.runNext()
        assertFalse(receipt.get())
        assertTrue(persistence.state.value is SaveState.Failed)
    }
}
