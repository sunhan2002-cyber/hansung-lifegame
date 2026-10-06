package com.example.lifegame.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.screens.GameScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression coverage for a constrained viewport, independent of the emulator window size. */
@RunWith(AndroidJUnit4::class)
class Week6UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun threeLongChoicesCanBeScrolledToAndReturnTheirOriginalObjects() {
        val event = event()
        val received = mutableListOf<Choice>()
        setSmallScreen {
            GameScreen(state(event), received::add, {})
        }

        event.choices.forEachIndexed { index, choice ->
            val label = choice.label
            scrollToText("game_content", label)
            assertTextFits(label, multiline = true)
            composeRule.onNodeWithText(label).performTouchInput { click() }
            composeRule.runOnIdle {
                assertEquals(index + 1, received.size)
                assertSame(choice, received.last())
            }
        }
    }

    @Test
    fun changingTheEventResetsTheScrollToItsHeading() {
        val currentEvent = mutableStateOf(event())
        setSmallScreen {
            GameScreen(state(currentEvent.value), {}, {})
        }
        scrollToText("game_content", currentEvent.value.choices.last().label)

        composeRule.runOnIdle {
            currentEvent.value = event(id = "next-event", title = "다음 사건의 시작")
        }

        // No test-driven scroll after the event changes: the screen must reset itself.
        composeRule.waitForIdle()
        val scroll = composeRule.onNodeWithTag("game_content").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals(0f, scroll.value(), 0.1f)
        composeRule.onNodeWithText("청소년기").assertIsDisplayed()
    }

    @Test
    fun allSevenStatsRemainReachableAndExposeClampedValues() {
        val stats = Stats(
            health = -10,
            fitness = 21,
            intelligence = 32,
            social = 43,
            wealth = 54,
            happiness = 130,
            luck = 76,
        )
        val expected = linkedMapOf(
            "건강" to 0, "운동능력" to 21, "지력" to 32, "사회성" to 43,
            "경제력" to 54, "행복" to 100, "운" to 76,
        )
        setSmallScreen {
            GameScreen(state(event(), stats), {}, {})
        }

        composeRule.onNodeWithTag("stats_button").performClick()
        expected.forEach { (name, value) ->
            composeRule.onNodeWithContentDescription(name)
                .performScrollTo()
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription, "$value / 100",
                ))
                .assert(SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(value.toFloat(), 0f..100f),
                ))
        }
    }

    @Test
    fun specialEventKeepsLongHeadingBodyAndMissingImageMessageReadable() {
        val event = event(
            title = "갑자기 찾아온 예상 밖의 사건에서도 긴 제목과 설명을 끝까지 읽고 선택할 수 있어야 합니다",
        ).copy(type = EventType.SPECIAL, imageId = "")
        setSmallScreen {
            GameScreen(state(event), {}, {})
        }

        // 첫 사건이 돌발일 때도 팝업이 나타나고 본문으로 진입할 수 있어야 한다.
        composeRule.onNodeWithTag("special_popup").assertIsDisplayed()
        composeRule.onNodeWithText("사건 보기").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("special_popup").assertDoesNotExist()
        scrollToText("game_content", "돌발 사건")
        assertTextFits("돌발 사건")
        scrollToText("game_content", event.title)
        assertTextFits(event.title, multiline = true)
        scrollToText("game_content", "이미지를 준비 중입니다")
        assertTextFits("이미지를 준비 중입니다")
        scrollToText("game_content", event.text)
        assertTextFits(event.text, multiline = true)
    }

    @Test
    fun resultUsesAppliedDeltaInsteadOfNominalChoiceDeltaAndOmitsZeroStats() {
        val result = result(delta = StatDelta(health = 2, wealth = -3))
        setSmallScreen {
            GameScreen(state(event(), result = result), {}, {})
        }

        scrollToText("game_content", "건강 +2")
        composeRule.onNodeWithText("건강 +2").assertIsDisplayed()
        composeRule.onNodeWithText("건강 +10").assertDoesNotExist()
        scrollToText("game_content", "경제력 -3")
        composeRule.onNodeWithText("경제력 -3").assertIsDisplayed()
        composeRule.onNodeWithText("행복 +0").assertDoesNotExist()
        composeRule.onNodeWithText("변화 없음").assertDoesNotExist()
    }

    @Test
    fun resultWithNoAppliedChangesSaysThereIsNoChange() {
        setSmallScreen {
            GameScreen(state(event(), result = result(delta = StatDelta())), {}, {})
        }

        scrollToText("game_content", "변화 없음")
        assertTextFits("변화 없음")
        composeRule.onNodeWithText("건강 +10").assertDoesNotExist()
        composeRule.onNodeWithText("건강").assertDoesNotExist()
    }

    @Test
    fun longResultAndNextButtonRemainReachableWithLargeText() {
        val result = result(text = "선택 결과의 긴 이야기:\n" + longBody)
        var nextCalls = 0
        setSmallScreen {
            GameScreen(state(event(), result = result), {}, { nextCalls++ })
        }

        scrollToText("game_content", result.engineResult.resultText)
        assertTextFits(result.engineResult.resultText, multiline = true)
        scrollToText("game_content", "다음 해로 · 14세")
        assertTextFits("다음 해로 · 14세")
        composeRule.onNodeWithText("다음 해로 · 14세").performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, nextCalls) }
    }

    @Test
    fun gameDoesNotExposeTheTemporaryEndingShortcut() {
        setSmallScreen {
            GameScreen(state(event().copy(text = "짧은 사건 본문")), {}, {})
        }

        composeRule.onNodeWithTag("stats_button").performClick()
        composeRule.onNodeWithText("결말 화면 확인 (임시)").assertDoesNotExist()
    }

    @Test
    fun specialPopupCanBeReadAndDismissedWithLargeText() {
        val current = mutableStateOf(state(event().copy(text = "짧은 본문")))
        setSmallScreen { GameScreen(current.value, {}, {}) }
        val special = event(id = "special-next", title = "예상하지 못했던 긴 제목의 돌발 사건이 찾아왔습니다")
            .copy(type = EventType.SPECIAL, text = "길고 복잡한 돌발 사건의 첫 문장도 끝까지 읽어야 합니다. ".repeat(4))
        composeRule.runOnIdle { current.value = state(special) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("special_popup").assertIsDisplayed()
        composeRule.onNodeWithText("사건 보기").performScrollTo().assertIsDisplayed()
        assertTextFits("사건 보기")
        composeRule.onNodeWithText("사건 보기").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("special_popup").assertDoesNotExist()
        scrollToText("game_content", special.choices.last().label)
        assertTextFits(special.choices.last().label, multiline = true)
    }

    @Test
    fun missingMappedImageStillShowsReadableFallback() {
        val event = event().copy(text = "짧은 본문")
        setSmallScreen {
            GameScreen(state(event).copy(imageFiles = mapOf(event.imageId to "missing-image.png")), {}, {})
        }
        scrollToText("game_content", "이미지를 준비 중입니다")
        assertTextFits("이미지를 준비 중입니다")
    }

    @Test
    fun historyAndReturnToNowRemainReachableWithLargeText() {
        val event = event().copy(text = "짧은 본문")
        val past = event.choices.first()
        val history = ChoiceHistory(
            eventOccurrenceId = "past-occurrence", eventId = event.eventId,
            choiceId = past.choiceId, beforeStats = Stats(), afterStats = Stats(health = 52),
            resultText = "지난 사건의 결과",
        )
        val state = state(event)
        setSmallScreen {
            GameScreen(state.copy(progress = state.progress!!.copy(choiceHistory = listOf(history))), {}, {})
        }
        scrollToText("game_content", "지난 기록 1개 · 아래로 당겨서 보기")
        composeRule.onNodeWithText("지난 기록 1개 · 아래로 당겨서 보기").performClick()
        composeRule.onNodeWithText("기록 보는 중").assertIsDisplayed()
        scrollToText("history_content", "→ ${past.label}")
        assertTextFits("→ ${past.label}", multiline = true)
        scrollToText("history_content", "지금으로 ↑")
        composeRule.onNodeWithText("지금으로 ↑").performClick()
        composeRule.onNodeWithTag("game_content").assertIsDisplayed()
        composeRule.onNodeWithTag("history_content").assertDoesNotExist()
    }

    private fun state(event: GameEvent, stats: Stats = Stats(), result: UiChoiceResult? = null) =
        LifeGameUiState(
            currentEvent = event,
            progress = GameProgress(
                runId = "test-run",
                currentEventOccurrenceId = event.eventId,
                stage = event.stage,
                stageEventCount = if (result == null) 0 else 1,
                stats = stats,
            ),
            lastResult = result,
            eventsById = mapOf(event.eventId to event),
        )

    private fun setSmallScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 2f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(320.dp, 400.dp).clipToBounds()) {
                        content()
                    }
                }
            }
        }
    }

    private fun scrollToText(container: String, text: String) {
        composeRule.onNodeWithTag(container).performScrollToNode(hasText(text))
        // A tall event card can exceed the viewport; reveal the target inside that item too.
        composeRule.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun assertTextFits(text: String, multiline: Boolean = false) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(layouts)
            }
        assertTrue("Expected a text layout for: $text", layouts.isNotEmpty())
        layouts.forEach { layout ->
            // Compose 1.7 String Text reconstructs semantics with the parent's width.
            // Compare actual line widths, not MultiParagraph's wider reconstruction box.
            assertFalse("Text height must fit: $text", layout.didOverflowHeight)
            repeat(layout.lineCount) { line ->
                assertFalse("Text must not be ellipsized: $text", layout.isLineEllipsized(line))
                val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
                assertTrue("Text line must fit: $text", lineWidth <= layout.size.width + 1f)
            }
            assertEquals("All characters must be laid out: $text", text.length,
                layout.getLineEnd(layout.lineCount - 1, visibleEnd = false))
            if (multiline) assertTrue("Long text should wrap", layout.lineCount > 1)
        }
    }

    private fun event(id: String = "first-event", title: String = "오늘의 사건") = GameEvent(
        eventId = id,
        stage = LifeStage.TEEN,
        title = title,
        text = longBody,
        imageId = "test_image",
        choices = List(3) { index ->
            Choice(
                choiceId = "route-${index + 1}",
                label = "${index + 1}번 행동으로 친구의 이야기를 끝까지 듣고 함께 해결할 방법을 찾아봅니다. 시간이 오래 걸리더라도 서로의 생각을 충분히 나누고 신중하게 결정합니다.",
                statDelta = StatDelta(health = 10 + index),
                resultText = "${index + 1}번 행동의 결과",
                addFlags = setOf("flag-${index + 1}"),
            )
        },
    )

    private fun result(delta: StatDelta = StatDelta(health = 2), text: String = "선택을 반영했습니다") =
        UiChoiceResult(
            eventId = "first-event",
            choice = Choice(
                choiceId = "route-1",
                label = "친구와 함께 문제를 해결하기 위해 서로의 생각을 듣고 마지막까지 신중하게 결정합니다",
                statDelta = StatDelta(health = 10),
                resultText = "명목상 선택 결과",
            ),
            engineResult = ChoiceResult(
                progress = GameProgress(runId = "test-run", currentEventOccurrenceId = "occurrence-1"),
                beforeStats = Stats(health = 98),
                afterStats = Stats(health = 98 + delta.health, wealth = 50 + delta.wealth),
                appliedDelta = delta,
                resultText = text,
                wasApplied = true,
            ),
        )

    private val longBody = (1..5).joinToString("\n\n") { paragraph ->
        "$paragraph 번째 문단입니다. 예상하지 못한 일이 생겨 주변 사람들의 이야기를 듣고 상황을 천천히 살펴봅니다. " +
            "긴 설명에서도 마지막 문장까지 읽고 나에게 맞는 행동을 선택할 수 있어야 합니다."
    }
}
