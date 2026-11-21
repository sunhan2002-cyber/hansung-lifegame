package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EndingTier
import com.example.lifegame.domain.model.EventConditions
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.ImageAsset
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.engine.AutomaticEventRule
import org.json.JSONArray
import org.json.JSONObject

data class PlayableContent(
    val content: V4Content,
    val endings: List<Ending>,
    val automaticEventRules: List<AutomaticEventRule>,
)

class ContentRepository(private val loader: CachedContentLoader) {
    constructor(context: Context) : this(CachedContentLoader { name ->
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    })

    private val periods = ContentAsset(PERIODS_FILE, V4ContentParser::parsePeriods) {
        when {
            it.isEmpty() -> "EMPTY_PERIODS"
            it.map { period -> period.period }.distinct().size != it.size -> "DUPLICATE_PERIOD"
            it.any { period -> period.years <= 0 } -> "INVALID_PERIOD_LENGTH"
            else -> null
        }
    }
    private val focuses = ContentAsset(FOCUSES_FILE, V4ContentParser::parseFocuses) {
        if (it.isEmpty()) "EMPTY_FOCUSES" else null
    }
    private val decisions = ContentAsset(EVENTS_FILE, V4ContentParser::parseDecisionEvents) {
        when {
            it.isEmpty() -> "EMPTY_DECISIONS"
            it.any { event -> event.choices.isEmpty() } -> "EMPTY_CHOICES"
            else -> null
        }
    }
    private val facts = ContentAsset(LIFE_FACTS_FILE, V4ContentParser::parseLifeFactRules)
    private val titles = ContentAsset(TITLES_FILE, V4ContentParser::parseTitles) {
        if (it.count { title -> title.isDefault } != 1) "DEFAULT_TITLE_REQUIRED" else null
    }
    private val automaticEvents = ContentAsset(AUTO_EVENTS_FILE, V4ContentParser::parseAutomaticEvents)
    private val automaticRules = ContentAsset(AUTO_EVENTS_FILE, AutomaticEventParser::parse)
    private val endings = ContentAsset(ENDINGS_FILE, V4ContentParser::parseEndings) {
        if (it.count { ending -> ending.isDefault } != 1) "DEFAULT_ENDING_REQUIRED" else null
    }
    private val images = ContentAsset(IMAGE_IDS_FILE, { raw: String ->
        val array = JSONArray(raw)
        List(array.length()) { array.getJSONObject(it).toImageAsset() }
    })
    private val legacyEvents = ContentAsset(EVENTS_FILE, { raw: String ->
        val array = JSONArray(raw)
        List(array.length()) { array.getJSONObject(it).toGameEvent() }
    })

    val issues: List<ContentIssue> get() = loader.issues
    val fingerprints: Map<String, String> get() = loader.fingerprints

    // Compatibility APIs keep callers that display optional/archived content safe. Play entry points
    // must use loadPlayableContent(), which never treats failed required files as empty valid content.
    fun loadPeriodDefinitions(): List<PeriodDefinition> = loader.load(periods).valueOrNull().orEmpty()
    fun loadFocuses(): List<Focus> = loader.load(focuses).valueOrNull().orEmpty()
    fun loadDecisionEvents(): List<DecisionEvent> = loader.load(decisions).valueOrNull().orEmpty()
    fun loadLifeFactRules() = loader.load(facts).valueOrNull().orEmpty()
    fun loadTitles(): List<LifeTitle> = loader.load(titles).valueOrNull().orEmpty()
    fun loadAutomaticEvents() = loader.load(automaticEvents).valueOrNull().orEmpty()
    fun loadAutomaticEventRules() = loader.load(automaticRules).valueOrNull().orEmpty()

