/**
 * Area: play — its "연출 비교" sections.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls this; only its body changes:
 * - `@Composable internal fun PlayMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "6. 플레이 화면": choice reveal, picked choice, stat change, big result. One PhoneFrame hosting the real
 *   DecisionScreen on a fake PeriodPlayUiState (avoid fake states that turn on its BackHandlers).
 * Tags: choice_reveal_*, choice_pick_*, stat_change_*, big_result_*.
 *
 * Each effect is an [EffectComparison] (`<key>_<VARIANT>` chips, `<key>_replay`, `<key>_apply`; extra controls
 * `stat_change_loss` / `stat_change_gain` and `big_result_tier_<TIER>`; previews wrapped in `<key>_preview`).
 * One real DecisionScreen on a fake life runs at a time, inside one PhoneFrame: the section whose variant chip or
 * ▶ 다시 재생 was pressed last takes it (choice reveal at first), the others show a short placeholder. The fake states
 * never turn on the screen's BackHandlers by themselves (no incident popup, no image preview); a pick swaps in a
 * fake result with a new yearKey, as the engine does, so the real reveal, pick and result effects run.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.EventImageExposureStatus
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.DecisionUi
import com.example.lifegame.ui.FactChangeUi
import com.example.lifegame.ui.OutcomeUi
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.PlayPhase
import com.example.lifegame.ui.StatChangeUi
import com.example.lifegame.ui.components.BigResultStyle
import com.example.lifegame.ui.components.ChoicePickStyle
import com.example.lifegame.ui.components.ChoiceRevealStyle
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.StatChangeStyle
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.statChanges

/** Group 6 · 플레이 화면 (choiceReveal · choicePick · statChange · bigResult). */
@Composable
internal fun PlayMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    // 실제 사건 화면은 한 번에 하나만 띄운다. 버전 칩이나 ▶ 다시 재생을 누른 효과가 그 화면을 가져간다.
    var showing by rememberSaveable { mutableStateOf(ChoiceDemoSection.REVEAL) }
    var gain by rememberSaveable { mutableStateOf(false) }
    var tier by rememberSaveable { mutableStateOf(ResultDemoTier.BIG_GOOD) }
    CompareNote(
        "가짜 인생으로 실제 사건 화면을 작은 틀에 띄워요. 버전 칩이나 ▶ 다시 재생을 누른 효과의 화면만 움직이고, " +
            "다른 효과는 지금 적용된 버전 그대로 함께 보여요.",
    )

    EffectComparison(
        title = "선택지 등장",
        hint = "본문이 끝나고 선택지가 처음 나타날 때. 본문을 한 번 누르면 바로 끝나요.",
        entries = ChoiceRevealStyle.entries,
        applied = applied.choiceReveal,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.CHOICE_REVEAL_KEY,
        onApply = { onApply(applied.copy(choiceReveal = it)) },
    ) { previewing, replayKey ->
        ChoiceDemoSlot(ChoiceDemoSection.REVEAL, showing, replayKey, onShow = { showing = it }) {
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(choiceReveal = previewing)) {
                ChoiceDemoScreen(MotionStyleStore.CHOICE_REVEAL_KEY, ChoiceDemoSection.REVEAL, replayKey)
            }
        }
    }

    EffectComparison(
        title = "고른 선택지",
        hint = "카드를 눌러 골라 보세요. ▶ 다시 재생을 누르면 선택지로 돌아가요.",
        entries = ChoicePickStyle.entries,
        applied = applied.choicePick,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.CHOICE_PICK_KEY,
        onApply = { onApply(applied.copy(choicePick = it)) },
    ) { previewing, replayKey ->
        ChoiceDemoSlot(ChoiceDemoSection.PICK, showing, replayKey, onShow = { showing = it }) {
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(choicePick = previewing)) {
                ChoiceDemoScreen(MotionStyleStore.CHOICE_PICK_KEY, ChoiceDemoSection.PICK, replayKey)
            }
        }
    }

    EffectComparison(
        title = "능력치 변화",
        hint = "결과 칩과 위 건강 게이지. 선택지가 잠깐 보인 뒤 결과로 넘어가요(카드를 눌러 골라도 돼요).",
        entries = StatChangeStyle.entries,
        applied = applied.statChange,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.STAT_CHANGE_KEY,
        onApply = { onApply(applied.copy(statChange = it)) },
        controls = { replay ->
            ChoiceChips(
                options = listOf(false, true),
                selected = gain,
                label = { if (it) "회복될 때 (건강 47 → 59)" else "깎일 때 (건강 72 → 47)" },
                tag = { if (it) "${MotionStyleStore.STAT_CHANGE_KEY}_gain" else "${MotionStyleStore.STAT_CHANGE_KEY}_loss" },
                onSelect = { gain = it; replay() },
            )
        },
    ) { previewing, replayKey ->
        ChoiceDemoSlot(ChoiceDemoSection.STAT, showing, replayKey, onShow = { showing = it }) {
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(statChange = previewing)) {
                ChoiceDemoScreen(MotionStyleStore.STAT_CHANGE_KEY, ChoiceDemoSection.STAT, replayKey, gain = gain)
            }
        }
    }

    EffectComparison(
        title = "큰 결과",
        hint = "결과의 등급을 골라 보세요. 평범한 결과는 조용해요.",
        entries = BigResultStyle.entries,
        applied = applied.bigResult,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.BIG_RESULT_KEY,
        onApply = { onApply(applied.copy(bigResult = it)) },
        controls = { replay ->
            ChoiceChips(
                options = ResultDemoTier.entries,
                selected = tier,
                label = { it.label },
                tag = { "${MotionStyleStore.BIG_RESULT_KEY}_tier_${it.name}" },
                onSelect = { tier = it; replay() },
            )
        },
    ) { previewing, replayKey ->
        ChoiceDemoSlot(ChoiceDemoSection.BIG, showing, replayKey, onShow = { showing = it }) {
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(bigResult = previewing)) {
                ChoiceDemoScreen(MotionStyleStore.BIG_RESULT_KEY, ChoiceDemoSection.BIG, replayKey, tier = tier)
            }
        }
        val impact = resultImpactOf(resultDemoOutcome(tier), DecisionKind.MILESTONE)
        CompareNote("이 결과의 등급: ${resultDemoTierLabel(impact)}")
    }
}

