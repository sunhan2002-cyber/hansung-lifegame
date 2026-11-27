package com.example.lifegame.ui

import androidx.compose.ui.geometry.Offset
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.ui.components.BigResultStyle
import com.example.lifegame.ui.components.ChoicePickStyle
import com.example.lifegame.ui.components.ChoiceRevealStyle
import com.example.lifegame.ui.components.MotionHaptic
import com.example.lifegame.ui.components.StatChangeStyle
import com.example.lifegame.ui.components.statChangeChipAlpha
import com.example.lifegame.ui.components.statChangeChipsTotalMs
import com.example.lifegame.ui.components.statChangeCount
import com.example.lifegame.ui.components.statChangeFloatFrame
import com.example.lifegame.ui.components.statChangeGaugeFillLevel
import com.example.lifegame.ui.components.statChangeGaugeTrailLevel
import com.example.lifegame.ui.components.statChangeLanding
import com.example.lifegame.ui.components.statChangeOdometerFrame
import com.example.lifegame.ui.components.statChangeSpring
import com.example.lifegame.ui.components.statChangeSpringPulse
import com.example.lifegame.ui.screens.ChoicePickEntrance
import com.example.lifegame.ui.screens.PICK_EXIT_MS
import com.example.lifegame.ui.screens.PICK_RESULT_DELAY_MS
import com.example.lifegame.ui.screens.ResultDemoTier
import com.example.lifegame.ui.screens.ResultImpact
import com.example.lifegame.ui.screens.ResultImpactTier
import com.example.lifegame.ui.screens.choiceBadgeScale
import com.example.lifegame.ui.screens.choiceDealProgress
import com.example.lifegame.ui.screens.choiceInkProgress
import com.example.lifegame.ui.screens.choicePickFlash
import com.example.lifegame.ui.screens.choicePickWindow
import com.example.lifegame.ui.screens.choiceRevealFadeAlpha
import com.example.lifegame.ui.screens.choiceRevealTotalMs
import com.example.lifegame.ui.screens.choiceRiseProgress
import com.example.lifegame.ui.screens.resultDemoOutcome
import com.example.lifegame.ui.screens.resultDemoTierLabel
import com.example.lifegame.ui.screens.resultFinalInk
import com.example.lifegame.ui.screens.resultImpactDurationMs
import com.example.lifegame.ui.screens.resultImpactHaptic
import com.example.lifegame.ui.screens.resultImpactOf
import com.example.lifegame.ui.screens.resultImpactOffsetDp
import com.example.lifegame.ui.screens.resultSwell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * play 영역(선택지 등장 · 고른 카드 · 능력치 변화 · 큰 결과)의 순수 계산. 화면 없이 확인한다:
 * 결과 등급 경계, 모든 시간표가 0에서 시작해 정확히 쉬는 자리에서 끝나는지, 스프링 · 숫자 굴림 · 게이지 계산.
 */
class PlayMotionMathTest {
    private fun outcome(
        vararg changes: Pair<String, Int>,
        facts: List<FactChangeUi> = emptyList(),
        opened: List<String> = emptyList(),
        endsLife: Boolean = false,
    ) = OutcomeUi(
        choiceLabel = "고른 길",
        text = "결과",
        changes = changes.map { (name, delta) -> StatChangeUi(name, delta) },
        endsLife = endsLife,
        factChanges = facts,
        openedEvents = opened,
    )

    private fun tierOf(outcome: OutcomeUi, kind: DecisionKind = DecisionKind.MILESTONE) = resultImpactOf(outcome, kind).tier

    // ---- 큰 결과: 등급 ----

