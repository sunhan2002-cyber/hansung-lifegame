package com.example.lifegame.domain.engine

import java.util.concurrent.atomic.AtomicBoolean

enum class CurrentLifeResetResult { SUCCESS, STORAGE_FAILURE, ALREADY_RUNNING }

/** 저장 삭제 성공 뒤에만 타이머·메모리 상태를 정리하는 리셋 트랜잭션. */
class CurrentLifeResetCoordinator {
    private val running = AtomicBoolean(false)

    fun reset(clearPersisted: () -> Boolean, clearRuntime: () -> Unit): CurrentLifeResetResult {
        if (!running.compareAndSet(false, true)) return CurrentLifeResetResult.ALREADY_RUNNING
        return try {
            if (!clearPersisted()) CurrentLifeResetResult.STORAGE_FAILURE
            else {
                clearRuntime()
                CurrentLifeResetResult.SUCCESS
            }
        } finally {
            running.set(false)
        }
    }
}
