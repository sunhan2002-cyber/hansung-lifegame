package com.example.lifegame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LifeGameColorScheme = lightColorScheme(
    // 주 버튼은 필름과 같은 먹색
    primary = LifeGameInk,
    onPrimary = Color.White,
    primaryContainer = LifeGameMuted,
    onPrimaryContainer = LifeGameInk,

    secondary = LifeGameInkSecondary,
    onSecondary = Color.White,
    // 진행 막대(StatBar)의 빈 부분 등. 기본 보라색이 섞이지 않게 선 색으로 맞춘다.
    secondaryContainer = LifeGameRule,
    onSecondaryContainer = LifeGameInk,

    background = LifeGamePaper,
    onBackground = LifeGameInk,

    surface = LifeGamePaper,
    onSurface = LifeGameInk,
    surfaceVariant = LifeGameMuted,
    onSurfaceVariant = LifeGameInkSecondary,
    surfaceContainerLowest = LifeGameCard,
    surfaceContainerLow = LifeGamePaper,
    surfaceContainer = LifeGamePaper,
    surfaceContainerHigh = LifeGamePaper,
    surfaceContainerHighest = LifeGameMuted,

    // 능력치 하락 등 부정적인 변화
    error = LifeGameStatDown,
    onError = Color.White,
    errorContainer = LifeGameStatDownSoft,
    onErrorContainer = LifeGameInk,

    outline = LifeGameRule,
    outlineVariant = LifeGameRule,
)

/**
 * 앱 글씨 배율 (2026-11-05 피드백 "게임 글씨가 조금 작다").
 * 화면마다 sp를 하나씩 고치지 않고 시스템 글꼴 배율에 곱한다.
 * 다만 시스템 최대 글꼴(200%)보다 커지지는 않게 한다. 큰 글꼴 배치는 200% 기준으로 검증돼 있다.
 */
const val APP_TEXT_SCALE = 1.12f
private const val MAX_TEXT_SCALE = 2f

fun appFontScale(systemFontScale: Float): Float =
    (systemFontScale * APP_TEXT_SCALE).coerceAtMost(maxOf(systemFontScale, MAX_TEXT_SCALE))

@Composable
fun LifeGameTheme(
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, appFontScale(density.fontScale)),
    ) {
        MaterialTheme(
            colorScheme = LifeGameColorScheme,
            typography = LifeGameTypography,
            content = content
        )
    }
}