/** Which section's preview holds the one real screen. */
private enum class ChoiceDemoSection { REVEAL, PICK, STAT, BIG }

/** Fake outcomes built to hit each result tier (labels as in the tier chips). */
internal enum class ResultDemoTier(val label: String) {
    CALM("평범"),
    NOTABLE("눈에 띔"),
    BIG_GOOD("큰 결과 · 좋음"),
    BIG_BAD("큰 결과 · 나쁨"),
    FINAL("인생의 끝"),
}

/**
 * Shows [content] when [section] holds the screen, otherwise a placeholder. A replay (variant chip or ▶ 다시 재생)
 * of this section hands the screen to it; the first composition does not.
 */
@Composable
private fun ChoiceDemoSlot(
    section: ChoiceDemoSection,
    showing: ChoiceDemoSection,
    replayKey: Int,
    onShow: (ChoiceDemoSection) -> Unit,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) { if (replayKey > 0) onShow(section) }
    if (section == showing) {
        content()
    } else {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(12.dp)).background(CompareNight).padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "다른 효과를 미리보는 중이에요.\n버전을 고르거나 ▶ 다시 재생을 누르면 여기에서 보여요.",
                color = ComparePaper.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The real DecisionScreen on a fake life, scaled into a PhoneFrame and wrapped in `<key>_preview` (production tags
 * inside it repeat across groups after the merge, so tests scope their lookups to this wrapper).
 * - REVEAL: the body types (one tap finishes it), then the choices reveal live.
 * - PICK: the choices are already shown; a tap picks.
 * - STAT / BIG: the choices show briefly, then the result arrives as if picked (or tap a card first).
 * A pick swaps in the result with a new yearKey, like the engine; 계속하기 returns to the choices.
 */
@Composable
private fun ChoiceDemoScreen(
    key: String,
    section: ChoiceDemoSection,
    replayKey: Int,
    gain: Boolean = false,
    tier: ResultDemoTier = ResultDemoTier.BIG_GOOD,
) {
    // 고른 등급 · 회복 여부가 바뀌면 선택지부터 다시 시작한다(바꾸는 칩이 다시 재생도 누른다).
    val base = remember(section, gain, tier) { choiceDemoStart(section, gain, round = "$replayKey-0") }
    var state by remember(base) { mutableStateOf(base) }
    var round by remember(base) { mutableStateOf(0) }
    fun pick(choiceId: String) {
        if (state.outcome != null) return
        round++
        state = choiceDemoResult(state, section, choiceId, gain, tier, round = "$replayKey-$round")
    }
    if (section == ChoiceDemoSection.STAT || section == ChoiceDemoSection.BIG) {
        LaunchedEffect(base) {
            awaitFrameMillis(700)
            pick(base.decision?.choices?.first()?.choiceId.orEmpty())
        }
    }
    Box(Modifier.fillMaxWidth().testTag("${key}_preview")) {
        PhoneFrame(Modifier.fillMaxWidth()) {
            DecisionScreen(
                state = state,
                onChoose = ::pick,
                onContinue = {
                    round++
                    state = choiceDemoStart(section, gain, round = "$replayKey-$round")
                },
                onResultAnimationConsumed = { displayId ->
                    val presentation = state.presentation
                    if (presentation.resultDisplayId == displayId) {
                        state = state.copy(presentation = presentation.consumeResultAnimation(displayId))
                    }
                },
                skipImagePreview = true,
            )
        }
    }
}

