package com.example.lifegame.ui

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.Stats
import org.json.JSONArray

// 8주차: 선택이 남긴 "인생 사실"(교육 · 직업 · 관계 · 가족 · 주거 · 경제력 · 업적 · 사망)을 화면용으로 만든다.
// 사실은 결과 플래그로 저장되고, 어떤 플래그가 어떤 사실인지는 life_facts.sample.json(시나리오 담당)에 있다.
// 게임 상태에 사실 필드(educationLevel, careerTrack …)가 생기면 buildLifeFacts의 입력만 바꾸면 된다.

const val COMPLETED_EVENT_PREFIX = "v4:event:"

/** life_facts.sample.json 한 줄. */
data class LifeFactEntry(
    val flag: String,
    val fact: String,
    val value: String,
    val label: String,
)

class LifeFactCatalog(entries: List<LifeFactEntry>) {
    private val byFlag = entries.associateBy { it.flag }

    fun entry(flag: String): LifeFactEntry? = byFlag[flag]

    companion object {
        val EMPTY = LifeFactCatalog(emptyList())
    }
}

fun parseLifeFacts(json: String): List<LifeFactEntry> = runCatching {
    val array = JSONArray(json)
    List(array.length()) { index ->
        val item = array.getJSONObject(index)
        LifeFactEntry(
            flag = item.getString("flag"),
            fact = item.getString("fact"),
            value = item.opt("value")?.toString().orEmpty(),
            label = item.getString("label"),
        )
    }
}.getOrElse { emptyList() }

/** 지금까지 쌓인 인생 사실. 값이 없으면 null이고, 화면에서 기본 문구로 바꿔 보여 준다. */
data class LifeFactsUi(
    val education: String? = null,
    val fieldOfStudy: String? = null,
    val career: String? = null,
    val careerLevel: String? = null,
    val employment: String? = null,
    val marital: String? = null,
    val children: Int = 0,
    val housing: String? = null,
    val wealthBand: String = wealthBandOf(Stats.DEFAULT_STAT),
    val wealthNote: String? = null,
    val healthStatus: String? = null,
    val achievements: List<String> = emptyList(),
) {
    /** 플레이 중 요약 줄. 생긴 사실만 짧게 이어 붙인다. */
    val shortLine: String
        get() = listOfNotNull(
            education,
            careerText(),
            marital,
            children.takeIf { it > 0 }?.let { "자녀 ${it}명" },
            housing,
            "경제력 $wealthBand",
            achievements.size.takeIf { it > 0 }?.let { "업적 ${it}개" },
        ).joinToString(" · ")
}

/** 경제력 수치를 말로 바꾼다. 게임 상태에 wealthBand가 생기면 그 값을 쓴다. */
fun wealthBandOf(wealth: Int): String = when {
    wealth < 25 -> "빠듯함"
    wealth < 50 -> "보통"
    wealth < 75 -> "여유"
    else -> "넉넉함"
}

/** 플래그가 붙은 순서대로 사실을 덮어쓴다. 마지막에 붙은 상태가 지금 상태다. */
fun buildLifeFacts(flags: Collection<String>, stats: Stats, catalog: LifeFactCatalog): LifeFactsUi {
    var facts = LifeFactsUi(wealthBand = wealthBandOf(stats.wealth))
    val achievements = mutableListOf<String>()
    flags.forEach { flag ->
        val entry = catalog.entry(flag) ?: return@forEach
        facts = when (entry.fact) {
            "educationLevel" -> facts.copy(education = entry.label)
            "fieldOfStudy" -> facts.copy(fieldOfStudy = entry.label)
            "careerTrack" -> facts.copy(career = entry.label)
            "careerLevel" -> facts.copy(careerLevel = entry.label)
            "employmentStatus" -> facts.copy(employment = entry.label)
            "maritalStatus" -> facts.copy(marital = entry.label)
            "childrenAtLeast" -> facts.copy(children = maxOf(facts.children, entry.value.toIntOrNull() ?: 1))
            "housingType" -> facts.copy(housing = entry.label)
            "wealthNote" -> facts.copy(wealthNote = entry.label)
            "healthStatus" -> facts.copy(healthStatus = entry.label)
            "achievement" -> {
                achievements += entry.label
                facts
            }
            else -> facts
        }
    }
    return facts.copy(achievements = achievements.distinct())
}

