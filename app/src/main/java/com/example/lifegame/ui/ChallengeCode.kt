package com.example.lifegame.ui

import java.util.zip.CRC32

/**
 * 같은 출발선 도전 코드(2026-11-13 승인된 확장 기능 4, 김재겸 과제의 생성 · 복사 · 입력 화면).
 * 시작 능력치(주사위 12개) · 성별 · 실행 시드만 담는다. 인생 기록은 담지 않고, 지난 인생 비교도 하지 않는다.
 *
 * 형식: `LG2-<성별 M/F>-<주사위 12자리>-<시드 36진수>-<검사 2자리>`.
 * LG1은 과거 보정식, LG2는 두 눈 합 증가 규칙이며 두 버전을 모두 읽는다.
 * 검사 자리는 앞부분의 CRC32를 36진수 두 자리로 줄인 값이라 한두 글자를 잘못 옮겨 적으면 대부분 걸러진다.
 *
 * 김우영 담당 도전 코드 API(생성 · 가져오기 · 검증 · 저장)가 들어오면 이 파일을 그 API로 바꾼다.
 */
data class ChallengeStart(
    val gender: Gender,
    val rollRaw: String,
    val seed: Long,
    val diceRule: DiceRule = DiceRule.SUM_LG2,
)

sealed interface ChallengeCodeResult {
    data class Valid(val start: ChallengeStart) : ChallengeCodeResult
    data class Invalid(val message: String) : ChallengeCodeResult
}

object ChallengeCode {
    private const val LEGACY_PREFIX = "LG1"
    private const val CURRENT_PREFIX = "LG2"
    private const val DICE_COUNT = 12

    fun encode(start: ChallengeStart): String {
        val dice = StartingRoll.decode(start.rollRaw, start.diceRule)?.rolls?.flatMap { listOf(it.first, it.second) }
            ?: error("주사위 값이 없다")
        val gender = if (start.gender == Gender.MALE) "M" else "F"
        val prefix = if (start.diceRule == DiceRule.LEGACY_LG1) LEGACY_PREFIX else CURRENT_PREFIX
        val body = "$prefix-$gender-${dice.joinToString("")}-${java.lang.Long.toString(start.seed and Long.MAX_VALUE, 36).uppercase()}"
        return "$body-${check(body)}"
    }

    fun decode(input: String): ChallengeCodeResult {
        val code = input.trim().uppercase().replace(" ", "")
        if (code.isEmpty()) return ChallengeCodeResult.Invalid("코드를 입력해 주세요.")
        val parts = code.split("-")
        if (parts.size != 5 || parts[0] !in setOf(LEGACY_PREFIX, CURRENT_PREFIX)) {
            return ChallengeCodeResult.Invalid("인생게임 도전 코드가 아니거나 지원하지 않는 형식입니다.")
        }
        val (_, gender, dice, seed, sum) = parts
        val body = parts.take(4).joinToString("-")
        if (sum != check(body)) return ChallengeCodeResult.Invalid("코드 일부가 틀렸습니다. 다시 확인해 주세요.")
        val g = when (gender) {
            "M" -> Gender.MALE
            "F" -> Gender.FEMALE
            else -> return ChallengeCodeResult.Invalid("성별 표시가 올바르지 않습니다.")
        }
        if (dice.length != DICE_COUNT || dice.any { it !in '1'..'6' }) {
            return ChallengeCodeResult.Invalid("주사위 값이 올바르지 않습니다.")
        }
        val seedValue = seed.lowercase().toLongOrNull(36)
            ?: return ChallengeCodeResult.Invalid("코드 일부가 틀렸습니다. 다시 확인해 주세요.")
        val raw = dice.chunked(2).joinToString(",") { "${it[0]},${it[1]}" }
        val rule = if (parts[0] == LEGACY_PREFIX) DiceRule.LEGACY_LG1 else DiceRule.SUM_LG2
        if (StartingRoll.decode(raw, rule) == null) return ChallengeCodeResult.Invalid("주사위 값이 올바르지 않습니다.")
        return ChallengeCodeResult.Valid(ChallengeStart(g, raw, seedValue, rule))
    }

    private fun check(body: String): String {
        val crc = CRC32().apply { update(body.toByteArray(Charsets.UTF_8)) }.value % (36 * 36)
        return java.lang.Long.toString(crc, 36).uppercase().padStart(2, '0')
    }
}