    private val playable: ContentLoadResult<PlayableContent> by lazy {
        val results = listOf(loader.load(periods), loader.load(focuses), loader.load(decisions),
            loader.load(facts), loader.load(titles), loader.load(automaticEvents),
            loader.load(endings), loader.load(automaticRules))
        val failures = results.filterIsInstance<ContentLoadResult.Failed>().flatMap { it.issues }.distinct().toMutableList()
        if (failures.isEmpty() && loadAutomaticEvents().map { it.eventId }.toSet() !=
            loadAutomaticEventRules().map { it.event.eventId }.toSet()) {
            failures += ContentIssue(AUTO_EVENTS_FILE, ContentFailureKind.VALIDATION,
                "INCOMPLETE_AUTOMATIC_RULES", loader.fingerprints[AUTO_EVENTS_FILE])
        }
        if (failures.isNotEmpty()) ContentLoadResult.Failed(failures)
        else ContentLoadResult.Ready(
            PlayableContent(loadV4Content(), loadEndings(), loadAutomaticEventRules()),
            loader.fingerprints,
        )
    }

    fun loadPlayableContent(): ContentLoadResult<PlayableContent> = playable

    fun loadV4Content(): V4Content = V4Content(
        periods = loadPeriodDefinitions(),
        focuses = loadFocuses(),
        decisions = loadDecisionEvents(),
        lifeFactRules = loadLifeFactRules(),
        titles = loadTitles(),
        automaticEvents = loadAutomaticEvents(),
    )

    fun loadEvents(): List<GameEvent> = loader.load(legacyEvents).valueOrNull().orEmpty()
    fun loadEndings(): List<Ending> = loader.load(endings).valueOrNull().orEmpty()
    fun loadImageAssets(): List<ImageAsset> = loader.load(images).valueOrNull().orEmpty()

    fun findImageAsset(imageId: String): ImageAsset? =
        loadImageAssets().firstOrNull { it.imageId == imageId }

    private fun JSONObject.toGameEvent(): GameEvent = GameEvent(
        eventId = getString("eventId"),
        stage = getString("stage").toLifeStage(),
        type = optString("type", "NORMAL").toEventType(),
        title = getString("title"),
        text = getString("text"),
        imageId = getString("imageId"),
        conditions = optJSONObject("conditions").toEventConditions(),
        choices = getJSONArray("choices").toChoices(),
        priority = optInt("priority", 0),
        isFallback = optBoolean("isFallback", false),
    )

    private fun JSONObject.toEnding(): Ending = Ending(
        endingId = getString("endingId"),
        title = getString("title"),
        summary = getString("summary"),
        priority = optInt("priority", 0),
        conditions = optJSONObject("conditions").toEndingConditions(),
        isDefault = optBoolean("isDefault", false),
        tier = optString("tier").takeIf { it.isNotBlank() }
            ?.let { runCatching { EndingTier.valueOf(it.uppercase()) }.getOrNull() }
            ?: if (optBoolean("isDefault", false)) EndingTier.DEFAULT else EndingTier.GENERAL_LIFE,
    )

    private fun JSONObject.toImageAsset(): ImageAsset = ImageAsset(
        imageId = getString("imageId"),
        usage = optString("usage", "event"),
        stage = optString("stage", ""),
        description = optString("description", ""),
        fileName = optString("fileName", ""),
        fallbackText = optString("fallbackText", optString("description", "")),
    )

    private fun JSONArray.toChoices(): List<Choice> = List(length()) { index ->
        val item = getJSONObject(index)
        Choice(
            choiceId = item.getString("choiceId"),
            label = item.getString("label"),
            statDelta = item.optJSONObject("statDelta").toStatDelta(),
            resultText = item.getString("resultText"),
            addFlags = item.optJSONArray("addFlags").toStringSet(),
            removeFlags = item.optJSONArray("removeFlags").toStringSet(),
        )
    }

    private fun JSONObject?.toEventConditions(): EventConditions = EventConditions(
        stats = this?.optJSONObject("stats").toStatConditions(),
        requiredFlags = this?.optJSONArray("requiredFlags").toStringSet(),
        blockedFlags = this?.optJSONArray("blockedFlags").toStringSet(),
    )

    private fun JSONObject?.toEndingConditions(): EndingConditions = EndingConditions(
        stats = this?.optJSONObject("minStats").toStatConditions(),
        requiredFlags = this?.optJSONArray("requiredFlags").toStringSet(),
        blockedFlags = this?.optJSONArray("blockedFlags").toStringSet(),
        lifeFacts = this?.optJSONObject("lifeFacts").toLifeFactConditions(),
    )