private const val DEMO_EVENT_ID = "motion_demo_fork"

private val DemoStats = Stats(health = 72, fitness = 55, intelligence = 64, social = 58, wealth = 52, happiness = 61, luck = 47)

private val DemoMarks = listOf("영아", "유년", "청소년", "청년", "중년", "노년")
    .mapIndexed { index, label -> PeriodMarkUi(label, if (index < 3) 1f else 0f, isCurrent = index == 3) }

private fun choiceDemoDecision(lockOne: Boolean) = DecisionUi(
    eventId = DEMO_EVENT_ID,
    kind = DecisionKind.MILESTONE,
    title = "졸업 후의 길이 갈라졌다",
    text = "졸업장을 받아 든 날, 단체 대화방이 조용해졌다.\n저마다 다른 길 앞에서 나도 한 걸음을 정해야 했다.",
    imageId = "",
    imageFile = null,
    choices = listOf(
        DecisionChoiceUi("demo_a", "A", "회사에 들어가 바로 일을 배운다", hint = "가능성 높음 · 경제력 · 사회성", healthEffectText = "건강 -3 감소"),
        if (lockOne) {
            DecisionChoiceUi(
                "demo_b", "B", "대학원에 진학해 연구를 계속한다", hint = "지력", available = false,
                lockReasons = listOf("지력 82 필요 (지금 64)", "대학 진학 필요"),
            )
        } else {
            DecisionChoiceUi("demo_b", "B", "대학원에 진학해 연구를 계속한다", hint = "가능성 반반쯤 · 지력")
        },
        DecisionChoiceUi("demo_c", "C", "일단 반년 동안 여행을 떠난다", hint = "행복 · 운"),
    ),
)

/** The choices page a section starts from (and returns to after 계속하기). */
private fun choiceDemoStart(section: ChoiceDemoSection, gain: Boolean, round: String): PeriodPlayUiState {
    val decision = choiceDemoDecision(lockOne = section == ChoiceDemoSection.REVEAL)
    val stats = if (section == ChoiceDemoSection.STAT && gain) DemoStats.copy(health = 47) else DemoStats
    return choiceDemoState(
        decision = decision,
        yearKey = "demo-$round",
        stats = stats,
        // 선택지 등장만 본문 타이핑부터 시작한다. 나머지는 이미 본 사건처럼 선택지부터(타이핑 · 그림 선노출 없음).
        seen = section != ChoiceDemoSection.REVEAL,
    )
}

/** The engine-like result of picking [choiceId]: new stats and facts, and a new yearKey (a moment was recorded). */
private fun choiceDemoResult(
    state: PeriodPlayUiState,
    section: ChoiceDemoSection,
    choiceId: String,
    gain: Boolean,
    tier: ResultDemoTier,
    round: String,
): PeriodPlayUiState {
    val decision = state.decision ?: return state
    val label = decision.choices.firstOrNull { it.choiceId == choiceId }?.label ?: decision.choices.first().label
    val authored = when (section) {
        ChoiceDemoSection.STAT -> if (gain) {
            OutcomeUi(label, "푹 쉬고 나니 몸이 한결 가벼워졌다.", listOf(StatChangeUi("건강", 12)), endsLife = false)
        } else {
            OutcomeUi(
                label,
                "밤샘이 이어지자 몸이 먼저 신호를 보냈다. 그래도 배운 것은 남았다.",
                listOf(StatChangeUi("건강", -25), StatChangeUi("지력", 5), StatChangeUi("행복", 3), StatChangeUi("사회성", -2)),
                endsLife = false,
            )
        }
        ChoiceDemoSection.BIG -> resultDemoOutcome(tier).copy(choiceLabel = label)
        ChoiceDemoSection.REVEAL, ChoiceDemoSection.PICK -> choiceDemoPickOutcome(choiceId, label)
    }
    val after = choiceDemoApply(state.stats, authored.changes)
    val outcome = authored.copy(changes = statChanges(state.stats, after))
    val displayId = "$DEMO_EVENT_ID:$round"
    return choiceDemoState(
        decision = decision,
        yearKey = "demo-$round",
        stats = after,
        outcome = outcome,
        seen = true,
        presentation = state.presentation.requestResultAnimation(displayId),
    )
}

