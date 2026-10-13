package com.example.lifegame.ui

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 실제 assets(periods · focuses · events)로 화면 상태를 처음부터 끝까지 만들어 본다.
 * 장 표지 → 자동 진행 → 결정 → 결과 → … → 끝 흐름이 끊기지 않고,
 * 화면에 나가는 문장에 숨은 결과 · 숫자 확률 · 이름 자리 표시가 없는지 확인한다.
 */
class PeriodPlayFlowTest {

    private val assets = File("src/main/assets")
    private val periods = V4ContentParser.parsePeriods(File(assets, "periods.sample.json").readText())
    private val name = "재겸"
    private val nameCall = nameCallOf(name)
    private val focuses = V4ContentParser.parseFocuses(File(assets, "focuses.sample.json").readText())
        .map { it.withPlayerName(name, nameCall) }
    private val decisions = V4ContentParser.parseDecisionEvents(File(assets, "events.sample.json").readText())
        .map { it.withPlayerName(name, nameCall) }
    private val content = PlayContent(
        periods = periods,
        focuses = focuses,
        decisions = decisions.associateBy { it.eventId },
        imageFiles = emptyMap(),
    )
    private val hiddenTexts = decisions.flatMap { event -> event.choices.flatMap { it.outcomes } }
        .filter { it.hidden }
        .map { it.resultText }
        .toSet()

    @Test
    fun `every period starts with a chapter and a full life reaches the end`() {
        for (seed in listOf(20261012L, 7L, 42L)) {
            val visitedChapters = playOneLife(seed, male = seed % 2 == 0L)
            // 사망 없이 끝나면 다섯 시기 모두 장 표지를 지난다.
            assertTrue("seed $seed: 장 표지가 한 번도 나오지 않았다", visitedChapters.isNotEmpty())
        }
    }

    private fun playOneLife(seed: Long, male: Boolean): List<String> {
        val session = PeriodGameSession(
            periods = periods,
            focuses = focuses,
            decisions = decisions,
            runIdFactory = { "run-$seed" },
            runSeedFactory = { seed },
        )
        val first = session.start().state
        var step = session.restore(
            first.copy(flags = first.flags + if (male) setOf(Gender.MALE.flag, MILITARY_SERVICE_FLAG) else setOf(Gender.FEMALE.flag)),
        )
        val chapters = mutableListOf<String>()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard < 500) {
            guard += 1
            val ui = buildPeriodPlayState(step, content, playerName = name)
            assertScreenTextIsClean(ui)
            step = when (ui.phase) {
                PlayPhase.CHAPTER -> {
                    chapters += ui.periodName
                    assertEquals("시기마다 초점은 5개", 5, ui.focusOptions.size)
                    session.selectFocus(ui.focusOptions.first().focusId)
                }
                PlayPhase.AUTO -> {
                    assertNotNull(ui.focus)
                    session.advance()
                }
                PlayPhase.DECISION -> {
                    val decision = requireNotNull(ui.decision)
                    assertTrue(decision.choices.isNotEmpty())
                    val resolution = session.choose(decision.choices.first().choiceId)
                    val after = buildPeriodPlayState(
                        resolution.step,
                        content,
                        playerName = name,
                        outcome = OutcomeUi(
                            choiceLabel = decision.choices.first().label,
                            text = resolution.outcome.resultText,
                            changes = statChanges(step.state.stats, resolution.step.state.stats),
                            endsLife = resolution.outcome.death,
                        ),
                        decision = decision,
                        decisionYear = step.state.yearInPeriod,
                    )
                    // 결과를 보여 주는 동안은 결정 화면에 머문다.
                    assertEquals(PlayPhase.DECISION, after.phase)
                    resolution.step
                }
                PlayPhase.ENDED -> break
            }
        }
        assertEquals("seed $seed: 500걸음 안에 끝나야 한다", TurnPhase.ENDING, step.phase)
        assertEquals(PlayPhase.ENDED, buildPeriodPlayState(step, content, playerName = name).phase)
        return chapters
    }

    private fun assertScreenTextIsClean(ui: PeriodPlayUiState) {
        val decision = ui.decision ?: return
        val visible = listOf(decision.title, decision.text) +
            decision.choices.flatMap { listOf(it.label, it.hint) }
        visible.forEach { text ->
            assertFalse("숫자 확률이 보이면 안 된다: $text", text.contains("%"))
            assertFalse("이름 자리 표시가 남았다: $text", text.contains("{name"))
            assertFalse("숨은 결과가 미리 보였다: $text", text in hiddenTexts)
        }
    }
}
