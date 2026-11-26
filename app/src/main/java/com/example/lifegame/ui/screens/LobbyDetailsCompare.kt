/**
 * Area: lobby2 — its "연출 비교" sections.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls these; only their bodies change:
 * - `@Composable internal fun LobbyDetailsSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "4. 로비 디테일": footer icons, the new-title dot, the time-of-day lobby.
 * - `@Composable internal fun TitleUnlockSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — composed
 *   in group "8. 인생의 끝" after bookends' EndingSections.
 * - `@Composable internal fun MiniLobby(intro: LobbyIntroState)` — stage-1 EntranceComparison still calls `MiniLobby(intro)`;
 *   lobby2 only added parameters with defaults (hour, time style, the new-title dot, its footer tags).
 *
 * Tags: footer_icon_* and new_badge_* (VariantCards: `<key>_<VARIANT>`, `<key>_<VARIANT>_apply`, per-card replay
 * `new_badge_<VARIANT>_replay`), lobby_time_* and title_unlock_* (EffectComparison chips, `_replay`, `_apply`, extra
 * controls `<key>_<control>`). Previews that hold real components keep their production tags inside
 * `<key>_preview` boxes; the demo footers use `demo_*` tags. Fake data only: no ViewModel, no stored state.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.RecordContent
import com.example.lifegame.ui.TitleEntryUi
import com.example.lifegame.ui.buildLifeRecordUi
import com.example.lifegame.ui.components.CompletionSealHeader
import com.example.lifegame.ui.components.EntranceStyle
import com.example.lifegame.ui.components.FooterIconStyle
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LobbyTimeStyle
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.NewBadgeStyle
import com.example.lifegame.ui.components.TitleUnlockStyle
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import kotlin.math.floor
import kotlin.math.roundToInt

/** Group 4 · 로비 디테일: footer icons, the new-title dot and the time-of-day lobby. */
@Composable
internal fun LobbyDetailsSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    LobbyFooterIconComparison(applied, onApply)
    LobbyNewBadgeComparison(applied, onApply)
    LobbyTimeComparison(applied, onApply)
}

/** Group 8 · 칭호 획득 팝업: the real popup over a small certificate, with first / repeat and short / long titles. */
@Composable
internal fun TitleUnlockSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    var repeat by remember { mutableStateOf(false) }
    var longTitle by remember { mutableStateOf(false) }
    var reduced by remember { mutableStateOf(false) }
    EffectComparison(
        title = "칭호 획득 팝업",
        hint = "인생이 끝나고 기록증이 처음 열릴 때만 나와요. '간직하기'를 누르면 메달이 기록증 메달 자리로 날아가요.",
        entries = TitleUnlockStyle.entries,
        applied = applied.titleUnlock,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.TITLE_UNLOCK_KEY,
        onApply = { onApply(applied.copy(titleUnlock = it)) },
        controls = { replay ->
            ChoiceChips(listOf(false, true), repeat, { if (it) "다시 받은 칭호 · 3번째" else "처음 받은 칭호" },
                { if (it) "title_unlock_repeat" else "title_unlock_first" }, { repeat = it; replay() })
            ChoiceChips(listOf(false, true), longTitle, { if (it) "긴 칭호" else "짧은 칭호" },
                { if (it) "title_unlock_long" else "title_unlock_short" }, { longTitle = it; replay() })
            ChoiceChips(listOf(false, true), reduced, { if (it) "움직임 줄이기로 보기" else "움직임 그대로" },
                { if (it) "title_unlock_reduced" else "title_unlock_motion" }, { reduced = it; replay() })
        },
    ) { previewing, replayKey ->
        val preferences = LocalExperiencePreferences.current
        CompositionLocalProvider(
            LocalMotionStyle provides applied.copy(titleUnlock = previewing),
            LocalExperiencePreferences provides if (reduced) preferences.copy(motionEnabled = false) else preferences,
        ) {
            Box(Modifier.testTag("title_unlock_preview")) {
                TitleUnlockMiniCertificate("title-unlock-demo-$replayKey", if (repeat) 3 else 1, longTitle)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// 로비 아래 아이콘
// ---------------------------------------------------------------------------------------------------------

@Composable
private fun LobbyFooterIconComparison(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    SectionTitle("로비 아래 아이콘", "아이콘 버튼을 눌러 보세요. 누를 때마다 아이콘이 움직여요.")
    ReducedMotionNote()
    FooterIconStyle.entries.forEach { style ->
        VariantCard(style.label, style.detail, selected = applied.footerIcon == style,
            tag = "${MotionStyleStore.FOOTER_ICON_KEY}_${style.name}", onApply = { onApply(applied.copy(footerIcon = style)) }) {
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(footerIcon = style)) {
                LobbyDemoFooterRow("demo_footer_${style.name}")
            }
        }
    }
}

/** The lobby's three real footer buttons on a night strip (they click nothing here). */
@Composable
private fun LobbyDemoFooterRow(tagPrefix: String, showNewDot: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CompareNight).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LobbyFooter("지난 인생", "${tagPrefix}_book", LobbyIcon.BOOK, stacked = true, Modifier.weight(1f)) {}
        LobbyFooter("칭호 수집", "${tagPrefix}_medal", LobbyIcon.MEDAL, stacked = true, Modifier.weight(1f),
            showNewDot = showNewDot, newDotDelayMillis = 240L) {}
        LobbyFooter("설정", "${tagPrefix}_gear", LobbyIcon.GEAR, stacked = true, Modifier.weight(1f)) {}
    }
}

