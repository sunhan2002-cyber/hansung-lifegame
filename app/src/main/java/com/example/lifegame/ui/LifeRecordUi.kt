package com.example.lifegame.ui

import com.example.lifegame.domain.engine.recapMomentsFrom
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.avatarLookOf
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.ImageRouting
import kotlin.math.roundToInt

/**
 * 10주차 종료 경험 · 기록 재열람 화면 모델.
 * 모두 저장된 [CompletedLifeRecord]에서 만든다. 화면에서 따로 들고 있는 목록이나 계산은 없다.
 */

/** 주마등 한 장면. 실제로 고른 선택 기록에서만 만든다. */
data class RecapMomentUi(
    val key: String,
    val periodName: String,
    val yearInPeriod: Int,
    val title: String,
    val choiceLabel: String,
    val resultText: String,
    /** 그 사건에서 실제로 보여 준 그림 후보(사건 그림 → 경로 장면). 사람이 그려진 기본 초상은 뺀다. */
    val images: List<ImageCandidate>,
    /** 배경 장면 위에 세울 그 시기의 플레이어. 실제로 보인 그림이 1인칭 사건 그림이면 화면에서 세우지 않는다. */
    val avatar: AvatarLook? = null,
    /** 이 사건 전용 그림 ID(있으면). 이 그림이 실제로 보이면 1인칭 장면으로 보고 얼굴을 합성하지 않는다. */
    val eventImageId: String? = null,
) {
    fun isFirstPerson(shown: ImageCandidate?): Boolean =
        shown != null && (shown.usage == "pov" || (eventImageId != null && shown.imageId == eventImageId))

    val place: String get() = "$periodName · ${yearInPeriod}번째 해"
}

data class TitleUi(val titleId: String, val text: String, val description: String)

data class LifeRecordUi(
    val runId: String,
    val playerName: String,
    val title: TitleUi?,
    val summary: EndingSummaryUi,
    /** 마지막 시기 · 체형 · 외형으로 조립한 모습 */
    val avatar: AvatarLook,
    val finalPeriodName: String,
    val recap: List<RecapMomentUi>,
    /** 미래 모습 뒤에 깔 배경. 가장 많이 걸어온 경로의 장면을 쓴다(사람이 그려진 초상은 제외). */
    val backdrop: List<ImageCandidate>,
    val pathTitle: String,
    val usesPhotoFace: Boolean,
)

/** 기록 화면을 만들 때 필요한 콘텐츠 묶음 */
data class RecordContent(
    val periods: List<PeriodDefinition>,
    val factCatalog: LifeFactCatalog = LifeFactCatalog.EMPTY,
    val imageRouting: ImageRouting = ImageRouting(),
    val imageFiles: Map<String, String> = emptyMap(),
    /** 주마등에서 주요 사건(전환점 · 큰 돌발)을 고르고 그때 보여 준 그림을 찾는 데 쓴다 */
    val decisions: Map<String, DecisionEvent> = emptyMap(),
)

private val PATH_STAT_KEYS = mapOf(
    LifePathKind.HEALTH to "health",
    LifePathKind.FITNESS to "fitness",
    LifePathKind.INTELLIGENCE to "intelligence",
    LifePathKind.SOCIAL to "social",
    LifePathKind.WEALTH to "wealth",
    LifePathKind.HAPPINESS to "happiness",
    LifePathKind.LUCK to "luck",
)

fun RunChoiceRecord.toChoiceRecordUi(periods: List<PeriodDefinition>): ChoiceRecordUi = ChoiceRecordUi(
    periodName = periods.firstOrNull { it.period == period }?.displayName ?: period.name,
    yearInPeriod = yearInPeriod,
    title = eventTitle,
    choiceLabel = choiceLabel,
)

