package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.*

/** The starting conditions used by both a real game and a reproducible QA run. */
data class SessionStartProfile(
    val genderFlag: String,
    val starterId: String?,
    val starterDelta: StatDelta = StatDelta(),
    val starterFlags: Set<String> = emptySet(),
    val rolledDelta: StatDelta? = null,
    val startingRollRaw: String? = null,
    val runSeed: Long? = null,
) {
    init {
        require(genderFlag in setOf("gender_male", "gender_female")) { "A starting gender is required" }
    }

    val initialFlags: Set<String> get() = buildSet {
        addAll(starterFlags)
        add(genderFlag)
        if (genderFlag == "gender_male") add("military_service_required")
    }

    /** Dice replace the starter bonus; they must never be added a second time. */
    val initialStats: Stats get() {
        val delta = rolledDelta ?: starterDelta
        fun stat(value: Int) = (Stats.DEFAULT_STAT + value).coerceIn(0, 100)
        return Stats(stat(delta.health), stat(delta.fitness), stat(delta.intelligence),
            stat(delta.social), stat(delta.wealth), stat(delta.happiness), stat(delta.luck))
    }
}

data class StartedPeriodSession(val session: PeriodGameSession, val step: TurnStep)

/** Single production configuration: QA must include the same authored automatic events as the app. */
class PeriodSessionFactory(
    private val periods: List<PeriodDefinition>,
    private val focuses: List<Focus>,
    private val decisions: List<DecisionEvent>,
    private val endings: List<Ending>,
    private val lifeFactRules: List<LifeFactRule>,
    private val automaticEvents: List<AutomaticEvent>,
    private val automaticEventRules: List<AutomaticEventRule>,
) {
    private fun engine() = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true)

    fun create(): PeriodGameSession = PeriodGameSession(
        periods = periods,
        focuses = focuses,
        decisions = decisions,
        endings = endings,
        lifeFactRules = lifeFactRules,
        automaticEvents = automaticEvents,
        engine = engine(),
        automaticEventPicker = automaticEventRules.takeIf { it.isNotEmpty() }
            ?.let { AutomaticEventPicker(it, periods) },
    )

    fun start(
        profile: SessionStartProfile,
        runId: String = "run-${System.currentTimeMillis()}",
        /** Explicit synthetic boundary input, never used by normal app initialization. */
        initialStatsOverride: Stats? = null,
    ): StartedPeriodSession {
        val session = create()
        val initial = engine().createPeriodRun(runId, profile.runSeed ?: System.currentTimeMillis(), periods)
        // Apply the complete profile BEFORE selecting the first event or applying automatic effects.
        val step = session.restore(initial.copy(
            stats = initialStatsOverride ?: profile.initialStats,
            flags = initial.flags + profile.initialFlags,
        ))
        return StartedPeriodSession(session, step)
    }
}