/** 선택 직후 "새로 생긴 사실" 한 줄. */
data class FactChangeUi(val category: String, val text: String)

fun factChanges(before: LifeFactsUi, after: LifeFactsUi): List<FactChangeUi> = buildList {
    fun changed(category: String, old: String?, new: String?) {
        if (new != null && new != old) add(FactChangeUi(category, new))
    }
    changed("교육", before.education, after.education)
    changed("전공", before.fieldOfStudy, after.fieldOfStudy)
    changed("직업", before.career, after.career)
    changed("직급", before.careerLevel, after.careerLevel)
    changed("일", before.employment, after.employment)
    changed("관계", before.marital, after.marital)
    if (after.children > before.children) add(FactChangeUi("가족", "자녀 ${after.children}명"))
    changed("주거", before.housing, after.housing)
    if (after.wealthBand != before.wealthBand) {
        add(FactChangeUi("경제력", "${before.wealthBand} → ${after.wealthBand}"))
    }
    changed("돈", before.wealthNote, after.wealthNote)
    changed("건강", before.healthStatus, after.healthStatus)
    (after.achievements - before.achievements.toSet()).forEach { add(FactChangeUi("업적", it)) }
}

/** 결말 · 요약 시트의 한 줄 (값이 없으면 기본 문구, [known]은 false). */
data class FactLineUi(val label: String, val value: String, val known: Boolean)

fun LifeFactsUi.educationCareerLines(): List<FactLineUi> = listOf(
    line("교육", education, "특별한 진학 기록 없음"),
    line("전공", fieldOfStudy, "정해진 전공 없음"),
    line("직업", listOfNotNull(career, careerLevel).joinToString(" · ").ifBlank { null }, "직업 기록 없음"),
    line("일", employment, "고용 기록 없음"),
)

fun LifeFactsUi.familyLifeLines(): List<FactLineUi> = listOf(
    line("관계", marital, "결혼 기록 없음"),
    line("자녀", children.takeIf { it > 0 }?.let { "${it}명" }, "자녀 없음"),
    line("주거", housing, "주거 변화 기록 없음"),
    FactLineUi("경제력", listOfNotNull(wealthBand, wealthNote).joinToString(" · "), true),
    line("건강", healthStatus, "특별한 건강 기록 없음"),
)

private fun line(label: String, value: String?, fallback: String) =
    FactLineUi(label, value ?: fallback, value != null)

/** 대표 스탯: 지금 가장 높은 능력치. 같으면 화면 순서(건강 → 운)가 앞선 것. */
data class StatValueUi(val name: String, val value: Int)

fun Stats.topStat(): StatValueUi =
    toDisplayMap().maxByOrNull { it.value }!!.let { StatValueUi(it.key, it.value) }

fun Stats.topStats(count: Int): List<StatValueUi> =
    toDisplayMap().entries.sortedByDescending { it.value }.take(count).map { StatValueUi(it.key, it.value) }

/** 일곱 스탯 경로. 경로 사건 ID는 path_<줄임말>_<번호>이다 (8주차 시나리오). */
enum class LifePathKind(val stat: String, val eventPrefix: String, val title: String) {
    HEALTH("건강", "path_hea_", "몸을 돌본 삶"),
    FITNESS("운동능력", "path_fit_", "몸으로 부딪힌 삶"),
    INTELLIGENCE("지력", "path_int_", "배우고 연구한 삶"),
    SOCIAL("사회성", "path_soc_", "사람과 함께한 삶"),
    WEALTH("경제력", "path_wea_", "일과 돈을 일군 삶"),
    HAPPINESS("행복", "path_hap_", "즐거움을 지킨 삶"),
    LUCK("운", "path_luk_", "운에 맡긴 삶"),
}

