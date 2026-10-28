package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.screens.TitleCollectionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 10주차 종료 경험 · 기록 화면. 실제 콘텐츠로 한 인생을 끝까지 돌려 만든 완료 기록으로
 * 주마등 → 미래 모습(Fake 지연 · 취소 → 대체) → 인생기록증 → 다시 보기, 칭호 수집, 홈의 지난 인생, 프로필 외형 · 사진 동의를 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class Week10EndingFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = ContentRepository(context)
    private val content = repository.loadV4Content()
    private val catalog = LifeFactCatalog(
        parseLifeFacts(context.assets.open("life_facts.sample.json").bufferedReader().use { it.readText() }),
    )
    private val recordContent = RecordContent(
        periods = content.periods,
        factCatalog = catalog,
        imageRouting = ImageRoutingRepository(context).load(),
        imageFiles = repository.loadImageAssets().filter { it.fileName.isNotBlank() }.associate { it.imageId to it.fileName },
        decisions = content.decisions.associateBy { it.eventId },
    )

    /** 실제 엔진으로 끝까지 산 인생. 선택 기록은 화면이 저장하는 것과 같은 모양으로 남긴다. */
    private fun playedRecord(seed: Long = 20261028L): CompletedLifeRecord {
        val session = PeriodGameSession(
            content.periods, content.focuses, content.decisions, repository.loadEndings(), content.lifeFactRules,
            engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
            runIdFactory = { "w10-$seed" }, runSeedFactory = { seed },
        )
        val decisions = content.decisions.associateBy { it.eventId }
        val choices = mutableListOf<RunChoiceRecord>()
        var step = session.start()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 800) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> session.selectFocus(content.focuses.first { it.period == step.state.period }.focusId)
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val pick = pending.choices.first { it.available }
                    val before = step.state
                    val resolution = session.choose(pick.choiceId)
                    choices += RunChoiceRecord(
                        eventId = pending.eventId,
                        choiceId = pick.choiceId,
                        outcomeId = resolution.outcome.outcomeId,
                        period = before.period,
                        yearInPeriod = before.yearInPeriod,
                        eventTitle = pending.title,
                        choiceLabel = pick.label,
                        resultText = resolution.outcome.resultText,
                        momentImageId = decisions[pending.eventId]?.momentImageId,
                    )
                    resolution.step
                }
                TurnPhase.ENDING -> step
            }
        }
        assertEquals(TurnPhase.ENDING, step.phase)
        return CompletedLifeRecord(
            runId = step.state.runId,
            playerName = "재겸",
            summary = "요약",
            endingId = null,
            endingTitle = "질문 하나로 평생을 산 사람",
            endingSummary = "끝까지 궁금해했다.",
            awardedTitle = AwardedTitle("title_test", "끝까지 궁금했던 사람", "질문을 멈추지 않았다"),
            finalState = step.state,
            choices = choices,
            avatar = AvatarConfig(skinToneId = "skin_deep", hairId = "hair_curly_brown", eyesId = "eyes_narrow"),
        )
    }

    @Test
    fun endingFlowGoesRecapThenFutureThenCertificateAndCanReplay() {
        val record = buildLifeRecordUi(playedRecord(), recordContent)
        assertTrue("주마등에 실제 장면이 있어야 한다", record.recap.isNotEmpty())
        var look by mutableStateOf<GenerationUi?>(null)
        var requested = 0
        var cancelled = 0
        var done = 0
        setScreen {
            EndingFlowScreen(
                record = record,
                futureLook = look,
                onRequestFuture = {
                    requested += 1
                    if (look == null) look = GenerationUi(record.runId, GenerationStatus.LOADING)
                },
                onCancelFuture = {
                    cancelled += 1
                    look = GenerationUi(record.runId, GenerationStatus.FALLBACK, "취소해서 기본 그림으로 보여 줍니다.")
                },
                onDone = { done += 1 },
            )
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("recap_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("recap_counter").assertTextContains("1 / ${record.recap.size}")
        composeRule.onNodeWithText(record.recap.first().title).assertExists()
        saveScreenshot("w10_recap.png")
        composeRule.mainClock.autoAdvance = true

        composeRule.onNodeWithTag("recap_skip").performClick()
        composeRule.onNodeWithTag("future_screen").assertIsDisplayed()
        composeRule.waitForIdle()
        assertTrue(requested >= 1)
        composeRule.onNodeWithTag("future_loading").assertIsDisplayed()
        saveScreenshot("w10_future_loading.png")
        composeRule.onNodeWithTag("future_cancel").performClick()
        assertEquals(1, cancelled)
        composeRule.onNodeWithTag("future_message").assertTextContains("기본 그림", substring = true)
        composeRule.onNodeWithTag("player_avatar").assertExists()
        saveScreenshot("w10_future_fallback.png")

        composeRule.onNodeWithTag("future_next").performScrollTo().performClick()
        composeRule.onNodeWithTag("certificate").assertIsDisplayed()
        composeRule.onNodeWithTag("certificate_name").assertTextContains("재겸")
        composeRule.onNodeWithTag("certificate_title").assertTextContains("끝까지 궁금했던 사람", substring = true)
        assertTextFits("칭호 · 끝까지 궁금했던 사람")
        saveScreenshot("w10_certificate_top.png")
        listOf("교육 · 직업", "가족 · 관계 · 주거 · 경제력", "주요 업적", "주요 선택").forEach { section ->
            composeRule.onNodeWithTag("certificate").performScrollToNode(hasText(section))
            composeRule.onNodeWithText(section).assertIsDisplayed()
        }
        // 미기록 사실은 지어내지 않고 "기록 없음"으로 남는다.
        val unknownLines = record.summary.educationCareer + record.summary.familyLife
        unknownLines.filterNot { it.known }.forEach { line -> assertTrue(line.value, line.value.contains("없")) }
        saveScreenshot("w10_certificate_facts.png")

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("certificate_replay").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("recap_screen").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("recap_skip").performClick()
        composeRule.onNodeWithTag("future_next").performScrollTo().performClick()
        composeRule.onNodeWithTag("certificate_done").performClick()
        assertEquals(1, done)
    }

    @Test
    fun reopenedRecordStartsAtTheCertificateWithAListButton() {
        val record = buildLifeRecordUi(playedRecord(seed = 7L), recordContent)
        var back = 0
        setScreen {
            EndingFlowScreen(
                record = record,
                futureLook = GenerationUi(record.runId, GenerationStatus.READY, "생성 서버 연결 전이라 예시 응답을 받아, 기기 안 그림으로 보여 줍니다."),
                onRequestFuture = {},
                onCancelFuture = {},
                onDone = { back += 1 },
                doneLabel = "목록으로",
                initialStep = EndingStep.CERTIFICATE,
            )
        }
        composeRule.onNodeWithTag("certificate").assertIsDisplayed()
        composeRule.onNodeWithText("목록으로").performClick()
        assertEquals(1, back)
    }

    @Test
    fun titleCollectionCountsPersistedTitles() {
        val titles = listOf(
            LifeTitle("title_a", "끝까지 궁금했던 사람", "질문을 멈추지 않았다", priority = 50),
            LifeTitle("title_b", "운동장의 주인", "운동장을 떠나지 않았다", priority = 40),
            LifeTitle("title_default", "평범하게 빛난 사람", "기본 칭호", priority = 0, isDefault = true),
        )
        val record = playedRecord().copy(awardedTitle = AwardedTitle("title_a", "끝까지 궁금했던 사람", "질문을 멈추지 않았다"))
        setScreen { TitleCollectionScreen(collection = buildTitleCollection(titles, listOf(record)), onBack = {}) }
        composeRule.onNodeWithTag("title_count").assertTextContains("1 / 3 받음")
        composeRule.onNodeWithTag("title_entry_title_a").assertTextContains("받은 인생: 재겸", substring = true)
        composeRule.onNodeWithTag("title_entry_title_b").assertTextContains("아직 받지 못한 칭호", substring = true)
        saveScreenshot("w10_titles.png")
    }

    @Test
    fun homeOpensPastLivesAndTitles() {
        val record = playedRecord()
        var opened: String? = null
        var titles = 0
        setScreen {
            HomeScreen(
                lifeCard = null,
                finishedLives = listOf(record.toFinishedLifeUi(content.periods)),
                onContinue = {},
                onNewLife = {},
                onOpenRecord = { opened = it },
                onOpenTitles = { titles += 1 },
            )
        }
        composeRule.onNodeWithTag("finished_life_0").performScrollTo().performClick()
        assertEquals(record.runId, opened)
        composeRule.onNodeWithTag("home_titles").performScrollTo().performClick()
        assertEquals(1, titles)
        saveScreenshot("w10_home_records.png")
    }

    @Test
    fun profileStartsWithoutPhotoAndDecliningConsentKeepsTheDefaultLook() {
        var started: NewLifeProfile? = null
        setScreen {
            CharacterSelectScreen(
                starters = listOf(StarterUi("s1", "골똘한 아기", "뭐든 뜯어보는 아기", null, com.example.lifegame.domain.model.StatDelta(), emptySet())),
                onStart = { started = it },
            )
        }
        composeRule.onNodeWithTag("option_hair_curly_brown").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_skin_deep").performScrollTo().performClick()
        // 미리보기 세 시기 모두 고른 외형으로 다시 그려진다.
        composeRule.onAllNodes(hasContentDescription("짙은 피부, 곱슬머리", substring = true)).assertCountEquals(3)

        composeRule.onNodeWithTag("photo_pick").performScrollTo().performClick()
        composeRule.onNodeWithTag("photo_consent_no").performClick()
        composeRule.onNodeWithTag("photo_message").assertTextContains("동의하지 않아 기본 모습으로 진행합니다.")
        saveScreenshot("w10_profile_consent_declined.png")

        composeRule.onNodeWithText("남").performScrollTo().performClick()
        composeRule.onNodeWithText("골똘한 아기").performScrollTo().performClick()
        composeRule.onNodeWithText("이 아기로 시작하기").performClick()
        val profile = requireNotNull(started)
        assertEquals("hair_curly_brown", profile.avatar.hairId)
        assertEquals("skin_deep", profile.avatar.skinToneId)
        assertNull(profile.avatar.portraitRef)
        assertFalse(profile.avatar.photoConsent)
    }

    // ---- helpers ----

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 1.3f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Expected a text layout for: $text", layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse("Text height must fit: $text", layout.didOverflowHeight)
            repeat(layout.lineCount) { line -> assertFalse("Text must not be ellipsized: $text", layout.isLineEllipsized(line)) }
        }
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(context.getExternalFilesDir("week10-frontend"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