/** Quiet (CALM) results for the reveal and pick previews, so the pick itself is what you watch. */
private fun choiceDemoPickOutcome(choiceId: String, label: String): OutcomeUi = when (choiceId) {
    "demo_b" -> OutcomeUi(label, "합격 통지서를 몇 번이나 다시 읽었다.", listOf(StatChangeUi("지력", 5), StatChangeUi("경제력", -3)), endsLife = false)
    "demo_c" -> OutcomeUi(label, "낯선 도시의 새벽 시장에서 처음으로 혼자 웃었다.", listOf(StatChangeUi("행복", 5), StatChangeUi("운", 2)), endsLife = false)
    else -> OutcomeUi(
        label,
        "첫 출근 날, 사원증 사진이 생각보다 잘 나왔다.",
        listOf(StatChangeUi("경제력", 6), StatChangeUi("건강", -3), StatChangeUi("사회성", 2)),
        endsLife = false,
    )
}

/** A fake outcome per tier (resultImpactOf puts each in its tier). */
internal fun resultDemoOutcome(tier: ResultDemoTier): OutcomeUi = when (tier) {
    ResultDemoTier.CALM -> OutcomeUi(
        "일단 반년 동안 여행을 떠난다",
        "평범한 하루가 조금 즐거웠다.",
        listOf(StatChangeUi("행복", 3), StatChangeUi("사회성", -2)),
        endsLife = false,
    )
    ResultDemoTier.NOTABLE -> OutcomeUi(
        "대학원에 진학해 연구를 계속한다",
        "교수님 연구실에 자리가 하나 났다.",
        listOf(StatChangeUi("지력", 4), StatChangeUi("행복", 2)),
        endsLife = false,
        factChanges = listOf(FactChangeUi("직업", "연구 보조")),
    )
    ResultDemoTier.BIG_GOOD -> OutcomeUi(
        "일단 반년 동안 여행을 떠난다",
        "여행길에 그린 그림들로 작은 전시를 열었다.",
        listOf(StatChangeUi("행복", 5)),
        endsLife = false,
        factChanges = listOf(FactChangeUi("업적", "첫 개인전")),
    )
    ResultDemoTier.BIG_BAD -> OutcomeUi(
        "회사에 들어가 바로 일을 배운다",
        "쉬지 않고 버틴 끝에 결국 병원 신세를 졌다.",
        listOf(StatChangeUi("건강", -25)),
        endsLife = false,
    )
    ResultDemoTier.FINAL -> OutcomeUi(
        "회사에 들어가 바로 일을 배운다",
        "긴 하루 끝에 조용히 눈을 감았다.",
        listOf(StatChangeUi("건강", -72)),
        endsLife = true,
    )
}

/** How the tier chips name a computed impact. */
internal fun resultDemoTierLabel(impact: ResultImpact): String = when (impact.tier) {
    ResultImpactTier.CALM -> ResultDemoTier.CALM.label
    ResultImpactTier.NOTABLE -> ResultDemoTier.NOTABLE.label
    ResultImpactTier.BIG -> if (impact.good) ResultDemoTier.BIG_GOOD.label else ResultDemoTier.BIG_BAD.label
    ResultImpactTier.FINAL -> ResultDemoTier.FINAL.label
}

private fun choiceDemoApply(stats: Stats, changes: List<StatChangeUi>): Stats {
    fun moved(name: String, value: Int) = (value + (changes.firstOrNull { it.name == name }?.delta ?: 0)).coerceIn(0, 100)
    return Stats(
        health = moved("건강", stats.health),
        fitness = moved("운동능력", stats.fitness),
        intelligence = moved("지력", stats.intelligence),
        social = moved("사회성", stats.social),
        wealth = moved("경제력", stats.wealth),
        happiness = moved("행복", stats.happiness),
        luck = moved("운", stats.luck),
    )
}

private fun choiceDemoState(
    decision: DecisionUi,
    yearKey: String,
    stats: Stats,
    outcome: OutcomeUi? = null,
    seen: Boolean,
    presentation: PresentationState = PresentationState(),
) = PeriodPlayUiState(
    runKey = "motion-demo",
    playerName = "재겸",
    phase = PlayPhase.DECISION,
    periodName = "청년기",
    chapterLabel = "제4장",
    periodYears = 5,
    periodIntro = "",
    yearInPeriod = 3,
    periodMarks = DemoMarks,
    previousPeriodName = "청소년기",
    stats = stats,
    focus = null,
    focusOptions = emptyList(),
    sceneImageId = "",
    sceneFile = null,
    log = emptyList(),
    yearKey = yearKey,
    decision = decision,
    outcome = outcome,
    isDead = outcome?.endsLife == true,
    autoPlaying = false,
    speed = 1,
    // 이미 본 사건이면 팝업 · 타이핑 · 그림 선노출 없이 선택지부터 나온다(뒤로가기를 잡는 상태가 생기지 않는다).
    presentation = if (seen) {
        presentation.copy(exposedEventId = decision.eventId, eventImageExposure = EventImageExposureStatus.SHOWN)
    } else {
        presentation
    },
)
