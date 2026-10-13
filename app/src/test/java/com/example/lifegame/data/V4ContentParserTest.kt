package com.example.lifegame.data

import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TransitionCue
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V4ContentParserTest {
    @Test
    fun `periods sample fields are preserved and sorted`() {
        val periods = V4ContentParser.parsePeriods(
            """
            [
              {"periodId":"CHILD","displayName":"유년기","chapterTitle":"제2장 유년기","order":1,"years":5},
              {"periodId":"INFANT","displayName":"영아기","chapterTitle":"제1장 영아기","order":0,"years":4,
               "startLabel":"시작","endLabel":"끝"}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf(LifePeriod.INFANT, LifePeriod.CHILD), periods.map { it.period })
        assertEquals(4, periods.first().years)
        assertEquals("제1장 영아기", periods.first().chapterTitle)
        assertEquals("시작", periods.first().startLabel)
        assertEquals("끝", periods.first().endLabel)
    }

    @Test
    fun `focus sample maps annual delta logs and image ids`() {
        val focus = V4ContentParser.parseFocuses(
            """
            [{
              "focusId":"focus_infant_bonding","period":"INFANT","name":"애착","description":"안정감",
              "annualStatDelta":{"사회성":2,"행복":1,"지력":-1},
              "sceneImageId":"img_scene_001","momentImageId":"img_moment_001",
              "yearlyLogTemplates":["기록 1","기록 2","기록 3"]
            }]
            """.trimIndent(),
        ).single()

        assertEquals("애착", focus.label)
        assertEquals(2, focus.statEffects.social)
        assertEquals(1, focus.statEffects.happiness)
        assertEquals(-1, focus.statEffects.intelligence)
        assertEquals("img_scene_001", focus.sceneImageId)
        assertEquals("img_moment_001", focus.momentImageId)
        assertEquals(3, focus.yearlyLogTemplates.size)
    }

    @Test
    fun `outcomes and chance weights map to decision event`() {
        val event = V4ContentParser.parseDecisionEvents(probabilityEventJson()).single()
        val outcome = event.choices.single().outcomes.single()

        assertEquals(DecisionKind.BIG_INCIDENT, event.kind)
        assertEquals(setOf(LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE), event.periods)
        assertEquals(60, event.statConditions.minHealth)
        assertEquals("img_pov_crash_001", event.povImageId)
        assertEquals("img_moment_crash_001", event.momentImageId)
        assertEquals(35, outcome.chance)
        assertFalse(outcome.primary)
        assertEquals(2, outcome.chanceWeights.health)
        assertTrue(outcome.hidden)
        assertTrue(outcome.death)
    }

    @Test
    fun `legacy choice becomes deterministic one hundred chance outcome`() {
        val event = V4ContentParser.parseDecisionEvents(
            """
            [{
              "eventId":"legacy_001","stage":"CHILD","type":"NORMAL","title":"옛 사건","text":"상황","imageId":"img_old",
              "choices":[{"choiceId":"A","label":"선택","statDelta":{"행복":3},"resultText":"결과","addFlags":["done"]}]
            }]
            """.trimIndent(),
        ).single()
        val outcome = event.choices.single().outcomes.single()

        assertEquals(DecisionKind.INCIDENT, event.kind)
        assertEquals(100, outcome.chance)
        assertEquals(3, outcome.statDelta.happiness)
        assertEquals(setOf("done"), outcome.addFlags)
        assertFalse(outcome.death)
    }

    @Test
    fun `milestone without an explicit year is still handled before random incidents`() {
        val periods = V4ContentParser.parsePeriods(
            """[{"periodId":"CHILD","displayName":"유년기","chapterTitle":"제2장","order":0,"years":2}]""",
        )
        val milestone = V4ContentParser.parseDecisionEvents(
            """
            [{
              "eventId":"child_milestone","stage":"CHILD","kind":"MILESTONE","title":"입학","text":"첫 등교",
              "imageId":"img_pov_school","conditions":{"stats":{},"requiredFlags":[],"blockedFlags":[]},
              "choices":[{"choiceId":"A","label":"들어간다","statDelta":{},"resultText":"교실에 들어갔다.","addFlags":[],
                "outcomes":[{"outcomeId":"ok","text":"교실에 들어갔다.","chance":100,"primary":true,"hidden":false,"death":false}]}]
            }]
            """.trimIndent(),
        ).single()
        val focus = Focus("study", LifePeriod.CHILD, "학업", StatDelta(), "공부")
        val engine = GameEngine()
        val selected = engine.selectFocus(engine.createPeriodRun("run", 1L, periods), focus)

        val step = engine.advancePeriodYear(
            selected,
            periods,
            listOf(focus),
            listOf(milestone),
            bigIncidentChancePercent = 0,
            incidentChancePercent = 0,
        )

        assertEquals("child_milestone", step.pendingDecision?.eventId)
        assertEquals(DecisionKind.MILESTONE, step.pendingDecision?.kind)
        assertFalse(step.canAutoAdvance)
    }

    @Test
    fun `loaded big incident stops auto advance and exposes pov then moment image`() {
        val periods = V4ContentParser.parsePeriods(
            """[{"periodId":"YOUNG_ADULT","displayName":"청년기","chapterTitle":"제4장","order":0,"years":1}]""",
        )
        val event = V4ContentParser.parseDecisionEvents(probabilityEventJson()).single()
        val focus = Focus(
            focusId = "career",
            period = LifePeriod.YOUNG_ADULT,
            label = "커리어",
            statEffects = StatDelta(health = 10),
            description = "직무 역량",
            sceneImageId = "img_scene_career_001",
            yearlyLogTemplates = listOf("일에 집중했다."),
        )
        val engine = GameEngine()
        val initial = engine.createPeriodRun("run", 20261012L, periods)
        val selected = engine.selectFocus(initial, focus)

        val pending = engine.advancePeriodYear(
            state = selected,
            periods = periods,
            focuses = listOf(focus),
            decisions = listOf(event),
            bigIncidentChancePercent = 100,
            incidentChancePercent = 0,
        )

        assertEquals(TurnPhase.DECISION, pending.phase)
        assertFalse(pending.canAutoAdvance)
        assertEquals(TransitionCue.DISTORT, pending.transitionCue)
        assertEquals("img_scene_career_001", pending.sceneImageId)
        assertEquals("img_pov_crash_001", pending.pendingDecision?.povImageId)

        val resolved = engine.resolvePeriodDecision(pending.state, event, "A", periods)
        assertEquals("img_moment_crash_001", resolved.step.momentImageId)
        assertEquals(TurnPhase.ENDING, resolved.step.phase)
        assertTrue(resolved.step.state.isEnding)
        assertNull(resolved.step.pendingDecision)
    }

    private fun probabilityEventJson(): String =
        """
        [{
          "eventId":"adult_big_001","stage":"ADULT","kind":"BIG_INCIDENT","title":"큰 사고","text":"순간 선택이 필요하다.",
          "imageId":"img_event_fallback","povImageId":"img_pov_crash_001","momentImageId":"img_moment_crash_001",
          "conditions":{"stats":{"건강":60},"requiredFlags":[],"blockedFlags":[]},
          "choices":[{
            "choiceId":"A","label":"피한다","statDelta":{},"resultText":"기본 결과","addFlags":[],
            "outcomes":[{
              "outcomeId":"fatal","text":"피하지 못했다.","chance":35,"primary":false,"hidden":true,"death":true,
              "statDelta":{"건강":-100},"addFlags":["crash"],"chanceWeights":{"건강":2}
            }]
          }]
        }]
        """.trimIndent()
}
