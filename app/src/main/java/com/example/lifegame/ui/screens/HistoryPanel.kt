package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.ui.LifeRecord
import com.example.lifegame.ui.chapter
import com.example.lifegame.ui.components.PerforationBand
import com.example.lifegame.ui.displayName
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePicture
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import com.example.lifegame.ui.theme.LifeGameSpecialPicture
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatUp

private sealed interface HistoryRow {
    val key: String
    data class Stage(val stage: LifeStage, override val key: String) : HistoryRow
    data class Record(val record: LifeRecord, val showDivider: Boolean) : HistoryRow {
        override val key: String get() = record.key
    }
}

/**
 * 기록 보기 상태. 지난 해들이 오래된 순서로 쌓이고 맨 아래가 가장 최근이다.
 * 맨 아래에서 한 번 더 위로 올리거나 "지금으로" 막대를 누르면 지금 화면으로 돌아간다.
 */
@Composable
fun HistoryPanel(
    records: List<LifeRecord>,
    nowAge: Int,
    nowTitle: String,
    nowIsSpecial: Boolean,
    onBackToNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(records) {
        buildList {
            records.forEachIndexed { index, record ->
                if (record.startsStage) add(HistoryRow.Stage(record.stage, "stage-$index"))
                val next = records.getOrNull(index + 1)
                add(HistoryRow.Record(record, showDivider = next != null && !next.startsStage))
            }
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (rows.size - 1).coerceAtLeast(0))
    val currentOnBack by rememberUpdatedState(onBackToNow)
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val overscrollUp = remember(threshold) {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    pulled -= available.y
                    if (pulled > threshold) {
                        pulled = 0f
                        currentOnBack()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Column(modifier) {
        HorizontalDivider(color = LifeGameRule)
        Box(
            Modifier
                .weight(1f)
                .nestedScroll(overscrollUp),
        ) {
            LazyColumn(state = listState) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is HistoryRow.Stage -> StageDivider(row.stage)
                        is HistoryRow.Record -> RecordRow(row.record, row.showDivider)
                    }
                }
            }
        }
        BackToNowBar(nowAge = nowAge, nowTitle = nowTitle, nowIsSpecial = nowIsSpecial, onClick = onBackToNow)
    }
}

@Composable
private fun StageDivider(stage: LifeStage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), color = LifeGameRule)
        Text(
            text = "제${stage.chapter()}장 · ${stage.displayName()}",
            modifier = Modifier.padding(horizontal = 10.dp),
            color = LifeGameInkTertiary,
            fontSize = 12.sp,
        )
        HorizontalDivider(Modifier.weight(1f), color = LifeGameRule)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordRow(record: LifeRecord, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${record.age}세",
                modifier = Modifier
                    .width(36.dp)
                    .padding(top = 2.dp),
                color = if (record.isSpecial) LifeGameSpecial else LifeGameInkTertiary,
                fontSize = 13.sp,
                fontWeight = if (record.isSpecial) FontWeight.Bold else FontWeight.Normal,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (record.isSpecial) {
                    Text("돌발 사건", color = LifeGameSpecial, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = record.title,
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontSize = 15.5.sp,
                    lineHeight = 24.sp,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "→ ${record.choiceLabel}",
                        color = LifeGameInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    record.changes.forEach { (name, delta) ->
                        Text(
                            text = "$name ${if (delta > 0) "+" else "−"}${kotlin.math.abs(delta)}",
                            color = if (delta > 0) LifeGameStatUp else LifeGameStatDown,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (record.isSpecial) LifeGameSpecialPicture else LifeGamePicture),
            )
        }
        if (showDivider) {
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = LifeGameRule)
        }
    }
}

/** 아래에 붙는 "지금으로" 막대. 지금 이 해만 작은 필름 한 컷으로 보여 준다. */
@Composable
private fun BackToNowBar(nowAge: Int, nowTitle: String, nowIsSpecial: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = LifeGameRule)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                Modifier
                    .size(width = 32.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LifeGameRule),
            )
            Text("맨 아래에서 위로 올리면 지금으로", color = LifeGameInkTertiary, fontSize = 11.5.sp)
        }
        PerforationBand(Modifier.fillMaxWidth().height(21.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LifeGameCard)
                .clickable(role = Role.Button, onClickLabel = "지금으로 돌아가기", onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (nowIsSpecial) LifeGameSpecialPicture else LifeGamePicture),
            )
            Column(Modifier.weight(1f)) {
                Text("지금 · ${nowAge}세", color = LifeGameInkSecondary, fontSize = 11.5.sp)
                Text(
                    text = nowTitle,
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("지금으로 ↑", color = LifeGameInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        PerforationBand(Modifier.fillMaxWidth().height(21.dp))
    }
}
