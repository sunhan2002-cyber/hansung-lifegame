package com.example.lifegame.domain.generation

import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.Stats

const val IMAGE_GENERATION_TIMEOUT_MS = 30_000L
const val TEXT_GENERATION_TIMEOUT_MS = 3_000L

data class FaceSheetRequest(val runId: String, val localPhotoRef: String, val consent: Boolean)
data class FutureLookRequest(
    val runId: String,
    val avatar: AvatarConfig,
    val period: LifePeriod,
    val bodyType: BodyType,
    val facts: LifeFacts,
    val stats: Stats,
)
data class RecapRequest(val runId: String, val choices: List<RunChoiceRecord>, val facts: LifeFacts)

sealed interface GenerationResult<out T> {
    data class Success<T>(val value: T) : GenerationResult<T>
    data class Failure(val reason: String, val retryable: Boolean = false) : GenerationResult<Nothing>
    data object TimedOut : GenerationResult<Nothing>
    data object Cancelled : GenerationResult<Nothing>
}
/**
 * 외부 공급자와 무관한 생성 계약. 플레이 도중 호출하지 않고 프로필/종료 흐름에서만 사용한다.
 * 실패·취소·시간 초과는 항상 로컬 대체 자산으로 이어져야 한다.
 */
interface GenerationClient {
    fun createFaceSheet(request: FaceSheetRequest): GenerationResult<String>
    fun createFutureLook(request: FutureLookRequest): GenerationResult<String>
    fun createRecap(request: RecapRequest): GenerationResult<String>
    fun cancel(runId: String)
}

enum class FakeGenerationMode { SUCCESS, FAILURE, TIMEOUT, CANCELLED }

class FakeGenerationClient(
    private val mode: FakeGenerationMode = FakeGenerationMode.SUCCESS,
) : GenerationClient {
    private val cancelledRuns = mutableSetOf<String>()

    override fun createFaceSheet(request: FaceSheetRequest): GenerationResult<String> {
        if (!request.consent) return GenerationResult.Failure("photo consent is required")
        return result(request.runId, "fake://face-sheet/${request.runId}")
    }

    override fun createFutureLook(request: FutureLookRequest): GenerationResult<String> =
        result(request.runId, "fake://future/${request.runId}/${request.bodyType.name.lowercase()}")

    override fun createRecap(request: RecapRequest): GenerationResult<String> =
        result(request.runId, request.choices.joinToString(" · ") { it.resultText }.ifBlank { "기록된 선택이 없다." })

    override fun cancel(runId: String) {
        cancelledRuns += runId
    }

    private fun <T> result(runId: String, value: T): GenerationResult<T> = when {
        runId in cancelledRuns || mode == FakeGenerationMode.CANCELLED -> GenerationResult.Cancelled
        mode == FakeGenerationMode.FAILURE -> GenerationResult.Failure("fake generation failure", retryable = true)
        mode == FakeGenerationMode.TIMEOUT -> GenerationResult.TimedOut
        else -> GenerationResult.Success(value)
    }
}

