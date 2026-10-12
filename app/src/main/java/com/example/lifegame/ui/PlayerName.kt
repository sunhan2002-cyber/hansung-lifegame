package com.example.lifegame.ui

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.GameEvent

/** 이름을 비워 두고 시작하면 쓰는 이름 */
const val DEFAULT_PLAYER_NAME = "플레이어"

/** 이름 입력 최대 글자 수. 사건 문장 안에서 줄이 지나치게 늘어나지 않게 한다. */
const val PLAYER_NAME_MAX_LENGTH = 10

/** 앞뒤 공백을 지우고 연속 공백은 하나로 줄인다. 비어 있으면 [DEFAULT_PLAYER_NAME]. */
fun normalizePlayerName(raw: String): String =
    raw.trim()
        .replace(Regex("\\s+"), " ")
        .take(PLAYER_NAME_MAX_LENGTH)
        .trim()
        .ifBlank { DEFAULT_PLAYER_NAME }

/**
 * 부르는 이름 (`{nameCall}`). 마지막 글자에 받침이 있으면 "아", 없으면 "야"를 붙인다.
 * 예: 선한 → 선한아, 민지 → 민지야. 한글로 끝나지 않는 이름은 그대로 부른다.
 */
fun nameCallOf(name: String): String {
    val last = name.lastOrNull() ?: return name
    if (last !in '가'..'힣') return name
    val hasFinalConsonant = (last - '가') % 28 != 0
    return name + if (hasFinalConsonant) "아" else "야"
}

/** 콘텐츠 문장의 `{name}`, `{nameCall}`을 실제 이름으로 바꾼다. */
fun String.withPlayerName(name: String, nameCall: String): String =
    replace("{nameCall}", nameCall).replace("{name}", name)

fun GameEvent.withPlayerName(name: String, nameCall: String): GameEvent = copy(
    title = title.withPlayerName(name, nameCall),
    text = text.withPlayerName(name, nameCall),
    choices = choices.map { choice ->
        choice.copy(
            label = choice.label.withPlayerName(name, nameCall),
            resultText = choice.resultText.withPlayerName(name, nameCall),
        )
    },
)

fun Ending.withPlayerName(name: String, nameCall: String): Ending = copy(
    title = title.withPlayerName(name, nameCall),
    summary = summary.withPlayerName(name, nameCall),
)

/** v4 결정 · 돌발. 선택지 문구와 숨은 결과까지 모두 바꿔 둔다. */
fun DecisionEvent.withPlayerName(name: String, nameCall: String): DecisionEvent = copy(
    title = title.withPlayerName(name, nameCall),
    text = text.withPlayerName(name, nameCall),
    choices = choices.map { option ->
        option.copy(
            label = option.label.withPlayerName(name, nameCall),
            outcomes = option.outcomes.map { outcome ->
                outcome.copy(resultText = outcome.resultText.withPlayerName(name, nameCall))
            },
        )
    },
)

/** v4 시기 초점. 해마다 쌓이는 기록 문장도 여기서 바꾼다. */
fun Focus.withPlayerName(name: String, nameCall: String): Focus = copy(
    label = label.withPlayerName(name, nameCall),
    description = description.withPlayerName(name, nameCall),
    yearlyLogTemplates = yearlyLogTemplates.map { it.withPlayerName(name, nameCall) },
)
