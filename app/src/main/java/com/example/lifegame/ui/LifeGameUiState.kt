package com.example.lifegame.ui

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EventConditions
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats

/**
 * 화면이 그대로 그릴 수 있는 형태의 상태다.
 * 지표 계산과 사건·결말 판정은 GameEngine이 하고, 여기서는 표시용 변환만 한다.
 */
data class LifeGameUiState(
    val hasSaveData: Boolean = false,
    val characterId: Int? = null,
    val progress: GameProgress? = null,
    val currentEvent: GameEvent? = null,
    val lastChoice: Choice? = null,
    val lastChanges: List<StatChange> = emptyList(),
    val lastResultText: String = "",
    val isLastEvent: Boolean = false,
    val ending: Ending? = null,
    /** events.sample.json이 아직 없어 임시 사건으로 진행 중인지 여부 */
    val usingFallbackEvents: Boolean = false,
)

/** 지표 한 줄: 이름과 현재 값 */
data class StatLine(val label: String, val value: Int)

/** 변화한 지표 한 줄: 이름과 실제 반영된 변화량 */
data class StatChange(val label: String, val delta: Int)

const val STAT_MAX = 100

fun Stats.toLines(): List<StatLine> = listOf(
    StatLine("건강", health),
    StatLine("운동능력", fitness),
    StatLine("지력", intelligence),
    StatLine("사회성", social),
    StatLine("경제력", wealth),
    StatLine("행복", happiness),
    StatLine("운", luck),
)

/** 변화량이 0인 지표는 결과 화면에 표시하지 않는다. */
fun StatDelta.toChanges(): List<StatChange> = listOf(
    StatChange("건강", health),
    StatChange("운동능력", fitness),
    StatChange("지력", intelligence),
    StatChange("사회성", social),
    StatChange("경제력", wealth),
    StatChange("행복", happiness),
    StatChange("운", luck),
).filter { it.delta != 0 }

fun LifeStage.label(): String = when (this) {
    LifeStage.INFANT -> "영아기"
    LifeStage.CHILD -> "유년기"
    LifeStage.TEEN -> "청소년기"
    LifeStage.ADULT -> "성인기"
}

/**
 * 신기훈 님의 events.sample.json이 앱에 들어오기 전까지 화면 확인에 쓰는 임시 사건이다.
 * JSON이 assets에 추가되면 ContentRepository.loadEvents()가 이 목록을 대신한다.
 */
val FALLBACK_EVENTS: List<GameEvent> = listOf(
    GameEvent(
        eventId = "fallback_infant_01",
        stage = LifeStage.INFANT,
        type = EventType.NORMAL,
        title = "첫 걸음마",
        text = "처음으로 걸음마를 떼려는 순간, 거실 저편에서 강아지가 꼬리를 흔들며 기다리고 있다.",
        imageId = "img_infant_home_002",
        conditions = EventConditions(),
        choices = listOf(
            Choice(
                choiceId = "fallback_infant_01_a",
                label = "강아지를 향해 용감하게 걸어간다",
                statDelta = StatDelta(fitness = 5, happiness = 3),
                resultText = "세 걸음 만에 넘어졌지만 강아지가 얼굴을 핥아 주었다.",
            ),
            Choice(
                choiceId = "fallback_infant_01_b",
                label = "엄마 손을 잡고 천천히 걷는다",
                statDelta = StatDelta(social = 4, health = 2),
                resultText = "안정적으로 다섯 걸음을 걸었다. 가족 모두가 박수를 쳤다.",
            ),
        ),
    ),
    GameEvent(
        eventId = "fallback_child_01",
        stage = LifeStage.CHILD,
        type = EventType.NORMAL,
        title = "발표회 무대",
        text = "유치원 발표회 날, 친구가 대사를 잊어버려 무대 위에서 얼어붙었다. 관객석이 조용해지고 모두가 너를 바라본다.",
        imageId = "img_child_school_012",
        conditions = EventConditions(),
        choices = listOf(
            Choice(
                choiceId = "fallback_child_01_a",
                label = "친구 대사를 대신 말해 준다",
                statDelta = StatDelta(social = 6, intelligence = 2),
                resultText = "무대가 자연스럽게 이어졌고 친구와 더 가까워졌다.",
                addFlags = setOf("helped_friend"),
            ),
            Choice(
                choiceId = "fallback_child_01_b",
                label = "내 대사만 크게 외친다",
                statDelta = StatDelta(happiness = 3, social = -3),
                resultText = "박수는 받았지만 친구가 조금 서운해 보인다.",
            ),
        ),
    ),
    GameEvent(
        eventId = "fallback_teen_01",
        stage = LifeStage.TEEN,
        type = EventType.SPECIAL,
        title = "자동문과의 대치",
        text = "등굣길 편의점 자동문이 너를 인식하지 못한다. 벌써 12번째 거절이다. 뒤에 줄 선 사람들이 웅성거리기 시작한다.",
        imageId = "img_teen_street_034",
        conditions = EventConditions(),
        choices = listOf(
            Choice(
                choiceId = "fallback_teen_01_a",
                label = "문 앞에서 팔을 크게 흔든다",
                statDelta = StatDelta(luck = -4, happiness = 2),
                resultText = "13번째 시도에 문이 열렸다. 지각했지만 전설이 되었다.",
            ),
            Choice(
                choiceId = "fallback_teen_01_b",
                label = "옆 사람 뒤에 붙어서 들어간다",
                statDelta = StatDelta(social = 3, luck = 3),
                resultText = "무사히 들어갔다. 역시 사람은 서로 도와야 한다.",
            ),
        ),
    ),
    GameEvent(
        eventId = "fallback_adult_01",
        stage = LifeStage.ADULT,
        type = EventType.NORMAL,
        title = "첫 월급",
        text = "첫 월급이 들어왔다. 통장에 찍힌 숫자를 몇 번이나 다시 확인한다.",
        imageId = "img_adult_office_051",
        conditions = EventConditions(),
        choices = listOf(
            Choice(
                choiceId = "fallback_adult_01_a",
                label = "가족에게 선물을 한다",
                statDelta = StatDelta(social = 5, happiness = 4, wealth = -3),
                resultText = "가족이 오래 기억할 저녁이 되었다.",
            ),
            Choice(
                choiceId = "fallback_adult_01_b",
                label = "전부 저축한다",
                statDelta = StatDelta(wealth = 6, happiness = -2),
                resultText = "통장 잔액이 늘었다. 조금 심심한 주말이었다.",
            ),
        ),
    ),
)
