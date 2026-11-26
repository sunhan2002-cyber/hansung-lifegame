package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameLobbyPaper
import com.example.lifegame.ui.theme.LifeGameNight
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * The "연출 비교" screen's shared kit. Owner: the integrator; frozen while the stage-2 areas work in parallel.
 * Every area builds its comparison sections (in its own *Compare.kt file) from these pieces, so all demos look,
 * replay and apply the same way.
 *
 * Rules for section authors:
 * - Tags: `<store_key>_<VARIANT>` for variant chips or cards, `<store_key>_replay`, `<store_key>_apply`, and
 *   `<store_key>_<control>` for extra controls (store keys: MotionStyleStore.*_KEY, e.g. choice_reveal).
 * - Wrap every preview in CompositionLocalProvider(LocalMotionStyle provides applied.copy(<field> = previewing)).
 *   Apply always passes `applied.copy(<field> = choice)` to onApply.
 * - Fake data only: no ViewModel, no game random stream, no SharedPreferences except through onApply.
 * - Every section shows ReducedMotionNote; under reduced motion previews show the final static state.
 * - At most one real full screen (PhoneFrame) and one GL dice stage composed per group at any time.
 * - Section titles are unnumbered; MotionCompareScreen numbers the groups at its call sites.
 */

internal val CompareNight = LifeGameNight
internal val ComparePanel = Color(0xFF1E323C)
internal val CompareGold = LifeGameGold
internal val ComparePaper = LifeGameLobbyPaper
internal val CompareInk = Color(0xFF253631)

@Composable
internal fun SectionTitle(title: String, hint: String) {
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = ComparePaper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = ComparePaper.copy(alpha = 0.65f), fontSize = 12.sp)
    }
}

@Composable
internal fun VariantCard(
    label: String,
    detail: String,
    selected: Boolean,
    tag: String,
    onApply: () -> Unit,
    sample: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComparePanel)
            .border(if (selected) 2.dp else 1.dp, if (selected) CompareGold else ComparePaper.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(14.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = ComparePaper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
            }
            ApplyChip(selected, onApply, "${tag}_apply")
        }
        sample()
    }
}

@Composable
internal fun ApplyChip(selected: Boolean, onApply: () -> Unit, tag: String) {
    Text(
        if (selected) "적용됨" else "적용",
        color = if (selected) CompareInk else CompareGold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) CompareGold else Color.Transparent)
            .border(1.dp, CompareGold, RoundedCornerShape(50))
            .clickable(enabled = !selected, role = Role.Button, onClick = onApply)
            .padding(horizontal = 14.dp, vertical = 8.dp).testTag(tag),
    )
}

/**
 * Radio chips that wrap onto more lines when needed, in the look of the stage-1 entrance chips: gold when selected.
 * Each chip is `selectable(role = RadioButton)` inside one `selectableGroup()`, at least 44dp tall, tagged [tag].
 * Tapping the selected chip again still calls [onSelect] (EffectComparison uses that to replay).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    tag: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).background(if (on) CompareGold else CompareNight)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp).testTag(tag(option)),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = if (on) CompareInk else ComparePaper, fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** "▶ 다시 재생": restarts a one-shot preview. */
@Composable
internal fun ReplayChip(tag: String, onReplay: () -> Unit, label: String = "▶ 다시 재생") {
    Box(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onReplay)
            .padding(horizontal = 10.dp, vertical = 8.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = CompareGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Says whether the previewed variant is the applied one, next to its apply chip (tagged [tag]). */
@Composable
internal fun ApplyRow(appliedHere: Boolean, previewingLabel: String, tag: String, onApply: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (appliedHere) "이 버전이 적용돼 있어요" else "미리보는 중: $previewingLabel",
            color = ComparePaper.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.weight(1f))
        ApplyChip(appliedHere, onApply, tag)
    }
}

/** A short muted line for hints and notes inside a group. */
@Composable
internal fun CompareNote(text: String, modifier: Modifier = Modifier) {
    Text(text, color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp, modifier = modifier)
}

