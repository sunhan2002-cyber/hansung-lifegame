package com.example.lifegame.domain.interaction

/**
 * 사건 본문 타이핑과 게임 진행 입력을 분리하는 순수 상태 계약.
 *
 * 본문이 덜 표시된 상태의 탭은 오직 본문 완성에만 소비된다. 이때 선택 적용,
 * 다음 사건 이동, 결과 적용, 보상 지급을 뜻하는 [NarrativeTapAction.FORWARD]는
 * 절대 반환하지 않는다. UI는 이 계약을 사용해 같은 탭을 뒤의 클릭 처리로
 * 전파하지 않아야 한다.
 */
data class NarrativeInputState(
    val displayedChars: Int,
    val totalChars: Int,
) {
    init {
        require(totalChars >= 0) { "totalChars must not be negative" }
        require(displayedChars in 0..totalChars) { "displayedChars must be within the narrative" }
    }

    val isComplete: Boolean get() = displayedChars == totalChars
}

enum class NarrativeTapAction {
    /** 현재 탭을 소비하고 본문만 완성한다. */
    COMPLETE_TEXT,

    /** 이미 완성된 본문에서만 상위 UI가 선택/진행 용도로 해석할 수 있다. */
    FORWARD,
}

data class NarrativeTapResult(
    val state: NarrativeInputState,
    val action: NarrativeTapAction,
)

fun NarrativeInputState.consumeTap(): NarrativeTapResult =
    if (isComplete) {
        NarrativeTapResult(this, NarrativeTapAction.FORWARD)
    } else {
        NarrativeTapResult(copy(displayedChars = totalChars), NarrativeTapAction.COMPLETE_TEXT)
    }
