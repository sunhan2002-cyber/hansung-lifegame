package com.example.lifegame.ui

/**
 * 화면에 보이는 선택지 라벨에서 끝의 불필요한 마침표를 뗀다(2026-11-13 김재겸 과제 7). 예: `수능을 본다.` → `수능을 본다`.
 *
 * 콘텐츠 JSON의 라벨 마침표는 시나리오 담당이 지운다(#76). 이 함수는 그 전에 저장된 기록(지난 인생의 주요 선택 ·
 * 주마등 · 결과 화면 복원)과 앞으로 실수로 남는 마침표까지 같은 모양으로 보여 주기 위한 화면 쪽 정리다.
 * 본문과 결과 문장은 건드리지 않는다. 말줄임표(`...`, `…`)와 따옴표 · 물음표 · 느낌표로 끝나는 라벨은 그대로 둔다.
 */
fun displayChoiceLabel(label: String): String {
    val trimmed = label.trimEnd()
    if (trimmed.endsWith("...") || trimmed.endsWith("…")) return trimmed
    return if (trimmed.endsWith('.')) trimmed.dropLast(1).trimEnd() else trimmed
}
