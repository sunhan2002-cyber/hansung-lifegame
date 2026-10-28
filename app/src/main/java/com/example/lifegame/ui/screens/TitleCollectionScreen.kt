package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * 아직 받지 못한 칭호는 이름만 흐리게 보이고 조건은 숨긴다.
 */
@Composable
fun TitleCollectionScreen(
    collection: TitleCollectionUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            items(collection.entries, key = { it.titleId }) { entry -> TitleRow(entry) }
        }
        LineButton(
            text = "홈으로",
            onClick = onBack,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun TitleRow(entry: TitleEntryUi) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (entry.earned) LifeGameCard else LifeGameMuted)
            .border(1.dp, if (entry.earned) LifeGameInk.copy(alpha = 0.5f) else LifeGameRule, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { stateDescription = if (entry.earned) "받음" else "아직 받지 못함" }
            .testTag("title_entry_${entry.titleId}"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
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
        }
    }
}
