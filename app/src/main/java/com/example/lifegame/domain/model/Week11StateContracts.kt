package com.example.lifegame.domain.model

/** 사진 UI의 진행 상태. 어떤 값도 게임 능력치·보상·생존 판정에 사용하지 않는다. */
enum class PhotoFlowStatus {
    NOT_REQUESTED,
    CONSENT_REQUIRED,
    PERMISSION_REQUIRED,
    PROCESSING,
    READY,
    FALLBACK,
}

enum class CutsceneStatus {
    NOT_STARTED,
    PLAYING,
    COMPLETED,
    SKIPPED,
}

enum class EventImageExposureStatus {
    NOT_REQUIRED,
    PENDING,
    SHOWN,
}

enum class ResultAnimationStatus {
    NONE,
    PENDING,
    CONSUMED,
}

/**
 * 화면 연출만을 위한 상태다. [PeriodRunState]와 분리해 연출 복원이나 반복 탭이
 * 엔진 진행·보상·사망 판정을 다시 호출할 수 없게 한다.
 */
data class PresentationState(
    val photoFlow: PhotoFlowStatus = PhotoFlowStatus.NOT_REQUESTED,
    val photoFailureReason: String? = null,
    val cutscene: CutsceneStatus = CutsceneStatus.NOT_STARTED,
    val exposedEventId: String? = null,
    val eventImageExposure: EventImageExposureStatus = EventImageExposureStatus.NOT_REQUIRED,
    val resultDisplayId: String? = null,
    val resultAnimation: ResultAnimationStatus = ResultAnimationStatus.NONE,
) {
    fun updatePhotoFlow(status: PhotoFlowStatus, failureReason: String? = null): PresentationState =
        copy(
            photoFlow = status,
            photoFailureReason = failureReason.takeIf { status == PhotoFlowStatus.FALLBACK },
        )

    fun requestEventImage(eventId: String?): PresentationState =
        if (eventId.isNullOrBlank()) copy(exposedEventId = null, eventImageExposure = EventImageExposureStatus.NOT_REQUIRED)
        else if (exposedEventId == eventId && eventImageExposure == EventImageExposureStatus.SHOWN) this
        else copy(exposedEventId = eventId, eventImageExposure = EventImageExposureStatus.PENDING)

    fun markEventImageShown(eventId: String): PresentationState =
        if (exposedEventId == eventId) copy(eventImageExposure = EventImageExposureStatus.SHOWN) else this

    fun requestResultAnimation(displayId: String): PresentationState =
        if (resultDisplayId == displayId) this
        else copy(resultDisplayId = displayId, resultAnimation = ResultAnimationStatus.PENDING)

    fun consumeResultAnimation(displayId: String): PresentationState =
        if (resultDisplayId == displayId) copy(resultAnimation = ResultAnimationStatus.CONSUMED) else this
}

/**
 * 한 선택 입력을 엔진에 한 번만 전달하는 작은 경계다. Compose의 빠른 연타나 재구성이
 * 같은 콜백을 반복해도 첫 입력만 통과하며, 엔진 실패 때만 재시도를 허용한다.
 */
internal class ChoiceApplicationGuard {
    private val accepted = mutableSetOf<String>()

    fun tryBegin(inputId: String): Boolean = inputId.isNotBlank() && accepted.add(inputId)

    fun release(inputId: String) {
        accepted.remove(inputId)
    }

    fun clear() = accepted.clear()
}

enum class RunNarrativeKind {
    YEARLY_STORY,
    CHOICE_RESULT,
}

/** 콘텐츠가 바뀌어도 과거 기록의 뜻이 변하지 않도록 실제 표시 문장을 저장한다. */
data class RunNarrativeRecord(
    val recordId: String,
    val kind: RunNarrativeKind,
    val period: LifePeriod,
    val yearInPeriod: Int,
    val text: String,
    val eventId: String? = null,
    val choiceId: String? = null,
) {
    init {
        require(recordId.isNotBlank()) { "record id must not be blank" }
        require(yearInPeriod > 0) { "record year must be positive" }
        require(text.isNotBlank()) { "record text must not be blank" }
        if (kind == RunNarrativeKind.CHOICE_RESULT) {
            require(!eventId.isNullOrBlank() && !choiceId.isNullOrBlank()) {
                "choice result records require event and choice ids"
            }
        }
    }
}

/** 같은 표시/복원 요청이 반복되어도 같은 기록은 한 번만 남긴다. */
fun List<RunNarrativeRecord>.appendOnce(record: RunNarrativeRecord): List<RunNarrativeRecord> =
    if (any { it.recordId == record.recordId }) this else this + record
