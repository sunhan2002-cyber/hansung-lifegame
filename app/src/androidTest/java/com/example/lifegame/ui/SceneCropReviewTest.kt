package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.theme.LifeGamePicture
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-03 후속: 사건 전용 그림이 실제 사건 화면에 연결됐는지와, 세로 화면에서 어디까지 보이는지 확인한다.
 * image_routing.json에 사건 그림으로 등록된 모든 사건을 실제 콘텐츠 · 실제 경로 선택(restoredDecisionUi)으로 띄운다.
 * 장면 보기 화면을 그 PNG의 가운데 크롭과 픽셀로 대조해 공용 그림으로 바뀌지 않았는지 보고, 검토용 화면을 남긴다.
 * 승인 그룹이 경로에 추가되면 같은 검사가 그 사건들도 자동으로 포함한다.
 */
@RunWith(AndroidJUnit4::class)
class SceneCropReviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val routing = ImageRoutingRepository(context).load()
    private val periods = V4ContentParser.parsePeriods(asset("periods.sample.json"))
    private val events = V4ContentParser.parseDecisionEvents(asset("events.sample.json"))
    private val content = PlayContent(
        periods = periods,
        focuses = emptyList(),
        decisions = events.associateBy { it.eventId },
        imageFiles = ContentRepository(context).loadImageAssets()
            .filter { it.fileName.isNotBlank() }
            .associate { it.imageId to it.fileName },
        imageRouting = routing,
    )

    /** 스탯 · 시기 · 초상 공용 그림은 사건 그림으로 세지 않는다. */
    private val genericIds = routing.statScenes.values + routing.periodScenes.values + routing.periodPortraits.values

    private val requestedEvents = InstrumentationRegistry.getArguments()
        .getString("eventIds")?.split(",")?.filter(String::isNotBlank)?.toSet()

    private val routedEvents: List<DecisionEvent> = events.filter { event ->
        val id = event.povImageId.orEmpty()
        id in routing.assets && id !in genericIds &&
            (requestedEvents == null || event.eventId in requestedEvents!!)
    }

    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    private fun decisionOf(event: DecisionEvent): DecisionUi =
        restoredDecisionUi(event, content, focusId = null, period = event.periods.minOf { it }).copy(
            text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
            choices = event.choices.take(3).mapIndexed { index, option ->
                DecisionChoiceUi(option.choiceId, ('A' + index).toString(), option.label, hint = "")
            },
        )

    private fun playState(decision: DecisionUi) = PeriodPlayUiState(
        runKey = "crop",
        playerName = "재겸",
        phase = PlayPhase.DECISION,
        periodName = "청년기",
        chapterLabel = "제4장",
        periodYears = 5,
        periodIntro = "",
        yearInPeriod = 2,
        periodMarks = periods.sortedBy { it.order }.map { PeriodMarkUi(it.shortName(), 0f, it.displayName == "청년기") },
        previousPeriodName = "청소년기",
        stats = Stats(health = 72),
        focus = null,
        focusOptions = emptyList(),
        sceneImageId = "",
        sceneFile = null,
        log = emptyList(),
        yearKey = decision.eventId,
        decision = decision,
        outcome = null,
        isDead = false,
        autoPlaying = false,
        speed = 1,
    )

    @Test
    fun routedEventArtIsShownOnThisPhone() = reviewAll("phone") { it() }

    @Test
    fun routedEventArtIsShownOnASmallPhone() = reviewAll("360x740") { screen ->
        Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { screen() }
    }

    private fun reviewAll(label: String, frame: @Composable (@Composable () -> Unit) -> Unit) {
        val expectedEvents = requestedEvents ?: setOf("path_hap_09", "path_int_10")
        assertTrue("요청한 사건이 모두 실제 이미지 경로에 있어야 한다", routedEvents.map { it.eventId }.containsAll(expectedEvents))
        assertTrue("검토 대상 사건이 비어 있으면 통과할 수 없다", routedEvents.isNotEmpty())
        var decision by mutableStateOf(decisionOf(routedEvents.first()))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            LifeGameTheme { frame { DecisionScreen(playState(decision), onChoose = {}, onContinue = {}) } }
        }
        val report = StringBuilder("eventId\timageId\tassetPath\tscreen\tvisibleSourceX\tmatch\n")
        routedEvents.forEach { event ->
            decision = decisionOf(event)
            advance(2_000)
            if (composeRule.onAllNodesWithTag("incident_popup").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithText("사건 보기").performClick()
                advance(400)
            }
            // 본문 출력 중이면 한 번 터치로 완성한다. 이미 완성돼 사건 그림 선노출(2026-11-06)이 떠 있으면 본문은 숨어 있다.
            if (composeRule.onAllNodesWithTag("decision_body", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("decision_body", useUnmergedTree = true).performTouchInput { click(center) }
                advance(TAP_GUARD_MILLIS + 50)
                advance(com.example.lifegame.ui.screens.PREVIEW_DELAY_MILLIS + 16)
            }
            // 선노출은 아래 제목이 겹치므로 한 번 터치로 넘긴 뒤 장면 보기로 크롭을 잰다.
            if (composeRule.onAllNodesWithTag("scene_preview").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("scene_preview").performTouchInput { click(center) }
                advance(TAP_GUARD_MILLIS + 50)
            }
            composeRule.onNodeWithTag("scene_view_button", useUnmergedTree = true).performTouchInput { click(center) }
            advance(16)

            val expectedId = event.povImageId.orEmpty()
            val scene = composeRule.onNodeWithTag("scene_clear")
            val size = scene.fetchSemanticsNode().size
            val loaded = requireNotNull(PixelAssetLoader.load(context, decision.images, size.width, size.height)) {
                "${event.eventId}: 그림을 하나도 읽지 못했다"
            }
            assertEquals("${event.eventId}: 사건 그림 대신 다른 후보가 쓰였다", expectedId, loaded.candidate.imageId)
            val expected = centerCrop(loaded.bitmap, size.width, size.height)
            val match = awaitMatch(expected) { scene.captureToImage().asAndroidBitmap() }
            assertTrue("${event.eventId}: 장면 보기 화면이 ${loaded.candidate.path}의 가운데 크롭과 다르다 ($match)", match >= 0.95f)
            save("${event.eventId}_${label}_scene.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

            // 장면 보기에서 한 번 터치로 돌아온 실제 플레이 화면
            composeRule.onNodeWithTag("scene_only").performTouchInput { click(center) }
            advance(16)
            save("${event.eventId}_${label}_play.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

            val scale = max(size.width / loaded.bitmap.width.toFloat(), size.height / loaded.bitmap.height.toFloat())
            val visible = size.width / scale / loaded.bitmap.width
            val from = (1 - visible) / 2
            report.append("${event.eventId}\t$expectedId\t${loaded.candidate.path}\t${size.width}x${size.height}\t")
                .append("%.3f..%.3f\t%.3f\n".format(from, from + visible, match))
        }
        File(outputDir(), "crop_report_$label.tsv").writeText(report.toString())
    }

    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.mainClock.advanceTimeByFrame()
    }

    /** 그림은 IO 스레드에서 읽히므로 실제 시간을 조금씩 기다리며 화면이 PNG와 같아질 때까지 본다. */
    private fun awaitMatch(expected: Bitmap, capture: () -> Bitmap): Float {
        var best = 0f
        repeat(60) {
            best = max(best, similarity(expected, capture()))
            if (best >= 0.95f) return best
            Thread.sleep(50)
            composeRule.mainClock.advanceTimeByFrame()
        }
        return best
    }

    /** 앱(SceneImage)과 같은 방식: 비율 유지 확대 후 가운데 정렬, 보간 없음. */
    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val scaled = Bitmap.createScaledBitmap(
            source, (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1), false,
        )
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).apply {
                drawColor(LifeGamePicture.toArgb())
                drawBitmap(scaled, (width - scaled.width) / 2f, (height - scaled.height) / 2f,
                    Paint().apply { isFilterBitmap = false; isDither = false })
            }
        }
    }

    private fun similarity(expected: Bitmap, actual: Bitmap): Float {
        if (expected.width != actual.width || expected.height != actual.height) return 0f
        var same = 0
        var total = 0
        for (gy in 1 until 48) for (gx in 1 until 24) {
            val x = expected.width * gx / 24
            val y = expected.height * gy / 48
            val a = expected.getPixel(x, y)
            val b = actual.getPixel(x, y)
            total++
            if (abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 12 &&
                abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 12 &&
                abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 12) same++
        }
        return same / total.toFloat()
    }

    private fun outputDir(): File = requireNotNull(context.getExternalFilesDir("followup-20261103-crop")).also {
        assertTrue(it.isDirectory || it.mkdirs())
    }

    private fun save(fileName: String, bitmap: Bitmap) {
        File(outputDir(), fileName).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