/**
 * 주마등 장면. 실제로 고른 선택 기록에서만 만든다.
 * 1) 장면 이미지가 기록된 선택(김우영 recapMomentsFrom)과 전환점 · 큰 돌발을 먼저 고르고,
 * 2) 모자라면 나머지 선택으로 채운 뒤, 3) 영아기부터 마지막까지 고르게 [limit]개를 시간순으로 뽑는다.
 * 그림은 플레이 중 그 사건에 보여 준 순서(사건 그림 → 경로 장면)를 그대로 따르고,
 * 1인칭 사건 그림이 아니면 그 시기의 같은 인물을 배경 위에 세운다.
 */
fun recapMomentsUi(
    choices: List<RunChoiceRecord>,
    content: RecordContent,
    limit: Int = 8,
    lookAt: (LifePeriod) -> AvatarLook? = { null },
): List<RecapMomentUi> {
    if (limit <= 0 || choices.isEmpty()) return emptyList()
    val distinct = choices.distinctBy { "${it.eventId}:${it.outcomeId}" }
    val withImages = recapMomentsFrom(distinct, distinct.size).map { "${it.eventId}:${it.outcomeId}" }.toSet()
    fun isMajor(choice: RunChoiceRecord): Boolean =
        "${choice.eventId}:${choice.outcomeId}" in withImages ||
            content.decisions[choice.eventId]?.kind in MAJOR_KINDS
    val major = distinct.filter(::isMajor)
    val picked = if (major.size >= limit) {
        spreadEvenly(major, limit)
    } else {
        // 주요 장면은 모두 넣고, 남는 자리는 나머지 선택에서 고르게 채운다. 순서는 원래 시간순.
        val fill = spreadEvenly(distinct.filterNot(::isMajor), limit - major.size).toSet()
        distinct.filter { it in fill || isMajor(it) }
    }
    return picked.mapIndexed { index, choice ->
        val imageId = choice.momentImageId?.takeIf(String::isNotBlank)
            ?: content.decisions[choice.eventId]?.povImageId?.takeIf(String::isNotBlank)
        val images = content.imageRouting
            .eventCandidates(choice.eventId, imageId, null, choice.period, content.imageFiles)
            .filter { it.usage != "character" }
        RecapMomentUi(
            key = "${index}:${choice.eventId}:${choice.outcomeId}",
            periodName = content.periods.firstOrNull { it.period == choice.period }?.displayName ?: choice.period.name,
            yearInPeriod = choice.yearInPeriod,
            title = choice.eventTitle,
            choiceLabel = choice.choiceLabel,
            resultText = choice.resultText,
            images = images,
            avatar = lookAt(choice.period)?.let { look ->
                look.copy(bodyType = choice.bodyType ?: com.example.lifegame.domain.model.BodyType.STANDARD)
            },
            eventImageId = imageId,
        )
    }
}

private val MAJOR_KINDS = setOf(DecisionKind.MILESTONE, DecisionKind.BIG_INCIDENT)

/** 순서를 지키며 처음과 끝을 포함해 고르게 [count]개를 고른다. */
internal fun <T> spreadEvenly(items: List<T>, count: Int): List<T> {
    if (items.size <= count) return items
    if (count == 1) return listOf(items.last())
    return (0 until count).map { i -> items[(i * (items.size - 1).toDouble() / (count - 1)).roundToInt()] }.distinct()
}

