package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.AutomaticEventParser
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.domain.engine.PeriodSessionFactory
import com.example.lifegame.domain.engine.SessionStartProfile
import com.example.lifegame.domain.model.*
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.screens.HomeSceneImageKey
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLobbyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun screen(fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                LifeGameTheme {
                    CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false, hapticsEnabled = false)) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) { content() }
                    }
                }
            }
        }
    }

    private fun waitForScene(id: String) {
        compose.waitUntil(8_000) {
            compose.onNodeWithTag("home_scene").fetchSemanticsNode().config[HomeSceneImageKey] == id
        }
        // PNG loading happens on IO, outside Compose idling; screenshots include the settled artwork.
        Thread.sleep(600)
        compose.waitForIdle()
    }

    @Test fun homeUsesBrightSystemIconsAndRestoresThePreviousStyleWhenLeaving() {
        val controller = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
        var originalStatus = false
        var originalNavigation = false
        var showHome by mutableStateOf(true)
        compose.runOnUiThread {
            originalStatus = controller.isAppearanceLightStatusBars
            originalNavigation = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
        try {
            screen {
                if (showHome) HomeScreen(null, emptyList(), {}, { showHome = false })
                else Box(Modifier.fillMaxSize().background(Color.White)) { Text("밝은 다음 화면") }
            }
            compose.runOnIdle {
                assertFalse(controller.isAppearanceLightStatusBars)
                assertFalse(controller.isAppearanceLightNavigationBars)
            }
            compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
            compose.onNodeWithText("밝은 다음 화면").assertIsDisplayed()
            compose.runOnIdle {
                assertTrue(controller.isAppearanceLightStatusBars)
                assertTrue(controller.isAppearanceLightNavigationBars)
            }
        } finally {
            compose.runOnUiThread {
                controller.isAppearanceLightStatusBars = originalStatus
                controller.isAppearanceLightNavigationBars = originalNavigation
            }
        }
    }

    @Test fun firstVisitUsesTheAdultPreviewAndDoesNotInventAPastLife() {
        var started = 0
        screen { HomeScreen(null, emptyList(), {}, { started++ }) }
        waitForScene("img_week10_bg_young_adult")
        compose.onNodeWithText("아기부터 노년까지").assertExists()
        compose.onNodeWithTag("home_avatar").assertExists()
        capture("first-360x640.png")
        compose.onNodeWithTag("home_past_lives").performScrollTo().performClick()
        compose.onNodeWithText("아직 끝까지 산 인생이 없다.").assertIsDisplayed()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
        compose.onNodeWithTag("home_overwrite_confirmation").assertDoesNotExist()
        assertEquals(1, started)
    }

    @Test fun currentLifeUsesTheSavedAppearanceAndRequiresOverwriteConfirmation() {
        var continued = 0
        var started = 0
        val card = LifeCardUi("민서", "중년기 · 3번째 해", "건강", Stats(), "가족과 산책했다.",
            AvatarLook(period = LifePeriod.MIDDLE, hairId = "hair_bob_brown"))
        screen { HomeScreen(card, emptyList(), { continued++ }, { started++ }) }
        waitForScene("img_week10_bg_middle")
        compose.onNodeWithText("민서").assertExists()
        compose.onNodeWithText("가족과 산책했다.").assertExists()
        compose.onNodeWithContentDescription("플레이어 모습: ${card.avatar.description}").assertExists()
        compose.onNodeWithTag("home_continue").performScrollTo().performClick()
        assertEquals(1, continued)
        compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
        compose.onNodeWithText("지금 사는 인생은 이어서 할 수 없게 됩니다.").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        assertEquals(0, started)
        compose.onNodeWithTag("home_new_life").performClick()
        compose.onNodeWithTag("home_confirm_new_life").performClick()
        assertEquals(1, started)
        capture("resume-360x640.png")
    }

    @Test fun unreadableSaveStillRequiresExplicitNewLifeConfirmation() {
        var started = 0
        screen { HomeScreen(null, emptyList(), {}, { started++ }, message = "저장을 불러오지 못했습니다.", hasRecoverableSave = true) }
        compose.onNodeWithTag("home_message").assertTextEquals("저장을 불러오지 못했습니다.")
        compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
        assertEquals(0, started)
        compose.onNodeWithText("불러오지 못한 저장 대신 새 인생을 시작합니다. 손상된 원본은 따로 보관됩니다.").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        assertEquals(0, started)
        compose.onNodeWithTag("home_new_life").performClick()
        compose.onNodeWithTag("home_confirm_new_life").performClick()
        assertEquals(1, started)
    }

    @Test fun completedLifeUsesItsActualRecapAndOpensTheMatchingRecord() {
        val record = playedRecord()
        val latest = record.recap.last { it.images.isNotEmpty() }
        var opened: String? = null
        val finished = FinishedLifeUi(record.runId, record.playerName, record.summary.lifeLine, record.title?.text)
        screen { HomeScreen(null, listOf(finished), {}, {}, latestRecord = record, onOpenRecord = { opened = it }) }
        waitForScene(latest.images.first().imageId)
        compose.onNodeWithText(record.title!!.text).assertExists()
        compose.onNodeWithText(latest.title).assertExists()
        capture("completed-360x640.png")
        compose.onNodeWithTag("home_past_lives").performScrollTo().performClick()
        compose.onNodeWithTag("finished_life_0").performClick()
        assertEquals(record.runId, opened)
        compose.onNodeWithTag("home_past_lives_dialog").assertDoesNotExist()
    }

    @Test fun firstPersonRecapDoesNotCompositeAnAvatar() {
        val record = playedRecord()
        val moment = record.recap.first().copy(images = listOf(ImageCandidate(
            "pov_fixture", "week10/scenes/img_week10_bg_young_adult.png", usage = "pov")),
            avatar = AvatarLook(period = LifePeriod.YOUNG_ADULT))
        screen { HomeScreen(null, emptyList(), {}, {}, latestRecord = record.copy(recap = listOf(moment))) }
        waitForScene("pov_fixture")
        compose.onNodeWithTag("home_avatar").assertDoesNotExist()
    }

    @Test fun largeTextKeepsEveryActionReachableOnSmallPhones() {
        var titles = 0
        var started = 0
        screen(fontScale = 2f) { HomeScreen(null, emptyList(), {}, { started++ }, onOpenTitles = { titles++ }) }
        waitForScene("img_week10_bg_young_adult")
        capture("large-text-top-360x640.png")
        listOf("home_new_life", "home_past_lives", "home_titles", "home_settings").forEach { tag ->
            val node = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().fetchSemanticsNode()
            val root = compose.onRoot().fetchSemanticsNode()
            assertTrue("$tag must stay inside the screen", node.boundsInRoot.left >= -1f && node.boundsInRoot.right <= root.size.width + 1f)
        }
        capture("large-text-actions-360x640.png")
        compose.onNodeWithTag("home_titles").performClick()
        assertEquals(1, titles)
        compose.onNodeWithTag("home_settings").performClick()
        compose.onNodeWithTag("experience_settings").assertIsDisplayed()
        capture("settings-large-text-360x640.png", "experience_settings")
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
        assertEquals(1, started)
    }

    private fun playedRecord(): LifeRecordUi {
        val repo = ContentRepository(context)
        val content = repo.loadV4Content()
        val rules = AutomaticEventParser.parse(context.assets.open("auto_events.sample.json").bufferedReader().use { it.readText() })
        val started = PeriodSessionFactory(content.periods, content.focuses, content.decisions, repo.loadEndings(),
            content.lifeFactRules, content.automaticEvents, rules).start(SessionStartProfile("gender_female", null, runSeed = 20261119L), "home-record")
        val session = started.session
        val choices = mutableListOf<RunChoiceRecord>()
        var step = started.step
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 200) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> session.selectFocus(content.focuses.first { it.period == step.state.period }.focusId)
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val choice = pending.choices.first { it.available }
                    val before = step.state
                    val event = content.decisions.first { it.eventId == pending.eventId }
                    val resolved = session.choose(choice.choiceId)
                    choices += RunChoiceRecord(event.eventId, choice.choiceId, resolved.outcome.outcomeId, before.period,
                        before.yearInPeriod, event.title, choice.label, resolved.outcome.resultText, momentImageId = event.momentImageId)
                    resolved.step
                }
                TurnPhase.ENDING -> step
            }
        }
        assertEquals(TurnPhase.ENDING, step.phase)
        val completed = CompletedLifeRecord(runId = "home-record", playerName = "기록 속 민서", summary = "저장된 한 인생",
            endingId = session.ending?.endingId, endingTitle = session.ending?.title, endingSummary = session.ending?.summary,
            awardedTitle = AwardedTitle("saved-title", "저장된 칭호", "완료 기록의 칭호"), finalState = step.state, choices = choices)
        return buildLifeRecordUi(completed, RecordContent(content.periods,
            imageRouting = ImageRoutingRepository(context).load(),
            imageFiles = repo.loadImageAssets().associate { it.imageId to it.fileName },
            decisions = content.decisions.associateBy { it.eventId }))
    }

    private fun capture(name: String, tag: String? = null) {
        val directory = requireNotNull(context.getExternalFilesDir("home-lobby"))
        directory.mkdirs()
        File(directory, name).outputStream().use { stream ->
            val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
            assertTrue(node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
