/**
 * Area: bookends — its "연출 비교" sections. Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls these; only their bodies change:
 * - `@Composable internal fun CreationSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "5. 새 인생 만들기": profile entrance, appearance pop, dice count-up (at most ONE GL dice stage composed).
 * - `@Composable internal fun EndingSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "8. 인생의 끝", before lobby2's TitleUnlockSection: recap moment, certificate seal, title reveal. The mini
 *   certificate's "칭호 팝업과 함께 보기" toggle passes a fake TitleUnlockUi(1) into the real CertificateView.
 * Tags: profile_entrance_*, appearance_pop_*, stat_count_up_*, recap_moment_*, certificate_seal_*, title_reveal_*.
 *
 * Stub: one "준비 중" line per section.
 */
package com.example.lifegame.ui.screens

import androidx.compose.runtime.Composable
import com.example.lifegame.ui.components.MotionStyle

/** Group 5 · 새 인생 만들기 (profileEntrance · appearancePop · statCountUp). Stub. */
@Composable
internal fun CreationSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}

/** Group 8 · 주마등 · 기록증 도장 · 칭호 공개 (recapMoment · certificateSeal · titleReveal). Stub. */
@Composable
internal fun EndingSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}