fun buildLifeRecordUi(record: CompletedLifeRecord, content: RecordContent): LifeRecordUi {
    val state = record.finalState
    val periods = content.periods.sortedBy { it.order }
    val facts = buildLifeFacts(state, content.factCatalog)
    val finalPeriod = state.endingPeriod ?: state.period
    val path = lifePathOf(record.endingId, state.flags, state.stats)
    val summary = buildEndingSummary(
        playerName = record.playerName,
        state = state,
        periods = periods,
        facts = facts,
        endingTitle = record.endingTitle,
        endingSummary = record.endingSummary,
        choices = record.choices.map { it.toChoiceRecordUi(periods) },
        endingId = record.endingId,
    )
    val endingTheme = when (path) {
        LifePathKind.HEALTH -> "health"
        LifePathKind.SOCIAL -> "relationship"
        LifePathKind.FITNESS, LifePathKind.INTELLIGENCE, LifePathKind.WEALTH -> "achievement"
        else -> "peace"
    }
    val endingArt = content.imageRouting.endingScenes[endingTheme]?.let(content.imageRouting.assets::get)
    val backdrop = endingArt ?: PATH_STAT_KEYS[path]
        ?.let(content.imageRouting.statScenes::get)
        ?.let(content.imageRouting.assets::get)
    return LifeRecordUi(
        runId = record.runId,
        playerName = record.playerName,
        title = record.awardedTitle?.let { TitleUi(it.titleId, it.text, it.description) },
        summary = summary,
        avatar = avatarLookOf(record.avatar, finalPeriod, state.stats),
        finalPeriodName = periods.firstOrNull { it.period == finalPeriod }?.displayName ?: finalPeriod.name,
        recap = recapMomentsUi(record.choices, content.copy(periods = periods)) { period ->
            avatarLookOf(record.avatar, period, state.stats)
        },
        backdrop = listOfNotNull(backdrop),
        pathTitle = path.title,
        usesPhotoFace = record.avatar.photoConsent && !record.avatar.faceSheetRef.isNullOrBlank(),
    )
}

/** 홈 화면 "지난 인생" 한 줄 */
fun CompletedLifeRecord.toFinishedLifeUi(periods: List<PeriodDefinition>): FinishedLifeUi {
    val finalPeriod = finalState.endingPeriod ?: finalState.period
    val periodName = periods.firstOrNull { it.period == finalPeriod }?.displayName ?: finalPeriod.name
    return FinishedLifeUi(
        key = runId,
        playerName = playerName,
        summary = endingTitle ?: summary,
        titleText = awardedTitle?.text,
        endedAt = if (finalState.isDead && finalPeriod != com.example.lifegame.domain.model.LifePeriod.OLD_AGE) {
            "${periodName}에 생을 마침"
        } else {
            "${periodName}까지 삶"
        },
    )
}

data class TitleEntryUi(
    val titleId: String,
    val text: String,
    val description: String,
    val earned: Boolean,
    /** 이 칭호를 받은 인생의 이름들 (최근 순) */
    val earnedBy: List<String>,
)

data class TitleCollectionUi(
    val entries: List<TitleEntryUi>,
) {
    val earnedCount: Int get() = entries.count { it.earned }
    val total: Int get() = entries.size
}

/**
 * 칭호 수집. 콘텐츠의 칭호 전체에, 완료 기록에 저장된 칭호 스냅샷을 겹친다.
 * 콘텐츠가 바뀌어 사라진 칭호도 받은 기록이 있으면 당시 문구 그대로 남는다.
 */
fun buildTitleCollection(titles: List<LifeTitle>, lives: List<CompletedLifeRecord>): TitleCollectionUi {
    val earnedBy = lives.mapNotNull { life -> life.awardedTitle?.let { it to life.playerName } }
        .groupBy({ it.first.titleId }, { it.second })
    val snapshots = lives.mapNotNull { it.awardedTitle }.associateBy { it.titleId }
    val known = titles.sortedWith(compareByDescending<LifeTitle> { it.priority }.thenBy { it.titleId }).map { title ->
        val snapshot = snapshots[title.titleId]
        TitleEntryUi(
            titleId = title.titleId,
            text = snapshot?.text ?: title.text,
            description = snapshot?.description ?: title.description,
            earned = title.titleId in earnedBy,
            earnedBy = earnedBy[title.titleId].orEmpty(),
        )
    }
    val retired = snapshots.values.filter { snapshot -> titles.none { it.titleId == snapshot.titleId } }.map {
        TitleEntryUi(it.titleId, it.text, it.description, earned = true, earnedBy = earnedBy[it.titleId].orEmpty())
    }
    val entries = known + retired
    return TitleCollectionUi(entries.sortedByDescending { it.earned })
}
