package com.example.lifegame.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.screens.CUTSCENE_FADE_MILLIS
import com.example.lifegame.ui.screens.CUTSCENE_HOLD_MILLIS
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.DOLJABI_EVENT_ID
import com.example.lifegame.ui.screens.DoljabiCutsceneScreen
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.TitleCollectionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-13 김재겸 과제(화면 흐름 · 시연 조작 · 커스터마이징)의 기기 테스트.
 * 돌잡이 컷신 표시 · 건너뛰기 · 완료 · 복원, 커스터마이징 선택값 저장 · 복원, 주사위 결과 줄(최종값 · 증감만, 11/20 변경),
 * 도전 코드 입력 · 오류, 결말 계기 카드 · 회상 · 다시 만난 사람, 칭호 목표.
 * 실제 ViewModel을 쓰는 테스트는 테스트 전용 이름의 저장소를 쓰고 끝나면 지운다(실제 저장 파일은 건드리지 않음).
 */
@RunWith(AndroidJUnit4::class)
class Week13ScreenFlowTest {
    @get:Rule val compose = createComposeRule()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week13_flow_${System.nanoTime()}_"
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

    private fun setScreen(fontScale: Float = 1f, width: Int = 360, height: Int = 740, content: @Composable () -> Unit) {
        compose.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = fontScale)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(width.dp, height.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    // ---- 돌잡이 컷신 ----

    @Test
    fun doljabiCutsceneShowsTheWholeSceneAndFinishesOnItsOwnOnce() {
        val finished = mutableListOf<CutsceneStatus>()
        var started = 0
        compose.mainClock.autoAdvance = false
        setScreen { DoljabiCutsceneScreen(playerName = "재겸", onStart = { started++ }, onFinish = { finished += it }) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, started)
        compose.onNodeWithTag("doljabi_cutscene").assertIsDisplayed()
        compose.onNodeWithTag("doljabi_cutscene_caption").assertTextContains("오늘은 재겸의 첫 생일이다.")
        compose.onNodeWithTag("doljabi_cutscene_skip").assertIsDisplayed()
        // 그림이 다른 스레드에서 읽히도록 실제 시간을 잠깐 준 뒤 캡처한다.
        Thread.sleep(300)
        advance(CUTSCENE_FADE_MILLIS)
        save("w13_doljabi_cutscene.png")
        assertTrue("2초 전에는 끝나지 않는다", finished.isEmpty())
        advance(CUTSCENE_HOLD_MILLIS + 16)
        assertEquals(listOf(CutsceneStatus.COMPLETED), finished)
        // 이미 끝난 뒤의 탭은 다시 알리지 않는다.
        compose.onRoot().performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(listOf(CutsceneStatus.COMPLETED), finished)
    }

    @Test
    fun doljabiCutsceneTapCompletesAndSkipSkips() {
        val finished = mutableListOf<CutsceneStatus>()
        compose.mainClock.autoAdvance = false
        setScreen { DoljabiCutsceneScreen(playerName = "", onStart = {}, onFinish = { finished += it }) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("doljabi_cutscene_caption").assertTextContains("오늘은 첫 생일이다.")
        compose.onNodeWithTag("doljabi_cutscene_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(listOf(CutsceneStatus.SKIPPED), finished)
        advance(CUTSCENE_FADE_MILLIS + CUTSCENE_HOLD_MILLIS + 16)
        assertEquals("건너뛴 뒤 자동 끝이 다시 기록되지 않는다", listOf(CutsceneStatus.SKIPPED), finished)
    }

    /**
     * 새 인생 → 첫 결정(돌잡이) 앞에서 컷신 → 건너뛰기 → 결정 화면(같은 그림 선노출 없이 본문부터).
     * 컷신 상태는 저장되고, 앱을 다시 열어 이어하면 컷신을 다시 보여 주지 않는다. 컷신은 능력치를 바꾸지 않는다.
     */
    @Test
    fun newLifeShowsTheCutsceneBeforeTheFirstDecisionAndRestoresItsState() {
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null))
        }
        val play = requireNotNull(vm.play)
        assertEquals(PlayPhase.DECISION, play.phase)
        assertEquals(DOLJABI_EVENT_ID, play.decision?.eventId)
        assertEquals(CutsceneStatus.NOT_STARTED, play.presentation.cutscene)
        val statsBefore = play.stats
        compose.mainClock.autoAdvance = false
        setScreen { PlayHost(gameViewModel = vm, onGoHome = {}) }
        advance(400) // 화면 전환 직후 터치 보호
        compose.onNodeWithTag("doljabi_cutscene").assertIsDisplayed()
        assertEquals(CutsceneStatus.PLAYING, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)
        compose.onNodeWithTag("doljabi_cutscene_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithTag("doljabi_cutscene").assertCountEquals(0)
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(vm.play).presentation.cutscene)
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(V4ProgressStore(app).load()).presentation.cutscene)
        assertEquals("컷신은 능력치를 바꾸지 않는다", statsBefore, requireNotNull(vm.play).stats)
        // 같은 돌잡이 그림을 선노출로 한 번 더 보여 주지 않고 본문부터.
        advance(16)
        compose.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        compose.onNodeWithTag("decision_body").assertExists()

