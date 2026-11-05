package com.example.lifegame.domain.model

enum class LifeStage {
    INFANT,
    CHILD,
    TEEN,
    ADULT,
}

enum class EventType {
    NORMAL,
    SPECIAL,
}

enum class GameScreenState {
    EVENT,
    CHOICE_RESULT,
    ENDING,
}

data class Stats(
    val health: Int = DEFAULT_STAT,
    val fitness: Int = DEFAULT_STAT,
    val intelligence: Int = DEFAULT_STAT,
    val social: Int = DEFAULT_STAT,
    val wealth: Int = DEFAULT_STAT,
    val happiness: Int = DEFAULT_STAT,
    val luck: Int = DEFAULT_STAT,
) {
    companion object {
        const val DEFAULT_STAT = 50
    }
}

data class StatDelta(
    val health: Int = 0,
    val fitness: Int = 0,
    val intelligence: Int = 0,
    val social: Int = 0,
    val wealth: Int = 0,
    val happiness: Int = 0,
    val luck: Int = 0,
)

data class StatConditions(
    val minHealth: Int? = null,
    val minFitness: Int? = null,
    val minIntelligence: Int? = null,
    val minSocial: Int? = null,
    val minWealth: Int? = null,
    val minHappiness: Int? = null,
    val minLuck: Int? = null,
    val maxHealth: Int? = null,
    val maxFitness: Int? = null,
    val maxIntelligence: Int? = null,
    val maxSocial: Int? = null,
    val maxWealth: Int? = null,
    val maxHappiness: Int? = null,
    val maxLuck: Int? = null,
) {
    fun matches(stats: Stats): Boolean =
        stats.health.isWithin(minHealth, maxHealth) &&
            stats.fitness.isWithin(minFitness, maxFitness) &&
            stats.intelligence.isWithin(minIntelligence, maxIntelligence) &&
            stats.social.isWithin(minSocial, maxSocial) &&
            stats.wealth.isWithin(minWealth, maxWealth) &&
            stats.happiness.isWithin(minHappiness, maxHappiness) &&
            stats.luck.isWithin(minLuck, maxLuck)

    fun description(): String = listOfNotNull(
        minHealth?.let { "건강 $it 이상" }, maxHealth?.let { "건강 $it 이하" },
        minFitness?.let { "운동능력 $it 이상" }, maxFitness?.let { "운동능력 $it 이하" },
        minIntelligence?.let { "지력 $it 이상" }, maxIntelligence?.let { "지력 $it 이하" },
        minSocial?.let { "사회성 $it 이상" }, maxSocial?.let { "사회성 $it 이하" },
        minWealth?.let { "경제력 $it 이상" }, maxWealth?.let { "경제력 $it 이하" },
        minHappiness?.let { "행복 $it 이상" }, maxHappiness?.let { "행복 $it 이하" },
        minLuck?.let { "운 $it 이상" }, maxLuck?.let { "운 $it 이하" },
    ).joinToString(" · ")

    private fun Int.isWithin(min: Int?, max: Int?): Boolean =
        (min == null || this >= min) && (max == null || this <= max)
}

data class EventConditions(
    val stats: StatConditions = StatConditions(),
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
)

data class Choice(
    val choiceId: String,
    val label: String,
    val statDelta: StatDelta = StatDelta(),
    val resultText: String,
    val addFlags: Set<String> = emptySet(),
    val removeFlags: Set<String> = emptySet(),
)

data class GameEvent(
    val eventId: String,
    val stage: LifeStage,
    val type: EventType = EventType.NORMAL,
    val title: String,
    val text: String,
    val imageId: String,
    val conditions: EventConditions = EventConditions(),
    val choices: List<Choice>,
    val priority: Int = 0,
    val isFallback: Boolean = false,
)

data class ImageAsset(
    val imageId: String,
    val usage: String,
    val stage: String,
    val description: String,
    val fileName: String,
    val fallbackText: String,
)


data class EndingConditions(
    val stats: StatConditions = StatConditions(),
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
    val lifeFacts: LifeFactConditions = LifeFactConditions(),
)

/** 8주차 결말 판정 순서. 숫자가 클수록 일반 priority보다 먼저 판정한다. */
enum class EndingTier(val rank: Int) {
    DEFAULT(0),
    GENERAL_LIFE(1),
    WEALTH_HOUSING_RELATIONSHIP(2),
    EDUCATION_CAREER_FAMILY(3),
    SPECIAL_ACHIEVEMENT(4),
}