/** Shown only under reduced motion, when every preview rests in its final state. */
@Composable
internal fun ReducedMotionNote() {
    if (rememberReduceMotion()) {
        Text("움직임 줄이기가 켜져 있어 마지막 모습만 보여요", color = CompareGold.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

/**
 * A real full screen, laid out at its design size ([designWidth] × [designHeight]) and scaled down to the
 * available width (never up), inside a 12dp rounded clip. Hit testing follows the scale, so the screen inside
 * stays tappable. The comparison root's safeDrawingPadding already consumed the window insets, so screens inside
 * lay out with zero insets.
 */
@Composable
internal fun PhoneFrame(
    modifier: Modifier = Modifier,
    designWidth: Dp = 360.dp,
    designHeight: Dp = 640.dp,
    background: Color = CompareNight,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.clip(RoundedCornerShape(12.dp)).background(background)) { measurables, constraints ->
        val width = designWidth.roundToPx().coerceAtLeast(1)
        val height = designHeight.roundToPx().coerceAtLeast(1)
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else width
        val scale = min(1f, available.toFloat() / width)
        val placeables = measurables.map { it.measure(Constraints.fixed(width, height)) }
        val frameWidth = (width * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val frameHeight = (height * scale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(frameWidth, frameHeight) {
            placeables.forEach { placeable ->
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

/**
 * One collapsible group of the comparison screen. The header is a button (펼치기 / 접기, state 펼침 / 접힘) tagged
 * [tag]; [content] is composed only while [expanded], so heavy previews cost nothing while the group is closed.
 */
@Composable
internal fun CompareGroup(
    title: String,
    hint: String,
    tag: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    // Only the header is clipped (for its press feedback); the content is not, so halos near the edge still show.
    Column(
        Modifier.fillMaxWidth()
            .border(if (expanded) 2.dp else 1.dp, if (expanded) CompareGold.copy(alpha = 0.6f) else ComparePaper.copy(alpha = 0.12f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clip(if (expanded) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp) else shape)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "접기" else "펼치기", onClick = onToggle)
                .semantics { stateDescription = if (expanded) "펼침" else "접힘" }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = ComparePaper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(hint, color = ComparePaper.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            Text(if (expanded) "▾" else "▸", color = CompareGold, fontSize = 18.sp, modifier = Modifier.clearAndSetSemantics { })
        }
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

/**
 * The standard one-shot comparison: a section title, then a panel with variant chips tagged
 * `${tagPrefix}_${entry.name}` (selecting one also replays), the variant's detail line, [controls], a ReplayChip
 * tagged `${tagPrefix}_replay`, the reduced-motion note, the preview (recomposed from scratch on every replay) and
 * an ApplyRow tagged `${tagPrefix}_apply`. The previewed variant starts at [applied].
 */
@Composable
internal fun <T : Enum<T>> EffectComparison(
    title: String,
    hint: String,
    entries: List<T>,
    applied: T,
    label: (T) -> String,
    detail: (T) -> String,
    tagPrefix: String,
    onApply: (T) -> Unit,
    controls: @Composable (replay: () -> Unit) -> Unit = {},
    preview: @Composable (previewing: T, replayKey: Int) -> Unit,
) {
    var previewing by remember { mutableStateOf(applied) }
    var replayKey by remember { mutableIntStateOf(0) }
    val replay: () -> Unit = { replayKey++ }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title, hint)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComparePanel).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChoiceChips(entries, previewing, label, { "${tagPrefix}_${it.name}" }, { previewing = it; replay() })
            Text(detail(previewing), color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
            controls(replay)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ReplayChip("${tagPrefix}_replay", replay)
            }
            ReducedMotionNote()
            key(replayKey) { preview(previewing, replayKey) }
            ApplyRow(applied == previewing, label(previewing), "${tagPrefix}_apply") { onApply(previewing) }
        }
    }
}