    @Test fun ordinaryResultsStayCalm() {
        assertEquals(ResultImpactTier.CALM, tierOf(outcome()))
        assertEquals(ResultImpactTier.CALM, tierOf(outcome("행복" to 3, "사회성" to -2)))
        // 11만큼 움직이고 건강은 4만 잃었다: 아직 평범하다.
        assertEquals(ResultImpactTier.CALM, tierOf(outcome("경제력" to 6, "건강" to -4, "사회성" to 1)))
        // 일반 돌발도 10만큼은 평범하다(큰 돌발만 10부터 크다).
        assertEquals(ResultImpactTier.CALM, tierOf(outcome("지력" to 5, "행복" to 5), DecisionKind.INCIDENT))
    }

    @Test fun notableResultsHaveANewFactAPathOrAVisibleMove() {
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome("행복" to 1, facts = listOf(FactChangeUi("직업", "회사원")))))
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome(opened = listOf("첫 승진 심사"))))
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome("건강" to -5)))
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome("지력" to 6, "행복" to 6)))
        // 연애 중은 새 사실이지만 결혼 · 이혼이 아니다.
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome(facts = listOf(FactChangeUi("관계", "연애 중")))))
    }

    @Test fun bigResultsAreHeavyLossesLifeEventsAndBigIncidents() {
        assertEquals(ResultImpactTier.BIG, tierOf(outcome("건강" to -15)))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome("경제력" to -8, "행복" to -7)))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome("행복" to 5, facts = listOf(FactChangeUi("업적", "첫 개인전")))))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome(facts = listOf(FactChangeUi("가족", "자녀 1명")))))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome(facts = listOf(FactChangeUi("관계", "결혼")))))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome(facts = listOf(FactChangeUi("관계", "이혼")))))
        assertEquals(ResultImpactTier.BIG, tierOf(outcome("지력" to 5, "행복" to 5), DecisionKind.BIG_INCIDENT))
        assertEquals(ResultImpactTier.NOTABLE, tierOf(outcome("건강" to -14)))
    }

    @Test fun theEndOfALifeIsFinalWhateverElseChanged() {
        assertEquals(ResultImpactTier.FINAL, tierOf(outcome("건강" to -30, endsLife = true)))
        assertEquals(ResultImpactTier.FINAL, tierOf(outcome(endsLife = true)))
    }

    @Test fun goodNewsIsNoDivorceNoHeavyHealthLossAndAMilestoneOrANetGain() {
        assertTrue(resultImpactOf(outcome("행복" to 5, facts = listOf(FactChangeUi("업적", "첫 개인전"))), DecisionKind.MILESTONE).good)
        assertFalse(resultImpactOf(outcome("건강" to -25), DecisionKind.MILESTONE).good)
        assertFalse(resultImpactOf(outcome(facts = listOf(FactChangeUi("관계", "이혼"))), DecisionKind.MILESTONE).good)
        assertFalse(resultImpactOf(outcome("경제력" to -8, "행복" to -7), DecisionKind.INCIDENT).good)
        assertTrue(resultImpactOf(outcome("경제력" to 8, "행복" to -7), DecisionKind.INCIDENT).good)
        // 갈림길은 잃은 것이 있어도 길을 정한 일이라 좋은 쪽이다(건강을 15 이상 잃지 않았다면).
        assertTrue(resultImpactOf(outcome("경제력" to -8, "행복" to -7), DecisionKind.MILESTONE).good)
    }

    @Test fun eachDemoOutcomeLandsInItsOwnTier() {
        for (tier in ResultDemoTier.entries) {
            val label = resultDemoTierLabel(resultImpactOf(resultDemoOutcome(tier), DecisionKind.MILESTONE))
            assertEquals("demo $tier", tier.label, label)
        }
    }

    // ---- 큰 결과: 시간표 · 진동 ----

    @Test fun impactsHaveDurationsOnlyWhenTheyDrawSomething() {
        val calm = ResultImpact(ResultImpactTier.CALM, good = true)
        val bigGood = ResultImpact(ResultImpactTier.BIG, good = true)
        val bigBad = ResultImpact(ResultImpactTier.BIG, good = false)
        val final = ResultImpact(ResultImpactTier.FINAL, good = false)
        assertEquals(0, resultImpactDurationMs(BigResultStyle.NONE, bigGood))
        assertEquals(0, resultImpactDurationMs(BigResultStyle.THUD, calm))
        assertEquals(0, resultImpactDurationMs(BigResultStyle.GLOW, calm))
        assertEquals(520, resultImpactDurationMs(BigResultStyle.THUD, bigGood))
        assertEquals(620, resultImpactDurationMs(BigResultStyle.THUD, bigBad))
        assertEquals(900, resultImpactDurationMs(BigResultStyle.GLOW, bigGood))
        assertEquals(1_700, resultImpactDurationMs(BigResultStyle.THUD, final))
        assertEquals(1_700, resultImpactDurationMs(BigResultStyle.GLOW, final))
    }

    @Test fun hapticsGrowWithTheTierAndCalmStaysSilent() {
        assertNull(resultImpactHaptic(ResultImpact(ResultImpactTier.CALM, true)))
        assertEquals(MotionHaptic.CONTEXT, resultImpactHaptic(ResultImpact(ResultImpactTier.NOTABLE, false)))
        assertEquals(MotionHaptic.CONFIRM, resultImpactHaptic(ResultImpact(ResultImpactTier.BIG, true)))
        assertEquals(MotionHaptic.REJECT, resultImpactHaptic(ResultImpact(ResultImpactTier.BIG, false)))
        assertEquals(MotionHaptic.HEAVY, resultImpactHaptic(ResultImpact(ResultImpactTier.FINAL, false)))
    }

    @Test fun contentOffsetsStartWhereTheyShouldAndRestAtZeroBeforeTheEnd() {
        val tiers = listOf(
            ResultImpact(ResultImpactTier.NOTABLE, true), ResultImpact(ResultImpactTier.NOTABLE, false),
            ResultImpact(ResultImpactTier.BIG, true), ResultImpact(ResultImpactTier.BIG, false),
            ResultImpact(ResultImpactTier.FINAL, false),
        )
        for (impact in tiers) {
            for (style in BigResultStyle.entries) {
                val end = resultImpactDurationMs(style, impact).toFloat()
                assertEquals("$style $impact rests by its end", Offset.Zero, resultImpactOffsetDp(style, impact, end))
                assertEquals("$style $impact never moves before it starts", Offset.Zero, resultImpactOffsetDp(style, impact, -10f))
            }
            // GLOW never moves anything.
            for (t in 0..1_000 step 20) assertEquals(Offset.Zero, resultImpactOffsetDp(BigResultStyle.GLOW, impact, t.toFloat()))
        }
        // 좋은 큰 결과는 6dp 아래에서 내려앉고, 좋은 눈에 띔은 3dp 떠올랐다 가라앉는다. 흔들림은 0에서 시작한다.
        assertEquals(6f, resultImpactOffsetDp(BigResultStyle.THUD, ResultImpact(ResultImpactTier.BIG, true), 0f).y, 1e-4f)
        assertEquals(-3f, resultImpactOffsetDp(BigResultStyle.THUD, ResultImpact(ResultImpactTier.NOTABLE, true), 0f).y, 1e-4f)
        assertEquals(0f, resultImpactOffsetDp(BigResultStyle.THUD, ResultImpact(ResultImpactTier.BIG, false), 0f).x, 0f)
        val shake = (1..420).maxOf { abs(resultImpactOffsetDp(BigResultStyle.THUD, ResultImpact(ResultImpactTier.BIG, false), it.toFloat()).x) }
        assertTrue("a bad big result really shakes ($shake dp)", shake in 3f..6f)
        assertEquals(Offset.Zero, resultImpactOffsetDp(BigResultStyle.THUD, ResultImpact(ResultImpactTier.FINAL, false), 300f))
    }

    @Test fun lightSwellsAndTheFinalInkReturnToNothing() {
        assertEquals(0f, resultSwell(0f, 80f, 520f), 0f)
        assertEquals(1f, resultSwell(80f, 80f, 520f), 1e-4f)
        assertEquals(0f, resultSwell(520f, 80f, 520f), 0f)
        assertEquals(0f, resultFinalInk(0f), 0f)
        assertEquals(0.5f, resultFinalInk(600f), 1e-4f)
        assertEquals(0f, resultFinalInk(1_700f), 0f)
        assertTrue((0..1_700).all { resultFinalInk(it.toFloat()) in 0f..0.5f })
    }

    // ---- 스프링 ----

    @Test fun springsStartAtRestSettleAndOnlyUnderdampedOnesOvershoot() {
        assertEquals(0f, statChangeSpring(0f, 0.5f, 520f), 0f)
        assertEquals(0f, statChangeSpring(-16f, 0.5f, 520f), 0f)
        assertEquals(1f, statChangeSpring(1_500f, 0.5f, 520f), 1e-3f)
        val bouncy = (1..600).maxOf { statChangeSpring(it.toFloat(), 0.5f, 520f) }
        assertTrue("an underdamped spring overshoots ($bouncy)", bouncy in 1.1f..1.25f)
        val critical = (1..600).map { statChangeSpring(it.toFloat(), 1f, 520f) }
        assertTrue("a critically damped spring never overshoots", critical.all { it <= 1f + 1e-5f })
        assertTrue("and rises monotonically", critical.zipWithNext().all { (a, b) -> b >= a - 1e-6f })
        assertEquals(0f, statChangeSpringPulse(0f, 0.45f, 800f), 0f)
        val peak = (1..300).maxOf { statChangeSpringPulse(it.toFloat(), 0.45f, 800f) }
        assertEquals("a pulse peaks at 1", 1f, peak, 0.01f)
        assertTrue("and dies out", abs(statChangeSpringPulse(600f, 0.45f, 800f)) < 0.01f)
    }

    // ---- 선택지 등장 ----

    @Test fun revealTimelinesEndWhenTheLastCardHasSettled() {
        assertEquals(0, choiceRevealTotalMs(ChoiceRevealStyle.NONE, 3))
        assertEquals(744, choiceRevealTotalMs(ChoiceRevealStyle.RISE, 3))
        assertEquals(740, choiceRevealTotalMs(ChoiceRevealStyle.DEAL, 3))
        assertEquals(600, choiceRevealTotalMs(ChoiceRevealStyle.INK, 3))
        assertEquals(604, choiceRevealTotalMs(ChoiceRevealStyle.RISE, 1))
        for (count in 1..3) {
            val rise = choiceRevealTotalMs(ChoiceRevealStyle.RISE, count).toFloat()
            val deal = choiceRevealTotalMs(ChoiceRevealStyle.DEAL, count).toFloat()
            val ink = choiceRevealTotalMs(ChoiceRevealStyle.INK, count).toFloat()
            for (index in 0 until count) {
                assertEquals("RISE card $index of $count rests", 1f, choiceRiseProgress(rise, index), 0f)
                assertEquals("RISE badge $index of $count rests", 1f, choiceBadgeScale(rise, index), 0f)
                assertEquals("DEAL card $index of $count rests", 1f, choiceDealProgress(deal, index), 0f)
                assertEquals("INK card $index of $count rests", 1f, choiceInkProgress(ink, index), 0f)
            }
            for (end in listOf(rise, deal, ink)) assertEquals(1f, choiceRevealFadeAlpha(end), 0f)
        }
    }

    @Test fun cardsRevealInSequenceFromNothing() {
        // RISE: 카드는 70ms 간격으로 320ms 동안 떠오르고, 세 장이 대략 460ms 안에 자리 잡는다(300ms 터치 보호 무렵).
        assertEquals(0f, choiceRiseProgress(0f, 0), 0f)
        assertEquals(0f, choiceRiseProgress(69f, 1), 0f)
        assertEquals(1f, choiceRiseProgress(320f, 0), 0f)
        assertEquals(1f, choiceRiseProgress(460f, 2), 0f)
        // 표식은 카드가 70% 올라왔을 때 1.22로 찍히고 스프링으로 돌아온다(그 전에는 1).
        assertEquals(1f, choiceBadgeScale(223f, 0), 0f)
        assertEquals(1.22f, choiceBadgeScale(224f, 0), 1e-4f)
        assertTrue(choiceBadgeScale(260f, 0) < 1.22f)
        // DEAL: 처음에는 0, 살짝 넘어갔다가 정확히 1에서 쉰다.
        assertEquals(0f, choiceDealProgress(0f, 0), 0f)
        assertEquals(0f, choiceDealProgress(170f, 2), 0f)
        assertTrue("a dealt card slides a little past its slot", (1..560).any { choiceDealProgress(it.toFloat(), 0) > 1.02f })
        // INK: 80ms 간격 440ms.
        assertEquals(0f, choiceInkProgress(80f, 1), 0f)
        assertEquals(1f, choiceInkProgress(520f, 1), 0f)
        assertEquals(0f, choiceRevealFadeAlpha(0f), 0f)
    }

    // ---- 고른 카드 ----

    @Test fun theResultPageSettlesWithinTheExitAndTheResultDelayKeepsTestMargins() {
        assertNull(choicePickWindow(ChoicePickStyle.NONE, ChoicePickEntrance.CARD))
        for (style in ChoicePickStyle.entries - ChoicePickStyle.NONE) {
            val text = requireNotNull(choicePickWindow(style, ChoicePickEntrance.TEXT))
            val card = requireNotNull(choicePickWindow(style, ChoicePickEntrance.CARD))
            val button = requireNotNull(choicePickWindow(style, ChoicePickEntrance.BUTTON))
            assertTrue("$style: text, then card, then button", text.startMs <= card.startMs && card.startMs <= button.startMs)
            assertTrue("$style: all settled by 520 ms", listOf(text, card, button).all { it.endMs <= 520 && it.startMs < it.endMs })
            assertFalse("$style: the event text only fades", text.rises)
            assertTrue("$style: the button rises (draw only)", button.rises)
        }
        assertTrue(requireNotNull(choicePickWindow(ChoicePickStyle.LIFT, ChoicePickEntrance.CARD)).rises)
        assertFalse("MORPH's card only fades so the arriving card lands on it", requireNotNull(choicePickWindow(ChoicePickStyle.MORPH, ChoicePickEntrance.CARD)).rises)
        assertEquals(460, PICK_EXIT_MS)
        assertEquals(180, PICK_RESULT_DELAY_MS)
        // ResultRewardOnceTest looks for the chips 300 ms after a live pick.
        assertTrue(PICK_RESULT_DELAY_MS <= 200 && PICK_RESULT_DELAY_MS < PICK_EXIT_MS)
    }

    @Test fun liftsGoldFlashIsQuickAndGone() {
        assertEquals(0f, choicePickFlash(0f), 0f)
        assertEquals(1f, choicePickFlash(70f), 1e-4f)
        assertEquals(0f, choicePickFlash(320f), 1e-4f)
        assertEquals(0f, choicePickFlash(PICK_EXIT_MS.toFloat()), 0f)
    }

    // ---- 능력치 변화 ----

    @Test fun chipTimelinesStaggerAndRest() {
        assertEquals(0, statChangeChipsTotalMs(StatChangeStyle.NONE, 4))
        assertEquals(880, statChangeChipsTotalMs(StatChangeStyle.ROLL, 4))
        assertEquals(1_110, statChangeChipsTotalMs(StatChangeStyle.FLOAT, 4))
        for (style in StatChangeStyle.entries) for (count in 1..6) {
            val end = statChangeChipsTotalMs(style, count).toFloat()
            for (index in 0 until count) {
                assertEquals(1f, statChangeChipAlpha(style, end, index), 0f)
                if (style == StatChangeStyle.ROLL) {
                    assertEquals(1f, statChangeLanding(end, index), 0f)
                    assertEquals(1f, statChangeCount(end, index), 0f)
                }
            }
        }
        assertEquals(0f, statChangeChipAlpha(StatChangeStyle.ROLL, 0f, 0), 0f)
        assertEquals(1f, statChangeChipAlpha(StatChangeStyle.NONE, 0f, 0), 0f)
        assertEquals(0f, statChangeCount(199f, 0), 0f)
        assertEquals(0f, statChangeLanding(59f, 1), 0f)
    }

    @Test fun theOdometerRollsFromZeroToTheChangeAndRestsOnIt() {
        assertEquals(0, statChangeOdometerFrame(25, 0f).value)
        assertEquals(1, statChangeOdometerFrame(25, 0f).next)
        val middle = statChangeOdometerFrame(25, 0.5f)
        assertEquals(12, middle.value)
        assertEquals(13, middle.next)
        assertEquals(0.5f, middle.fraction, 1e-4f)
        val end = statChangeOdometerFrame(25, 1f)
        assertEquals(25, end.value)
        assertEquals(25, end.next)
        assertEquals(0f, end.fraction, 0f)
        val frames = (0..100).map { statChangeOdometerFrame(7, it / 100f) }
        assertTrue("never counts down", frames.zipWithNext().all { (a, b) -> b.value >= a.value })
        assertTrue("never passes the change", frames.all { it.value <= 7 && it.next <= 7 })
    }

    @Test fun floatingNumbersRiseForGainsDropForLossesAndVanish() {
        val gain = requireNotNull(statChangeFloatFrame(5, 0f))
        assertEquals(1.2f, gain.scale, 1e-4f)
        assertEquals(1f, gain.alpha, 0f)
        assertTrue(requireNotNull(statChangeFloatFrame(5, 400f)).dyDp < -10f)
        assertNull(statChangeFloatFrame(5, 780f))
        val loss = requireNotNull(statChangeFloatFrame(-3, 300f))
        assertTrue(loss.dyDp > 0f)
        assertNull(statChangeFloatFrame(-3, 640f))
        assertNull(statChangeFloatFrame(-3, -1f))
        assertNull(statChangeFloatFrame(0, 100f))
    }

    @Test fun theGaugeFillsAfterItsDelayAndALossDrainsBehindTheFill() {
        // 얻을 때: 늦춤 동안은 예전 값, 그 뒤 560ms에 걸쳐 새 값까지.
        assertEquals(40f, statChangeGaugeFillLevel(40, 70, ms = 100f, delayMs = 180f), 0f)
        assertEquals(70f, statChangeGaugeFillLevel(40, 70, ms = 740f, delayMs = 180f), 0f)
        assertTrue(statChangeGaugeFillLevel(40, 70, ms = 400f, delayMs = 180f) in 40f..70f)
        // 잃을 때(강조): 맞은 뒤 220ms 머물렀다가 420ms에 걸쳐 빠진다.
        assertEquals(72f, statChangeGaugeTrailLevel(72, 47, ms = 380f, delayMs = 180f, accents = true), 0f)
        assertEquals(47f, statChangeGaugeTrailLevel(72, 47, ms = 820f, delayMs = 180f, accents = true), 0f)
        // 잔잔한 게이지는 늦춤 뒤 바로 빠진다.
        assertTrue(statChangeGaugeTrailLevel(72, 47, ms = 300f, delayMs = 0f, accents = false) < 72f)
        assertEquals(47f, statChangeGaugeTrailLevel(72, 47, ms = 560f, delayMs = 0f, accents = false), 0f)
        // 방향이 반대인 계산은 끼어들지 않는다.
        assertEquals(47f, statChangeGaugeFillLevel(72, 47, ms = 0f, delayMs = 0f), 0f)
        assertEquals(70f, statChangeGaugeTrailLevel(40, 70, ms = 0f, delayMs = 0f, accents = true), 0f)
    }
}
