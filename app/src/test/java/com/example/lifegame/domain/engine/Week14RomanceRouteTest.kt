package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TurnPhase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 11/20 신기훈: 상대별 연애 경로 3개가 실제 만남 → 첫 데이트 → 추가 에피소드 → 결혼 논의 → 양가 인사 → 결혼 준비 →
 * 플레이어 본인의 본식으로 이어지고, 거절·결별·보류·비혼도 열려 있는지 고정 시드로 확인한다.
 * 앱과 같은 파서·세션·실제 자산을 쓴다. 각 시나리오는 정해진 시드 범위를 앞에서부터 돌려 처음 성립하는 시드를 기록한다.
 * 결과: build/reports/week14/romance_routes.json
 */
class Week14RomanceRouteTest {
    private val assets = File("src/main/assets")
    private fun json(name: String) = File(assets, "$name.sample.json").readText()
    private val periods = V4ContentParser.parsePeriods(json("periods"))
    private val focuses = V4ContentParser.parseFocuses(json("focuses"))
    private val events = V4ContentParser.parseDecisionEvents(json("events"))
    private val endings = V4ContentParser.parseEndings(json("endings"))
    private val facts = V4ContentParser.parseLifeFactRules(json("life_facts"))
    private val ceremonies = setOf("love_wedding_01", "love_wedding_first_love", "love_wedding_music", "love_wedding_book")

    private data class Step(
        val period: LifePeriod, val year: Int, val eventId: String, val choiceId: String, val outcomeId: String,
        val before: Set<String>, val added: Set<String>,
    )

    private data class Life(val seed: Long, val steps: List<Step>, val flags: Set<String>)

    private fun StatDelta.values() = listOf(health, fitness, intelligence, social, wealth, happiness, luck)

