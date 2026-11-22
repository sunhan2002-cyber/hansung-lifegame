package com.example.lifegame.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.images.coverPlacement
import com.example.lifegame.ui.screens.CUTSCENE_FADE_MILLIS
import com.example.lifegame.ui.screens.CUTSCENE_HOLD_MILLIS
import com.example.lifegame.ui.screens.DOLJABI_EVENT_ID
import com.example.lifegame.ui.screens.DOLJABI_IMAGE_DESCRIPTION
import com.example.lifegame.ui.screens.CHANGE_HOLD_MILLIS
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.theme.LifeGamePicture
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-16 프론트엔드 마무리 과제의 기기 테스트.
 * - 과제 4: 실제 첫 화면 → 새 인생 → 프로필 "이 인생 시작하기" → 돌잡이 컷신 → 첫 사건(돌잡이). 완료 · 건너뛰기 · 뒤로가기 · 재생 중 종료 복원.
 * - 과제 2: 실제 ViewModel로 진행해 자동 사건 카드(능력치 변화 / 기록만)가 자동 진행 화면에 보이고, 이어하기에서 다시 적용되지 않는다.
 * - 과제 5: 360×640dp · 글자 200%에서 컷신 · 자동 사건 카드가 잘리지 않는다.
 * 실제 ViewModel은 테스트 전용 이름의 저장소를 쓰고 끝나면 지운다(에뮬레이터의 실제 저장은 건드리지 않음).
 */
