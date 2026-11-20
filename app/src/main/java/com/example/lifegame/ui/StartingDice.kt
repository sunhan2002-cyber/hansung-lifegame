package com.example.lifegame.ui

import com.example.lifegame.domain.model.StatDelta
import kotlin.random.Random

/**
 * 새 인생의 시작 능력치를 주사위로 정한다 (2026-11-05 피드백 1번: 아기 유형을 고르지 않고 주사위로 정한다).
 * 능력치마다 주사위 두 개를 굴린다. LG2부터 두 눈의 합을 그대로 기본값 50에 더한다.
 * LG1 도전 코드와 기존 저장은 [DiceRule.LEGACY_LG1]로 이전 보정식을 재현한다.
 */
enum class DiceRule { LEGACY_LG1, SUM_LG2 }

data class StatRoll(
    val label: String,
    val first: Int,
    val second: Int,
    val rule: DiceRule = DiceRule.SUM_LG2,
) {
    val sum: Int get() = first + second
    val delta: Int get() = when (rule) {
        DiceRule.LEGACY_LG1 -> (sum - 7) * 2
        DiceRule.SUM_LG2 -> sum
    }
    val finalValue: Int get() = 50 + delta
}

data class StartingStatResult(
    val label: String,
    val first: Int,
    val second: Int,
    val sum: Int,
    val increase: Int,
    val finalValue: Int,
)

data class StartingRoll(val rolls: List<StatRoll>, val rule: DiceRule = DiceRule.SUM_LG2) {
    init {
        require(rolls.size == StartingDice.STAT_LABELS.size)
        require(rolls.all { it.rule == rule })
    }

    /** UI·시뮬레이터가 함께 쓰는 단일 결과 계약. */
    val results: List<StartingStatResult> get() = rolls.map {
        StartingStatResult(it.label, it.first, it.second, it.sum, it.delta, it.finalValue)
    }
    val delta: StatDelta
        get() {
            fun of(label: String) = rolls.firstOrNull { it.label == label }?.delta ?: 0
            return StatDelta(
                fitness = of("운동능력"),
                intelligence = of("지력"),
                social = of("사회성"),
                wealth = of("경제력"),
                happiness = of("행복"),
                luck = of("운"),
            )
        }

    /** 화면 회전 · 프로세스 복원용 문자열 ("3,4,6,1,...") */
    fun encode(): String = rolls.joinToString(",") { "${it.first},${it.second}" }

    companion object {
        fun decode(raw: String?, rule: DiceRule = DiceRule.SUM_LG2): StartingRoll? {
            val values = raw?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: return null
            if (values.size != StartingDice.STAT_LABELS.size * 2 || values.any { it !in 1..6 }) return null
            return StartingRoll(
                StartingDice.STAT_LABELS.mapIndexed { index, label ->
                    StatRoll(label, values[index * 2], values[index * 2 + 1], rule)
                },
                rule,
            )
        }
    }
}

object StartingDice {
    val STAT_LABELS = listOf("운동능력", "지력", "사회성", "경제력", "행복", "운")

    /** 처음 굴린 뒤 다시 굴릴 수 있는 횟수 */
    const val REROLLS = 3

    fun roll(random: Random = Random.Default, rule: DiceRule = DiceRule.SUM_LG2): StartingRoll =
        StartingRoll(
            STAT_LABELS.map { StatRoll(it, random.nextInt(1, 7), random.nextInt(1, 7), rule) },
            rule,
        )

    /**
     * 굴린 능력치와 가장 닮은 성향을 고른다. 성향 플래그(starter_*)로 열리는 선택지가 있어서
     * 주사위로 바꿔도 성향은 하나 정해 둔다. 점수는 성향의 시작 보정과 굴린 값의 내적이다.
     */
    fun tendencyOf(delta: StatDelta, starters: List<StarterUi>): StarterUi? =
        starters.maxByOrNull { starter ->
            val bias = starter.statDelta
            delta.fitness * bias.fitness + delta.intelligence * bias.intelligence + delta.social * bias.social +
                delta.wealth * bias.wealth + delta.happiness * bias.happiness + delta.luck * bias.luck
        }
}
