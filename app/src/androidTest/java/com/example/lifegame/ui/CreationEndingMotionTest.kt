package com.example.lifegame.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarAssetLookKey
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.components.AppearancePopStyle
import com.example.lifegame.ui.components.CertificateSealStyle
import com.example.lifegame.ui.components.CompletionEffectProgress
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.LocalTouchExploration
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.ProfileEntranceStyle
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.components.RecapMomentStyle
import com.example.lifegame.ui.components.StatCountUpStyle
import com.example.lifegame.ui.components.TitleRevealStyle
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.screens.ProfileAppearancePreview
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.DicePanel
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage-2 bookends motion (새 인생 · 엔딩): every variant keeps taps, text and speech exactly as before, rests in the
 * same pixels as no motion, waits for the real events it follows (the drawn look, the landed dice), never cascades the
 * recap timer and never replays the certificate.
 */
@RunWith(AndroidJUnit4::class)
class CreationEndingMotionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before @After fun clearStoredStyle() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
    }

    /** Motion on unless [motion] is false. The animator scale is pinned, so a device with animations off still runs motion. */
    private fun screen(
        motion: Boolean = true,
        touchExploration: Boolean = false,
        height: Int = 740,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 1f,
                    LocalTouchExploration provides touchExploration,
                ) {
                    Box(Modifier.requiredSize(360.dp, height.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.bitmap(): Bitmap = captureToImage().asAndroidBitmap()

    /**
     * performScrollTo() keeps re-issuing an animated semantics scroll until the node is fully visible. With the main
     * clock paused that animation never runs, so the call never returns: let the clock run just for the scroll.
     */
    private fun SemanticsNodeInteraction.scrollToWithRunningClock(): SemanticsNodeInteraction {
        val paused = !compose.mainClock.autoAdvance
        compose.mainClock.autoAdvance = true
        try {
            performScrollTo()
        } finally {
            if (paused) compose.mainClock.autoAdvance = false
        }
        return this
    }

    // ---- profileEntrance --------------------------------------------------------------------------------

    @Test fun everyProfileEntranceKeepsTapsLandingWhileItPlays() {
        var style by mutableStateOf(ProfileEntranceStyle.NONE)
        compose.mainClock.autoAdvance = false
        screen {
            key(style) {
                CompositionLocalProvider(LocalMotionStyle provides MotionStyle(profileEntrance = style, statCountUp = StatCountUpStyle.NONE)) {
                    CharacterSelectScreen(starters = emptyList(), onStart = {}, dice3D = false)
                }
            }
        }
        for (entrance in ProfileEntranceStyle.entries) {
            compose.runOnIdle { style = entrance }
            compose.mainClock.advanceTimeByFrame()
            // Mid-entrance: the gender and dice groups are still fading in (or not even started).
            compose.mainClock.advanceTimeBy(120)
            compose.onAllNodesWithText("새 인생").assertCountEquals(1)
            compose.onNodeWithText("남").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText("남").assertIsSelected()
            // Visible already (its lower edge touches the viewport); performScrollTo would spin with the clock paused.
            compose.onNodeWithTag("dice_roll").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("dice_reroll").assertExists()
            compose.onNodeWithText("이 인생 시작하기").assertExists()
        }
    }

    @Test fun theDiceGroupIsOpaqueAndInPlaceFromTheFrameTheRollAppears() {
        compose.mainClock.autoAdvance = false
        screen {
            CompositionLocalProvider(LocalMotionStyle provides MotionStyle(profileEntrance = ProfileEntranceStyle.RISE, statCountUp = StatCountUpStyle.NONE)) {
                CharacterSelectScreen(starters = emptyList(), onStart = {}, dice3D = false)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        // The dice group would start rising at 380 ms; roll long before that.
        compose.mainClock.advanceTimeBy(64)
        // Visible already (its lower edge touches the viewport); performScrollTo would spin with the clock paused.
        compose.onNodeWithTag("dice_roll").performClick()
        compose.mainClock.advanceTimeByFrame()
        val stage = compose.onNodeWithTag("dice_3d_stage").bitmap()
        val row = compose.onNodeWithTag("dice_row_운동능력").bitmap()
        val position = compose.onNodeWithTag("dice_panel").fetchSemanticsNode().positionInRoot
        compose.mainClock.advanceTimeBy(1_500)
        assertEquals(position, compose.onNodeWithTag("dice_panel").fetchSemanticsNode().positionInRoot)
        assertTrue("the stage never fades or moves once it holds the dice", stage.sameAs(compose.onNodeWithTag("dice_3d_stage").bitmap()))
        assertTrue(row.sameAs(compose.onNodeWithTag("dice_row_운동능력").bitmap()))
    }

    @Test fun reducedMotionAndTouchExplorationShowTheSettledProfileFromTheFirstFrame() {
        var mode by mutableIntStateOf(0) // 0: reduced motion, 1: touch exploration
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = mode == 1, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 1f,
                    LocalTouchExploration provides (mode == 1),
                ) {
                    key(mode) {
                        Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                            CharacterSelectScreen(starters = emptyList(), onStart = {}, dice3D = false)
                        }
                    }
                }
            }
        }
        for (next in 0..1) {
            compose.runOnIdle { mode = next }
            compose.mainClock.advanceTimeByFrame()
            val title = compose.onNodeWithText("새 인생").bitmap()
            val dice = compose.onNodeWithTag("dice_panel").bitmap()
            compose.mainClock.advanceTimeBy(1_500)
            assertTrue("mode $next: the title is plain from frame 1", title.sameAs(compose.onNodeWithText("새 인생").bitmap()))
            assertTrue("mode $next: the groups rest from frame 1", dice.sameAs(compose.onNodeWithTag("dice_panel").bitmap()))
        }
    }

    @Test fun theProfileEntranceDoesNotReplayAfterRecreation() {
        compose.mainClock.autoAdvance = false
        val restoration = ManualRestoreHost(compose)
        restoration.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 1f,
                    LocalTouchExploration provides false,
                    LocalMotionStyle provides MotionStyle(profileEntrance = ProfileEntranceStyle.RISE),
                ) {
                    Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                        CharacterSelectScreen(starters = emptyList(), onStart = {}, dice3D = false)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        val settled = compose.onNodeWithTag("dice_panel").bitmap()
        restoration.recreate()
        assertEquals(1, restoration.recreations)
        assertTrue("restored screens show up settled", settled.sameAs(compose.onNodeWithTag("dice_panel").bitmap()))
    }

    // ---- appearancePop ----------------------------------------------------------------------------------

    /** Advances frames (real time passes for the off-thread layer decode) until [count] avatars draw a look containing [part]. */
    private fun awaitAvatarLayers(count: Int, part: String) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            compose.mainClock.advanceTimeByFrame()
            val drawn = compose.onAllNodesWithTag("player_avatar_layers", useUnmergedTree = true).fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(AvatarAssetLookKey) }
            if (drawn.size == count && drawn.all { part in it }) return
            check(SystemClock.uptimeMillis() < deadline) { "avatar layers never showed '$part': $drawn" }
            Thread.sleep(16)
        }
    }

    @Test fun appearancePopFollowsTheDrawnLookUpdatesDescriptionsAtOnceAndRestsLikeNoPop() {
        var style by mutableStateOf(AppearancePopStyle.NONE)
        var hair by mutableStateOf("hair_default")
        compose.mainClock.autoAdvance = false
        screen {
            key(style) {
                Column {
                    CompositionLocalProvider(LocalMotionStyle provides MotionStyle(appearancePop = style)) {
                        Box(Modifier.testTag("popping")) { ProfileAppearancePreview(skinId = "skin_default", hairId = hair, eyesId = "eyes_default") }
                    }
                    CompositionLocalProvider(LocalMotionStyle provides MotionStyle(appearancePop = AppearancePopStyle.NONE)) {
                        Box(Modifier.testTag("still")) { ProfileAppearancePreview(skinId = "skin_default", hairId = hair, eyesId = "eyes_default") }
                    }
                }
            }
        }
        // Warm the layer cache for both looks, so the pop below starts right after the change.
        awaitAvatarLayers(8, AvatarLookLabels.SHORT)
        compose.runOnIdle { hair = "hair_long_black" }
        awaitAvatarLayers(8, AvatarLookLabels.LONG)
        for (pop in AppearancePopStyle.entries - AppearancePopStyle.NONE) {
            compose.runOnIdle { style = pop; hair = "hair_default" }
            awaitAvatarLayers(8, AvatarLookLabels.SHORT)
            assertTrue("$pop: the first display never pops", previewsMatch())
            compose.runOnIdle { hair = "hair_long_black" }
            compose.mainClock.advanceTimeByFrame()
            // Descriptions follow the requested look immediately, in all four ages of both previews.
            assertEquals(8, compose.onAllNodes(hasContentDescription(AvatarLookLabels.LONG, substring = true)).fetchSemanticsNodes().size)
            awaitAvatarLayers(8, AvatarLookLabels.LONG)
            var popped = false
            repeat(16) {
                if (!previewsMatch()) popped = true
                compose.mainClock.advanceTimeBy(48)
            }
            assertTrue("$pop: the change is answered with a pop", popped)
            compose.mainClock.advanceTimeBy(1_000)
            assertTrue("$pop: at rest it is pixel-identical to no pop", previewsMatch())
        }
    }

    private fun previewsMatch(): Boolean =
        compose.onNodeWithTag("popping").bitmap().sameAs(compose.onNodeWithTag("still").bitmap())

    private object AvatarLookLabels {
        const val SHORT = "짧은 검은 머리"
        const val LONG = "긴 생머리"
    }

    // ---- statCountUp ------------------------------------------------------------------------------------

    @Test fun statCountKeepsFormulaAndSpeechWaitsForTheLandingAndRestsLikeNoCount() {
        val first = checkNotNull(StartingRoll.decode("3,4,2,5,6,1,1,4,5,4,2,2"))
        val code = checkNotNull(StartingRoll.decode("1,3,3,4,3,4,1,4,3,4,3,4"))
        var style by mutableStateOf(StatCountUpStyle.NONE)
        var roll by mutableStateOf<StartingRoll?>(null)
        var nonce by mutableIntStateOf(0)
        compose.mainClock.autoAdvance = false
        screen {
            key(style) {
                CompositionLocalProvider(LocalMotionStyle provides MotionStyle(statCountUp = style)) {
                    DicePanel(roll = roll, rollNonce = nonce, tendency = null, rerollsLeft = 3,
                        onRoll = { roll = first; nonce += 1 }, allow3D = false)
                }
            }
        }
        fun rowImages() = listOf("운동능력", "지력").map { compose.onNodeWithTag("dice_formula_$it", useUnmergedTree = true).bitmap() }
        fun sameImages(a: List<Bitmap>, b: List<Bitmap>) = a.zip(b).all { (x, y) -> x.sameAs(y) }
        fun assertTextAndSpeech(of: StartingRoll) {
            startingStatRows(of).forEach { row ->
                compose.onNodeWithTag("dice_formula_${row.label}", useUnmergedTree = true).assertTextEquals(row.formula)
                val spoken = compose.onNodeWithTag("dice_row_${row.label}").fetchSemanticsNode()
                    .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
                assertEquals(row.speech, spoken)
            }
        }

        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("dice_roll").performClick()
        compose.mainClock.advanceTimeBy(2_000)
        val still = rowImages()

        for (count in StatCountUpStyle.entries - StatCountUpStyle.NONE) {
            compose.runOnIdle { roll = null; nonce = 0; style = count }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("dice_roll").performClick()
            compose.mainClock.advanceTimeByFrame()
            assertTextAndSpeech(first)
            // Static dice land at once, but the count still waits for the 490 ms roll floor.
            compose.mainClock.advanceTimeBy(300)
            assertFalse("$count: before the landing the rows do not show the final values yet", sameImages(still, rowImages()))
            assertTextAndSpeech(first)
            compose.mainClock.advanceTimeBy(500)
            assertTextAndSpeech(first)
            compose.mainClock.advanceTimeBy(800)
            assertTextAndSpeech(first)
            compose.mainClock.advanceTimeBy(800)
            assertTrue("$count: at rest the rows are pixel-identical to no count", sameImages(still, rowImages()))

            // A challenge code replaces the roll without a roll of the player's own: no count, values at once.
            compose.runOnIdle { roll = code }
            compose.mainClock.advanceTimeByFrame()
            val replaced = rowImages()
            assertTextAndSpeech(code)
            compose.mainClock.advanceTimeBy(1_500)
            assertTrue("$count: a code never counts", sameImages(replaced, rowImages()))
        }
    }

    // ---- recapMoment ------------------------------------------------------------------------------------

    private fun recapRecord(): LifeRecordUi = LifeRecordUi(
        runId = "bookends-recap",
        playerName = "민서",
        title = null,
        summary = endingSummary(),
        avatar = AvatarLook(period = LifePeriod.OLD_AGE),
        finalPeriodName = "노년기",
        recap = listOf(
            "infant" to LifePeriod.INFANT,
            "teen" to LifePeriod.TEEN,
            "old_age" to LifePeriod.OLD_AGE,
        ).mapIndexed { index, (scene, period) ->
            RecapMomentUi(
                key = "bookends_recap_$index",
                periodName = scene,
                yearInPeriod = index + 1,
                title = "장면 ${index + 1}",
                choiceLabel = "고른 선택 ${index + 1}",
                resultText = "그렇게 ${index + 1}번째 장면이 지나갔다.",
                images = listOf(ImageCandidate("bookends_recap_$index", "week10/scenes/img_week10_bg_$scene.png")),
                avatar = AvatarLook(period = period),
            )
        },
        backdrop = emptyList(),
        pathTitle = "배우고 연구한 삶",
        usesPhotoFace = false,
    )

    @Test fun noRecapMomentCascadesTheTimerWhileTestsWaitForIdle() {
        var style by mutableStateOf(RecapMomentStyle.NONE)
        val record = recapRecord()
        screen {
            key(style) {
                CompositionLocalProvider(LocalMotionStyle provides MotionStyle(recapMoment = style)) {
                    EndingFlowScreen(record, null, {}, {}, {})
                }
            }
        }
        for (moment in RecapMomentStyle.entries) {
            compose.runOnIdle { style = moment }
            compose.waitForIdle()
            compose.onNodeWithTag("recap_counter").assertTextEquals("1 / ${record.recap.size}")
            compose.onNodeWithText(record.recap.first().title).assertExists()
            compose.onNodeWithTag("recap_skip").assertHasClickAction()
        }
    }

    @Test fun theRecapStillAdvancesEveryTwoPointEightSeconds() {
        val record = recapRecord()
        compose.mainClock.autoAdvance = false
        screen {
            CompositionLocalProvider(LocalMotionStyle provides MotionStyle(recapMoment = RecapMomentStyle.DEVELOP)) {
                EndingFlowScreen(record, null, {}, {}, {})
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(2_700)
        compose.onNodeWithTag("recap_counter").assertTextEquals("1 / ${record.recap.size}")
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("recap_counter").assertTextEquals("2 / ${record.recap.size}")
    }

    // ---- certificateSeal · titleReveal -----------------------------------------------------------------

    private fun endingSummary() = EndingSummaryUi(
        headline = "평범하게 빛난 사람",
        endingSummary = "하루하루를 궁금해하며 살았다.",
        pathLine = "배우고 연구한 삶 · 지력 82",
        lifeLine = "민서 · 배우고 연구한 삶",
        death = null,
        endedAt = "노년기 5번째 해",
        educationCareer = emptyList(),
        familyLife = emptyList(),
        achievements = emptyList(),
        topStats = emptyList(),
        choices = emptyList(),
    )

    private fun certificateRecord(): LifeRecordUi = recapRecord().copy(
        runId = "bookends-certificate",
        title = TitleUi("bookends_title", "끝까지 궁금했던 사람", "질문을 멈추지 않았다"),
    )

    private fun progress() = compose.onNodeWithTag("completion_header").fetchSemanticsNode().config[CompletionEffectProgress]

    private fun openFreshCertificate() {
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("recap_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("future_next").scrollToWithRunningClock().performClick()
        compose.mainClock.advanceTimeByFrame()
    }

    /** The parts of the certificate that the reveal moves, without the off-thread avatar. */
    private fun certificateImages() = listOf("certificate_name", "certificate_title", "ending_headline")
        .map { compose.onNodeWithTag(it).bitmap() }

    @Test fun everySealAndTitleRevealKeepsTheHeaderTimelineAndTheTitleText() {
        val record = certificateRecord()
        val titleText = "칭호 · ${checkNotNull(record.title).text}"
        var style by mutableStateOf(MotionStyle.Default)
        compose.mainClock.autoAdvance = false
        screen {
            key(style) {
                CompositionLocalProvider(LocalMotionStyle provides style) {
                    EndingFlowScreen(record, GenerationUi(record.runId, GenerationStatus.FALLBACK), {}, {}, {})
                }
            }
        }
        for (seal in CertificateSealStyle.entries) for (title in TitleRevealStyle.entries) {
            compose.runOnIdle { style = MotionStyle(certificateSeal = seal, titleReveal = title) }
            openFreshCertificate()
            compose.onNodeWithTag("certificate_title").assertTextEquals(titleText)
            compose.mainClock.advanceTimeBy(96)
            compose.onNodeWithTag("certificate_title_badge").assertIsDisplayed()
            assertTrue("$seal/$title at 96 ms", progress() < 1f)
            compose.mainClock.advanceTimeBy(160)
            assertTrue("$seal/$title mid-reveal", progress() in 0.05f..0.95f)
            compose.mainClock.advanceTimeBy(250)
            compose.onNodeWithTag("certificate_title").assertTextEquals(titleText)
            compose.mainClock.advanceTimeBy(650)
            assertEquals(1f, progress(), .001f)
            compose.onNodeWithTag("certificate_title").assertTextEquals(titleText)
        }
    }

    @Test fun aReplayedCertificateIsAlreadySettledOnItsFirstFrame() {
        val record = certificateRecord()
        compose.mainClock.autoAdvance = false
        screen {
            CompositionLocalProvider(LocalMotionStyle provides MotionStyle(certificateSeal = CertificateSealStyle.STAMP, titleReveal = TitleRevealStyle.SPIN_STAMP)) {
                EndingFlowScreen(record, GenerationUi(record.runId, GenerationStatus.FALLBACK), {}, {}, {})
            }
        }
        openFreshCertificate()
        compose.mainClock.advanceTimeBy(1_500)
        val settled = certificateImages()
        compose.onNodeWithTag("certificate_replay").performClick()
        compose.mainClock.advanceTimeByFrame()
        openFreshCertificate()
        assertEquals(1f, progress(), .001f)
        assertTrue("주마등 다시 보기 → the certificate does not reveal again", settled.zip(certificateImages()).all { (a, b) -> a.sameAs(b) })
    }

    @Test fun reducedMotionShowsTheStampedCertificateFromTheFirstFrame() {
        val record = certificateRecord()
        compose.mainClock.autoAdvance = false
        screen(motion = false) {
            EndingFlowScreen(record, GenerationUi(record.runId, GenerationStatus.FALLBACK), {}, {}, {})
        }
        openFreshCertificate()
        assertEquals(1f, progress(), .001f)
        val first = certificateImages()
        compose.mainClock.advanceTimeBy(1_500)
        assertTrue(first.zip(certificateImages()).all { (a, b) -> a.sameAs(b) })
    }

    @Test fun underTouchExplorationTheCardIsThereAtOnceWhileTheHeaderStillSweeps() {
        val record = certificateRecord()
        compose.mainClock.autoAdvance = false
        screen(touchExploration = true) {
            EndingFlowScreen(record, GenerationUi(record.runId, GenerationStatus.FALLBACK), {}, {}, {})
        }
        openFreshCertificate()
        val first = certificateImages()
        compose.mainClock.advanceTimeBy(96)
        // The header's sweep is not an entrance (nothing starts transparent): it plays as before.
        assertTrue(progress() < 1f)
        compose.mainClock.advanceTimeBy(1_400)
        assertEquals(1f, progress(), .001f)
        assertTrue("a node announced by TalkBack is visible from the first frame", first.zip(certificateImages()).all { (a, b) -> a.sameAs(b) })
    }

    // ---- 연출 비교 ---------------------------------------------------------------------------------------

    @Test fun comparisonSectionsOfferEveryVariantAndApplyToTheWholeApp() {
        screen(motion = false, height = 640) { ProvideMotionStyle(allowOverride = true) { MotionCompareScreen(onBack = {}) } }
        fun choose(key: String, variant: Enum<*>) {
            compose.onNode(hasTestTag("${key}_${variant.name}")).performScrollTo().performClick()
            compose.onNodeWithTag("${key}_apply").performScrollTo().performClick()
        }
        fun assertOffers(key: String, entries: List<Enum<*>>) {
            entries.forEach { compose.onNodeWithTag("${key}_${it.name}").assertExists() }
            compose.onNodeWithTag("${key}_replay").assertExists()
            compose.onNodeWithTag("${key}_apply").assertExists()
            compose.onNodeWithTag("${key}_preview").assertExists()
        }

        compose.onNodeWithTag("motion_group_creation").performScrollTo().performClick()
        assertOffers(MotionStyleStore.PROFILE_ENTRANCE_KEY, ProfileEntranceStyle.entries)
        assertOffers(MotionStyleStore.APPEARANCE_POP_KEY, AppearancePopStyle.entries)
        assertOffers(MotionStyleStore.STAT_COUNT_UP_KEY, StatCountUpStyle.entries)
        // The real profile screen sits inside its own preview (its tags repeat elsewhere in the group).
        compose.onNode(hasTestTag("profile_content") and hasAnyAncestor(hasTestTag("${MotionStyleStore.PROFILE_ENTRANCE_KEY}_preview"))).assertExists()
        choose(MotionStyleStore.PROFILE_ENTRANCE_KEY, ProfileEntranceStyle.UNFOLD)
        choose(MotionStyleStore.APPEARANCE_POP_KEY, AppearancePopStyle.INK)
        choose(MotionStyleStore.STAT_COUNT_UP_KEY, StatCountUpStyle.SLOT)

        compose.onNodeWithTag("motion_group_ending").performScrollTo().performClick()
        assertOffers(MotionStyleStore.RECAP_MOMENT_KEY, RecapMomentStyle.entries)
        assertOffers(MotionStyleStore.CERTIFICATE_SEAL_KEY, CertificateSealStyle.entries)
        assertOffers(MotionStyleStore.TITLE_REVEAL_KEY, TitleRevealStyle.entries)
        compose.onNodeWithTag("${MotionStyleStore.CERTIFICATE_SEAL_KEY}_quiet").assertExists()
        compose.onNodeWithTag("${MotionStyleStore.TITLE_REVEAL_KEY}_popup").assertExists()
        compose.onNode(hasTestTag("certificate_title") and hasAnyAncestor(hasTestTag("${MotionStyleStore.CERTIFICATE_SEAL_KEY}_preview"))).assertExists()
        choose(MotionStyleStore.RECAP_MOMENT_KEY, RecapMomentStyle.DEVELOP)
        choose(MotionStyleStore.CERTIFICATE_SEAL_KEY, CertificateSealStyle.INK_BLEED)
        choose(MotionStyleStore.TITLE_REVEAL_KEY, TitleRevealStyle.SPIN_STAMP)

        compose.runOnIdle {
            assertEquals(
                MotionStyle.Default.copy(
                    profileEntrance = ProfileEntranceStyle.UNFOLD,
                    appearancePop = AppearancePopStyle.INK,
                    statCountUp = StatCountUpStyle.SLOT,
                    recapMoment = RecapMomentStyle.DEVELOP,
                    certificateSeal = CertificateSealStyle.INK_BLEED,
                    titleReveal = TitleRevealStyle.SPIN_STAMP,
                ),
                MotionStyleStore(context).read(),
            )
        }
    }
}