/**
 * 가장 많이 걸어온 경로. 겪은 경로 사건 수(1개당 10점)와 그 스탯 값을 더해 고른다.
 * 경로 사건을 하나도 겪지 않았으면 가장 높은 능력치를 따른다.
 * 예: 지력 100에 지력 경로 4개, 건강 76에 건강 경로 5개 → 140 대 126으로 지력.
 */
fun dominantPath(flags: Collection<String>, stats: Stats): LifePathKind {
    val values = stats.toDisplayMap()
    val counts = LifePathKind.entries.associateWith { path ->
        flags.count { it.startsWith(COMPLETED_EVENT_PREFIX + path.eventPrefix) }
    }
    if (counts.values.all { it == 0 }) {
        val top = stats.topStat().name
        return LifePathKind.entries.first { it.stat == top }
    }
    return counts.filterValues { it > 0 }
        .maxBy { (path, count) -> count * 10 + (values[path.stat] ?: 0) }
        .key
}

/** "연구원(책임자)"처럼 직업과 직급을 한 덩어리로. 직업이 없으면 고용 상태. */
fun LifeFactsUi.careerText(): String? = when {
    career != null && careerLevel != null -> "$career($careerLevel)"
    career != null -> career
    else -> employment
}

// ---- 잠긴 선택지의 실제 이유 ----

/** 플래그를 사람이 읽을 이름으로 바꿀 때 쓰는 자료. */
data class FlagLookup(
    val catalog: LifeFactCatalog = LifeFactCatalog.EMPTY,
    /** eventId → 사건 제목 */
    val eventTitles: Map<String, String> = emptyMap(),
    /** 플래그 → 그 플래그를 주는 사건 제목 */
    val flagSources: Map<String, String> = emptyMap(),
) {
    fun nameOf(flag: String): String? =
        catalog.entry(flag)?.label
            ?: flag.removePrefix(COMPLETED_EVENT_PREFIX).takeIf { flag.startsWith(COMPLETED_EVENT_PREFIX) }
                ?.let { eventTitles[it] }?.let { "「$it」" }

    companion object {
        fun from(decisions: Collection<DecisionEvent>, catalog: LifeFactCatalog): FlagLookup {
            val sources = mutableMapOf<String, String>()
            decisions.forEach { event ->
                event.choices.forEach { option ->
                    option.outcomes.forEach { outcome ->
                        outcome.addFlags.forEach { flag -> sources.putIfAbsent(flag, event.title) }
                    }
                }
            }
            return FlagLookup(
                catalog = catalog,
                eventTitles = decisions.associate { it.eventId to it.title },
                flagSources = sources,
            )
        }
    }
}

/**
 * 선택지가 잠긴 실제 이유. 예: "지력 60 필요 (지금 52)", "대학 진학 필요", "「고등학교 원서」 이후 가능".
 * 조건을 모두 만족하면 빈 목록.
 */
fun DecisionOption.lockReasons(stats: Stats, flags: Set<String>, lookup: FlagLookup): List<String> = buildList {
    addAll(statConditions.unmetReasons(stats))
    requiredFlags.filterNot(flags::contains).forEach { flag ->
        val name = lookup.nameOf(flag)
        add(
            when {
                flag.startsWith(COMPLETED_EVENT_PREFIX) && name != null -> "$name 이후 가능"
                name != null -> "$name 필요"
                lookup.flagSources[flag] != null -> "「${lookup.flagSources[flag]}」에서 이어지는 선택 필요"
                else -> "앞선 선택이 필요해요"
            },
        )
    }
    blockedFlags.filter(flags::contains).forEach { flag ->
        val name = lookup.nameOf(flag)
        add(
            when {
                flag.startsWith(COMPLETED_EVENT_PREFIX) && name != null -> "$name 이후에는 고를 수 없음"
                name != null -> "이미 '$name' 상태라 고를 수 없음"
                lookup.flagSources[flag] != null -> "「${lookup.flagSources[flag]}」의 선택 때문에 고를 수 없음"
                else -> "지금 상황과 맞지 않아요"
            },
        )
    }
}