data class Ending(
    val endingId: String,
    val title: String,
    val summary: String,
    val priority: Int,
    val conditions: EndingConditions = EndingConditions(),
    val isDefault: Boolean = false,
    val tier: EndingTier = if (isDefault) EndingTier.DEFAULT else EndingTier.GENERAL_LIFE,
)

data class ChoiceHistory(
    val eventOccurrenceId: String,
    val eventId: String,
    val choiceId: String,
    val beforeStats: Stats,
    val afterStats: Stats,
    val resultText: String,
)

data class GameProgress(
    val runId: String,
    val contentVersion: String = "1",
    val saveFormatVersion: Int = 1,
    val selectedImageRef: String? = null,
    val stage: LifeStage = LifeStage.INFANT,
    val currentEventOccurrenceId: String,
    val screenState: GameScreenState = GameScreenState.EVENT,
    val stats: Stats = Stats(),
    val flags: Set<String> = emptySet(),
    val completedEventIds: Set<String> = emptySet(),
    val handledOccurrenceIds: Set<String> = emptySet(),
    val choiceHistory: List<ChoiceHistory> = emptyList(),
    val specialEventCount: Int = 0,
    val stageEventCount: Int = 0,
)

data class ChoiceResult(
    val progress: GameProgress,
    val beforeStats: Stats,
    val afterStats: Stats,
    val appliedDelta: StatDelta,
    val resultText: String,
    val wasApplied: Boolean,
)

/** v4에서 한 장(chapter)을 구성하는 생애 시기다. */
enum class LifePeriod {
    INFANT,
    CHILD,
    TEEN,
    YOUNG_ADULT,
    MIDDLE,
    OLD_AGE,
}

data class PeriodDefinition(
    val period: LifePeriod,
    val order: Int,
    val years: Int,
    val displayName: String = period.name,
    val chapterTitle: String = displayName,
    val startLabel: String = "",
    val endLabel: String = "",
) {
    init {
        require(order >= 0) { "period order must be zero or greater" }
        require(years > 0) { "period years must be greater than zero" }
    }
}

data class Focus(
    val focusId: String,
    val period: LifePeriod,
    val label: String,
    val statEffects: StatDelta,
    val description: String,
    val sceneImageId: String? = null,
    val momentImageId: String? = null,
    val yearlyLogTemplates: List<String> = emptyList(),
)

enum class TransitionCue {
    NOISE,
    DISTORT,
    CHAPTER,
}

enum class DecisionKind {
    MILESTONE,
    INCIDENT,
    BIG_INCIDENT,
}

/** 일곱 스탯을 사건 경로와 직접 연결할 때 사용하는 안정적인 내부 키다. */
enum class StatKey {
    HEALTH,
    FITNESS,
    INTELLIGENCE,
    SOCIAL,
    WEALTH,
    HAPPINESS,
    LUCK,
}

/**
 * 선택으로 누적되는 인생 사실. 문자열 값은 life_facts.sample.json의 value와 같게 유지해
 * DB·시나리오가 직업이나 학력 종류를 늘려도 앱 업데이트 없이 저장할 수 있다.
 */
data class LifeFacts(
    val educationLevel: String? = null,
    val fieldOfStudy: String? = null,
    val careerTrack: String? = null,
    val careerLevel: String? = null,
    val employmentStatus: String? = null,
    val maritalStatus: String? = null,
    val childrenCount: Int = 0,
    val housingType: String? = null,
    val wealthBand: String = "MIDDLE",
    val achievementFlags: Set<String> = emptySet(),
    val deathPeriod: LifePeriod? = null,
    val deathCause: String? = null,
)

data class LifeFactRule(
    val flag: String,
    val fact: String,
    val value: String,
    val label: String = value,
)

