package com.example.lifegame.ui

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 일곱 스탯 집중 플레이를 같은 시드 · 같은 시작 조건으로 끝까지 돌려,
 * 결말 화면에 나가는 요약이 경로마다 구분되는지 확인한다 (8주차 완료 기준).
 */
class LifePathEndingTest {

    private val assets = File("src/main/assets")
    private val periods = V4ContentParser.parsePeriods(File(assets, "periods.sample.json").readText())
    private val focuses = V4ContentParser.parseFocuses(File(assets, "focuses.sample.json").readText())
    private val decisions = V4ContentParser.parseDecisionEvents(File(assets, "events.sample.json").readText())
    private val catalog = LifeFactCatalog(parseLifeFacts(File(assets, "life_facts.sample.json").readText()))
    private val content = PlayContent(
        periods = periods,
        focuses = focuses,
        decisions = decisions.associateBy { it.eventId },
        imageFiles = emptyMap(),
        factCatalog = catalog,
        lookup = FlagLookup.from(decisions, catalog),
    )

    private data class PathRun(val target: LifePathKind, val step: TurnStep, val ui: PeriodPlayUiState)

    private fun StatDelta.valueOf(stat: String): Int = toDisplayMap()[stat] ?: 0

    private fun play(target: LifePathKind, seed: Long = 20261020L): PathRun {
        val session = PeriodGameSession(
            periods = periods,
            focuses = focuses,
            decisions = decisions,
            engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
            runIdFactory = { "path-${target.name}" },
            runSeedFactory = { seed },
        )
        var step = session.start()
        val records = mutableListOf<ChoiceRecordUi>()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 600) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> {
                    val options: List<Focus> = focuses.filter { it.period == step.state.period }
                    val pick = options.maxByOrNull { it.statEffects.valueOf(target.stat) } ?: options.first()
                    session.selectFocus(pick.focusId)
                }
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val event = content.decisions.getValue(pending.eventId)
                    val available = pending.choices.filter { it.available }
                    val pick = available.maxByOrNull { preview ->
                        val option = event.choices.first { it.choiceId == preview.choiceId }
                        option.outcomes.firstOrNull { !it.hidden }?.let { o ->
                            o.statDelta.valueOf(target.stat) * 10 + (if (o.death) -1000 else 0)
                        } ?: 0
                    } ?: available.first()
                    val period = periods.first { it.period == step.state.period }.displayName
                    records += ChoiceRecordUi(period, step.state.yearInPeriod, event.title, pick.label)
                    session.choose(pick.choiceId).step
                }
                TurnPhase.ENDING -> step
            }
        }
        assertEquals("${target.name}: 끝까지 가야 한다", TurnPhase.ENDING, step.phase)
        val ui = buildPeriodPlayState(step, content, playerName = "재겸", choiceLog = records)
        return PathRun(target, step, ui)
    }

    @Test
    fun `seven stat focused lives produce different ending summaries`() {
        val runs = LifePathKind.entries.map { play(it) }
        runs.forEach { run ->
            val ending = requireNotNull(run.ui.ending)
            assertTrue("결말 화면에는 주요 선택이 남아야 한다", ending.choices.isNotEmpty())
            assertTrue("교육 · 직업 블록은 항상 네 줄", ending.educationCareer.size == 4)
            assertTrue("가족 · 생활 블록은 항상 다섯 줄", ending.familyLife.size == 5)
            assertFalse("요약에 이름 자리 표시가 남으면 안 된다", ending.lifeLine.contains("{name"))
        }
        val lifeLines = runs.map { it.ui.ending!!.lifeLine + "|" + it.ui.ending!!.pathLine }
        assertEquals("일곱 경로의 결말 요약이 모두 달라야 한다", 7, lifeLines.toSet().size)
        val pathsMatched = runs.count { dominantPath(it.step.state.flags, it.step.state.stats) == it.target }
        assertEquals("일곱 집중 플레이 모두 그 스탯의 경로로 요약돼야 한다", 7, pathsMatched)
    }
}