private fun StatConditions.unmetReasons(stats: Stats): List<String> = buildList {
    fun min(name: String, need: Int?, now: Int) {
        if (need != null && now < need) add("$name $need 필요 (지금 $now)")
    }
    fun max(name: String, limit: Int?, now: Int) {
        if (limit != null && now > limit) add("$name $limit 이하일 때만 (지금 $now)")
    }
    min("건강", minHealth, stats.health); max("건강", maxHealth, stats.health)
    min("운동능력", minFitness, stats.fitness); max("운동능력", maxFitness, stats.fitness)
    min("지력", minIntelligence, stats.intelligence); max("지력", maxIntelligence, stats.intelligence)
    min("사회성", minSocial, stats.social); max("사회성", maxSocial, stats.social)
    min("경제력", minWealth, stats.wealth); max("경제력", maxWealth, stats.wealth)
    min("행복", minHappiness, stats.happiness); max("행복", maxHappiness, stats.happiness)
    min("운", minLuck, stats.luck); max("운", maxLuck, stats.luck)
}

/**
 * 이번 선택으로 새로 조건이 맞게 된 사건 (예: 「첫 직장」). 같은 시기나 이후 시기의 것만, 최대 [limit]개.
 */
fun newlyOpenedEvents(
    before: Set<String>,
    after: Set<String>,
    current: LifePeriod,
    decisions: Collection<DecisionEvent>,
    periods: List<PeriodDefinition>,
    limit: Int = 2,
): List<String> {
    val added = after - before
    if (added.isEmpty()) return emptyList()
    val order = periods.associate { it.period to it.order }
    val now = order[current] ?: 0
    return decisions.asSequence()
        .filter { it.requiredFlags.any(added::contains) }
        .filter { after.containsAll(it.requiredFlags) && it.blockedFlags.none(after::contains) }
        .filterNot { (COMPLETED_EVENT_PREFIX + it.eventId) in after }
        .filter { event -> event.periods.any { (order[it] ?: 0) >= now } }
        .sortedBy { event -> event.periods.minOf { order[it] ?: 0 } }
        .map { it.title }
        .distinct()
        .take(limit)
        .toList()
}

// ---- 결말 요약 ----

data class DeathInfoUi(
    val periodName: String,
    val yearInPeriod: Int,
    val cause: String,
    val detail: String?,
    /** 노년기 전에 끝났으면 true */
    val early: Boolean,
)

data class ChoiceRecordUi(
    val periodName: String,
    val yearInPeriod: Int,
    val title: String,
    val choiceLabel: String?,
)

data class EndingSummaryUi(
    /** 결말 제목 또는 사망 문장 */
    val headline: String,
    /** 결말 설명 (있을 때만) */
    val endingSummary: String?,
    /** "배우고 연구한 삶 · 지력 88" */
    val pathLine: String,
    /** 사실을 이어 붙인 한 줄 인생 요약 */
    val lifeLine: String,
    val death: DeathInfoUi?,
    val endedAt: String,
    val educationCareer: List<FactLineUi>,
    val familyLife: List<FactLineUi>,
    val achievements: List<String>,
    val topStats: List<StatValueUi>,
    val choices: List<ChoiceRecordUi>,
)

const val NATURAL_DEATH_TEXT = "노년기를 마치고 자연스럽게 생을 마쳤다."
private const val SUDDEN_HEALTH_DEATH_TEXT = "건강이 급격히 나빠져"