    private fun session(seed: Long) = PeriodGameSession(
        periods, focuses, events, endings, facts,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "week14-$seed" }, runSeedFactory = { seed },
    )

    /** 만남 사건에서 교제를 시작하는 선택과 거절하는 선택. */
    private val dateChoice = mapOf(
        "teen_021" to "C", "path_soc_03" to "A", "adult_077" to "A", "path_hap_05" to "A", "adult_075" to "A",
        "adult_074" to "C", "path_soc_11" to "C",
    )
    private val declineChoice = mapOf(
        "teen_021" to "B", "path_soc_03" to "B", "adult_077" to "B", "path_hap_05" to "B", "adult_075" to "C",
        "adult_074" to "B", "path_soc_11" to "B", "adult_073" to "B", "adult_076" to "B", "adult_078" to "C",
        "adult_005" to "B", "adult_006" to "B", "adult_008" to "C", "love_letter_01" to "A",
    )
    private val routeMeetings = mapOf(
        "first_love" to setOf("teen_021", "path_soc_03", "adult_077"),
        "music" to setOf("path_hap_05", "adult_075"),
        "book" to setOf("adult_074", "path_soc_11"),
    )
    private val progress = mapOf(
        "love_marriage_talk_01" to "A", "adult_032" to "A", "love_family_meet_01" to "B", "path_soc_05" to "A",
        "love_wedding_prep_01" to "A", "love_wedding_01" to "A",
    )

    private fun play(seed: Long, focusStat: Int, pick: (String, List<String>) -> String?): Life {
        val game = session(seed)
        var step = game.start()
        val trace = mutableListOf<Step>()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 200) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> game.selectFocus(
                    focuses.filter { it.period == step.state.period }.maxBy { it.statEffects.values()[focusStat] }.focusId,
                )
                TurnPhase.AUTO_PROGRESS -> game.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val options = pending.choices.filter { it.available }.map { it.choiceId }
                    val hash = Math.floorMod("$seed:${pending.eventId}:${step.state.yearInPeriod}".hashCode(), options.size)
                    val chosen = pick(pending.eventId, options)?.takeIf { it in options } ?: options[hash]
                    val before = step.state.flags
                    val resolved = game.choose(chosen)
                    trace += Step(step.state.period, step.state.yearInPeriod, pending.eventId, chosen, resolved.outcome.outcomeId,
                        before, resolved.step.state.flags - before)
                    resolved.step
                }
                TurnPhase.ENDING -> step
            }
        }
        return Life(seed, trace, step.state.flags)
    }

    private fun routePolicy(route: String, overrides: Map<String, String> = emptyMap()): (String, List<String>) -> String? = { id, _ ->
        overrides[id] ?: when {
            id in routeMeetings.getValue(route) -> dateChoice[id]
            id in declineChoice -> declineChoice[id]
            id.startsWith("love_") && id.endsWith(route) -> "A"
            else -> progress[id]
        }
    }

    /** 모든 시도에서 지켜야 하는 순서. 위반이 있으면 메시지를 돌려준다. */
    private fun violations(life: Life): List<String> = life.steps.flatMap { s ->
        buildList {
            if ("dating" in s.added && s.before.any { it in setOf("dating", "engaged", "married") }) add("dating_while_in_relationship:${s.eventId}")
            if ("engaged" in s.added && !s.before.containsAll(setOf("dating", "rel_steady"))) add("engaged_without_steady:${s.eventId}")
            if ("married" in s.added) {
                if (s.eventId !in ceremonies) add("married_outside_ceremony:${s.eventId}")
                if (!s.before.containsAll(setOf("engaged", "families_met", "wedding_ready"))) add("married_without_preparation:${s.eventId}")
                val spouse = s.added.filter { it.startsWith("spouse_") }
                if (spouse.size != 1) add("spouse_flag_count:${s.eventId}")
                val route = spouse.singleOrNull()?.removePrefix("spouse_")
                if (route != null && route != "other" && "partner_$route" !in s.before) add("spouse_not_current_partner:${s.eventId}")
            }
            if ("has_child" in s.added && "married" !in s.before) add("child_without_marriage:${s.eventId}")
        }
    }

    private fun search(name: String, focusStat: Int, policy: (String, List<String>) -> String?, ok: (Life) -> Boolean): Pair<Life?, Int> {
        var tried = 0
        for (seed in 20261120L until 20261120L + 4000) {
            val life = play(seed, focusStat, policy)
            tried++
            assertEquals("$name seed $seed", emptyList<String>(), violations(life))
            if (ok(life)) return life to tried
        }
        return null to tried
    }

    private fun Life.married(atEvent: String, partner: String) =
        steps.any { it.eventId == atEvent && "married" in it.added && partner in it.before }

    @Test fun threeRoutesReachTheirOwnCeremonyAndAllExitsStayOpen() {
        // focus index: 0 건강 1 운동 2 지력 3 사회성 4 경제력 5 행복 6 운
        val scenarios = linkedMapOf<String, Triple<Int, (String, List<String>) -> String?, (Life) -> Boolean>>(
            "route_first_love" to Triple(3, routePolicy("first_love")) { l -> l.married("love_wedding_first_love", "partner_first_love") },
            "route_music" to Triple(5, routePolicy("music")) { l -> l.married("love_wedding_music", "partner_music") },
            "route_book" to Triple(2, routePolicy("book")) { l -> l.married("love_wedding_book", "partner_book") },
            "decline_meeting" to Triple(3, { id, _ -> declineChoice[id] ?: progress[id] }) { l ->
                l.steps.any { it.eventId in dateChoice && it.choiceId == declineChoice[it.eventId] } && "married" !in l.flags
            },
            "breakup" to Triple(3, routePolicy("first_love", mapOf("love_date_first_love" to "C", "love_conflict_first_love" to "C"))) { l ->
                l.steps.any { "breakup_once" in it.added }
            },
            "postpone_talk" to Triple(2, routePolicy("book", mapOf("love_marriage_talk_01" to "B", "adult_032" to "B"))) { l ->
                l.steps.any { "long_term_dating" in it.added } && "married" !in l.flags
            },
            "postpone_wedding" to Triple(5, routePolicy("music", mapOf("love_wedding_prep_01" to "B"))) { l ->
                l.steps.any { "wedding_postponed" in it.added } && "married" !in l.flags
            },
            "single_life" to Triple(3, { id, _ ->
                mapOf("adult_073" to "B", "path_hap_05" to "B", "path_soc_11" to "B", "cross_02" to "A")[id] ?: declineChoice[id]
            }) { l -> "single_life" in l.flags && "married" !in l.flags },
        )
        val report = JSONObject()
        scenarios.forEach { (name, spec) ->
            val (life, tried) = search(name, spec.first, spec.second, spec.third)
            assertNotNull("$name: 4000개 고정 시드 안에서 성립해야 한다", life)
            life!!
            val love = life.steps.filter {
                it.eventId.startsWith("love_") || it.eventId in dateChoice || it.eventId in setOf("adult_032", "path_soc_05", "adult_073")
            }
            report.put(name, JSONObject().apply {
                put("seed", life.seed); put("seedsTried", tried)
                put("steps", JSONArray(love.map { "${it.period}:${it.year} ${it.eventId}:${it.choiceId}:${it.outcomeId} +${it.added.filterNot { f -> f.startsWith("v4:") }.sorted()}" }))
            })
            println("WEEK14_ROUTE $name seed=${life.seed} tried=$tried")
        }
        File("build/reports/week14").apply { mkdirs() }.resolve("romance_routes.json").writeText(report.toString(2))
        assertTrue(report.length() == scenarios.size)
    }
}