// ---------------------------------------------------------------------------------------------------------
// 새 칭호 알림 점
// ---------------------------------------------------------------------------------------------------------

@Composable
private fun LobbyNewBadgeComparison(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    SectionTitle("새 칭호 알림 점", "새 칭호를 받으면 '칭호 수집' 버튼에 금색 점이 떠요. 칭호 수집을 열면 새 칭호가 맨 위에 '새로 받음'으로 보이고 점은 꺼져요.")
    ReducedMotionNote()
    NewBadgeStyle.entries.forEach { style ->
        key(style) {
            var replay by remember { mutableIntStateOf(0) }
            VariantCard(style.label, style.detail, selected = applied.newBadge == style,
                tag = "${MotionStyleStore.NEW_BADGE_KEY}_${style.name}", onApply = { onApply(applied.copy(newBadge = style)) }) {
                CompositionLocalProvider(LocalMotionStyle provides applied.copy(newBadge = style)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                key(replay) { LobbyDemoFooterRow("demo_new_badge_${style.name}", showNewDot = true) }
                            }
                            ReplayChip("${MotionStyleStore.NEW_BADGE_KEY}_${style.name}_replay", { replay++ }, "▶ 다시")
                        }
                        // What the dot leads to: the new title on top of the collection, with its medal revealed once.
                        key(replay) {
                            LobbyTitleRowSample(
                                TitleEntryUi("demo_new_title_${style.name}", "함께 걸어온 사람", "곁에 있던 사람들을 기억했다",
                                    earned = true, earnedBy = listOf("민서")),
                                isNew = true,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LifeGamePaper).padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// 시간대 로비
// ---------------------------------------------------------------------------------------------------------

/** "지금" in the hour chips: the device's hour (LobbyClock.System). */
private const val LOBBY_TIME_NOW = -1f
private val LobbyTimeDemoHours = listOf(7f, 13f, 18f, 19.5f, 22f, LOBBY_TIME_NOW)

private fun lobbyTimeDemoLabel(hour: Float): String = when (hour) {
    7f -> "아침 7시"
    13f -> "낮 1시"
    18f -> "노을 6시"
    19.5f -> "저녁 7시 반"
    22f -> "밤 10시"
    else -> "지금"
}

private fun lobbyTimeDemoTag(hour: Float): String =
    if (hour < 0f) "${MotionStyleStore.LOBBY_TIME_KEY}_now"
    else "${MotionStyleStore.LOBBY_TIME_KEY}_hour_${hour.toString().removeSuffix(".0").replace('.', '_')}"

/** "18:30 · 노을": the scrubbed hour and the bucket it falls in. */
private fun lobbyTimeReadout(hour: Float): String {
    val h = lobbyNormalizedHour(hour)
    var hh = floor(h).toInt()
    var mm = ((h - hh) * 60f).roundToInt()
    if (mm == 60) { hh = (hh + 1) % 24; mm = 0 }
    val bucket = when (lobbyTimeBucket(h)) {
        LobbyTimeBucket.MORNING -> "아침"
        LobbyTimeBucket.DAY -> "낮"
        LobbyTimeBucket.SUNSET -> "노을"
        LobbyTimeBucket.NIGHT -> "밤"
    }
    return "%02d:%02d · %s".format(hh, mm, bucket)
}

@Composable
private fun LobbyTimeComparison(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    var hour by remember { mutableFloatStateOf(22f) }
    var followNow by remember { mutableStateOf(false) }
    EffectComparison(
        title = "시간대 로비",
        hint = "기기 시각을 따라 로비의 빛이 바뀌어요. 시간을 고르거나 막대를 밀어 하루를 훑어 보세요.",
        entries = LobbyTimeStyle.entries,
        applied = applied.lobbyTime,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.LOBBY_TIME_KEY,
        onApply = { onApply(applied.copy(lobbyTime = it)) },
        controls = {
            ChoiceChips(LobbyTimeDemoHours, if (followNow) LOBBY_TIME_NOW else hour, ::lobbyTimeDemoLabel, ::lobbyTimeDemoTag, { choice ->
                followNow = choice == LOBBY_TIME_NOW
                hour = if (followNow) lobbyNormalizedHour(LobbyClock.System.hourOfDay()) else choice
            })
            Slider(
                value = hour,
                onValueChange = { followNow = false; hour = (it * 4f).roundToInt() / 4f },
                valueRange = 0f..24f,
                steps = 95,
                colors = SliderDefaults.colors(thumbColor = CompareGold, activeTrackColor = CompareGold,
                    inactiveTrackColor = ComparePaper.copy(alpha = 0.2f), activeTickColor = CompareGold.copy(alpha = 0f),
                    inactiveTickColor = ComparePaper.copy(alpha = 0f)),
                modifier = Modifier.testTag("${MotionStyleStore.LOBBY_TIME_KEY}_slider"),
            )
            Text(lobbyTimeReadout(hour), color = ComparePaper, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("${MotionStyleStore.LOBBY_TIME_KEY}_readout"))
        },
    ) { previewing, replayKey ->
        CompositionLocalProvider(LocalMotionStyle provides applied.copy(lobbyTime = previewing)) {
            val reduceMotion = rememberReduceMotion()
            // Replay re-runs the quick lobby intro, so the new-title dot pops again too.
            val intro = LobbyIntroState(applied.entrance, active = applied.entrance != EntranceStyle.NONE && !reduceMotion,
                quick = true, key = replayKey)
            Box(Modifier.testTag("${MotionStyleStore.LOBBY_TIME_KEY}_preview")) {
                MiniLobby(intro, hour = hour, timeStyle = previewing, hasNewTitles = true, footerTagPrefix = "demo_lobby_time")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// MiniLobby (shared with stage 1's entrance comparison)
// ---------------------------------------------------------------------------------------------------------

private val MiniTitleStyle = TextStyle(color = ComparePaper, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold,
    fontSize = 28.sp, letterSpacing = 2.sp)

/**
 * A scaled-down lobby that replays the chosen entrance with the same building blocks as the real one: the time-of-day
 * light at [hour] ([timeStyle]), the real footer buttons (tagged `[footerTagPrefix]_past_lives` …) and, with
 * [hasNewTitles], the new-title dot. The defaults show today's daytime lobby without a dot.
 */
@Composable
internal fun MiniLobby(
    intro: LobbyIntroState,
    hour: Float = LobbyClock.NEUTRAL_HOUR,
    timeStyle: LobbyTimeStyle = LocalMotionStyle.current.lobbyTime,
    hasNewTitles: Boolean = false,
    footerTagPrefix: String = "demo_lobby",
) {
    val scene = rememberHomeScene(null, null)
    val phase = rememberAmbientPhase()
    val look = remember(hour, timeStyle) { lobbyLookAt(hour, timeStyle) }
    val newDotDelay = remember(intro.key) { lobbyNewDotDelayMillis(intro) }
    Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(12.dp)).clipToBounds().background(CompareNight)) {
        HomeSceneBackground(scene, phase, Modifier.fillMaxSize(), look)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to CompareNight.copy(alpha = 0.6f), 0.5f to CompareNight.copy(alpha = 0.1f), 0.8f to CompareNight.copy(alpha = 0.85f), 1f to CompareNight,
        )))
        if (look.hasParticles) LobbyTimeParticles(look, phase, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("선택으로 만드는 한 사람의 일생", color = ComparePaper.copy(alpha = 0.9f), fontSize = 11.sp,
                modifier = Modifier.lobbyEntrance(intro, 0))
            StampedTitle("인생게임", MiniTitleStyle, CompareGold, intro, Modifier.lobbyEntrance(intro, LobbyIntroState.TITLE_INDEX))
            HomeSceneAvatar(scene, phase, Modifier.fillMaxWidth().weight(1f).lobbyEntrance(intro, 2, groupOpacity = true))
            Column(Modifier.lobbyEntrance(intro, 3), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("아기부터 노년까지", color = CompareGold, fontSize = 11.sp)
                Text("이번엔, 어떤 삶을 살게 될까?", color = ComparePaper, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.lobbyEntrance(intro, 4, rise = false)) {
                InkButton("새 인생 살기", {}, compact = true, containerColor = CompareGold, contentColor = CompareInk, highlight = true)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp).lobbyEntrance(intro, 6, rise = false), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LobbyFooter("지난 인생", "${footerTagPrefix}_past_lives", LobbyIcon.BOOK, stacked = true, Modifier.weight(1f)) {}
                LobbyFooter("칭호 수집", "${footerTagPrefix}_titles", LobbyIcon.MEDAL, stacked = true, Modifier.weight(1f),
                    showNewDot = hasNewTitles, newDotDelayMillis = newDotDelay) {}
                LobbyFooter("설정", "${footerTagPrefix}_settings", LobbyIcon.GEAR, stacked = true, Modifier.weight(1f)) {}
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// 칭호 획득 팝업: a small certificate wrapped exactly like CertificateView
// ---------------------------------------------------------------------------------------------------------

private fun titleUnlockDemoRecord(runId: String, longTitle: Boolean): LifeRecordUi = buildLifeRecordUi(
    CompletedLifeRecord(
        runId = runId, playerName = "민서", summary = "지나온 삶", endingId = null, endingTitle = "지나온 삶",
        endingSummary = "하루하루를 쌓아 왔다", choices = emptyList(),
        finalState = PeriodRunState(runId, 41L, LifePeriod.OLD_AGE, isEnding = true),
        awardedTitle = if (longTitle) {
            AwardedTitle("demo_long_title", "계단 두 칸씩 오르는 사람", "서두르지 않으면서도 늘 한 걸음 앞에 있었다")
        } else {
            AwardedTitle("demo_title", "함께 걸어온 사람", "곁에 있던 사람들을 기억했다")
        },
    ),
    RecordContent(emptyList()),
)

@Composable
private fun TitleUnlockMiniCertificate(runId: String, timesEarned: Int, longTitle: Boolean) {
    val record = remember(runId, longTitle) { titleUnlockDemoRecord(runId, longTitle) }
    val title = record.title ?: return
    val state = rememberTitleUnlockState(record, TitleUnlockUi(timesEarned), celebrate = true)
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth().height(440.dp).clip(shape).background(LifeGamePaper).titleUnlockOutsideTouch(state)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompletionSealHeader(runId = record.runId, titleEarned = true, celebrate = true, onConsumed = {},
                badgeModifier = Modifier.titleUnlockAnchor(state))
            Column(
                Modifier.fillMaxWidth().clip(shape).background(LifeGameCard).border(1.dp, LifeGameRule, shape).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(record.playerName, color = LifeGameInk, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("칭호 · ${title.text}", color = LifeGameSpecial, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp)
                Text(title.description, color = LifeGameInkSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Text("마지막 시기 · 노년기", color = LifeGameInkSecondary, fontSize = 13.sp)
            Text("남긴 한 줄 · 하루하루를 쌓아 왔다", color = LifeGameInkSecondary, fontSize = 13.sp)
        }
        TitleUnlockOverlay(state, record, Modifier.matchParentSize())
    }
}
