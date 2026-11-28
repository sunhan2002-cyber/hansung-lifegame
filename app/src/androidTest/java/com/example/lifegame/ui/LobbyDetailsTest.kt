package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.FooterIconStyle
import com.example.lifegame.ui.components.LobbyTimeStyle
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.NewBadgeStyle
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.components.TitleSeenStore
import com.example.lifegame.ui.components.TitleUnlockStyle
import com.example.lifegame.ui.components.rememberHasNewTitles
import com.example.lifegame.ui.components.rememberNewTitleIds
import com.example.lifegame.ui.components.rememberTitleSeenState
import com.example.lifegame.ui.screens.HomeLightKey
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.screens.LobbyClock
import com.example.lifegame.ui.screens.LobbyFooter
import com.example.lifegame.ui.screens.LobbyIcon
import com.example.lifegame.ui.screens.LobbyIntroMemory
import com.example.lifegame.ui.screens.LobbyTimeBucket
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.screens.TitleCollectionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 로비 디테일(lobby2): 아래 아이콘 · 새 칭호 점 · 칭호 수집의 '새로 받음' · 시간대 로비 · 연출 비교 화면. */
@RunWith(AndroidJUnit4::class)
class LobbyDetailsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before @After fun clearStores() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
        context.deleteSharedPreferences(TitleSeenStore.PREFERENCES_NAME)
    }

    private fun screen(motion: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            LifeGameTheme {
                // Pin the animator scale too, so "motion on" really runs the animated path on any device.
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides if (motion) 1f else 0f,
                ) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    private fun life(runId: String, titleId: String?) = CompletedLifeRecord(
        runId = runId, playerName = "민서", summary = "지나온 삶", endingId = null, endingTitle = null, endingSummary = null,
        awardedTitle = titleId?.let { AwardedTitle(it, "칭호 $it", "설명 $it") },
        finalState = PeriodRunState(runId, 41L, LifePeriod.OLD_AGE, isEnding = true), choices = emptyList(),
    )

    private fun lightOf(tag: String = "home_scene") = compose.onNodeWithTag(tag).fetchSemanticsNode().config[HomeLightKey]

    @Test fun everyFooterIconStyleClicksEachFooterExactlyOncePerTapWithMotionOn() {
        val clicks = mutableMapOf<String, Int>()
        screen(motion = true) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FooterIconStyle.entries.forEach { iconStyle ->
                    CompositionLocalProvider(LocalMotionStyle provides MotionStyle.Default.copy(footerIcon = iconStyle)) {
                        Row(Modifier.fillMaxWidth()) {
                            LobbyIcon.entries.forEach { icon ->
                                val tag = "footer_${iconStyle.name}_${icon.name}"
                                LobbyFooter(icon.name, tag, icon, stacked = true, Modifier.weight(1f), showNewDot = icon == LobbyIcon.MEDAL) {
                                    clicks[tag] = (clicks[tag] ?: 0) + 1
                                }
                            }
                        }
                        // The large-text layout: icon left of the label.
                        LobbyFooter("칭호 수집", "footer_${iconStyle.name}_wide", LobbyIcon.MEDAL, stacked = false, Modifier.fillMaxWidth()) {
                            clicks["footer_${iconStyle.name}_wide"] = (clicks["footer_${iconStyle.name}_wide"] ?: 0) + 1
                        }
                    }
                }
            }
        }
        val tags = FooterIconStyle.entries.flatMap { style -> LobbyIcon.entries.map { "footer_${style.name}_${it.name}" } + "footer_${style.name}_wide" }
        // Two quick taps each: every press restarts the icon's motion, and each tap still clicks exactly once.
        tags.forEach { tag -> repeat(2) { compose.onNodeWithTag(tag).performScrollTo().performClick() } }
        compose.runOnIdle { tags.forEach { tag -> assertEquals(tag, 2, clicks[tag]) } }
    }

    @Test fun theNewTitleDotOnlyAddsAStateDescription() {
        var titles = 0
        var hasNew by mutableStateOf(true)
        LobbyIntroMemory.playedThisProcess = false
        screen(motion = true) { HomeScreen(null, emptyList(), {}, {}, onOpenTitles = { titles++ }, hasNewTitles = hasNew) }
        compose.onNodeWithTag("home_titles").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "새 칭호 있음"))
        // The label stays the button's only text: icons and the dot are drawn.
        compose.onNodeWithTag("home_titles").assertTextEquals("칭호 수집")
        compose.onNodeWithTag("home_titles").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, titles) }
        compose.runOnIdle { hasNew = false }
        compose.onNodeWithTag("home_titles").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithTag("home_titles").assertTextEquals("칭호 수집")
    }

    @Test fun theCollectionPutsNewTitlesFirstWithANewMarkAndKeepsTodaysOrderOtherwise() {
        val collection = TitleCollectionUi(listOf(
            TitleEntryUi("old", "오래된 칭호", "예전에 받았다", earned = true, earnedBy = listOf("가")),
            TitleEntryUi("locked", "못 받은 칭호", "목표", earned = false, earnedBy = emptyList()),
            TitleEntryUi("fresh", "새 칭호", "방금 받았다", earned = true, earnedBy = listOf("나")),
        ))
        var newIds by mutableStateOf(setOf("fresh"))
        screen(motion = true) { TitleCollectionScreen(collection, {}, newTitleIds = newIds) }
        fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top
        assertTrue("the new title comes first", top("title_entry_fresh") < top("title_entry_old"))
        compose.onNodeWithTag("title_entry_fresh").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "새로 받음"))
        compose.onNodeWithTag("title_entry_old").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "받음"))
        compose.onNodeWithTag("earned_badge_fresh", useUnmergedTree = true).assertExists()
        // Without new titles: today's order and semantics.
        compose.runOnIdle { newIds = emptySet() }
        assertTrue(top("title_entry_old") < top("title_entry_fresh"))
        compose.onNodeWithTag("title_entry_fresh").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "받음"))
    }

    @Test fun theSeenStoreSeedsOnTheFirstLobbyAndClearsWhenTheCollectionOpens() {
        var lives by mutableStateOf(listOf(life("a", "kept")))
        var page by mutableStateOf("home")
        var hasNew: Boolean? = null
        var newIds: Set<String>? = null
        compose.setContent {
            val seen = rememberTitleSeenState()
            key(page) {
                if (page == "home") hasNew = rememberHasNewTitles(seen, lives) else newIds = rememberNewTitleIds(seen, lives)
            }
        }
        // An existing player's titles are seeded as seen: no spurious dot.
        compose.runOnIdle { assertEquals(false, hasNew) }
        assertEquals(setOf("kept"), TitleSeenStore(context).read())
        compose.runOnIdle { lives = listOf(life("b", "fresh"), life("a", "kept"), life("c", null)) }
        compose.runOnIdle { assertEquals(true, hasNew) }
        // Opening the collection snapshots the new titles for this visit, then marks everything seen.
        compose.runOnIdle { page = "titles" }
        compose.runOnIdle { assertEquals(setOf("fresh"), newIds) }
        assertEquals(setOf("kept", "fresh"), TitleSeenStore(context).read())
        compose.runOnIdle { page = "home" }
        compose.runOnIdle { assertEquals(false, hasNew) }
    }

    @Test fun nothingIsStoredBeforeTheFirstLobby() {
        assertNull(TitleSeenStore(context).read())
    }

    @Test fun theLobbyLightFollowsTheHourAndTheDefaultIsTodaysDay() {
        var hour by mutableStateOf(22f)
        var style by mutableStateOf(MotionStyle.Default)
        screen(motion = false) {
            CompositionLocalProvider(LocalMotionStyle provides style) { HomeScreen(null, emptyList(), {}, {}, hourOfDay = hour) }
        }
        assertEquals(LobbyTimeBucket.NIGHT.name, lightOf())
        compose.runOnIdle { hour = 7.5f }
        assertEquals(LobbyTimeBucket.MORNING.name, lightOf())
        compose.runOnIdle { hour = LobbyClock.NEUTRAL_HOUR }
        assertEquals(LobbyTimeBucket.DAY.name, lightOf())
        // NONE keeps today's lobby at night too.
        compose.runOnIdle { hour = 22f; style = MotionStyle.Default.copy(lobbyTime = LobbyTimeStyle.NONE) }
        assertEquals(LobbyTimeBucket.DAY.name, lightOf())
    }

    @Test fun directHomeScreenCallersSeeTodaysDaytimeLobby() {
        screen(motion = true) { HomeScreen(null, emptyList(), {}, {}) }
        assertEquals(LobbyTimeBucket.DAY.name, lightOf())
    }

    @Test fun theAppReadsTheInjectedLobbyClock() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "lobby_details_${System.nanoTime()}_"
        val names = mutableSetOf<String>()
        val app = object : Application() {
            init { attachBaseContext(base) }
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences(prefix + name, mode).also { names += prefix + name }
        }
        try {
            lateinit var vm: LifeGameViewModel
            compose.runOnUiThread { vm = LifeGameViewModel(app) }
            compose.setContent { LifeGameApp(gameViewModel = vm, lobbyClock = LobbyClock.fixed(22f)) }
            compose.waitUntil(8_000) { compose.onAllNodesWithTag("home_scene").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(LobbyTimeBucket.NIGHT.name, lightOf())
        } finally {
            names.forEach {
                base.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
                base.deleteSharedPreferences(it)
            }
        }
    }

    @Test fun theComparisonScreenPreviewsAndAppliesEveryLobbyDetail() {
        val default = MotionStyle.Default
        screen(motion = false) { ProvideMotionStyle(allowOverride = true) { MotionCompareScreen(onBack = {}) } }
        compose.onNodeWithTag("motion_group_lobby").performScrollTo().performClick()
        val footer = FooterIconStyle.entries.first { it != default.footerIcon }
        val badge = NewBadgeStyle.entries.first { it != default.newBadge }
        // A time style that really changes the sky (NONE would keep the daytime light at 7 h).
        val time = LobbyTimeStyle.entries.first { it != default.lobbyTime && it != LobbyTimeStyle.NONE }
        // The demo footers are real buttons.
        compose.onNodeWithTag("demo_footer_${footer.name}_gear").performScrollTo().performClick()
        compose.onNodeWithTag("footer_icon_${footer.name}_apply").performScrollTo().performClick()
        compose.onNodeWithTag("new_badge_${badge.name}_apply").performScrollTo().performClick()
        compose.onNodeWithTag("lobby_time_${time.name}").performScrollTo().performClick()
        compose.onNodeWithTag("lobby_time_hour_7").performScrollTo().performClick()
        compose.onNodeWithTag("lobby_time_readout").assertTextEquals("07:00 · 아침")
        assertEquals(LobbyTimeBucket.MORNING.name,
            compose.onNode(hasTestTag("home_scene") and hasAnyAncestor(hasTestTag("lobby_time_preview"))).fetchSemanticsNode().config[HomeLightKey])
        compose.onNodeWithTag("lobby_time_apply").performScrollTo().performClick()
        assertEquals(default.copy(footerIcon = footer, newBadge = badge, lobbyTime = time), MotionStyleStore(context).read())

        // Group 8: the popup over its small certificate. Motion is off, so it shows settled and keep removes it at once.
        compose.onNodeWithTag("motion_group_ending").performScrollTo().performClick()
        val unlock = TitleUnlockStyle.entries.first { it != default.titleUnlock && it != TitleUnlockStyle.NONE }
        compose.onNodeWithTag("title_unlock_${unlock.name}").performScrollTo().performClick()
        val popup = hasTestTag("title_unlock") and hasAnyAncestor(hasTestTag("title_unlock_preview"))
        compose.onNode(popup).assertExists()
        compose.onNodeWithTag("title_unlock_preview").performScrollTo()
        compose.onNode(hasTestTag("title_unlock_keep") and hasAnyAncestor(hasTestTag("title_unlock_preview"))).performClick()
        compose.onNode(popup).assertDoesNotExist()
        compose.onNodeWithTag("title_unlock_apply").performScrollTo().performClick()
        assertEquals(unlock, MotionStyleStore(context).read().titleUnlock)
    }
}
