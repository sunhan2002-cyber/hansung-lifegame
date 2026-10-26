package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Offline QA executable using the same parser, session and authored assets as the app. */
object Week10SimulationRunner {
    private val assets = File("src/main/assets")
    private fun json(name: String) = File(assets, "$name.sample.json").readText()
    private val periods = V4ContentParser.parsePeriods(json("periods"))
    private val focuses = V4ContentParser.parseFocuses(json("focuses"))
    private val events = V4ContentParser.parseDecisionEvents(json("events"))
    private val byId = events.associateBy { it.eventId }
    private val facts = V4ContentParser.parseLifeFactRules(json("life_facts"))
    private val endings = V4ContentParser.parseEndings(json("endings"))
    private val titles = JSONArray(json("titles"))
    private val prefixes = listOf("hea", "fit", "int", "soc", "wea", "hap", "luk")
    private val labels = listOf("건강", "운동능력", "지력", "사회성", "경제력", "행복", "운")
    private val initialBoundaries = listOf(1, 34, 35, 59, 60, 69, 70, 79, 80, 99, 100)

    private enum class Policy { RANDOM, TARGET, LOW_HEALTH, LOW_STATS, BOUNDARY }
    private data class Spec(val policy: Policy, val target: StatKey, val seed: Long)

    private fun session(seed: Long) = PeriodGameSession(
        periods, focuses, events, endings, facts,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "week10-$seed" }, runSeedFactory = { seed },
    )

    @JvmStatic fun main(args: Array<String>) {
        val out = File(args.getOrElse(0) { "build/reports/week10" }).apply { mkdirs() }
        val began = System.currentTimeMillis()
        val specs = buildList {
            for (policy in listOf(Policy.RANDOM, Policy.TARGET)) {
                for (target in StatKey.entries) repeat(500) { i ->
                    add(Spec(policy, target, 20261000L + i))
                }
            }
            for (policy in listOf(Policy.LOW_HEALTH, Policy.LOW_STATS, Policy.BOUNDARY)) {
                repeat(1000) { i -> add(Spec(policy, StatKey.entries[i % 7], 20262000L + i)) }
            }
        }
        val groups = linkedMapOf<String, MutableList<JSONObject>>()
        val seenEvents = mutableSetOf<String>()
        val seenOutcomes = mutableSetOf<String>()
        val seenEndings = mutableSetOf<String>()
        val issues = mutableListOf<JSONObject>()
        val examples = linkedMapOf<String, MutableList<JSONObject>>()
        var replayChecks = 0
        File(out, "runs.jsonl").bufferedWriter().use { writer ->
            specs.forEachIndexed { index, spec ->
                val run = play(spec, sampleRestoration = index % 250 == 0)
                if (index % 500 == 0) {
                    val replay = play(spec, sampleRestoration = false)
                    if (listOf("trace", "endingId", "facts", "stats", "flags").any { run.get(it).toString() != replay.get(it).toString() }) {
                        run.getJSONArray("violations").put("REPLAY_MISMATCH")
                    }
                    replayChecks++
                }
                writer.appendLine(run.toString())
                val key = "${spec.policy}/${spec.target}"
                groups.getOrPut(key) { mutableListOf() }.add(JSONObject(run.toString()).apply { remove("trace") })
                val trace = run.getJSONArray("trace")
                repeat(trace.length()) { i ->
                    val row = trace.getJSONObject(i)
                    seenEvents += row.getString("eventId")
                    seenOutcomes += "${row.getString("eventId")}:${row.getString("choiceId")}:${row.getString("outcomeId")}"
                }
                seenEndings += run.optString("endingId")
                val fail = run.getJSONArray("violations")
                if (fail.length() > 0) issues += run
                val match = run.optBoolean("pathEnding")
                val sampleKey = "$key/${if (match) "path" else "other"}"
                val bucket = examples.getOrPut(sampleKey) { mutableListOf() }
                if (bucket.size < (if (match) 3 else 1)) bucket += run
                if ((index + 1) % 500 == 0) println("WEEK10_PROGRESS ${index + 1}/${specs.size} violations=${issues.size}")
            }
        }
        val report = JSONObject().apply {
            put("schemaVersion", 1)
            put("sourceCommit", args.getOrElse(1) { "unrecorded" })
            put("runs", specs.size)
            put("durationMs", System.currentTimeMillis() - began)
            put("replayChecks", replayChecks)
            put("restorationSampleInterval", 250)
            put("titleStatus", "QA_EXPECTATION_ONLY_NOT_APP_IMPLEMENTATION")
            put("assetSha256", JSONObject(assets.listFiles()!!.filter { it.extension == "json" }.associate {
                it.name to MessageDigest.getInstance("SHA-256").digest(it.readBytes()).joinToString("") { b -> "%02x".format(b) }
            }))
            put("groups", JSONObject(groups.mapValues { (_, runs) -> summarize(runs) }))
            put("seenEvents", seenEvents.size)
            put("unseenEvents", JSONArray(events.map { it.eventId }.filterNot { it in seenEvents }.sorted()))
            put("seenOutcomes", seenOutcomes.size)
            put("seenEndings", seenEndings.size)
            put("unseenEndings", JSONArray(endings.map { it.endingId }.filterNot { it in seenEndings }.sorted()))
            put("invariantFailureRuns", issues.size)
        }
        File(out, "summary.json").writeText(report.toString(2))
        File(out, "counterexamples.json").writeText(JSONArray(issues.take(50)).toString(2))
        File(out, "reproduction_examples.json").writeText(JSONObject(examples).toString(2))
        File(out, "ending_witnesses.json").writeText(endingWitnesses(seenEndings).toString(2))
        println("WEEK10_COMPLETE runs=${specs.size} invariantFailureRuns=${issues.size} report=${out.absolutePath}")
        check(issues.isEmpty()) { "Invariant failures recorded; inspect counterexamples.json" }
    }

    private fun play(spec: Spec, sampleRestoration: Boolean): JSONObject {
        val game = session(spec.seed)
        var step = game.start()
        val initial = when (spec.policy) {
            Policy.LOW_STATS -> Stats(10, 10, 10, 10, 10, 10, 10)
            Policy.BOUNDARY -> initialBoundaries[(spec.seed % initialBoundaries.size).toInt()].let { v -> Stats(v, v, v, v, v, v, v) }
            else -> Stats()
        }
        if (initial != Stats()) step = game.restoreSaved(step.state.copy(stats = initial))
        val trace = JSONArray()
        val violations = mutableListOf<String>()
        var steps = 0
        while (step.phase != TurnPhase.ENDING && steps++ < 150) {
            if (step.state.isDead) violations += "DEAD_BUT_PROGRESSING"
            if (step.state.stats.values().any { it !in 0..100 }) violations += "STAT_OUT_OF_RANGE"
            if (sampleRestoration) {
                val restored = session(spec.seed).restoreSaved(step.state)
                if (restored.state != step.state || restored.phase != step.phase || restored.pendingDecision != step.pendingDecision) {
                    violations += "ENGINE_RESTORE_MISMATCH:${step.state.period}:${step.state.yearInPeriod}"
                }
            }
            step = when (step.phase) {
                TurnPhase.CHAPTER -> {
                    val candidates = focuses.filter { it.period == step.state.period }
                    val picked = when (spec.policy) {
                        Policy.LOW_HEALTH -> candidates.minBy { it.statEffects.health }
                        Policy.LOW_STATS -> candidates.minBy { it.statEffects.values().sum() }
                        else -> candidates.maxBy { it.statEffects.values()[spec.target.ordinal] }
                    }
                    game.selectFocus(picked.focusId)
                }
                TurnPhase.AUTO_PROGRESS -> game.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val options = pending.choices.filter { it.available }
                    if (options.isEmpty()) { violations += "ALL_CHOICES_LOCKED:${pending.eventId}"; break }
                    val event = byId.getValue(pending.eventId)
                    val hash = Math.floorMod("${spec.seed}:${event.eventId}:${step.state.period}:${step.state.yearInPeriod}".hashCode(), options.size)
                    val picked = when (spec.policy) {
                        Policy.TARGET -> options.maxBy { preview -> expected(event.choices.first { it.choiceId == preview.choiceId }, spec.target) }
                        Policy.LOW_HEALTH -> options.minBy { preview -> expected(event.choices.first { it.choiceId == preview.choiceId }, StatKey.HEALTH) }
                        else -> options[hash]
                    }
                    val before = step.state
                    val resolved = game.choose(picked.choiceId)
                    trace.put(JSONObject().apply {
                        put("period", before.period.name); put("year", before.yearInPeriod)
                        put("eventId", event.eventId); put("kind", event.kind.name)
                        put("choiceId", picked.choiceId); put("outcomeId", resolved.outcome.outcomeId)
                        put("lockedChoiceIds", JSONArray(pending.choices.filterNot { it.available }.map { it.choiceId }))
                        put("beforeStats", before.stats.toJson()); put("afterStats", resolved.step.state.stats.toJson())
                        put("addedFlags", JSONArray((resolved.step.state.flags - before.flags).sorted()))
                    })
                    if (sampleRestoration) {
                        val twice = runCatching { game.choose(picked.choiceId) }
                        if (twice.isSuccess || game.state != resolved.step.state) violations += "DUPLICATE_CHOICE_APPLIED:${event.eventId}"
                    }
                    resolved.step
                }
                TurnPhase.ENDING -> error("handled above")
            }
        }
        val state = step.state
        if (!state.isEnding) violations += "DID_NOT_END_WITHIN_150"
        if (!state.isDead) violations += "ENDING_WITHOUT_FINAL_DEATH"
        if (state.lifeFacts.deathPeriod in setOf(LifePeriod.INFANT, LifePeriod.CHILD)) violations += "FORBIDDEN_EARLY_DEATH"
        if (state.endingPeriod != state.lifeFacts.deathPeriod) violations += "DEATH_PERIOD_MISMATCH"
        if (state.lifeFacts.deathCause.isNullOrBlank()) violations += "MISSING_DEATH_CAUSE"
        if (game.ending == null) violations += "NO_ENDING_RESOLVED"
        if (sampleRestoration && state.isEnding && game.advance().state != state) violations += "PROGRESSED_AFTER_ENDING"
        return JSONObject().apply {
            put("policy", spec.policy.name); put("target", spec.target.name); put("seed", spec.seed)
            put("initialStats", initial.toJson()); put("endingId", game.ending?.endingId ?: "")
            put("pathEnding", game.ending?.endingId?.startsWith("ending_path_${prefixes[spec.target.ordinal]}_") == true)
            put("deathPeriod", state.lifeFacts.deathPeriod?.name ?: "UNKNOWN"); put("deathCause", state.lifeFacts.deathCause ?: "")
            put("stats", state.stats.toJson()); put("facts", factsJson(state.lifeFacts))
            put("flags", JSONArray(state.flags.sorted())); put("expectedTitleId", expectedTitle(state))
            put("bigIncidents", (0 until trace.length()).count { trace.getJSONObject(it).getString("kind") == "BIG_INCIDENT" })
            put("trace", trace); put("steps", steps); put("violations", JSONArray(violations.distinct()))
        }
    }

    private fun expected(choice: DecisionOption, stat: StatKey): Double = choice.outcomes.sumOf {
        (it.statDelta.values()[stat.ordinal] + if (stat == StatKey.HEALTH) it.lifeDelta else 0).toDouble() * it.chance
    } / choice.outcomes.sumOf { it.chance }

    /** Independent QA expectation. The production app does not yet load titles. */
    private fun expectedTitle(state: PeriodRunState): String {
        val all = (0 until titles.length()).map { titles.getJSONObject(it) }
        return (all.filterNot { it.optBoolean("isDefault") }.filter { title ->
            val c = title.getJSONObject("conditions")
            val mins = c.optJSONObject("minStats") ?: JSONObject()
            mins.keys().asSequence().all { key -> key in labels && state.stats.values()[labels.indexOf(key)] >= mins.getInt(key) } &&
                strings(c.optJSONArray("requiredFlags")).all { it in state.flags } &&
                strings(c.optJSONArray("blockedFlags")).none { it in state.flags }
        }.sortedWith(compareByDescending<JSONObject> { it.getInt("priority") }.thenBy { it.getString("titleId") }).firstOrNull()
            ?: all.single { it.optBoolean("isDefault") }).getString("titleId")
    }

    private fun summarize(runs: List<JSONObject>): JSONObject {
        fun counts(key: String) = JSONObject(runs.groupingBy { it.optString(key) }.eachCount().toSortedMap())
        val tracesBig = runs.map { row -> row.getInt("bigIncidents") }
        return JSONObject().apply {
            put("runs", runs.size); put("pathEndings", runs.count { it.getBoolean("pathEnding") })
            put("pathEndingRate", runs.count { it.getBoolean("pathEnding") }.toDouble() / runs.size)
            put("endings", counts("endingId")); put("deathPeriods", counts("deathPeriod"))
            put("expectedTitles", counts("expectedTitleId"))
            put("bigIncidentHistogram", JSONObject(tracesBig.groupingBy { it.toString() }.eachCount()))
            put("meanBigIncidents", tracesBig.average())
            put("factDistributions", JSONObject(listOf("educationLevel", "fieldOfStudy", "careerTrack", "careerLevel", "employmentStatus", "maritalStatus", "childrenCount", "housingType", "wealthBand").associateWith { key ->
                JSONObject(runs.groupingBy { it.getJSONObject("facts").opt(key)?.toString() ?: "UNRECORDED" }.eachCount().toSortedMap())
            }))
        }
    }

    /** Condition witnesses do not prove the state is reachable through a normal life. */
    private fun endingWitnesses(observed: Set<String>): JSONArray = JSONArray(endings.map { ending ->
        val c = ending.conditions
        val s = c.stats
        val state = PeriodRunState(
            runId = "condition-witness", runSeed = 1, period = LifePeriod.OLD_AGE,
            stats = Stats(s.minHealth ?: s.maxHealth ?: 50, s.minFitness ?: s.maxFitness ?: 50,
                s.minIntelligence ?: s.maxIntelligence ?: 50, s.minSocial ?: s.maxSocial ?: 50,
                s.minWealth ?: s.maxWealth ?: 50, s.minHappiness ?: s.maxHappiness ?: 50, s.minLuck ?: s.maxLuck ?: 50),
            flags = c.requiredFlags,
            lifeFacts = LifeFacts(educationLevel = c.lifeFacts.educationLevel, fieldOfStudy = c.lifeFacts.fieldOfStudy,
                careerTrack = c.lifeFacts.careerTrack, careerLevel = c.lifeFacts.careerLevel,
                employmentStatus = c.lifeFacts.employmentStatus, maritalStatus = c.lifeFacts.maritalStatus,
                childrenCount = c.lifeFacts.minChildrenCount ?: 0, housingType = c.lifeFacts.housingType,
                wealthBand = c.lifeFacts.wealthBand ?: "MIDDLE", achievementFlags = c.lifeFacts.requiredAchievements),
            isEnding = true, isDead = true, endingPeriod = LifePeriod.OLD_AGE,
        )
        val actual = GameEngine().resolveEnding(state, endings).endingId
        JSONObject().apply {
            put("endingId", ending.endingId); put("observedInSimulation", ending.endingId in observed)
            put("syntheticResolverEnding", actual); put("syntheticWitnessMatches", actual == ending.endingId)
            put("scope", "SYNTHETIC_CONDITION_ONLY_NOT_PLAY_REACHABILITY")
        }
    })

    private fun strings(a: JSONArray?): List<String> = if (a == null) emptyList() else List(a.length()) { a.getString(it) }
    private fun Stats.values() = listOf(health, fitness, intelligence, social, wealth, happiness, luck)
    private fun StatDelta.values() = listOf(health, fitness, intelligence, social, wealth, happiness, luck)
    private fun Stats.toJson() = JSONObject(labels.zip(values()).toMap())
    private fun factsJson(f: LifeFacts) = JSONObject().apply {
        put("educationLevel", f.educationLevel ?: JSONObject.NULL); put("fieldOfStudy", f.fieldOfStudy ?: JSONObject.NULL)
        put("careerTrack", f.careerTrack ?: JSONObject.NULL); put("careerLevel", f.careerLevel ?: JSONObject.NULL)
        put("employmentStatus", f.employmentStatus ?: JSONObject.NULL); put("maritalStatus", f.maritalStatus ?: JSONObject.NULL)
        put("childrenCount", f.childrenCount); put("housingType", f.housingType ?: JSONObject.NULL)
        put("wealthBand", f.wealthBand); put("achievementFlags", JSONArray(f.achievementFlags.sorted()))
    }
}
