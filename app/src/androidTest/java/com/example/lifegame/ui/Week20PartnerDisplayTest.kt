package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-20 과제(김재겸 프론트): 연애 사건과 인생 기록에서 같은 상대와 실제 결혼식 결과가 보인다(글꼴 100% · 200%, 360dp 폭).
 * 상대 · 결혼식을 고르는 규칙은 JVM `Week20FrontendTest`가 실제 콘텐츠로 확인한다. 여기서는 화면에 그대로 그려지는지 본다.
 */
@RunWith(AndroidJUnit4::class)
class Week20PartnerDisplayTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val periods = V4ContentParser.parsePeriods(context.assets.open("periods.sample.json").bufferedReader().use { it.readText() })
    private val events = V4ContentParser.parseDecisionEvents(context.assets.open("events.sample.json").bufferedReader().use { it.readText() })
        .associateBy { it.eventId }

    private val partner = PartnerUi("partner_music", "adult_075", "모닥불 옆자리", "청년기", 1, "교제 중")

    private fun setScreen(fontScale: Float, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = fontScale)) {
                LifeGameTheme { Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { content() } }
            }
        }
    }

    private fun loveDecision(): PeriodPlayUiState {
        val event = events.getValue("love_date_01")
        val decision = DecisionUi(
            eventId = event.eventId, kind = event.kind, title = event.title,
            text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
            imageId = "", imageFile = null,
            choices = event.choices.mapIndexed { index, option ->
                DecisionChoiceUi(option.choiceId, ('A' + index).toString(), option.label, hint = "")
            },
            partner = partner,
        )
        return PeriodPlayUiState(
            runKey = "partner", playerName = "재겸", phase = PlayPhase.DECISION, periodName = "청년기", chapterLabel = "제4장",
            periodYears = 5, periodIntro = "", yearInPeriod = 2,
            periodMarks = periods.sortedBy { it.order }.map { PeriodMarkUi(it.shortName(), 0f, it.displayName == "청년기") },
            previousPeriodName = "청소년기", stats = Stats(health = 72), focus = null, focusOptions = emptyList(),
            sceneImageId = "", sceneFile = null, log = emptyList(), yearKey = "partner", decision = decision, outcome = null,
            isDead = false, autoPlaying = false, speed = 1,
        )
    }

    @Test
    fun aLoveEventShowsThePartnerItStartedWith() {
        compose.mainClock.autoAdvance = true
        setScreen(2f) { DecisionScreen(loveDecision(), onChoose = {}, onContinue = {}, skipImagePreview = true) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTagExists("decision_partner") }
        compose.onNodeWithTag("decision_partner", useUnmergedTree = true)
            .assertTextContains("함께하는 사람 · 「모닥불 옆자리」에서 만난 사람 · 교제 중")
        save("w20_love_event_partner_200.png")
    }

    @Test
    fun theRecordContinuesWithTheSamePartnerAndTheRealWedding() {
        val story = PartnerStoryUi(
            partner = partner.copy(status = "결혼"),
            moments = listOf(
                PartnerMomentUi("love_date_01", "세 번째 데이트", "고른 것", "청년기", 2),
                PartnerMomentUi("love_family_meet_01", "양가 인사", "같이 준비한다", "청년기", 4),
            ),
            wedding = PartnerMomentUi("love_wedding_music", "연주로 채운 축가", "직접 노래한다", "청년기", 5),
            status = "결혼",
        )
        val record = LifeRecordUi(
            runId = "run-partner", playerName = "재겸", title = null,
            summary = EndingSummaryUi(
                headline = "평범하게 빛난 사람", endingSummary = "요약", pathLine = "사람과 함께한 삶", lifeLine = "재겸",
                death = null, endedAt = "노년기 5번째 해", educationCareer = emptyList(), familyLife = emptyList(),
                achievements = emptyList(), topStats = emptyList(), choices = emptyList(),
            ),
            avatar = AvatarLook(period = LifePeriod.OLD_AGE), finalPeriodName = "노년기", recap = emptyList(),
            backdrop = emptyList(), pathTitle = "사람과 함께한 삶", usesPhotoFace = false, partnerStory = story,
        )
        compose.mainClock.autoAdvance = true
        setScreen(1f) {
            EndingFlowScreen(record = record, futureLook = null, onRequestFuture = {}, onCancelFuture = {}, onDone = {}, initialStep = EndingStep.CERTIFICATE)
        }
        compose.onNodeWithTag("certificate_partner").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("certificate_partner_name", useUnmergedTree = true)
            .assertTextContains("「모닥불 옆자리」에서 만난 사람(청년기 1번째 해) · 결혼")
        compose.onNodeWithTag("certificate_partner_moment_love_family_meet_01", useUnmergedTree = true)
            .assertTextContains("「양가 인사」", substring = true)
        compose.onNodeWithTag("certificate_wedding", useUnmergedTree = true).performScrollTo()
            .assertTextContains("결혼식 · 청년기 5번째 해 「연주로 채운 축가」 · 직접 노래한다")
        save("w20_certificate_partner.png")
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagExists(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun save(fileName: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(context.getExternalFilesDir("w20-captures"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
