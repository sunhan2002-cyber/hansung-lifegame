package com.example.lifegame.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.OpenAiFaceSheetClient
import com.example.lifegame.domain.generation.FaceSheetRequest
import com.example.lifegame.domain.generation.GenerationResult
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.avatar.avatarLookOf
import com.example.lifegame.ui.screens.AutoProgressScreen
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-06 통합지시서 김재겸 과제: 사진 실패 경로 · 복원, 작은 화면(360×640dp, 글꼴 130%)에서
 * 사진 얼굴 · 나이별 외형 · 최신순 기록 구분 · AI 동의 문구 캡처.
 */
@RunWith(AndroidJUnit4::class)
class IterationScreensTest {
    @get:Rule val compose = createComposeRule()

    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun sheetFile(name: String): File {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("test_face_sheet.png").use { it.readBytes() }
        return File(target.filesDir, "face_sheets/$name").apply {
            parentFile?.mkdirs()
            writeBytes(bytes)
        }
    }

    private fun setSmall(content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                LifeGameTheme { Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) { content() } }
            }
        }
    }

    /** 대화상자가 떠 있으면 맨 위 창(대화상자)을 찍는다. */
    private fun save(name: String) {
        compose.waitForIdle()
        val dir = requireNotNull(target.getExternalFilesDir("iter-20261106")).apply { mkdirs() }
        File(dir, name).outputStream().use {
            compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun brokenPhotoOrNoConsentFailsBeforeAnyUpload() {
        // 키 문자열은 쓰이지 않는다: 두 경우 모두 사진을 읽기 전 · 읽는 단계에서 끝나 네트워크로 나가지 않는다.
        val client = OpenAiFaceSheetClient(target, apiKey = "unused-in-this-test", model = "gpt-image-2.5-flare")
        val notAnImage = File(target.cacheDir, "not_an_image.png").apply { writeText("this is not a picture") }
        val broken = client.createFaceSheet(FaceSheetRequest("t1", Uri.fromFile(notAnImage).toString(), consent = true))
        assertTrue(broken is GenerationResult.Failure)
        assertEquals("사진을 읽지 못했습니다.", (broken as GenerationResult.Failure).reason)
        val missing = client.createFaceSheet(FaceSheetRequest("t2", Uri.fromFile(File(target.cacheDir, "nope.jpg")).toString(), consent = true))
        assertTrue(missing is GenerationResult.Failure)
        val noConsent = client.createFaceSheet(FaceSheetRequest("t3", Uri.fromFile(notAnImage).toString(), consent = false))
        assertTrue(noConsent is GenerationResult.Failure)
    }

    @Test
    fun restoredLifeWhoseFaceFileIsGoneUsesTheDefaultLook() {
        val kept = sheetFile("restore_kept.png")
        val gone = sheetFile("restore_gone.png").also { it.delete() }
        val keptLook = avatarLookOf(AvatarConfig(faceSheetRef = Uri.fromFile(kept).toString(), photoConsent = true), LifePeriod.MIDDLE, Stats())
        val goneLook = avatarLookOf(AvatarConfig(faceSheetRef = Uri.fromFile(gone).toString(), photoConsent = true), LifePeriod.MIDDLE, Stats())
        val noConsent = avatarLookOf(AvatarConfig(faceSheetRef = Uri.fromFile(kept).toString(), photoConsent = false), LifePeriod.MIDDLE, Stats())
        assertNotNull(keptLook.photoFaceSheet)
        assertNull(goneLook.photoFaceSheet)
        assertNull(noConsent.photoFaceSheet)
        compose.setContent { PlayerAvatar(goneLook, Modifier.requiredSize(80.dp, 130.dp)) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("player_avatar_layers", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, compose.onAllNodesWithTag("player_avatar_photo", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun autoProgressOnASmallScreenShowsThePhotoFaceAndNewestStoryFirst() {
        val sheet = sheetFile("small_auto.png")
        val periods = listOf("영아", "유년", "청소년", "청년", "중년", "노년")
        val state = PeriodPlayUiState(
            runKey = "small",
            playerName = "재겸",
            phase = PlayPhase.AUTO,
            periodName = "노년기",
            chapterLabel = "제6장",
            periodYears = 5,
            periodIntro = "",
            yearInPeriod = 2,
            periodMarks = periods.mapIndexed { i, p -> PeriodMarkUi(p, if (i < 5) 1f else 0.4f, i == 5) },
            previousPeriodName = "중년기",
            stats = Stats(health = 48),
            focus = null,
            focusOptions = emptyList(),
            sceneImageId = "",
            sceneFile = null,
            log = listOf(
                LogLineUi("story-1", "노년기 · 1번째 해", "아침잠이 줄어 해 뜨기 전에 눈을 뜨는 날이 많아졌다.", LogKind.STORY),
                LogLineUi("1", "노년기 · 1번째 해", "손주에게 옛날 사진을 보여 주며 한참 웃었다.", LogKind.CHOICE),
                LogLineUi("story-2", "노년기 · 2번째 해", "공원 벤치에 앉아 지나가는 사람들을 구경하는 게 일과가 되었다.", LogKind.STORY),
                LogLineUi("2", "노년기 · 2번째 해", "동네 합창단에 들어가 매주 연습에 나갔다.", LogKind.CHOICE),
            ),
            yearKey = "small:2",
            decision = null,
            outcome = null,
            isDead = false,
            autoPlaying = false,
            speed = 1,
            avatar = AvatarLook(period = LifePeriod.OLD_AGE, usesPhotoFace = true, photoFaceSheet = sheet.path),
        )
        setSmall { AutoProgressScreen(state, onAdvanceYear = {}, onTogglePlaying = {}, onSpeedChange = {}, onSkipToDecision = {}) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("player_avatar_photo", useUnmergedTree = true).fetchSemanticsNodes().size >= 2 }
        save("small_auto_photo_old_age.png")
        // 맨 위가 가장 최근 선택 결과, 그 아래가 같은 해 이야기.
        compose.onNodeWithTag("year_log_line_0", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("동네 합창단에 들어가 매주 연습에 나갔다.").assertIsDisplayed()
        val top = compose.onNodeWithText("동네 합창단에 들어가 매주 연습에 나갔다.").fetchSemanticsNode().positionInRoot.y
        val older = compose.onNodeWithText("공원 벤치에 앉아 지나가는 사람들을 구경하는 게 일과가 되었다.").fetchSemanticsNode().positionInRoot.y
        assertTrue("최신 기록이 위", top < older)
        compose.onAllNodesWithTag("log_kind_choice", useUnmergedTree = true).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        compose.onAllNodesWithTag("log_kind_story", useUnmergedTree = true).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun aiConsentTextOnASmallScreenSaysThePhotoIsSent() {
        setSmall { CharacterSelectScreen(starters = emptyList(), onStart = {}, aiFace = true) }
        compose.onNodeWithTag("photo_pick").performScrollTo().performClick()
        compose.onNodeWithText("OpenAI", substring = true).assertIsDisplayed()
        save("small_ai_consent.png")
        compose.onNodeWithTag("photo_consent_no").performClick()
        compose.onNodeWithTag("photo_message").assertExists()
    }
}
