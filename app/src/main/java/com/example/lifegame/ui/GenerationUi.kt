package com.example.lifegame.ui

import com.example.lifegame.domain.generation.FakeGenerationMode
import com.example.lifegame.domain.generation.IMAGE_GENERATION_TIMEOUT_MS

/**
 * 생성 요청의 화면 상태. 실제 생성 서버는 연결 전이라 [FakeGenerationClient]만 쓴다.
 * READY도 "예시 응답"이며, 화면은 항상 기기 안 그림으로 그린다. 실패해도 진행은 막지 않는다.
 */
enum class GenerationStatus { IDLE, LOADING, READY, FALLBACK }

data class GenerationUi(
    /** 요청 대상(runId 또는 사진 참조). 다른 대상의 결과가 섞이지 않게 비교한다. */
    val key: String,
    val status: GenerationStatus,
    val message: String? = null,
    /** Fake가 돌려준 참조 문자열(fake://...). 화면에 그대로 그리지 않는다. */
    val value: String? = null,
)

/**
 * 개발용 Fake 응답 시나리오. 기본은 성공이며, 디버그 빌드 홈 화면에서 바꿔 실패 경로를 재현한다.
 * 시간 초과는 실제 제한 시간(30초) 대신 짧은 제한을 둬서 시연 중에 같은 대체 경로를 확인한다.
 */
enum class FakeScenario(
    val label: String,
    val mode: FakeGenerationMode,
    val latencyMillis: Long,
    val timeoutMillis: Long = IMAGE_GENERATION_TIMEOUT_MS,
) {
    SUCCESS("성공", FakeGenerationMode.SUCCESS, latencyMillis = 900),
    SLOW("지연", FakeGenerationMode.SUCCESS, latencyMillis = 6_000),
    FAILURE("실패", FakeGenerationMode.FAILURE, latencyMillis = 700),
    TIMEOUT("시간 초과", FakeGenerationMode.SUCCESS, latencyMillis = 5_000, timeoutMillis = 2_000),
    CANCELLED("취소", FakeGenerationMode.CANCELLED, latencyMillis = 700),
}