fun buildDeathInfo(state: PeriodRunState, periods: List<PeriodDefinition>): DeathInfoUi? {
    if (!state.isDead) return null
    val period = state.endingPeriod ?: state.moments.lastOrNull()?.period ?: state.period
    val periodName = periods.firstOrNull { it.period == period }?.displayName ?: period.name
    val last = state.moments.lastOrNull { it.period == period } ?: state.moments.lastOrNull()
    val text = last?.text
    val cause = when {
        text == NATURAL_DEATH_TEXT -> "노환 (자연사)"
        text?.contains(SUDDEN_HEALTH_DEATH_TEXT) == true -> "건강 악화"
        state.stats.health <= 0 -> "건강이 바닥남"
        else -> "사건 중 사고"
    }
    return DeathInfoUi(
        periodName = periodName,
        yearInPeriod = last?.yearInPeriod ?: state.yearInPeriod,
        cause = cause,
        detail = text?.takeIf { it != NATURAL_DEATH_TEXT },
        early = period != LifePeriod.OLD_AGE,
    )
}

/**
 * 결말 화면 내용을 만든다. [endingTitle]은 결말 데이터에서 고른 제목이다.
 * 노년기 전에 죽었으면 성취형 결말 제목 대신 사망 문장을 앞세운다.
 */
fun buildEndingSummary(
    playerName: String,
    state: PeriodRunState,
    periods: List<PeriodDefinition>,
    facts: LifeFactsUi,
    endingTitle: String?,
    endingSummary: String?,
    choices: List<ChoiceRecordUi>,
): EndingSummaryUi {
    val death = buildDeathInfo(state, periods)
    val path = dominantPath(state.flags, state.stats)
    val top = state.stats.topStat()
    val endPeriod = state.endingPeriod ?: state.moments.lastOrNull()?.period ?: state.period
    val endedAt = periods.firstOrNull { it.period == endPeriod }?.displayName ?: endPeriod.name
    val headline = when {
        death != null && death.early -> "${death.periodName} ${death.yearInPeriod}번째 해에 생을 마쳤다"
        endingTitle != null -> endingTitle
        death != null -> "${death.periodName}에서 생을 마쳤다"
        else -> "${endedAt}까지 살았다"
    }
    val lifeLine = listOfNotNull(
        "$playerName · ${path.title}",
        facts.education,
        facts.careerText(),
        facts.marital,
        facts.children.takeIf { it > 0 }?.let { "자녀 ${it}명" },
        facts.housing,
        facts.achievements.firstOrNull()?.let { "대표 업적: $it" },
        death?.let { "${it.periodName}에 마침" } ?: "${endedAt}까지",
    ).joinToString(" · ")
    return EndingSummaryUi(
        headline = headline,
        endingSummary = endingSummary.takeIf { death?.early != true },
        pathLine = "${path.title} · 대표 능력치 ${top.name} ${top.value}",
        lifeLine = lifeLine,
        death = death,
        endedAt = endedAt,
        educationCareer = facts.educationCareerLines(),
        familyLife = facts.familyLifeLines(),
        achievements = facts.achievements,
        topStats = state.stats.topStats(3),
        choices = choices,
    )
}

/** 저장에서 이어 할 때는 고른 선택지 이름이 없으므로 겪은 사건 제목만으로 기록을 되살린다. */
fun choiceRecordsFromFlags(
    flags: Collection<String>,
    decisions: Map<String, DecisionEvent>,
    periods: List<PeriodDefinition>,
): List<ChoiceRecordUi> {
    val names = periods.associate { it.period to it.displayName }
    return flags.filter { it.startsWith(COMPLETED_EVENT_PREFIX) }
        .mapNotNull { decisions[it.removePrefix(COMPLETED_EVENT_PREFIX)] }
        .map { event ->
            val period = event.periods.minByOrNull { p -> periods.firstOrNull { it.period == p }?.order ?: 0 }
            ChoiceRecordUi(
                periodName = period?.let { names[it] } ?: "",
                yearInPeriod = 0,
                title = event.title,
                choiceLabel = null,
            )
        }
}
