package com.example.lifegame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.rememberPixelAsset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSpecial

/**
 * 10주차 최종 시안의 플레이 화면 부품.
 * 상단: 투명 배경 위 캐릭터 + 두꺼운 건강 게이지(숫자 없음, 상태별 색).
 * 하단: -O-O-O- 시기선. 사건 분류는 버튼이 아닌 글자 효과.
 */

/** 건강 게이지 색 구간. 80~100 초록, 60~79 주황, 0~59 빨강. 화면에는 숫자를 쓰지 않는다. */
enum class HealthLevel(val label: String, val color: Color) {
    GOOD("좋음", Color(0xFF2E9D5B)),
    CAUTION("주의", Color(0xFFE38A1C)),
    DANGER("위험", Color(0xFFD8403A)),
}

fun healthLevelOf(health: Int): HealthLevel = when {
    health >= 80 -> HealthLevel.GOOD
    health >= 60 -> HealthLevel.CAUTION
    else -> HealthLevel.DANGER
}

/** 테스트가 게이지 색 구간을 읽을 수 있게 둔 의미 속성. 화면에는 보이지 않는다. */
val HealthLevelKey = SemanticsPropertyKey<HealthLevel>("HealthLevel")
var SemanticsPropertyReceiver.healthLevel by HealthLevelKey

/** 사건 배경이나 어두운 장면 위에서도 읽히도록 흰 글자에 그림자를 준다. */
val OnSceneShadow = Shadow(color = Color.Black.copy(alpha = 0.72f), offset = Offset(0f, 1.5f), blurRadius = 6f)

@Composable
fun HealthGauge(
    health: Int,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    barHeight: Dp = 14.dp,
) {
    val value = health.coerceIn(0, 100)
    val level = healthLevelOf(value)
    val track = if (onDark) Color.White.copy(alpha = 0.24f) else LifeGameRule
    val labelColor = if (onDark) LifeGamePaper else LifeGameInk
    Column(
        modifier
            .fillMaxWidth()
            .testTag("health_gauge")
            .semantics(mergeDescendants = true) {
                contentDescription = "건강"
                stateDescription = "건강 ${level.label}"
                healthLevel = level
            }
            .progressSemantics(value.toFloat(), 0f..100f),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "건강",
                color = labelColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                style = if (onDark) TextStyle(shadow = OnSceneShadow) else TextStyle.Default,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(track),
        ) {
            if (value > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(value / 100f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(level.color)
                        .testTag("health_gauge_fill"),
                )
            }
        }
    }
}

/**
 * 상단 줄: 캐릭터와 건강 게이지만 배경 위에 떠 있다. 카드 · 버튼 바탕은 없다.
 * 캐릭터를 누르면 능력치 · 이번 시기 초점 · 인생 사실 창이 열린다.
 */
@Composable
fun PlayTopBar(
    health: Int,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    avatar: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 18.dp, top = 6.dp, bottom = 2.dp)
            .testTag("play_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(width = 60.dp, height = 68.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onOpenDetails)
                .semantics(mergeDescendants = true) { contentDescription = "플레이어 현재 모습, 능력치와 이번 시기 초점 보기" }
                .testTag("player_avatar_button"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            avatar(Modifier.size(width = 56.dp, height = 66.dp))
        }
        HealthGauge(health = health, onDark = onDark, modifier = Modifier.weight(1f))
    }
}

/**
 * 하단 시기선 -O-O-O-. 지난 시기는 채운 원과 선, 지금 시기는 강조 테두리의 큰 원과 굵은 이름,
 * 아직 오지 않은 시기는 빈 원과 옅은 선이다. 밝은 바탕은 유지한다.
 */
