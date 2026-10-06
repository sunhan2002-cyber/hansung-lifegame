package com.example.lifegame.ui

import com.example.lifegame.domain.model.Ending
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