        // 앱을 다시 열어 이어하면 컷신 없이 결정 화면.
        lateinit var restarted: LifeGameViewModel
        compose.runOnUiThread { restarted = LifeGameViewModel(app).apply { continueLife() } }
        assertEquals(CutsceneStatus.SKIPPED, requireNotNull(restarted.play).presentation.cutscene)
        assertFalse(
            com.example.lifegame.ui.screens.shouldPlayDoljabiCutscene(
                restarted.play?.decision?.eventId, restarted.play?.outcome != null, requireNotNull(restarted.play).presentation.cutscene,
            ),
        )
    }

    // ---- 캐릭터 선택: 주사위 계산식 · 도전 코드 · 커스터마이징 ----

    /** 도전 코드로 주사위를 정해 첫 줄(운동능력)이 눈 1 · 눈 3이 되게 한다. 나머지는 3 · 4(합 7, 보정 0). */
    private val fixedCode = ChallengeCode.encode(ChallengeStart(Gender.FEMALE, "1,3,3,4,3,4,3,4,3,4,3,4", seed = 20261113L))

    /** Finish the IME resize before finding the button's touch coordinates. Never bypass onApply. */
    private fun enterCodeAndTapApply(code: String) {
        compose.onNodeWithTag("challenge_input").performScrollTo().performTextInput(code)
        compose.onNodeWithTag("challenge_input").assertTextContains(code, substring = false)
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("challenge_apply").performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        compose.onNodeWithTag("challenge_apply").performTouchInput { click() }
    }

    private fun applyFixedChallengeCode() {
        enterCodeAndTapApply(fixedCode)
        // A missed tap must fail here, before later appearance or dice assertions mask its cause.
        compose.onNodeWithTag("challenge_message").assertTextContains("도전 코드의 출발선", substring = true)
    }

    @Test
    fun diceRowsLinkEachStatToItsEyesSumIncreaseAndFinalValue() {
        compose.mainClock.autoAdvance = true
        setScreen { CharacterSelectScreen(starters = emptyList(), onStart = {}) }
        applyFixedChallengeCode()
        compose.onNodeWithTag("challenge_message").assertExists()
        // 2026-11-20 과제: 줄마다 "눈 + 눈 = 합 · 기본 ± 증가량 = 최종값". 숫자는 로직 결과 그대로다(규칙은 김우영 담당).
        val rows = startingStatRows(checkNotNull(StartingRoll.decode("1,3,3,4,3,4,3,4,3,4,3,4")))
        compose.onNodeWithTag("dice_rule").performScrollTo().assertIsDisplayed()
        rows.forEach { expected ->
            val row = compose.onNodeWithTag("dice_row_${expected.label}").performScrollTo()
            row.assertIsDisplayed()
            val spoken = row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
            assertEquals(expected.speech, spoken)
            compose.onNodeWithTag("dice_formula_${expected.label}", useUnmergedTree = true).assertTextEquals(expected.formula)
        }
        // 3D 위에는 지금 보이는 눈의 능력치 이름이 붙고(처음은 운동능력), 줄을 누르면 그 능력치의 눈으로 바뀐다.
        compose.onNodeWithTag("dice_3d_label").performScrollTo().assertTextEquals("운동능력의 두 눈")
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 1, 주사위 3, 합계 4")
        compose.onNodeWithTag("dice_row_지력").performScrollTo().performClick()
        compose.onNodeWithTag("dice_3d_label").performScrollTo().assertTextEquals("지력의 두 눈")
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("지력: 주사위 3, 주사위 4, 합계 7")
        compose.onNodeWithTag("dice_row_지력").assertIsSelected()
        compose.onNodeWithTag("dice_row_운동능력").assertIsNotSelected()
        // 줄을 눌러도 다시 굴리지 않는다: 코드로 정한 출발선은 그대로이고 재굴림 버튼도 생기지 않는다.
        compose.onNodeWithTag("dice_no_reroll").assertExists()
        compose.onNodeWithTag("dice_formula_운동능력", useUnmergedTree = true).assertTextEquals(rows.first().formula)
        save("w13_dice_values.png")
    }

    @Test
    fun aWrongChallengeCodeShowsAnErrorAndKeepsTheScreen() {
        compose.mainClock.autoAdvance = true
        setScreen { CharacterSelectScreen(starters = emptyList(), onStart = {}) }
        compose.onNodeWithTag("challenge_apply").performScrollTo().performClick()
        compose.onNodeWithTag("challenge_error").assertTextContains("코드를 입력해 주세요.")
        val broken = fixedCode.dropLast(1) + if (fixedCode.last() == 'A') "B" else "A"
        enterCodeAndTapApply(broken)
        compose.onNodeWithTag("challenge_error").assertTextContains("코드 일부가 틀렸습니다", substring = true)
        compose.onAllNodesWithTag("dice_row_운동능력").assertCountEquals(0)
    }

    @Test
    fun chosenHairAndOutfitColorsGoIntoTheNewLifeAndSurviveTheSave() {
        var started: NewLifeProfile? = null
        compose.mainClock.autoAdvance = true
        setScreen { CharacterSelectScreen(starters = emptyList(), onStart = { started = it }) }
        applyFixedChallengeCode()
        compose.onNodeWithTag("option_hair_color_blond").performScrollTo().performClick()
        compose.onNodeWithTag("option_outfit_green").performScrollTo().performClick()
        // 미리보기 세 장(영아 · 청소년 · 중년)에 같은 설명이 붙고, 노년은 흰머리라 염색 표시가 없다.
        assertEquals(3, compose.onAllNodes(hasContentDescription("금발 염색", substring = true)).fetchSemanticsNodes().size)
        assertEquals(4, compose.onAllNodes(hasContentDescription("초록 옷", substring = true)).fetchSemanticsNodes().size)
        compose.onNodeWithTag("appearance_preview").performScrollTo()
        Thread.sleep(300) // 레이어 그림을 읽고 색을 입히는 시간
        compose.waitForIdle()
        save("w13_customization.png")
        compose.onNodeWithText("이 인생 시작하기").performClick()
        val profile = requireNotNull(started)
        assertEquals("hair_color_blond", profile.avatar.hairColorId)
        assertEquals("outfit_green", profile.avatar.outfitColorId)
        assertEquals(20261113L, profile.runSeed)

        // 실제 ViewModel로 시작해 저장한 뒤 다시 열어도 같은 외형 · 같은 시드다.
        lateinit var vm: LifeGameViewModel
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(profile)
        }
        val saved = requireNotNull(V4ProgressStore(app).load())
        assertEquals("hair_color_blond", saved.avatar.hairColorId)
        assertEquals("outfit_green", saved.avatar.outfitColorId)
        assertEquals(20261113L, saved.state.runSeed)
        lateinit var restarted: LifeGameViewModel
        compose.runOnUiThread { restarted = LifeGameViewModel(app).apply { continueLife() } }
        val look = requireNotNull(restarted.play).avatar
        assertEquals("hair_color_blond", look.hairColorId)
        assertEquals("outfit_green", look.outfitColorId)
    }

    @Test
    fun characterScreenFitsASmallScreenWithLargeText() {
        compose.mainClock.autoAdvance = true
        setScreen(fontScale = 2.0f, width = 360, height = 640) { CharacterSelectScreen(starters = emptyList(), onStart = {}) }
        applyFixedChallengeCode()
        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width
        listOf("dice_row_운동능력", "dice_row_운", "option_outfit_purple", "option_hair_color_ash", "challenge_code").forEach { tag ->
            val node = compose.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode()
            val right = node.positionInRoot.x + node.size.width
            assertTrue("$tag 가 화면 오른쪽 밖으로 나간다: $right > $rootWidth", right <= rootWidth + 1)
        }
        compose.onNodeWithTag("dice_row_운동능력").performScrollTo().assertIsDisplayed()
        save("w13_character_small_200.png")
    }

    // ---- 결말 · 칭호 확장 ----

    private fun recordWith(
        recalls: List<RecallLine> = emptyList(),
        turningPoints: List<TurningPointUi> = emptyList(),
        npcs: List<NpcReturnUi> = emptyList(),
    ): LifeRecordUi = LifeRecordUi(
        runId = "run-ext",
        playerName = "재겸",
        title = null,
        summary = EndingSummaryUi(
            headline = "평범하게 빛난 사람",
            endingSummary = "요약",
            pathLine = "사람과 함께한 삶 · 사회성 80",
            lifeLine = "재겸 · 사람과 함께한 삶",
            death = null,
            endedAt = "노년기 5번째 해",
            educationCareer = emptyList(),
            familyLife = emptyList(),
            achievements = emptyList(),
            topStats = emptyList(),
            choices = emptyList(),
        ),
        avatar = AvatarLook(period = LifePeriod.OLD_AGE),
        finalPeriodName = "노년기",
        recap = emptyList(),
        backdrop = emptyList(),
        pathTitle = "사람과 함께한 삶",
        usesPhotoFace = false,
        recalls = recalls,
        turningPoints = turningPoints,
        npcsMetAgain = npcs,
    )

    @Test
    fun certificateShowsTurningPointsRecallsAndPeopleOnlyFromTheRecord() {
        val record = recordWith(
            recalls = listOf(RecallLine("recall_dol_music", "돌잡이 때 피아노 장난감을 꼭 쥐던 그 손이었다.")),
            turningPoints = listOf(
                TurningPointUi("tp_edu_high_school", "교육", "teen_019", "고등학교 원서", "청소년기", 2, "인문계", "열여섯 살, 원서 칸에 '인문계'를 적었다."),
                TurningPointUi("tp_family_proposal", "가족", "love_marriage_talk_01", "결혼이라는 단어", "청년기", 4, "결혼한다", "'우리도 결혼 생각해 볼까?'라는 말에 '결혼한다'."),
            ),
            npcs = listOf(NpcReturnUi("npc_transfer_child", "급식실 전학생", "혼자 밥 먹는 전학생", "유년기", listOf("동창회"))),
        )
        compose.mainClock.autoAdvance = true
        setScreen { EndingFlowScreen(record = record, futureLook = null, onRequestFuture = {}, onCancelFuture = {}, onDone = {}, initialStep = EndingStep.CERTIFICATE) }
        compose.onNodeWithTag("certificate_turning_points").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("turning_point_tp_edu_high_school").assertTextContains("인문계", substring = true)
        compose.onNodeWithTag("certificate_recall_recall_dol_music").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("certificate_npc_npc_transfer_child").performScrollTo()
            .assertTextContains("급식실 전학생: 유년기 「혼자 밥 먹는 전학생」에서 만나 「동창회」에서 다시 만났다")
        save("w13_certificate_extensions.png")
    }

    @Test
    fun certificateDrawsNoExtensionWithoutRecords() {
        compose.mainClock.autoAdvance = true
        setScreen { EndingFlowScreen(record = recordWith(), futureLook = null, onRequestFuture = {}, onCancelFuture = {}, onDone = {}, initialStep = EndingStep.CERTIFICATE) }
        compose.onNodeWithTag("certificate").assertExists()
        compose.onAllNodesWithTag("certificate_turning_points").assertCountEquals(0)
        compose.onAllNodesWithTag("certificate_recalls").assertCountEquals(0)
        compose.onAllNodesWithTag("certificate_npcs").assertCountEquals(0)
    }

    @Test
    fun lockedTitlesShowAHintAndCanBeTargetedForTheNextLife() {
        val collection = TitleCollectionUi(
            listOf(
                TitleEntryUi("title_a", "끝까지 궁금했던 사람", "질문을 멈추지 않았다", earned = true, earnedBy = listOf("재겸")),
                TitleEntryUi("title_b", "칠각형 인간", "숨김", earned = false, earnedBy = emptyList(), goal = "일곱 능력치를 고르게 65 이상", hint = "어느 하나 포기하지 않은 사람만 닿는 모양이 있다."),
            ),
        )
        var target: String? = null
        var targetSet = 0
        compose.mainClock.autoAdvance = true
        setScreen {
            TitleCollectionScreen(
                collection = collection,
                onBack = {},
                targetTitleId = target,
                onSetTarget = { target = it; targetSet++ },
            )
        }
        compose.onNodeWithTag("title_entry_title_b").assertTextContains("힌트 · 어느 하나 포기하지 않은 사람만 닿는 모양이 있다.", substring = true)
        compose.onAllNodesWithTag("title_target_title_a").assertCountEquals(0)
        compose.onNodeWithTag("title_target_title_b").performClick()
        assertEquals(1, targetSet)
        assertEquals("title_b", target)
        save("w13_title_goals.png")
    }

    private fun save(fileName: String) {
        compose.mainClock.advanceTimeByFrame()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(base.getExternalFilesDir("week13-jaegyeom"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream -> assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
        assertNotNull(bitmap)
    }
}
