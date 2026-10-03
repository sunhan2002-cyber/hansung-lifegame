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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.screens.ChoiceResultScreen
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
            GameScreen(event, Stats(), received::add, {})
        }

        event.choices.forEachIndexed { index, choice ->
            val label = "${'A' + index}. ${choice.label}"
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
            GameScreen(currentEvent.value, Stats(), {}, {})
        }
        scrollToText("game_content", "C. ${currentEvent.value.choices.last().label}")

        composeRule.runOnIdle {
            currentEvent.value = event(id = "next-event", title = "다음 사건의 시작")
        }

        // No test-driven scroll after the event changes: the screen must reset itself.
        composeRule.onNodeWithText("다음 사건의 시작").assertIsDisplayed()
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
            GameScreen(event(), stats, {}, {})
        }

        expected.forEach { (name, value) ->
            composeRule.onNodeWithTag("game_content")
                .performScrollToNode(hasContentDescription(name))
            composeRule.onNodeWithContentDescription(name)
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
            GameScreen(event, Stats(), {}, {})
        }

        scrollToText("game_content", "돌발 이벤트")
        assertTextFits("돌발 이벤트")
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
            ChoiceResultScreen(result, {})
        }

        scrollToText("result_content", "건강")
        scrollToText("result_content", "+2")
        composeRule.onNode(hasText("건강") and hasText("+2")).assertIsDisplayed()
        composeRule.onNodeWithText("+10").assertDoesNotExist()
        scrollToText("result_content", "경제력")
        scrollToText("result_content", "-3")
        composeRule.onNode(hasText("경제력") and hasText("-3")).assertIsDisplayed()
        composeRule.onNodeWithText("행복").assertDoesNotExist()
        composeRule.onNodeWithText("변화 없음").assertDoesNotExist()
    }

    @Test
    fun resultWithNoAppliedChangesSaysThereIsNoChange() {
        setSmallScreen {
            ChoiceResultScreen(result(delta = StatDelta()), {})
        }

        scrollToText("result_content", "변화 없음")
        assertTextFits("변화 없음")
        composeRule.onNodeWithText("+10").assertDoesNotExist()
        composeRule.onNodeWithText("건강").assertDoesNotExist()
    }

    @Test
    fun longResultAndNextButtonRemainReachableWithLargeText() {
        val result = result(text = longBody)
        var nextCalls = 0
        setSmallScreen {
            ChoiceResultScreen(result, { nextCalls++ })
        }

        scrollToText("result_content", result.engineResult.resultText)
        assertTextFits(result.engineResult.resultText, multiline = true)
        scrollToText("result_content", "다음 사건")
        assertTextFits("다음 사건")
        composeRule.onNodeWithText("다음 사건").performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, nextCalls) }
    }

    @Test
    fun gameDoesNotExposeTheTemporaryEndingShortcut() {
        setSmallScreen {
            GameScreen(event().copy(text = "짧은 사건 본문"), Stats(), {}, {})
        }

        // Traverse the entire lazy list; absence in just the initial viewport is insufficient.
        val failure = runCatching {
            composeRule.onNodeWithTag("game_content")
                .performScrollToNode(hasText("결말 화면 확인 (임시)"))
        }.exceptionOrNull()
        assertTrue(
            "The temporary ending action must be absent from the whole scrollable screen",
            failure is AssertionError && failure.message.orEmpty().contains("No node found"),
        )
    }

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
