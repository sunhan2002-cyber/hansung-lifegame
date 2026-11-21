package com.example.lifegame.ui

import kotlin.math.abs

/**
 * 프로필 주사위 결과 한 줄(2026-11-20 과제: 3D 눈과 능력치 줄을 1:1로 연결).
 *
 * 눈 · 합 · 증가량 · 최종값은 엔진의 [StartingStatResult]를 사용한다.
 */
data class StartingStatRowUi(
    val label: String,
    val first: Int,
    val second: Int,
    val sum: Int,
    val increase: Int,
    val base: Int,
    val finalValue: Int,
) {
    /** "1 + 4 = 5 · 50 + 5 = 55". 줄바꿈은 가운데 점에서만 일어나도록 덩어리 안은 줄바꿈 없는 공백을 쓴다. */
    val diceText: String get() = "$first$NBSP+$NBSP$second$NBSP=$NBSP$sum"
    val signText: String get() = if (increase < 0) "−" else "+"
    val increaseText: String get() = "$signText$NBSP${abs(increase)}"
    val formula: String get() = "$diceText · $base$NBSP$increaseText$NBSP=$NBSP$finalValue"

    /** 화면 낭독: "경제력, 주사위 1과 4, 합 5, 50에서 5 올라 55". 화면 숫자와 같은 값을 쓴다. */
    val speech: String get() {
        val change = when {
            increase > 0 -> "${base}에서 $increase 올라 $finalValue"
            increase < 0 -> "${base}에서 ${-increase} 내려 $finalValue"
            else -> "$base 그대로 $finalValue"
        }
        return "$label, 주사위 $first${andParticle(first)} $second, 합 $sum, $change"
    }

    companion object {
        private const val NBSP = " "

        /** 숫자 1~6을 한국어로 읽을 때의 "과/와"(일·삼·육은 받침이 있다). */
        fun andParticle(number: Int): String = if (number % 10 in setOf(1, 3, 6, 7, 8, 0)) "과" else "와"
    }
}

fun startingStatRows(roll: StartingRoll): List<StartingStatRowUi> = roll.results.map { stat ->
    StartingStatRowUi(
        label = stat.label,
        first = stat.first,
        second = stat.second,
        sum = stat.sum,
        increase = stat.increase,
        base = stat.finalValue - stat.increase,
        finalValue = stat.finalValue,
    )
}