@RunWith(AndroidJUnit4::class)
class Week13FrontendFinishFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week13_finish_${System.nanoTime()}_"
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int) =
            base.getSharedPreferences(prefix + name, mode).also { prefNames += prefix + name }
    }

    @After fun cleanOnlyTestPreferences() {
        prefNames.forEach {
            base.getSharedPreferences(it, android.content.Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(it)
        }
    }

    /** 첫 줄(운동능력) 눈 1 · 3, 나머지 3 · 4. 같은 시드라 매번 같은 인생이 된다. */
    private val code = ChallengeCode.encode(ChallengeStart(Gender.FEMALE, "1,3,3,4,3,4,3,4,3,4,3,4", seed = 20261116L))

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun count(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun mark(text: String) = android.util.Log.d("FinishFlow", text)

    private fun awaitTag(tag: String, timeoutMs: Long = 8_000) {
        val end = System.currentTimeMillis() + timeoutMs
        mark("await $tag")
        var loops = 0
        while (count(tag) == 0) {
            if (loops++ % 20 == 0) mark("await $tag loop $loops")
            check(System.currentTimeMillis() < end) { "$tag 화면이 나타나지 않았다" }
            Thread.sleep(10)
            compose.mainClock.advanceTimeByFrame()
        }
        mark("await $tag done")
    }

    private fun small(fontScale: Float, width: Int, height: Int, content: @Composable () -> Unit) {
        compose.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = fontScale)) {
                LifeGameTheme { Box(Modifier.requiredSize(width.dp, height.dp).clipToBounds()) { content() } }
            }
        }
    }

    private fun SemanticsNodeInteraction.assertInsideWidth(label: String): SemanticsNodeInteraction {
        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width
        val node = fetchSemanticsNode()
        val right = node.positionInRoot.x + node.size.width
        assertTrue("$label 가 화면 오른쪽 밖으로 나간다: $right > $rootWidth", node.positionInRoot.x >= -1 && right <= rootWidth + 1)
        return this
    }

    /**
     * 스크롤하지 않아도 화면 안에서 시작한다(윗부분이 보인다). 시계를 멈춘 테스트에서 performScrollTo는 스크롤 애니메이션이
     * 끝나지 않아 멈추므로 위치로 확인한다.
     */
    private fun SemanticsNodeInteraction.assertStartsOnScreen(label: String): SemanticsNodeInteraction {
        val rootHeight = compose.onRoot().fetchSemanticsNode().size.height
        val top = fetchSemanticsNode().positionInRoot.y
        assertTrue("$label 가 스크롤해야 보인다: top=$top, 화면 높이=$rootHeight", top >= 0 && top < rootHeight)
        return this
    }

    // ---- 과제 4: 새 게임 → 돌잡이 컷신 → 첫 사건 ----

    @Test
    fun newGameFromTheHomeScreenPlaysTheCutsceneThenTheFirstEvent() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread { vm = LifeGameViewModel(app) }
        compose.setContent { LifeGameApp(gameViewModel = vm) }
        compose.onNodeWithText("새 인생 살기").performClick()
        compose.onNodeWithTag("challenge_input").performScrollTo().performTextInput(code)
        compose.onNodeWithTag("challenge_apply").performScrollTo().performClick()
        compose.onNodeWithText("이 인생 시작하기").assertIsDisplayed()
        // 시계를 멈춘 뒤 누른다(컷신이 저절로 끝나기 전에 확인하려고). 시작 버튼은 스크롤 밖 아래에 붙어 있다.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("이 인생 시작하기").performClick()
        awaitTag("doljabi_cutscene")

        // 새 인생의 첫 화면이 돌잡이 컷신이고, 그 뒤에 기다리는 첫 사건이 돌잡이다.
        val play = requireNotNull(vm.play)
        assertEquals(DOLJABI_EVENT_ID, play.decision?.eventId)
        assertEquals(1, play.yearInPeriod)
        assertEquals(CutsceneStatus.PLAYING, play.presentation.cutscene)
        assertEquals(CutsceneStatus.PLAYING, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)
        compose.onNodeWithTag("doljabi_cutscene_image").assertExists()
        assertEquals(
            1,
            compose.onAllNodesWithTag("doljabi_cutscene_image").fetchSemanticsNodes()
                .count { hasContentDescription(DOLJABI_IMAGE_DESCRIPTION).matches(it) },
        )
        compose.onAllNodesWithTag("decision_body").assertCountEquals(0)
        val statsBefore = play.stats

        // 완료: 2초 뒤 저절로 끝나고 첫 사건 본문 → 선택지.
        Thread.sleep(300)
        advance(CUTSCENE_FADE_MILLIS + CUTSCENE_HOLD_MILLIS + 32)
        assertEquals(CutsceneStatus.COMPLETED, requireNotNull(vm.play).presentation.cutscene)
        assertEquals(CutsceneStatus.COMPLETED, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)
        compose.onAllNodesWithTag("doljabi_cutscene").assertCountEquals(0)
        awaitTag("decision_body")
        compose.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        compose.onNodeWithTag("decision_title").assertTextContains("돌잡이")
        advance(TAP_GUARD_MILLIS + 50)
        tapDoljabiNode("decision_body")
        advance(TAP_GUARD_MILLIS + 50)
        awaitTag("decision_choice_A")
        assertEquals("컷신은 능력치를 바꾸지 않는다", statsBefore, requireNotNull(vm.play).stats)
        save("finish_newgame_first_event.png")
    }

    @Test
    fun doljabiPianoKeepsNeutralArtThroughTheChoiceAndRestoresIt() =
        verifyDoljabiChoiceArtwork("A")

    @Test
    fun doljabiGavelShowsTheGraspOnlyAfterChoosingItAndRestoresIt() =
        verifyDoljabiChoiceArtwork("B")

    @Test
    fun doljabiBanknoteKeepsNeutralArtThroughTheChoiceAndRestoresIt() =
        verifyDoljabiChoiceArtwork("C")

    /** Actual new-life state, native taps and fresh ViewModels, including pre-image-snapshot saves. */
    private fun verifyDoljabiChoiceArtwork(choiceId: String) {
        val neutralId = "img_infant_toys_001"
        val neutralPath = "followup_20261120/img_infant_toys_001.png"
        val graspId = "img_doljabi_grasp_001"
        val expectedId = if (choiceId == "B") graspId else neutralId
        val expectedPath = if (choiceId == "B") {
            "cutscenes/week11/img_doljabi_grasp_001.png"
        } else neutralPath
        lateinit var first: LifeGameViewModel
        compose.runOnUiThread {
            first = LifeGameViewModel(app)
            first.startNewLife(NewLifeProfile("검증", Gender.FEMALE, null, runSeed = 20261116L))
        }
        var current by mutableStateOf(first)
        var generation by mutableStateOf(0)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                key(generation) { PlayHost(gameViewModel = current, onGoHome = {}) }
            }
        }
        val store = V4ProgressStore(app)
        val before = requireNotNull(first.play).stats
        awaitTag("doljabi_cutscene")
        advance(CUTSCENE_FADE_MILLIS + TAP_GUARD_MILLIS + 32)
        assertDoljabiPixels("doljabi_cutscene_image", neutralPath, fill = false)
        save("doljabi_${choiceId}_cutscene.png")
        tapDoljabiNode("doljabi_cutscene_image")
        awaitTag("decision_body")
        advance(TAP_GUARD_MILLIS + 50)
        tapDoljabiNode("decision_body")
        advance(TAP_GUARD_MILLIS + 50)
        awaitTag("decision_choice_$choiceId")
        assertEquals(before, requireNotNull(first.play).stats)
        assertTrue(requireNotNull(store.load()).choices.isEmpty())
        assertDoljabiRoute(first, neutralId, neutralPath)
        save("doljabi_${choiceId}_choice.png")
        inspectDoljabiScene(neutralPath)
        scrollDoljabiNode("decision_choice_$choiceId")
        tapDoljabiNode("decision_choice_$choiceId")
        awaitTag("outcome_text")
        advance(CHANGE_HOLD_MILLIS + TAP_GUARD_MILLIS + 50)
        assertDoljabiRoute(first, expectedId, expectedPath)
        val chosen = requireNotNull(store.load())
        assertEquals(1, chosen.choices.size)
        assertEquals(choiceId, chosen.pendingOutcome?.choiceId)
        assertEquals(expectedId, chosen.pendingOutcome?.eventImageId)
        assertEquals(expectedId, chosen.choices.single().eventImageId)
        save("doljabi_${choiceId}_result.png")
        inspectDoljabiScene(expectedPath, "doljabi_${choiceId}_scene.png")

        fun restoreAndInspect(fileName: String?) {
            lateinit var restored: LifeGameViewModel
            compose.runOnUiThread {
                restored = LifeGameViewModel(app).apply { continueLife() }
                current = restored
                generation++
            }
            advance(TAP_GUARD_MILLIS + 50)
            awaitTag("outcome_text")
            compose.onAllNodesWithTag("doljabi_cutscene").assertCountEquals(0)
            assertDoljabiRoute(restored, expectedId, expectedPath)
            assertEquals(chosen.state.stats, requireNotNull(restored.play).stats)
            assertEquals(requireNotNull(first.play).outcome, restored.play?.outcome)
            assertEquals(1, requireNotNull(store.load()).choices.size)
            inspectDoljabiScene(expectedPath, fileName)
        }

        assertTrue(store.awaitDurable().get(10, TimeUnit.SECONDS))
        restoreAndInspect("doljabi_${choiceId}_resumed.png")
        for (oldImageId in listOf(null, neutralId, graspId)) {
            val legacy = chosen.copy(
                pendingOutcome = requireNotNull(chosen.pendingOutcome).copy(eventImageId = oldImageId),
                choices = chosen.choices.map { it.copy(eventImageId = oldImageId) },
            )
            assertTrue(store.save(legacy))
            assertTrue(store.awaitDurable().get(10, TimeUnit.SECONDS))
            restoreAndInspect(null)
        }
    }

    private fun tapDoljabiNode(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun scrollDoljabiNode(tag: String) {
        compose.mainClock.autoAdvance = true
        try {
            compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        } finally {
            compose.mainClock.autoAdvance = false
        }
        advance(TAP_GUARD_MILLIS + 50)
    }

    private fun inspectDoljabiScene(expectedPath: String, fileName: String? = null) {
        scrollDoljabiNode("scene_view_button")
        tapDoljabiNode("scene_view_button")
        awaitTag("scene_only")
        assertDoljabiPixels("scene_clear", expectedPath, fill = true)
        if (fileName != null) save(fileName)
        tapDoljabiNode("scene_only")
        advance(TAP_GUARD_MILLIS + 50)
    }

    private fun assertDoljabiRoute(vm: LifeGameViewModel, expectedId: String, expectedPath: String) {
        val decision = requireNotNull(vm.play?.decision)
        assertEquals(DOLJABI_EVENT_ID, decision.eventId)
        assertEquals(expectedId, decision.imageId)
        val loaded = requireNotNull(PixelAssetLoader.load(base, decision.images, 720, 1480))
        assertEquals(expectedId, loaded.candidate.imageId)
        assertEquals(expectedPath, loaded.candidate.path)
    }

    /** Compare the visible raster against a literal approved asset, independent of the model's selected path. */
    private fun assertDoljabiPixels(tag: String, expectedPath: String, fill: Boolean) {
        val node = compose.onNodeWithTag(tag)
        val size = node.fetchSemanticsNode().size
        // The native loader may sample the 1448x1086 master to 724x543 for the small cutscene.
        // Resize that decoded raster, as SceneImage does, while keeping the expected path literal.
        val loaded = requireNotNull(PixelAssetLoader.load(
            base,
            listOf(ImageCandidate(
                imageId = expectedPath.substringAfterLast('/').removeSuffix(".png"),
                path = expectedPath,
                usage = "scene",
            )),
            size.width,
            size.height,
        ))
        assertEquals(expectedPath, loaded.candidate.path)
        val source = loaded.bitmap
        val expected = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val placement = if (fill) coverPlacement(source.width, source.height, size.width, size.height, loaded.focal) else null
        val scale = min(size.width / source.width.toFloat(), size.height / source.height.toFloat())
        val drawnWidth = placement?.width ?: source.width * scale
        val drawnHeight = placement?.height ?: source.height * scale
        val left = placement?.left ?: (size.width - drawnWidth) / 2f
        val top = placement?.top ?: (size.height - drawnHeight) / 2f
        Canvas(expected).apply {
            drawColor(LifeGamePicture.toArgb())
            drawBitmap(source, null, RectF(left, top, left + drawnWidth, top + drawnHeight),
                Paint().apply { isFilterBitmap = false; isDither = false })
        }
        mark("$tag reference=$expectedPath decoded=${source.width}x${source.height} viewport=${size.width}x${size.height} fill=$fill")
        var best = 0f
        var lastActual: Bitmap? = null
        try {
            repeat(60) {
                lastActual?.recycle()
                val actual = node.captureToImage().asAndroidBitmap()
                lastActual = actual
                var matching = 0
                var sampled = 0
                for (row in 1 until 48) for (column in 1 until 24) {
                    val x = size.width * column / 24
                    val y = size.height * row / 48
                    val a = expected.getPixel(x, y)
                    val b = actual.getPixel(x, y)
                    if (abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 12 &&
                        abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 12 &&
                        abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 12) matching++
                    sampled++
                }
                best = max(best, matching / sampled.toFloat())
                if (best >= 0.95f) return
                Thread.sleep(25)
                compose.mainClock.advanceTimeByFrame()
            }
            val directory = requireNotNull(base.getExternalFilesDir("week13-finish"))
            assertTrue(directory.isDirectory || directory.mkdirs())
            val name = "doljabi_reference_${tag}_${expectedPath.substringAfterLast('/').removeSuffix(".png")}"
            for ((suffix, bitmap) in listOf("expected" to expected, "actual" to requireNotNull(lastActual))) {
                File(directory, "${name}_${suffix}.png").outputStream().use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            }
            assertTrue("$tag differs from $expectedPath (pixel match $best)", best >= 0.95f)
        } finally {
            expected.recycle()
            lastActual?.recycle()
            // PixelAssetLoader owns and caches source; it must stay alive for the native screen.
        }
    }

    @Test
    fun backDuringTheCutsceneSkipsItAndStaysOnTheFirstEvent() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.MALE, null))
        }
        compose.mainClock.autoAdvance = false
        compose.setContent { LifeGameTheme { PlayHost(gameViewModel = vm, onGoHome = { error("뒤로가기가 홈으로 나가면 안 된다") }) } }
        awaitTag("doljabi_cutscene")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithTag("doljabi_cutscene").assertCountEquals(0)
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(vm.play).presentation.cutscene)
        assertEquals(DOLJABI_EVENT_ID, vm.play?.decision?.eventId)
        awaitTag("decision_body")
        compose.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        // 끝난 컷신은 다시 바뀌지 않는다(뒤로가기 뒤 자동 끝 시간이 지나도 SKIPPED).
        advance(CUTSCENE_FADE_MILLIS + CUTSCENE_HOLD_MILLIS + 32)
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(vm.play).presentation.cutscene)
    }

    @Test
    fun closingTheAppDuringTheCutscenePlaysItAgainAndOnceDoneNeverAgain() {
        lateinit var first: LifeGameViewModel
        compose.runOnUiThread {
            first = LifeGameViewModel(app)
            first.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null))
        }
        var current by mutableStateOf(first)
        compose.mainClock.autoAdvance = false
        compose.setContent { LifeGameTheme { PlayHost(gameViewModel = current, onGoHome = {}) } }
        awaitTag("doljabi_cutscene")
        assertEquals(CutsceneStatus.PLAYING, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)

        // 재생 중 종료 → 이어하기: 컷신을 다시 보여 준다.
        lateinit var second: LifeGameViewModel
        compose.runOnUiThread { second = LifeGameViewModel(app).apply { continueLife() } }
        assertEquals(CutsceneStatus.PLAYING, requireNotNull(second.play).presentation.cutscene)
        current = second
        compose.mainClock.advanceTimeByFrame()
        awaitTag("doljabi_cutscene")
        advance(400) // 화면이 처음 그려진 직후의 연타 보호 시간(PlayHost)이 지나야 터치가 들어간다.
        compose.onNodeWithTag("doljabi_cutscene_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)

        // 끝난 뒤 종료 → 이어하기: 컷신 없이 첫 사건 본문.
        lateinit var third: LifeGameViewModel
        compose.runOnUiThread { third = LifeGameViewModel(app).apply { continueLife() } }
        current = third
        compose.mainClock.advanceTimeByFrame()
        advance(400)
        compose.onAllNodesWithTag("doljabi_cutscene").assertCountEquals(0)
        awaitTag("decision_body")
        assertEquals(DOLJABI_EVENT_ID, third.play?.decision?.eventId)
    }

    // ---- 과제 2: 자동 사건이 실제 화면에 ----

    /** 실제 ViewModel로 앱 순서대로 진행한다. [until]을 만족하는 화면 상태가 나오면 멈춘다. */
    private fun driveUntil(vm: LifeGameViewModel, until: (PeriodPlayUiState) -> Boolean): PeriodPlayUiState {
        val started = System.currentTimeMillis()
        repeat(300) { index ->
            var p: PeriodPlayUiState? = null
            compose.runOnUiThread { p = vm.play }
            val play = requireNotNull(p)
            android.util.Log.d(
                "FinishFlow",
                "#$index ${play.phase} ${play.periodName} ${play.yearInPeriod} outcome=${play.outcome != null} " +
                    "decision=${play.decision?.eventId} auto=${play.automaticEvent?.eventId} t=${System.currentTimeMillis() - started}",
            )
            if (until(play)) return play
            check(System.currentTimeMillis() - started < 120_000) { "2분 안에 조건에 맞는 화면이 나오지 않았다(#$index)" }
            compose.runOnUiThread {
                when (play.phase) {
                    PlayPhase.DECISION -> if (play.outcome != null) vm.continueAfterOutcome()
                        else vm.chooseDecision(requireNotNull(play.decision).choices.first { it.available }.choiceId)
                    PlayPhase.CHAPTER -> vm.selectFocus(play.focusOptions.first().focusId)
                    PlayPhase.AUTO -> vm.advanceYear()
                    PlayPhase.ENDED -> error("조건에 맞는 화면 없이 인생이 끝났다")
                }
            }
        }
        error("조건에 맞는 화면이 나오지 않았다")
    }

    @Test
    fun theDoljabiPhotoAppearsAsARecordOnlyAutomaticEventRightAfterTheChoice() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null, runSeed = 20261116L))
        }
        // 피아노 선택 → 결과 → 기록 전용 돌잡이 사진. 확장 회상은 콘텐츠 showIn에 등록된 후속 선택 사건에서만 나온다.
        compose.runOnUiThread { vm.chooseDecision("A") }
        assertNotNull(vm.play?.outcome)
        assertNull("결과를 보는 동안 자동 사건은 없다", vm.play?.automaticEvent)
        compose.runOnUiThread { vm.continueAfterOutcome() }
        assertNull("자동 진행 타이머가 돌기 전에는 자동 사건이 없다", vm.play?.automaticEvent)
        compose.runOnUiThread { vm.advanceYear() }
        val play = requireNotNull(vm.play)
        val event = requireNotNull(play.automaticEvent)
        assertEquals("auto_infant_dol_photo_music", event.eventId)
        assertTrue(event.recordOnly)
        assertNull("자동 사진은 확장 회상 showIn 대상이 아니다", event.recall)
        assertEquals(PlayPhase.AUTO, play.phase)

        compose.mainClock.autoAdvance = false
        small(fontScale = 2.0f, width = 360, height = 640) { PlayHost(gameViewModel = vm, onGoHome = {}) }
        advance(400)
        awaitTag("auto_event_card")
        compose.onNodeWithTag("auto_event_card").assertInsideWidth("자동 사건 카드").assertStartsOnScreen("자동 사건 카드")
        compose.onNodeWithTag("auto_event_text", useUnmergedTree = true).assertTextContains(event.text)
            .assertInsideWidth("자동 사건 문장").assertStartsOnScreen("자동 사건 문장")
        compose.onNodeWithTag("auto_event_record_only", useUnmergedTree = true).assertIsDisplayed()
            .assertInsideWidth("기록 전용 안내").assertStartsOnScreen("기록 전용 안내")
        compose.onAllNodesWithTag("auto_event_changes", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("auto_event_recall", useUnmergedTree = true).assertCountEquals(0)
        save("finish_auto_record_only_360x640_200.png")

        // 자동 사건 카드는 한 해(3초)보다 길게, 읽을 시간만큼 머문다. 그 뒤 그해 결정(돌발)이 그대로 나온다.
        val hold = automaticEventHoldMillis(event)
        assertTrue(hold > YEAR_MILLIS_AT_1X)
        advance(YEAR_MILLIS_AT_1X + 100)
        assertEquals("읽는 동안에는 넘어가지 않는다", PlayPhase.AUTO, requireNotNull(vm.play).phase)
        advance(hold - YEAR_MILLIS_AT_1X)
        assertEquals(PlayPhase.DECISION, requireNotNull(vm.play).phase)
        assertNull(vm.play?.automaticEvent)
    }

    private fun recordOnlyAutomaticEvent(): LifeGameViewModel {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null, runSeed = 20261116L))
            vm.chooseDecision("A")
            vm.continueAfterOutcome()
            vm.advanceYear()
        }
        assertEquals("auto_infant_dol_photo_music", vm.play?.automaticEvent?.eventId)
        return vm
    }

    @Test
    fun readingDetailsAtLargeTextPausesTheAutomaticEventUntilExplicitResume() {
        val vm = recordOnlyAutomaticEvent()
        val before = requireNotNull(vm.play)
        val event = requireNotNull(before.automaticEvent)
        val records = requireNotNull(V4ProgressStore(app).load()).state.automaticEventRecords
        val hold = automaticEventHoldMillis(event)
        compose.mainClock.autoAdvance = false
        small(fontScale = 2f, width = 360, height = 640) { PlayHost(gameViewModel = vm, onGoHome = {}) }
        advance(400)
        awaitTag("auto_event_card")
        compose.onNodeWithTag("auto_event_record_only", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("player_avatar_button").assertIsDisplayed().performClick()
        awaitTag("life_facts_sheet")
        advance(400)
        assertFalse(requireNotNull(vm.play).autoPlaying)
        advance(hold * 2)
        assertEquals(before.yearInPeriod, vm.play?.yearInPeriod)
        assertEquals(event.eventId, vm.play?.automaticEvent?.eventId)
        assertEquals(before.stats, vm.play?.stats)
        assertEquals(records, requireNotNull(V4ProgressStore(app).load()).state.automaticEventRecords)

        // Use the sheet's own accessibility dismiss action, not the Activity's back dispatcher.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        advance(500)
        compose.onAllNodesWithTag("life_facts_sheet").assertCountEquals(0)
        assertFalse(requireNotNull(vm.play).autoPlaying)
        advance(hold + 100)
        assertEquals(event.eventId, vm.play?.automaticEvent?.eventId)
        compose.onNodeWithTag("play_pause").assertIsDisplayed().performClick()
        advance(64)
        assertTrue(requireNotNull(vm.play).autoPlaying)
        advance(hold + 100)
        assertEquals(PlayPhase.DECISION, vm.play?.phase)
        assertNull(vm.play?.automaticEvent)
        assertEquals(records, requireNotNull(V4ProgressStore(app).load()).state.automaticEventRecords)
    }

    @Test
    fun scrollingTheLargeAutomaticCardAlsoPausesUntilExplicitResume() {
        val vm = recordOnlyAutomaticEvent()
        val before = requireNotNull(vm.play)
        val event = requireNotNull(before.automaticEvent)
        val hold = automaticEventHoldMillis(event)
        compose.mainClock.autoAdvance = false
        small(fontScale = 2f, width = 360, height = 640) { PlayHost(gameViewModel = vm, onGoHome = {}) }
        advance(400)
        awaitTag("auto_event_card")
        compose.onNodeWithTag("auto_progress_content").performTouchInput { swipeUp(durationMillis = 250) }
        advance(500)
        assertFalse(requireNotNull(vm.play).autoPlaying)
        advance(hold * 2)
        assertEquals(before.yearInPeriod, vm.play?.yearInPeriod)
        assertEquals(event.eventId, vm.play?.automaticEvent?.eventId)
        assertEquals(before.stats, vm.play?.stats)
        compose.onNodeWithTag("play_pause").assertIsDisplayed().performClick()
        advance(hold + 100)
        assertEquals(PlayPhase.DECISION, vm.play?.phase)
        assertNull(vm.play?.automaticEvent)
    }

    @Test
    fun aStatChangingAutomaticEventShowsItsChangesAndSurvivesRestoreWithoutReapplying() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.MALE, null, runSeed = 7L))
        }
        val shown = driveUntil(vm) { it.automaticEvent?.recordOnly == false }
        val event = requireNotNull(shown.automaticEvent)
        val savedState = requireNotNull(V4ProgressStore(app).load()).state
        val recordsBefore = savedState.automaticEventRecords.size
        assertEquals(event.eventId, savedState.automaticEventRecords.last().eventId)

        // 앱을 다시 열어 이어하면 같은 카드를 다시 보여 주고, 능력치 · 기록은 그대로(다시 적용하지 않음).
        lateinit var restored: LifeGameViewModel
        compose.runOnUiThread { restored = LifeGameViewModel(app).apply { continueLife() } }
        val again = requireNotNull(restored.play)
        assertEquals(event.eventId, again.automaticEvent?.eventId)
        assertEquals(event.changes, again.automaticEvent?.changes)
        assertEquals(shown.stats, again.stats)
        assertEquals(recordsBefore, requireNotNull(V4ProgressStore(app).load()).state.automaticEventRecords.size)

        mark("restored ${again.automaticEvent?.eventId}")
        compose.mainClock.autoAdvance = false
        small(fontScale = 2.0f, width = 360, height = 640) { PlayHost(gameViewModel = restored, onGoHome = {}) }
        mark("content set")
        advance(400)
        mark("advanced")
        awaitTag("auto_event_card")
        compose.onNodeWithTag("auto_event_card").assertInsideWidth("자동 사건 카드").assertStartsOnScreen("자동 사건 카드")
        val first = event.changes.first()
        compose.onNodeWithTag("auto_event_changes", useUnmergedTree = true).assertTextContains(first.name, substring = true)
        compose.onAllNodesWithTag("auto_event_record_only", useUnmergedTree = true).assertCountEquals(0)
        // 기록 줄에는 "자동" 표시가 붙고, 같은 문장이 카드와 기록에 두 번 보이지 않는다.
        assertEquals(0, compose.onAllNodesWithText(event.text).fetchSemanticsNodes().size - 1)
        save("finish_auto_stat_360x640_200.png")

        // 이어서 진행하면 다음 단계는 결정이고, 자동 사건 기록은 늘지 않았다.
        compose.runOnUiThread { restored.advanceYear() }
        assertEquals(PlayPhase.DECISION, requireNotNull(restored.play).phase)
        assertEquals(recordsBefore, requireNotNull(V4ProgressStore(app).load()).state.automaticEventRecords.size)
        assertFalse(restored.play?.automaticEvent != null)
    }

    // ---- 과제 5: 작은 화면 · 큰 글자 ----

    @Test
    fun theCutsceneFitsASmallScreenWithLargeText() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("가나다라마바사아자차", Gender.FEMALE, null))
        }
        compose.mainClock.autoAdvance = false
        small(fontScale = 2.0f, width = 360, height = 640) { PlayHost(gameViewModel = vm, onGoHome = {}) }
        awaitTag("doljabi_cutscene")
        Thread.sleep(300)
        advance(CUTSCENE_FADE_MILLIS)
        compose.onNodeWithTag("doljabi_cutscene_skip").assertIsDisplayed().assertInsideWidth("건너뛰기")
        compose.onNodeWithTag("doljabi_cutscene_caption").assertIsDisplayed().assertInsideWidth("캡션")
        compose.onNodeWithTag("doljabi_cutscene_image").assertIsDisplayed().assertInsideWidth("컷신 그림")
        save("finish_cutscene_360x640_200.png")
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithText(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text), useUnmergedTree = true)

    private fun save(fileName: String) {
        compose.mainClock.advanceTimeByFrame()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(base.getExternalFilesDir("week13-finish"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream -> assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
    }
}
