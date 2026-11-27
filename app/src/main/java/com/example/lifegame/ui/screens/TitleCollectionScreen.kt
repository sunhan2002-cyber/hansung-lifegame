package com.example.lifegame.ui.screens

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.TitleCollectionUi
import com.example.lifegame.ui.TitleEntryUi
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.TitleMedallion
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGameMuted
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial

/**
 * 칭호 수집. 완료 기록에 영구 저장된 칭호를 콘텐츠의 칭호 목록 위에 표시한다.
 * 아직 받지 못한 칭호는 이름과 이야기식 힌트만 보이고 정확한 조건(플래그 · 확률)은 숨긴다.
 * "다음 판에서 노리기"로 하나를 고르면 그 목표가 새 인생 화면에 보인다(2026-11-13 승인된 확장 기능 3).
 * [newTitleIds]는 이번 방문에서 '새로 받음'으로 보일 칭호다(lobby2 영역). 비어 있으면 지금 순서 · 의미 그대로다.
 */
@Composable
fun TitleCollectionScreen(
    collection: TitleCollectionUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    targetTitleId: String? = null,
    onSetTarget: (String?) -> Unit = {},
    newTitleIds: Set<String> = emptySet(),
) {
    // 새로 받은 칭호가 맨 위에 온다. 안정 정렬이라 나머지는 원래 순서 그대로다(비어 있으면 목록을 그대로 쓴다).
    val entries = remember(collection.entries, newTitleIds) {
        if (newTitleIds.isEmpty()) collection.entries else collection.entries.sortedByDescending { it.earned && it.titleId in newTitleIds }
    }
    // 이번 방문에서 메달 공개를 이미 보여 준 칭호. 목록이 항목을 다시 만들어도 공개를 되풀이하지 않는다.
    // Saveable: after rotation or recreation the '새로 받음' reveal does not play again.
    val revealed = rememberSaveable(saver = listSaver<MutableSet<String>, String>({ it.toList() }, { it.toMutableSet() })) {
        mutableSetOf()
    }
    Column(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            Text(
                text = "칭호 수집",
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "${collection.earnedCount} / ${collection.total} 받음",
                color = LifeGameInkSecondary,
                fontSize = 15.sp,
                modifier = Modifier.testTag("title_count"),
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("title_collection"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (collection.entries.isEmpty()) {
                item { Text("칭호 데이터를 불러오지 못했다.", color = LifeGameInkTertiary, fontSize = 15.sp) }
            }
            itemsIndexed(entries, key = { _, entry -> entry.titleId }) { index, entry ->
                TitleRow(
                    entry = entry,
                    targeted = entry.titleId == targetTitleId,
                    onToggleTarget = { onSetTarget(if (entry.titleId == targetTitleId) null else entry.titleId) },
                    isNew = entry.earned && entry.titleId in newTitleIds,
                    revealDelayMillis = NEW_TITLE_REVEAL_DELAY_MS + NEW_TITLE_REVEAL_STAGGER_MS * index,
                    revealed = revealed,
                )
            }
        }
        LineButton(
            text = "홈으로",
            onClick = onBack,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** 새로 받은 칭호의 메달 공개: 목록이 뜬 뒤 160ms부터 한 줄에 90ms씩 차례로(lobby2 영역). */
private const val NEW_TITLE_REVEAL_DELAY_MS = 160
private const val NEW_TITLE_REVEAL_STAGGER_MS = 90
private const val NEW_TITLE_REVEAL_MS = 720

/** "연출 비교" 화면용: 목록 밖에서 칭호 한 줄을 그린다. [isNew]이면 '새로 받음' 표시와 메달 공개가 함께 나온다. */
@Composable
internal fun LobbyTitleRowSample(entry: TitleEntryUi, isNew: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        TitleRow(entry, isNew = isNew, revealDelayMillis = NEW_TITLE_REVEAL_DELAY_MS, revealed = remember { mutableSetOf() })
    }
}

@Composable
private fun TitleRow(
    entry: TitleEntryUi,
    targeted: Boolean = false,
    onToggleTarget: () -> Unit = {},
    isNew: Boolean = false,
    revealDelayMillis: Int = 0,
    revealed: MutableSet<String>? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (entry.earned) LifeGameCard else LifeGameMuted)
            .border(
                width = if (targeted) 2.dp else 1.dp,
                color = when {
                    targeted -> LifeGameSpecial
                    entry.earned -> LifeGameInk.copy(alpha = 0.5f)
                    else -> LifeGameRule
                },
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    stateDescription = when {
                        entry.earned && isNew -> LOBBY_NEW_TITLE_STATE
                        entry.earned -> "받음"
                        targeted -> "다음 판 목표"
                        else -> "아직 받지 못함"
                    }
                }
                .testTag("title_entry_${entry.titleId}"),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                if (entry.earned) {
                    val badge = Modifier.size(36.dp).testTag("earned_badge_${entry.titleId}")
                    if (isNew) {
                        val reveal = rememberNewTitleReveal(entry.titleId, revealDelayMillis, revealed)
                        TitleMedallion(badge, progress = { reveal.value })
                    } else {
                        TitleMedallion(badge)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (isNew) NewTitlePill()
                    TitleRowText(entry, targeted)
                }
            }
        }
        if (!entry.earned && entry.goal != null) {
            Text(
                text = if (targeted) "노리는 중 · 해제" else "다음 판에서 노리기",
                color = if (targeted) LifeGameSpecial else LifeGameInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onToggleTarget)
                    .heightIn(min = 48.dp)
                    .padding(vertical = 13.dp)
                    .testTag("title_target_${entry.titleId}"),
            )
        }
    }
}

