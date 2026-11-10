package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.screens.ChapterFocusScreen
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.PREVIEW_DELAY_MILLIS
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-09 보완 8번: 작은 화면(360×640dp, 글꼴 130%)에서 모든 사건의 결정 화면을 띄워
 * 글씨 잘림 · 선택지 카드 겹침 · 마지막 선택지까지 스크롤로 닿는지를 잰다.
 *
 * 카드는 실제 선택지 문구와 실제 가능성 · 능력치 태그(`DecisionOption.hint`)를 쓰고, 보조 줄이 가장 길어지는 경우를
 * 보려고 열린 선택지마다 "건강 +10 회복"을, 선택지가 셋 이상이면 마지막 하나를 잠금(잠금 이유 두 줄)으로 둔다.
 * 그림은 크롭 검사(`SceneCropReviewTest`)에서 따로 보므로 여기서는 싣지 않는다(배치는 그림과 무관).
 */
@RunWith(AndroidJUnit4::class)
class SmallScreenLayoutAuditTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val periods = V4ContentParser.parsePeriods(asset("periods.sample.json"))
    private val events = V4ContentParser.parseDecisionEvents(asset("events.sample.json"))

    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    private fun decisionOf(event: DecisionEvent): DecisionUi = DecisionUi(
        eventId = event.eventId,
        kind = event.kind,
        title = event.title,
        text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
        imageId = "",
        imageFile = null,
        choices = event.choices.mapIndexed { index, option ->
            val locked = event.choices.size >= 3 && index == event.choices.lastIndex
            DecisionChoiceUi(
                choiceId = option.choiceId,
                key = ('A' + index).toString(),
                label = option.label,
                hint = option.hint(Stats()),
                healthEffectText = if (locked) null else "건강 +10 회복",
                available = !locked,
                lockReasons = if (locked) listOf("지력 60 필요 (지금 52)", "대학 진학 필요") else emptyList(),
            )
        },
    )

    private fun playState(decision: DecisionUi) = PeriodPlayUiState(
        runKey = "small-audit",
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

    @Test fun foldedChoiceCardsFitASmallScreen() = audit(expandTags = false)

    @Test fun expandedChoiceCardsFitASmallScreen() = audit(expandTags = true)

    /**
     * 실기 360×640dp · 글꼴 130%에서 초점 고르기 화면 제목 "이번 시기에 집중할 것"이 옆 안내문에 밀려
     * "이번 시 / 기에 집 / 중할 것" 세 줄로 꺾였다(2026-11-09 캡처 w22). 같은 SectionLabel을 쓰는 새 인생 화면도 본다.
     */
    @Test
    fun sectionHeadingsStayOnOneLineOnASmallScreen() {
        val chapter = playState(decisionOf(events.first())).copy(
            phase = PlayPhase.CHAPTER,
            decision = null,
            periodName = "유년기",
            chapterLabel = "제2장",
            focusOptions = listOf(
                FocusOptionUi("study", "학업", "공부와 책에 시간을 쓴다. 노는 시간이 줄어 몸은 조금 굳는다.", listOf(StatChangeUi("지력", 1), StatChangeUi("운동능력", -1))),
                FocusOptionUi("friends", "친구", "친구들과 어울리며 사회성을 키운다. 숙제는 자꾸 뒤로 밀린다.", listOf(StatChangeUi("사회성", 1))),
            ),
        )
        var screen by mutableStateOf("chapter")
        setSmall {
            if (screen == "chapter") ChapterFocusScreen(chapter, onSelectFocus = {}) else CharacterSelectScreen(starters = emptyList(), onStart = {})
        }
        compose.mainClock.autoAdvance = true
        val headings = mapOf(
            "chapter" to listOf("이번 시기에 집중할 것"),
            "profile" to listOf("성별", "타고난 능력치", "모습"),
        )
        headings.forEach { (name, titles) ->
            screen = name
            compose.waitForIdle()
            titles.forEach { title ->
                val node = compose.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode()
                val lines = requireNotNull(layoutOf(node)).lineCount
                assertEquals("$name: 제목 \"$title\"이 한 줄", 1, lines)
            }
            save("small_headings_$name.png")
        }
    }

    /**
     * 실제 앱 360×640dp · 글꼴 130%에서 결과 칩 네 개(건강 +10 · 지력 +2 · 사회성 −2 · 행복 +4)가 한 줄에 들어가지 않아
     * 마지막 칩이 좁아지며 글자가 세로로 꺾였다(2026-11-09 캡처 w09). 이제는 다음 줄로 넘어가고 결과 카드 문구를 덮지 않는다.
     */
    @Test
    fun resultChipsWrapInsteadOfSqueezingOnASmallScreen() {
        val outcome = OutcomeUi(
            choiceLabel = "피아노 장난감을 잡는다.",
            text = "건반을 두드리자 박수가 터졌다.",
            changes = listOf(
                StatChangeUi("건강", 10), StatChangeUi("지력", 2), StatChangeUi("사회성", -2),
                StatChangeUi("행복", 4), StatChangeUi("운동능력", 3), StatChangeUi("경제력", -5),
            ),
            endsLife = false,
        )
        val decision = decisionOf(events.first { it.choices.size >= 2 })
        setSmall { DecisionScreen(playState(decision).copy(outcome = outcome), onChoose = {}, onContinue = {}) }
        advance(400)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val chips = compose.onAllNodesWithTag("outcome_change_chip", useUnmergedTree = true).fetchSemanticsNodes().map { unclipped(it) }
        assertEquals(outcome.changes.size, chips.size)
        chips.forEach { chip ->
            assertTrue("칩이 화면 가로 안: $chip", chip.left >= root.left - 0.5f && chip.right <= root.right + 0.5f)
            assertTrue("칩 글자가 세로로 꺾이지 않는다(가로로 긴 알약): $chip", chip.width > chip.height)
        }
        val heights = chips.map { it.height }
        assertTrue("모든 칩이 한 줄 높이: $heights", heights.max() - heights.min() < 1.5f)
        chips.forEachIndexed { i, a ->
            chips.drop(i + 1).forEach { b ->
                val overlap = a.intersect(b)
                assertTrue("칩끼리 겹치지 않는다: $a / $b", overlap.width <= 0.5f || overlap.height <= 0.5f)
            }
        }
        assertTrue("작은 화면에서는 두 줄 이상으로 넘어간다", chips.map { it.top }.distinct().size >= 2)
        val labelTop = unclipped(compose.onNodeWithText(outcome.choiceLabel, useUnmergedTree = true).fetchSemanticsNode()).top
        chips.forEach { assertTrue("칩이 결과 카드의 선택 문구를 덮지 않는다", it.bottom <= labelTop + 0.5f) }
        save("small_result_chips.png")
    }

    /**
     * 2026-11-09 자동 스크롤: 작은 화면에서 선노출 뒤 선택지가 첫 화면 아래(카드 A 윗부분만)에 있었다.
     * 선택지가 처음 나타나면 한 번 스크롤해 첫 카드가 다 보이고, 사용자가 위로 다시 올리거나 화면이 다시 만들어져도
     * 다시 끌어내리지 않는다.
     */
    @Test
    fun choicesScrollIntoViewOnceAfterThePreview() {
        val event = events.maxBy { e -> e.text.length + e.choices.sumOf { it.label.length } }
        val host = ManualRestoreHost(compose)
        setSmall(host) { DecisionScreen(playState(decisionOf(event)), onChoose = {}, onContinue = {}) }
        reachChoices()
        advance(1_000)
        val viewport = compose.onNodeWithTag("decision_content").fetchSemanticsNode().boundsInRoot
        val first = cardBounds("A")
        assertTrue("${event.eventId}: 선택지가 나타나면 첫 카드가 다 보인다 ($first / $viewport)", first.top >= viewport.top - 0.5f && first.bottom <= viewport.bottom + 0.5f)
        val scrolled = scrollRange().value()
        assertTrue("${event.eventId}: 작은 화면에서는 실제로 스크롤했다", scrolled > 0f)
        save("small_choices_auto_scrolled.png")
        // 사용자가 본문을 다시 보려고 위로 올리면 그대로 둔다.
        scrollContentBy(-scrolled)
        advance(1_000)
        assertEquals(0f, scrollRange().value(), 0.5f)
        // 화면이 다시 만들어져도(회전 등) 다시 스크롤하지 않고, 위치도 그대로다.
        host.recreate()
        advance(1_000)
        assertEquals(1, host.recreations)
        assertEquals(0f, scrollRange().value(), 0.5f)
    }

    private fun setSmall(host: ManualRestoreHost? = null, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        val framed: @androidx.compose.runtime.Composable () -> Unit = {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) { content() }
                }
            }
        }
        if (host != null) host.setContent(framed) else compose.setContent(framed)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun audit(expandTags: Boolean) {
        val mode = if (expandTags) "expanded" else "folded"
        var decision by mutableStateOf(decisionOf(events.first()))
        setSmall { DecisionScreen(playState(decision), onChoose = {}, onContinue = {}) }
        val issues = mutableListOf<String>()
        val report = StringBuilder("eventId\tchoices\tmaxLabelLines\tlastCardBelowFoldDp\tneedsScroll\tautoScrollDp\tissues\n")
        val heaviest = events.sortedByDescending { e -> e.choices.sumOf { it.label.length } }.take(4).map { it.eventId }.toSet()
        var toggled = false
        events.forEachIndexed { index, event ->
            android.util.Log.i("SmallAudit", "$mode ${index + 1}/${events.size} ${event.eventId}")
            decision = decisionOf(event)
            reachChoices()
            val found = mutableListOf<String>()
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            // 선택지가 나타나면 자동 스크롤로 첫 카드가 다 보인다(2026-11-09).
            advance(1_000)
            val shownViewport = compose.onNodeWithTag("decision_content").fetchSemanticsNode().boundsInRoot
            val firstShown = cardBounds(decision.choices.first().key)
            if (firstShown.top < shownViewport.top - 0.5f || firstShown.bottom > shownViewport.bottom + 0.5f) found += "선택지가 나타난 뒤 첫 카드가 다 보이지 않음"
            val autoScrollDp = scrollRange().value() / density()
            // 사용자가 직접 펼치면 카드가 길어지므로 자동 스크롤 확인 뒤에 펼친다(다음 사건부터는 펼친 채 나타난다).
            if (expandTags && !toggled && compose.onAllNodesWithTag("choice_details_toggle", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("choice_details_toggle", useUnmergedTree = true).performTouchInput { click(center) }
                compose.mainClock.advanceTimeByFrame()
                toggled = true
            }
            // 맨 위(제목)부터 본 상태에서 마지막 카드가 얼마나 아래에 있는지 잰다. 스크롤 위치는 사건이 바뀌어도 남으므로 먼저 0으로.
            scrollRange().value().takeIf { it > 0f }?.let { scrollContentBy(-it) }
            val viewport = compose.onNodeWithTag("decision_content").fetchSemanticsNode().boundsInRoot
            val keys = decision.choices.map { it.key }
            val cards = keys.map { cardBounds(it) }
            val belowFold = cards.last().bottom - viewport.bottom
            val belowFoldDp = belowFold / density()
            cards.zipWithNext().forEachIndexed { i, (a, b) ->
                if (a.bottom > b.top + 0.5f) found += "카드 ${keys[i]}·${keys[i + 1]} 겹침 ${(a.bottom - b.top) / density()}dp"
            }
            cards.forEachIndexed { i, c ->
                if (c.left < root.left - 0.5f || c.right > root.right + 0.5f) found += "카드 ${keys[i]} 가로로 화면 밖"
            }
            var maxLabelLines = 0
            keys.forEachIndexed { i, key ->
                textNodesUnder("decision_choice_$key").forEach { (node, layout) ->
                    val b = unclipped(node)
                    val c = cards[i]
                    val name = node.config.getOrNull(SemanticsProperties.TestTag) ?: layout.layoutInput.text.text.take(12)
                    // 결정 화면 글자에는 maxLines · 말줄임이 없어 잘림은 "부모(카드 · 화면) 밖으로 나감"으로만 생긴다.
                    // (GetTextLayoutResult의 hasVisualOverflow는 내용 폭에 맞춘 한 줄 글자도 넘침으로 돌려줘 판정에 쓰지 않는다.)
                    if (b.left < c.left - 0.5f || b.right > c.right + 0.5f || b.top < c.top - 0.5f || b.bottom > c.bottom + 0.5f) {
                        found += "카드 $key 글자가 카드 밖: $name"
                    }
                    if (name == "choice_label_$key") maxLabelLines = maxOf(maxLabelLines, layout.lineCount)
                }
            }
            listOf("decision_title", "decision_body").forEach { tag ->
                compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
                    val b = unclipped(node)
                    if (b.right > root.right + 0.5f || b.left < root.left - 0.5f) found += "$tag 가로로 화면 밖"
                }
            }
            if (event.eventId in heaviest) save("small_${mode}_${event.eventId}_top.png")
            // 마지막 선택지까지 스크롤로 닿고(스크롤 범위 ≥ 화면 아래로 나간 만큼), 카드 하나가 화면보다 크지 않다.
            val range = scrollRange().maxValue()
            if (belowFold > range + 0.5f) found += "마지막 카드가 스크롤로 닿지 않음 (${"%.0f".format((belowFold - range) / density())}dp 모자람)"
            cards.forEachIndexed { i, c -> if (c.height > viewport.height + 0.5f) found += "카드 ${keys[i]}가 화면보다 큼" }
            if (event.eventId in heaviest && belowFold > 0.5f) {
                scrollContentBy(belowFold)
                save("small_${mode}_${event.eventId}_last.png")
                val last = cardBounds(keys.last())
                if (last.bottom > viewport.bottom + 0.5f) found += "스크롤 뒤에도 마지막 카드 아래가 잘림"
            }
            report.append("${event.eventId}\t${keys.size}\t$maxLabelLines\t${"%.0f".format(belowFoldDp)}\t${belowFoldDp > 0.5f}\t${"%.0f".format(autoScrollDp)}\t${found.joinToString("; ")}\n")
            issues += found.map { "${event.eventId}: $it" }
        }
        File(outputDir(), "layout_report_$mode.tsv").writeText(report.toString())
        assertTrue("작은 화면 배치 문제 ${issues.size}건: ${issues.take(20)}", issues.isEmpty())
    }

    /** 본문 출력 → 그림 선노출 → 선택지까지 넘긴다. 큰 사건이면 팝업부터 닫는다. */
    private fun reachChoices() {
        advance(2_000)
        if (compose.onAllNodesWithTag("incident_popup").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("사건 보기").performClick()
            advance(400)
        }
        if (compose.onAllNodesWithTag("decision_body", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("decision_body", useUnmergedTree = true).performTouchInput { click(center) }
            advance(TAP_GUARD_MILLIS + 50)
            advance(PREVIEW_DELAY_MILLIS + 16)
        }
        if (compose.onAllNodesWithTag("scene_preview").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("scene_preview").performTouchInput { click(center) }
            advance(TAP_GUARD_MILLIS + 50)
        }
    }

    /** 화면 밖(스크롤 아래)에 있어도 잘리지 않은 실제 위치 · 크기. boundsInRoot는 부모에 잘린 값이라 쓰지 않는다. */
    private fun unclipped(node: SemanticsNode): Rect = Rect(node.positionInRoot, node.size.toSize())

    private fun cardBounds(key: String): Rect = unclipped(compose.onNodeWithTag("decision_choice_$key").fetchSemanticsNode())

    private fun scrollRange(): ScrollAxisRange =
        compose.onNodeWithTag("decision_content").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]

    /** performScrollTo는 시계를 멈춘 테스트에서 스크롤 애니메이션을 끝없이 기다리므로, 스크롤 동작을 직접 부르고 시계를 넘긴다. */
    private fun scrollContentBy(dy: Float) {
        compose.onNodeWithTag("decision_content").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
        advance(1_000)
    }

    private fun textNodesUnder(tag: String): List<Pair<SemanticsNode, TextLayoutResult>> =
        compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node -> layoutOf(node)?.let { node to it } }

    private fun layoutOf(node: SemanticsNode): TextLayoutResult? {
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val results = mutableListOf<TextLayoutResult>()
        action(results)
        return results.firstOrNull()
    }

    private fun density() = context.resources.displayMetrics.density

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun outputDir(): File = requireNotNull(context.getExternalFilesDir("small-20261109")).also { it.mkdirs() }

    private fun save(name: String) {
        File(outputDir(), name).outputStream().use {
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