@Composable
fun PeriodTrack(
    marks: List<PeriodMarkUi>,
    modifier: Modifier = Modifier,
    yearLabel: String? = null,
) {
    val currentIndex = marks.indexOfFirst { it.isCurrent }
    val current = marks.getOrNull(currentIndex)
    Column(
        modifier
            .fillMaxWidth()
            .background(LifeGamePaper)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
            .testTag("period_track")
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    if (current != null) {
                        append("지금 시기 ${current.label}")
                        if (yearLabel != null) append(" $yearLabel")
                        append(", ${marks.size}개 시기 중 ${currentIndex + 1}번째")
                    }
                }
            },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            if (marks.isEmpty()) return@Canvas
            // 원은 아래 이름 칸의 한가운데에 놓는다(이름과 원이 어긋나지 않게).
            val y = size.height / 2f
            val cellWidth = size.width / marks.size
            fun cx(index: Int) = cellWidth * (index + 0.5f)
            val small = 6.dp.toPx()
            val big = 9.dp.toPx()
            val line = 3.dp.toPx()
            // 원과 원 사이를 잇는 선. 지나온 구간은 진하게, 남은 구간은 옅게.
            for (index in 0 until marks.lastIndex) {
                val passed = currentIndex >= 0 && index < currentIndex
                val startX = cx(index) + (if (index == currentIndex) big else small)
                val endX = cx(index + 1) - (if (index + 1 == currentIndex) big else small)
                if (endX > startX) {
                    drawLine(
                        color = if (passed) LifeGameInk else LifeGameRule,
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = line,
                    )
                }
            }
            marks.forEachIndexed { index, mark ->
                val center = Offset(cx(index), y)
                when {
                    mark.isCurrent -> {
                        drawCircle(LifeGamePaper, radius = big, center = center)
                        drawCircle(LifeGameSpecial, radius = big - 1.5.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
                        drawCircle(LifeGameSpecial, radius = big * 0.42f, center = center)
                    }
                    index < currentIndex -> drawCircle(LifeGameInk, radius = small, center = center)
                    else -> {
                        drawCircle(LifeGamePaper, radius = small, center = center)
                        drawCircle(LifeGameRule, radius = small - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
                    }
                }
            }
        }
        // 칸 너비가 고정이라 이름은 1.3배까지만 커진다(그 이상은 접근성 설명으로 읽는다).
        val fontScale = LocalDensity.current.fontScale
        fun capped(size: Float) = (size * minOf(fontScale, 1.3f) / fontScale).sp
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            marks.forEachIndexed { index, mark ->
                Text(
                    text = mark.label,
                    color = when {
                        mark.isCurrent -> LifeGameInk
                        index < currentIndex -> LifeGameInk.copy(alpha = 0.72f)
                        else -> LifeGameInkTertiary.copy(alpha = 0.8f)
                    },
                    fontSize = if (mark.isCurrent) capped(13f) else capped(11.5f),
                    fontWeight = if (mark.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (mark.isCurrent) Modifier.testTag("period_track_current") else Modifier),
                )
            }
        }
    }
}

fun DecisionKind.markLabel(): String = when (this) {
    DecisionKind.MILESTONE -> "인생의 전환점"
    DecisionKind.INCIDENT -> "돌발사건"
    DecisionKind.BIG_INCIDENT -> "돌발선택"
}

/**
 * 사건 분류 표시. 누를 수 있는 알약 버튼이 아니라 글자 효과다.
 * 처음 나타날 때 글자 간격이 좁혀지며 떠오르고, 모션 줄이기에서는 바로 정지 상태로 보인다.
 */
@Composable
fun DecisionKindMark(
    kind: DecisionKind,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val isIncident = kind != DecisionKind.MILESTONE
    val progress = remember(kind) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(kind, animate) {
        if (animate) progress.animateTo(1f, tween(520)) else progress.snapTo(1f)
    }
    val color = if (isIncident) Color(0xFFFF8A80) else Color(0xFFF3D27A)
    Column(
        modifier.widthIn(min = 48.dp).testTag("decision_kind_mark"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = kind.markLabel(),
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (1.2f + (1f - progress.value) * 7f).sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = OnSceneShadow),
            modifier = Modifier.graphicsLayer {
                val p = progress.value
                alpha = p
                val scale = 1.18f - 0.18f * p
                scaleX = scale
                scaleY = scale
            },
        )
        // 짧은 밑줄이 양옆으로 펼쳐진다. 정지 상태에서도 같은 길이로 남는다.
        Canvas(Modifier.size(width = 64.dp, height = 3.dp).padding(top = 1.dp)) {
            val half = size.width / 2f * progress.value
            drawLine(
                color = color.copy(alpha = 0.85f),
                start = Offset(size.width / 2f - half, size.height / 2f),
                end = Offset(size.width / 2f + half, size.height / 2f),
                strokeWidth = size.height,
            )
        }
    }
}

/**
 * 캐릭터 뒤 배경 그림. 화면을 꽉 채우도록 자르고, 그림이 없으면 바닥선만 있는 빈 무대를 그린다.
 * 캐릭터는 이 위에 따로 얹는다(배경 · 캐릭터 레이어 분리).
 */
@Composable
fun SceneBackdrop(candidates: List<ImageCandidate>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.background(StageEmpty).testTag("scene_backdrop")) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val height = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        val loaded = if (candidates.isEmpty()) null else rememberPixelAsset(candidates, width, height)
        if (loaded != null) {
            Image(
                painter = BitmapPainter(loaded.bitmap.asImageBitmap(), filterQuality = FilterQuality.None),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val floor = size.height * 0.86f
                drawRect(StageFloor, topLeft = Offset(0f, floor), size = androidx.compose.ui.geometry.Size(size.width, size.height - floor))
            }
        }
    }
}

private val StageEmpty = Color(0xFFF3ECDD)
private val StageFloor = Color(0xFFD9BE93)

/** 장면 보기 진입 버튼. 어두운 배경 위의 가는 테두리 글자 버튼이다. */
@Composable
fun SceneViewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = if (enabled) 0.34f else 0.18f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
            .testTag("scene_view_button"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "장면 보기",
            color = LifeGamePaper.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
