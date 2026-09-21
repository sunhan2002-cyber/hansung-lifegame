package com.example.lifegame.ui

/*
 * 화면 연결용 임시 모델과 샘플 사건이다.
 * 김우영 담당 GameModels.kt/GameEngine.kt, 김선한 담당 ContentRepository.kt가 준비되면
 * LifeGameViewModel이 이 샘플 대신 실제 엔진과 JSON 데이터를 사용하도록 교체한다.
 */

val STAT_NAMES = listOf("건강", "운동능력", "지력", "사회성", "경제력", "행복", "운")

const val STAT_MIN = 0
const val STAT_MAX = 100
const val STAT_START = 50

data class UiChoice(
    val label: String,
    val text: String,
    val resultText: String,
    val statChanges: Map<String, Int>,
)

data class UiEvent(
    val id: String,
    val stage: String,
    val text: String,
    val isSpecial: Boolean,
    val choiceA: UiChoice,
    val choiceB: UiChoice,
)

data class UiChoiceResult(
    val eventId: String,
    val choice: UiChoice,
    val appliedChanges: Map<String, Int>,
    val isLastEvent: Boolean,
)

data class UiEnding(
    val title: String,
    val description: String,
)

data class LifeGameUiState(
    val characterId: Int? = null,
    val hasSaveData: Boolean = false,
    val eventIndex: Int = 0,
    val stats: Map<String, Int> = STAT_NAMES.associateWith { STAT_START },
    val lastResult: UiChoiceResult? = null,
    val choiceHistory: List<String> = emptyList(),
    val ending: UiEnding? = null,
)

val SAMPLE_EVENTS = listOf(
    UiEvent(
        id = "sample_baby_01",
        stage = "영아기",
        text = "처음으로 걸음마를 떼려는 순간, 거실 저편에서 강아지가 꼬리를 흔들며 기다리고 있다.",
        isSpecial = false,
        choiceA = UiChoice("A", "강아지를 향해 용감하게 걸어간다", "세 걸음 만에 넘어졌지만 강아지가 얼굴을 핥아 주었다.", mapOf("운동능력" to 5, "행복" to 3)),
        choiceB = UiChoice("B", "엄마 손을 잡고 천천히 걷는다", "안정적으로 다섯 걸음을 걸었다. 가족 모두가 박수를 쳤다.", mapOf("사회성" to 4, "건강" to 2)),
    ),
    UiEvent(
        id = "sample_child_01",
        stage = "유년기",
        text = "유치원 발표회 날, 친구가 대사를 잊어버려 무대 위에서 얼어붙었다. 관객석이 조용해지고 모두가 너를 바라본다.",
        isSpecial = false,
        choiceA = UiChoice("A", "친구 대사를 대신 말해 준다", "무대가 자연스럽게 이어졌고 친구와 더 가까워졌다.", mapOf("사회성" to 6, "지력" to 2)),
        choiceB = UiChoice("B", "내 대사만 크게 외친다", "박수는 받았지만 친구가 조금 서운해 보인다.", mapOf("행복" to 3, "사회성" to -3)),
    ),
    UiEvent(
        id = "sample_special_01",
        stage = "학령기 · 돌발",
        text = "등굣길 편의점 자동문이 너를 인식하지 못한다. 벌써 12번째 거절이다. 뒤에 줄 선 사람들이 웅성거리기 시작한다.",
        isSpecial = true,
        choiceA = UiChoice("A", "문 앞에서 팔을 크게 흔든다", "13번째 시도에 문이 열렸다. 지각했지만 전설이 되었다.", mapOf("운" to -4, "행복" to 2)),
        choiceB = UiChoice("B", "옆 사람 뒤에 붙어서 들어간다", "무사히 들어갔다. 역시 사람은 서로 도와야 한다.", mapOf("사회성" to 3, "운" to 3)),
    ),
)