data class LifeFactConditions(
    val educationLevel: String? = null,
    val fieldOfStudy: String? = null,
    val careerTrack: String? = null,
    val careerLevel: String? = null,
    val employmentStatus: String? = null,
    val maritalStatus: String? = null,
    val minChildrenCount: Int? = null,
    val housingType: String? = null,
    val wealthBand: String? = null,
    val requiredAchievements: Set<String> = emptySet(),
) {
    fun matches(facts: LifeFacts): Boolean =
        educationLevel.matches(facts.educationLevel) &&
            fieldOfStudy.matches(facts.fieldOfStudy) &&
            careerTrack.matches(facts.careerTrack) &&
            careerLevel.matches(facts.careerLevel) &&
            employmentStatus.matches(facts.employmentStatus) &&
            maritalStatus.matches(facts.maritalStatus) &&
            (minChildrenCount == null || facts.childrenCount >= minChildrenCount) &&
            housingType.matches(facts.housingType) &&
            wealthBand.matches(facts.wealthBand) &&
            facts.achievementFlags.containsAll(requiredAchievements)

    private fun String?.matches(actual: String?): Boolean = this == null || this == actual
}

/**
 * 능력치 50을 기준으로 10점 차이마다 결과 가중치에 더할 값이다.
 * 양수는 해당 능력치가 높을수록, 음수는 낮을수록 결과 가능성을 높인다.
 */
data class ChanceWeights(
    val health: Int = 0,
    val fitness: Int = 0,
    val intelligence: Int = 0,
    val social: Int = 0,
    val wealth: Int = 0,
    val happiness: Int = 0,
    val luck: Int = 0,
)

data class Outcome(
    val outcomeId: String,
    val resultText: String,
    val statDelta: StatDelta = StatDelta(),
    val addFlags: Set<String> = emptySet(),
    val chance: Int,
    val primary: Boolean = false,
    val chanceWeights: ChanceWeights = ChanceWeights(),
    val hidden: Boolean = false,
    val death: Boolean = false,
    val deathCause: String? = null,
    /** Legacy content field. It is merged into health during resolution. */
    val lifeDelta: Int = 0,
    /** 제거를 먼저 적용하고 addFlags를 나중에 적용한다. 충돌 시 추가가 최종 상태다. */
    val removeFlags: Set<String> = emptySet(),
) {
    init {
        require(chance >= 0) { "outcome chance must be zero or greater" }
    }
}

data class DecisionOption(
    val choiceId: String,
    val label: String,
    val outcomes: List<Outcome>,
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
    val statConditions: StatConditions = StatConditions(),
    val lifeFactConditions: LifeFactConditions = LifeFactConditions(),
) {
    init {
        require(outcomes.isNotEmpty()) { "decision option must have at least one outcome" }
        require(outcomes.sumOf { it.chance } > 0) { "decision option must have a positive outcome chance" }
    }
}

data class DecisionEvent(
    val eventId: String,
    val periods: Set<LifePeriod>,
    val kind: DecisionKind,
    val title: String,
    val text: String,
    val choices: List<DecisionOption>,
    val weight: Int = 1,
    val yearInPeriod: Int? = null,
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
    val statConditions: StatConditions = StatConditions(),
    val povImageId: String? = null,
    val momentImageId: String? = null,
    /** path_hea_*, path_fit_* 같은 8주차 사건 ID에서 파서가 채운다. */
    val pathStat: StatKey? = null,
    val lifeFactConditions: LifeFactConditions = LifeFactConditions(),
) {
    init {
        require(periods.isNotEmpty()) { "decision event must target at least one period" }
        require(choices.isNotEmpty()) { "decision event must have at least one choice" }
        require(weight > 0) { "decision event weight must be greater than zero" }
        require(yearInPeriod == null || yearInPeriod > 0) { "milestone year must be greater than zero" }
    }
}

data class DecisionChoicePreview(
    val choiceId: String,
    val label: String,
    val likelihood: OutcomeLikelihood? = null,
    val keyStats: List<String> = emptyList(),
    /** 선택 전에 충족해야 하는 능력치 조건을 화면에 표시한다. */
    val statConditionText: String? = null,
    val available: Boolean = true,
    val lockReason: String? = null,
)

/** UI가 엔진의 가중치 계산을 복제하지 않고 선택지 가능성을 표시하기 위한 질적 구간이다. */
enum class OutcomeLikelihood(val label: String) {
    LOW("낮음"),
    HALF("반반쯤"),
    HIGH("높음"),
    VERY_HIGH("매우 높음"),
}

/** UI에는 결과 목록을 포함하지 않아 hidden 결과가 preview로 새지 않는다. */
data class PendingDecision(
    val eventId: String,
    val kind: DecisionKind,
    val title: String,
    val text: String,
    val choices: List<DecisionChoicePreview>,
    val povImageId: String? = null,
)

