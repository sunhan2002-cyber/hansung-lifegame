/**
 * Area: time (해 도장 · 시기선 점 이동). Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — these five signatures stay exactly as they are (class bodies may change and overloads may be
 * added, but nothing may be removed or renamed, because PlayChrome.kt and LifeGameApp.kt call them):
 * - `@Stable internal class LifeClock`
 * - `internal val LocalLifeClock = compositionLocalOf<LifeClock?> { null }` — provided only inside PlayHost, so
 *   PeriodTrack rendered anywhere else (tests, previews, the comparison screen without a fake clock) never animates.
 * - `@Composable internal fun rememberLifeClock(play: PeriodPlayUiState?): LifeClock?` — called once in PlayHost
 *   before any branch; the stub returns null.
 * - `@Stable internal class PeriodTrackMotion` with `companion object { val Rest }`, and
 *   `@Composable internal fun rememberPeriodTrackMotion(marks, yearLabel, stampYield): PeriodTrackMotion` — called
 *   by PeriodTrack on every composition; the stub returns [PeriodTrackMotion.Rest].
 * - `internal fun DrawScope.drawPeriodTrackLine(marks, currentIndex, motion)` — PeriodTrack's whole Canvas body. With
 *   [PeriodTrackMotion.Rest] it draws today's lifeline exactly (moved here verbatim from PlayChrome.kt).
 *
 * The stamp shows only the label the screen already has — `yearLabel`, i.e. "N번째 해" — never an age and never
 * "세" (user decision, 2026-11-26).
 */
package com.example.lifegame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSpecial

/** What PlayHost knows about the run's position in time, for the lifeline and the period turn. Stub: empty. */
@Stable
internal class LifeClock

/** Provided by PlayHost next to LocalPlayMenu; null everywhere else, which keeps every time effect off. */
internal val LocalLifeClock = compositionLocalOf<LifeClock?> { null }

/** PlayHost's clock for the current run. Stub: null (no time effects yet). */
@Composable
internal fun rememberLifeClock(play: PeriodPlayUiState?): LifeClock? = null

/** The lifeline's animated state (marker travel, hop, year stamp), read only while drawing. Stub: always at rest. */
@Stable
internal class PeriodTrackMotion {
    companion object {
        /** Nothing moving: [drawPeriodTrackLine] draws today's static lifeline. */
        val Rest = PeriodTrackMotion()
    }
}

/**
 * The motion for one PeriodTrack. [yearLabel] is the label PeriodTrack shows ("3번째 해"); [stampYield] is true
 * while something more important needs the bottom of the screen (DecisionScreen passes `choicesVisible`): a running
 * stamp then skips its hold and absorbs or fades within MotionTimeline.STAMP_YIELD_MS, and no new stamp starts.
 * Stub: [PeriodTrackMotion.Rest].
 */
@Composable
internal fun rememberPeriodTrackMotion(marks: List<PeriodMarkUi>, yearLabel: String?, stampYield: Boolean): PeriodTrackMotion =
    PeriodTrackMotion.Rest

/**
 * PeriodTrack's lifeline -O-O-O-, drawn into its 24dp Canvas. With [PeriodTrackMotion.Rest] this is exactly the
 * drawing PeriodTrack had before stage 2. Drawing may extend above the Canvas (no ancestor clips).
 */
internal fun DrawScope.drawPeriodTrackLine(marks: List<PeriodMarkUi>, currentIndex: Int, motion: PeriodTrackMotion) {
    if (marks.isEmpty()) return
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
