package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
import com.example.lifegame.ui.theme.LifeGameCard
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
            items(collection.entries, key = { it.titleId }) { entry ->
                TitleRow(
                    entry = entry,
                    targeted = entry.titleId == targetTitleId,
                    onToggleTarget = { onSetTarget(if (entry.titleId == targetTitleId) null else entry.titleId) },
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

@Composable
private fun TitleRow(entry: TitleEntryUi, targeted: Boolean = false, onToggleTarget: () -> Unit = {}) {
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
                        entry.earned -> "받음"
                        targeted -> "다음 판 목표"
                        else -> "아직 받지 못함"
                    }
                }
                .testTag("title_entry_${entry.titleId}"),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                if (entry.earned) TitleMedallion(Modifier.size(36.dp).testTag("earned_badge_${entry.titleId}"))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