    private fun JSONObject?.toLifeFactConditions(): LifeFactConditions = LifeFactConditions(
        educationLevel = this?.optString("educationLevel")?.takeIf { it.isNotBlank() },
        fieldOfStudy = this?.optString("fieldOfStudy")?.takeIf { it.isNotBlank() },
        careerTrack = this?.optString("careerTrack")?.takeIf { it.isNotBlank() },
        careerLevel = this?.optString("careerLevel")?.takeIf { it.isNotBlank() },
        employmentStatus = this?.optString("employmentStatus")?.takeIf { it.isNotBlank() },
        maritalStatus = this?.optString("maritalStatus")?.takeIf { it.isNotBlank() },
        minChildrenCount = this?.optNullableInt("minChildrenCount"),
        housingType = this?.optString("housingType")?.takeIf { it.isNotBlank() },
        wealthBand = this?.optString("wealthBand")?.takeIf { it.isNotBlank() },
        requiredAchievements = this?.optJSONArray("requiredAchievements").toStringSet(),
    )

    private fun JSONObject?.toStatConditions(): StatConditions = StatConditions(
        minHealth = this?.optNullableInt("건강") ?: this?.optNullableInt("health"),
        minFitness = this?.optNullableInt("운동능력") ?: this?.optNullableInt("fitness"),
        minIntelligence = this?.optNullableInt("지력") ?: this?.optNullableInt("intelligence"),
        minSocial = this?.optNullableInt("사회성") ?: this?.optNullableInt("social"),
        minWealth = this?.optNullableInt("경제력") ?: this?.optNullableInt("wealth"),
        minHappiness = this?.optNullableInt("행복") ?: this?.optNullableInt("happiness"),
        minLuck = this?.optNullableInt("운") ?: this?.optNullableInt("luck"),
    )

    private fun JSONObject?.toStatDelta(): StatDelta = StatDelta(
        health = this?.optNullableInt("건강") ?: this?.optNullableInt("health") ?: 0,
        fitness = this?.optNullableInt("운동능력") ?: this?.optNullableInt("fitness") ?: 0,
        intelligence = this?.optNullableInt("지력") ?: this?.optNullableInt("intelligence") ?: 0,
        social = this?.optNullableInt("사회성") ?: this?.optNullableInt("social") ?: 0,
        wealth = this?.optNullableInt("경제력") ?: this?.optNullableInt("wealth") ?: 0,
        happiness = this?.optNullableInt("행복") ?: this?.optNullableInt("happiness") ?: 0,
        luck = this?.optNullableInt("운") ?: this?.optNullableInt("luck") ?: 0,
    )

    private fun JSONArray?.toStringSet(): Set<String> =
        if (this == null) emptySet() else List(length()) { index -> getString(index) }.toSet()

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun String.toLifeStage(): LifeStage = when (uppercase()) {
        "INFANT", "유아기" -> LifeStage.INFANT
        "CHILD", "아동기" -> LifeStage.CHILD
        "TEEN", "청소년기" -> LifeStage.TEEN
        "ADULT", "성인기" -> LifeStage.ADULT
        else -> error("Unknown life stage: $this")
    }

    private fun String.toEventType(): EventType = when (uppercase()) {
        "SPECIAL", "돌발" -> EventType.SPECIAL
        else -> EventType.NORMAL
    }

    private companion object {
        const val PERIODS_FILE = "periods.sample.json"
        const val FOCUSES_FILE = "focuses.sample.json"
        const val EVENTS_FILE = "events.sample.json"
        const val ENDINGS_FILE = "endings.sample.json"
        const val IMAGE_IDS_FILE = "image_ids.sample.json"
        const val LIFE_FACTS_FILE = "life_facts.sample.json"
        const val TITLES_FILE = "titles.sample.json"
        const val AUTO_EVENTS_FILE = "auto_events.sample.json"
    }
}
