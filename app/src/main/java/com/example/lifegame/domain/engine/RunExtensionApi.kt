package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.ChallengeCodeState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.RunExtensionState
import com.example.lifegame.domain.model.TitleGoalState

/** 승인된 회상·계기·칭호 목표·도전 코드·NPC 재등장의 단일 근거 API. */
object RunExtensionApi {
    fun recallEvidence(
        sourceEventId: String,
        sourceChoiceIds: Set<String>,
        choices: List<RunChoiceRecord>,
    ): RunChoiceRecord? = choices.firstOrNull {
        it.eventId == sourceEventId && (sourceChoiceIds.isEmpty() || it.choiceId in sourceChoiceIds)
    }

    /** 실제 선택 기록이 있는 사건만 시간순으로 2~3장 반환한다. 2장 미만이면 빈 목록이다. */
    fun catalystRecords(
        candidateEventIds: Collection<String>,
        choices: List<RunChoiceRecord>,
        max: Int = 3,
    ): List<RunChoiceRecord> {
        val candidates = candidateEventIds.toSet()
        val records = choices.filter { it.eventId in candidates }.distinctBy { it.eventId }.take(max.coerceIn(2, 3))
        return records.takeIf { it.size >= 2 }.orEmpty()
    }

    fun canNpcReturn(
        originEventIds: Collection<String>,
        originChoiceIds: Set<String>,
        requiredRelationshipFlags: Set<String>,
        choices: List<RunChoiceRecord>,
        flags: Set<String>,
    ): Boolean = flags.containsAll(requiredRelationshipFlags) &&
        choices.any { record ->
            record.eventId in originEventIds && (originChoiceIds.isEmpty() || record.choiceId in originChoiceIds)
        }

    fun snapshot(
        choices: List<RunChoiceRecord>,
        recallCandidateEventIds: Collection<String>,
        catalystCandidateEventIds: Collection<String>,
        relationshipFlagCatalog: Set<String>,
        currentFlags: Set<String>,
        titleGoal: TitleGoalState?,
        challengeCode: ChallengeCodeState,
    ): RunExtensionState = RunExtensionState(
        recallSourceEventIds = choices.map { it.eventId }.filterTo(linkedSetOf()) { it in recallCandidateEventIds },
        catalystEventIds = catalystRecords(catalystCandidateEventIds, choices).map { it.eventId },
        titleGoal = titleGoal,
        challengeCode = challengeCode,
        relationshipFlags = currentFlags.intersect(relationshipFlagCatalog),
    )
}