data class LifeMoment(
    val period: LifePeriod,
    val yearInPeriod: Int,
    val text: String,
    val momentImageId: String? = null,
)

data class PeriodRunState(
    val runId: String,
    val runSeed: Long,
    val period: LifePeriod,
    val yearInPeriod: Int = 1,
    val selectedFocusId: String? = null,
    val stats: Stats = Stats(),
    val flags: Set<String> = emptySet(),
    val lifeFacts: LifeFacts = LifeFacts(),
    val dominantFocusStat: StatKey? = null,
    val recentChoiceIds: List<String> = emptyList(),
    val pendingDecisionId: String? = null,
    val moments: List<LifeMoment> = emptyList(),
    val isEnding: Boolean = false,
    val isDead: Boolean = false,
    /** 종료가 발생한 생애 시기. 종료 후 다음 시기로 이동해도 이 값은 유지한다. */
    val endingPeriod: LifePeriod? = null,
)

/** titles.sample.json 한 항목. 결말과 같은 능력치·플래그 조건 계약을 사용한다. */
data class LifeTitle(
    val titleId: String,
    val text: String,
    val description: String,
    val priority: Int,
    val conditions: EndingConditions = EndingConditions(),
    val isDefault: Boolean = false,
)

/** 완료 당시 문구를 함께 저장해 콘텐츠 업데이트 뒤에도 과거 기록의 의미가 바뀌지 않게 한다. */
data class AwardedTitle(
    val titleId: String,
    val text: String,
    val description: String,
)

data class RunChoiceRecord(
    val eventId: String,
    val choiceId: String,
    val outcomeId: String,
    val period: LifePeriod,
    val yearInPeriod: Int,
    val eventTitle: String,
    val choiceLabel: String,
    val resultText: String,
    val momentImageId: String? = null,
    /** 체형은 선택 결과 당시의 스냅샷. 이전 저장에는 없으므로 null을 허용한다. */
    val bodyType: BodyType? = null,
    /** 선택 당시 사건 화면 식별자. povImageId를 우선하고 없으면 imageId를 사용한다. */
    val eventImageId: String? = null,
)

/** 결과 화면에서 종료되어도 같은 결과를 복원하기 위한 적용 완료 스냅샷. */
data class SavedOutcome(
    val eventId: String,
    val choiceId: String,
    val outcomeId: String,
    val choiceLabel: String,
    val resultText: String,
    val beforeStats: Stats,
    val afterStats: Stats,
    val beforeFacts: LifeFacts,
    val afterFacts: LifeFacts,
    val endsLife: Boolean,
    val openedEvents: List<String> = emptyList(),
    /** 결과 화면과 앱 재실행 뒤에도 같은 사건 그림을 복원하기 위한 불변 스냅샷. */
    val eventImageId: String? = null,
)

enum class BodyType {
    SLIM,
    STANDARD,
    MUSCULAR,
}

data class AvatarConfig(
    val skinToneId: String = "skin_default",
    val hairId: String = "hair_default",
    val eyesId: String = "eyes_default",
    val portraitRef: String? = null,
    val faceSheetRef: String? = null,
    val photoConsent: Boolean = false,
)

/** 앱 재실행 뒤에도 다시 열 수 있는 불변 완료 인생 기록. */
data class CompletedLifeRecord(
    val runId: String,
    val playerName: String,
    val summary: String,
    val endingId: String?,
    val endingTitle: String?,
    val endingSummary: String?,
    val awardedTitle: AwardedTitle?,
    val finalState: PeriodRunState,
    val choices: List<RunChoiceRecord>,
    val avatar: AvatarConfig = AvatarConfig(),
)

enum class TurnPhase {
    CHAPTER,
    AUTO_PROGRESS,
    DECISION,
    ENDING,
}

data class TurnStep(
    val state: PeriodRunState,
    val phase: TurnPhase,
    val recordText: String? = null,
    val transitionCue: TransitionCue? = null,
    val pendingDecision: PendingDecision? = null,
    val sceneImageId: String? = null,
    val momentImageId: String? = null,
    val canAutoAdvance: Boolean = false,
    val needsFocusSelection: Boolean = false,
)

data class OutcomeResolution(
    val outcome: Outcome,
    val step: TurnStep,
)

// Notion 카드에서 사용하는 초기 명칭과 GitHub 명세의 확정 명칭을 함께 지원한다.
typealias PlayerStats = Stats
typealias Event = GameEvent
