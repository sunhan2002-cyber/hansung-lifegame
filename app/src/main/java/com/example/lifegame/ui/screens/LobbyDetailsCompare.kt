/**
 * Area: lobby2 — its "연출 비교" sections. Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls these; only their bodies change:
 * - `@Composable internal fun LobbyDetailsSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "4. 로비 디테일": footer icons, the new-title dot, the time-of-day lobby.
 * - `@Composable internal fun TitleUnlockSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — composed
 *   in group "8. 인생의 끝" after bookends' EndingSections.
 * - `@Composable internal fun MiniLobby(intro: LobbyIntroState)` — moved here verbatim from MotionCompareScreen;
 *   stage-1 EntranceComparison still calls `MiniLobby(intro)`, so lobby2 may only add parameters with defaults
 *   (hour, timeStyle, hasNewTitles, the real LobbyFooter …).
 * Tags: footer_icon_*, new_badge_*, lobby_time_*, title_unlock_* (see MotionCompareKit for the scheme).
 *
 * Stub: one "준비 중" line per section.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.theme.LifeGameSerif

/** Group 4 · 로비 디테일 (footerIcon · newBadge · lobbyTime). Stub. */
@Composable
internal fun LobbyDetailsSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}

/** Group 8 · 칭호 획득 팝업 (titleUnlock). Stub. */
@Composable
internal fun TitleUnlockSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}

private val MiniTitleStyle = TextStyle(color = ComparePaper, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold,
    fontSize = 28.sp, letterSpacing = 2.sp)

/** A scaled-down lobby that replays the chosen entrance with the same building blocks as the real one. */
@Composable
internal fun MiniLobby(intro: LobbyIntroState) {
    val scene = rememberHomeScene(null, null)
    val phase = rememberAmbientPhase()
    Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(12.dp)).clipToBounds().background(CompareNight)) {
        HomeSceneBackground(scene, phase, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to CompareNight.copy(alpha = 0.6f), 0.5f to CompareNight.copy(alpha = 0.1f), 0.8f to CompareNight.copy(alpha = 0.85f), 1f to CompareNight,
        )))
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
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).lobbyEntrance(intro, 6, rise = false), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("지난 인생", "칭호 수집", "설정").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(ComparePaper.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(5.dp))
                        Text(it, color = ComparePaper.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
