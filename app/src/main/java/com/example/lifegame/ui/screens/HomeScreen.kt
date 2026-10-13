package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.FinishedLifeUi
import com.example.lifegame.ui.LifeCardUi
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.PixelFigurePlaceholder
import com.example.lifegame.ui.components.SectionLabel
import com.example.lifegame.ui.components.ThinRule
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif

/**
 * 홈(프로필) 화면 (R-110). 앱을 켜면 처음 보는 화면이다.
 * 진행 중인 인생 카드, 이어하기 / 새 인생 살기, 지난 인생 목록을 보여 준다.
 */
@Composable
fun HomeScreen(
    lifeCard: LifeCardUi?,
    finishedLives: List<FinishedLifeUi>,
    onContinue: () -> Unit,
    onNewLife: () -> Unit,
    modifier: Modifier = Modifier,
    onDesignPreview: (() -> Unit)? = null,
    message: String? = null,
) {
    var confirmOverwrite by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "인생게임",
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "한 사람의 일생을 필름 한 롤에 담는다.",
                color = LifeGameInkSecondary,
                fontSize = 13.sp,
            )
        }

        if (message != null) {
            Text(message, color = LifeGameInkSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(4.dp))
        SectionLabel(text = "지금 사는 인생")
        if (lifeCard != null) {
            LifeCard(lifeCard)
            InkButton(text = "이어하기", onClick = onContinue)
            LineButton(text = "새 인생 살기", onClick = { confirmOverwrite = true })
        } else {
            EmptyLifeCard()
            InkButton(text = "새 인생 살기", onClick = onNewLife)
        }

        onDesignPreview?.let { openPreview ->
            LineButton(text = "디자인 미리보기", onClick = openPreview)
        }

        Spacer(Modifier.height(8.dp))
        SectionLabel(text = "지난 인생", trailing = if (finishedLives.isEmpty()) null else "${finishedLives.size}번")
        if (finishedLives.isEmpty()) {
            Text(
                text = "아직 끝까지 산 인생이 없다.",
                color = LifeGameInkTertiary,
                fontSize = 13.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                finishedLives.forEachIndexed { index, life ->
                    if (index > 0) ThinRule()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(life.playerName, color = LifeGameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(life.summary, color = LifeGameInkSecondary, fontSize = 14.sp, fontFamily = LifeGameSerif)
                    }
                }
            }
        }
    }

    if (confirmOverwrite) {
        AlertDialog(
            onDismissRequest = { confirmOverwrite = false },
            containerColor = LifeGamePaper,
            title = { Text("새 인생을 시작할까요?", fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold) },
            text = { Text("지금 사는 인생은 이어서 할 수 없게 됩니다.", color = LifeGameInkSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmOverwrite = false
                    onNewLife()
                }) { Text("새로 시작", color = LifeGameInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmOverwrite = false }) { Text("취소", color = LifeGameInkSecondary) }
            },
        )
    }
}

@Composable
private fun LifeCard(card: LifeCardUi) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(16.dp)
            .testTag("life_card"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PixelFigurePlaceholder(height = 72.dp, modifier = Modifier.padding(top = 4.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.playerName,
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(card.place, color = LifeGameInkSecondary, fontSize = 13.sp)
            if (card.focusLabel != null) {
                Text("초점 · ${card.focusLabel}", color = LifeGameInkSecondary, fontSize = 13.sp)
            }
            if (card.lastLine != null) {
                Text(
                    text = card.lastLine,
                    modifier = Modifier.padding(top = 6.dp),
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyLifeCard() {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "아직 시작한 인생이 없다.",
            color = LifeGameInk,
            fontFamily = LifeGameSerif,
            fontSize = 16.sp,
        )
        Text(
            text = "이름과 성별, 타고난 기질을 정하면 영아기부터 시작한다.",
            color = LifeGameInkSecondary,
            fontSize = 13.sp,
        )
    }
}