@Composable
private fun TitleRowText(entry: TitleEntryUi, targeted: Boolean) {
    run {
        Text(
            text = entry.text,
            color = if (entry.earned) LifeGameSpecial else LifeGameInkTertiary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        if (entry.earned) {
            Text(entry.description, color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Text(
                text = "받은 인생: " + entry.earnedBy.distinct().joinToString(", "),
                color = LifeGameInkTertiary,
                fontSize = 13.sp,
            )
        } else {
            Text("아직 받지 못한 칭호", color = LifeGameInkTertiary, fontSize = 13.sp)
            entry.hint?.let { Text("힌트 · $it", color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp) }
            if (targeted && entry.goal != null) {
                Text("다음 판 목표 · ${entry.goal}", color = LifeGameSpecial, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

/** 새로 받은 칭호의 상태 설명. 화면의 '새로 받음' 표시는 그림으로만 두고 낭독은 이 한 번이다. */
internal const val LOBBY_NEW_TITLE_STATE = "새로 받음"

@Composable
private fun NewTitlePill() {
    Text(
        text = LOBBY_NEW_TITLE_STATE,
        color = Color(0xFF253631),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LifeGameGold)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clearAndSetSemantics { },
    )
}

/**
 * 새로 받은 칭호 메달의 공개 진행(0 → 1). 메달 자체의 공개 연출(크기 · 기울기 · 반짝임)을 한 번만 쓴다.
 * 방문마다 한 번: 목록이 항목을 다시 만들면 [revealed]에 있어 바로 1이다. 움직임 줄이기 · 화면 낭독 탐색에서는 1이다.
 */
@Composable
private fun rememberNewTitleReveal(titleId: String, delayMillis: Int, revealed: MutableSet<String>?): State<Float> {
    val firstShow = remember(titleId) { revealed != null && titleId !in revealed }
    val play = firstShow && rememberEntranceMotionAllowed()
    val progress = remember(titleId) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(progress, play) {
        if (!play) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        revealed?.add(titleId)
        progress.animateTo(1f, tween(NEW_TITLE_REVEAL_MS, delayMillis = delayMillis, easing = FastOutSlowInEasing))
    }
    return progress.asState()
}
