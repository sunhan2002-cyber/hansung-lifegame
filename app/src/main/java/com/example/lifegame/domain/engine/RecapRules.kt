package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.RunChoiceRecord

/**
 * 실제 선택 이력에 기록된 장면만 시간순으로 돌려준다.
 * 같은 사건·결과·이미지 조합은 최초 한 번만 남기고, 제한을 넘으면 최근 장면을 선택한 뒤 시간순을 유지한다.
 */
fun recapMomentsFrom(
    choices: List<RunChoiceRecord>,
    limit: Int = 8,
): List<RunChoiceRecord> {
    require(limit >= 0) { "limit must be zero or greater" }
    if (limit == 0) return emptyList()
    return choices
        .filter { !it.momentImageId.isNullOrBlank() }
        .distinctBy { "${it.eventId}:${it.outcomeId}:${it.momentImageId}" }
        .takeLast(limit)
}
