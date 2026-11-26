/**
 * Area: bookends — its "연출 비교" sections.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls these; only their bodies change:
 * - `@Composable internal fun CreationSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "5. 새 인생 만들기": profile entrance, appearance pop, dice count-up (at most ONE GL dice stage composed: the
 *   count-up panel; the real profile screen in the entrance preview shows static dice).
 * - `@Composable internal fun EndingSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "8. 인생의 끝", before lobby2's TitleUnlockSection: recap moment, certificate seal, title reveal. The mini
 *   certificate's "칭호 팝업과 함께 보기" toggle (off at start) passes a fake TitleUnlockUi(1) into the real CertificateView.
 * Tags (store keys): profile_entrance_*, appearance_pop_*, stat_count_up_*, recap_moment_*, certificate_seal_*,
 * title_reveal_* — `<key>_<VARIANT>` chips, `<key>_replay`, `<key>_apply`, extra controls `<key>_<control>`, and every
 * preview sits in `Box(Modifier.testTag("<key>_preview"))` (real components inside keep their production tags).
 * Fake data only: fixed rolls, looks and records; no ViewModel, no game random stream.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.EndingSummaryUi
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.RecapMomentUi
import com.example.lifegame.ui.StartingRoll
import com.example.lifegame.ui.TitleUi
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.AvatarOption
import com.example.lifegame.ui.avatar.AvatarOptions
import com.example.lifegame.ui.components.AppearancePopStyle
import com.example.lifegame.ui.components.CertificateSealStyle
import com.example.lifegame.ui.components.LifeMutedOnDark
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.ProfileEntranceStyle
import com.example.lifegame.ui.components.RecapMomentStyle
import com.example.lifegame.ui.components.StatCountUpStyle
import com.example.lifegame.ui.components.TitleRevealStyle
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import kotlinx.coroutines.delay

/** Group 5 · 새 인생 만들기 (profileEntrance · appearancePop · statCountUp). */
@Composable
internal fun CreationSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    ProfileEntranceSection(applied, onApply)
    ProfileAppearancePopSection(applied, onApply)
    ProfileStatCountSection(applied, onApply)
}

/** Group 8 · 주마등 · 기록증 도장 · 칭호 공개 (recapMoment · certificateSeal · titleReveal). */
@Composable
internal fun EndingSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    RecapMomentSection(applied, onApply)
    CertificateRevealSection(applied, onApply)
}

// ---- 5. 새 인생 만들기 ------------------------------------------------------------------------------------

/** The real profile screen in a phone frame; every replay is a fresh screen (fresh saved state), so it enters again. */
@Composable
private fun ProfileEntranceSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    val key = MotionStyleStore.PROFILE_ENTRANCE_KEY
    EffectComparison(
        title = "새 인생 화면 등장",
        hint = "실제 새 인생 화면이에요. 앱에서는 화면 전환(0.7초 동안 흐려지며 바뀜)과 겹쳐 보여요.",
        entries = ProfileEntranceStyle.entries,
        applied = applied.profileEntrance,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = key,
        onApply = { onApply(applied.copy(profileEntrance = it)) },
    ) { previewing, _ ->
        CompositionLocalProvider(LocalMotionStyle provides applied.copy(profileEntrance = previewing)) {
            Box(Modifier.testTag("${key}_preview")) {
                // Static dice here: the group's one GL stage belongs to the count-up preview below.
                PhoneFrame { CharacterSelectScreen(starters = emptyList(), onStart = {}, dice3D = false) }
            }
        }
    }
}

/** The real age previews and option chips; "바꿔 보기" (and picking a variant) changes the hair, so the pop plays. */
@Composable
private fun ProfileAppearancePopSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    val key = MotionStyleStore.APPEARANCE_POP_KEY
    var previewing by remember { mutableStateOf(applied.appearancePop) }
    var hairId by remember { mutableStateOf(ProfileDemoHairCycle.first()) }
    var hairColorId by remember { mutableStateOf(AvatarOptions.hairColors.first().id) }
    var outfitColorId by remember { mutableStateOf(AvatarOptions.outfitColors.first().id) }
    val changeLook = {
        hairId = ProfileDemoHairCycle[(ProfileDemoHairCycle.indexOf(hairId) + 1) % ProfileDemoHairCycle.size]
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("모습 바꿀 때 톡", "옵션을 바꾸면 새 모습이 실제로 그려진 뒤 네 시기 모습이 반응해요.")
        ProfileComparePanel {
            ChoiceChips(AppearancePopStyle.entries, previewing, { it.label }, { "${key}_${it.name}" }, { previewing = it; changeLook() })
            ProfileCompareDetail(previewing.detail)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ReplayChip("${key}_replay", changeLook, label = "▶ 바꿔 보기")
            }
            ReducedMotionNote()
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(appearancePop = previewing)) {
                Column(
                    Modifier.testTag("${key}_preview").fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(LifeGamePaper).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileAppearancePreview(
                        skinId = AvatarOptions.skins.first().id,
                        hairId = hairId,
                        eyesId = AvatarOptions.eyes.first().id,
                        hairColorId = hairColorId,
                        outfitColorId = outfitColorId,
                    )
                    ProfileOptionChips("머리", AvatarOptions.hairs, hairId) { hairId = it }
                    ProfileOptionChips("머리 색", AvatarOptions.hairColors.map { AvatarOption(it.id, it.label) }, hairColorId) { hairColorId = it }
                    ProfileOptionChips("옷 색", AvatarOptions.outfitColors.map { AvatarOption(it.id, it.label) }, outfitColorId) { outfitColorId = it }
                }
            }
            ApplyRow(applied.appearancePop == previewing, previewing.label, "${key}_apply") {
                onApply(applied.copy(appearancePop = previewing))
            }
        }
    }
}

/**
 * The real dice panel on three fixed rolls (no Random). Its own roll buttons and "다시 굴리기" step through them; this is
 * the group's one GL dice stage.
 */
@Composable
private fun ProfileStatCountSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    val key = MotionStyleStore.STAT_COUNT_UP_KEY
    var previewing by remember { mutableStateOf(applied.statCountUp) }
    var nonce by remember { mutableIntStateOf(0) }
    val roll = if (nonce == 0) null else StartingRoll.decode(ProfileDemoRolls[(nonce - 1) % ProfileDemoRolls.size])
    val rollNext = { nonce += 1 }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("주사위 숫자 세기", "주사위가 실제로 멈춘 뒤 숫자가 움직여요. 글자와 낭독은 처음부터 최종값이에요.")
        ProfileComparePanel {
            ChoiceChips(StatCountUpStyle.entries, previewing, { it.label }, { "${key}_${it.name}" }, { previewing = it; rollNext() })
            ProfileCompareDetail(previewing.detail)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ReplayChip("${key}_replay", rollNext, label = "▶ 다시 굴리기")
            }
            ReducedMotionNote()
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(statCountUp = previewing)) {
                Box(
                    Modifier.testTag("${key}_preview").fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(LifeGamePaper).padding(10.dp),
                ) {
                    DicePanel(roll = roll, rollNonce = nonce, tendency = null, rerollsLeft = PROFILE_DEMO_REROLLS, onRoll = rollNext)
                }
            }
            ApplyRow(applied.statCountUp == previewing, previewing.label, "${key}_apply") {
                onApply(applied.copy(statCountUp = previewing))
            }
        }
    }
}

// ---- 8. 인생의 끝 ------------------------------------------------------------------------------------------

/** The real recap stage on three fake moments: the same 2.8 s auto-advance, a tap goes on, every replay starts over. */
@Composable
private fun RecapMomentSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    val key = MotionStyleStore.RECAP_MOMENT_KEY
    EffectComparison(
        title = "주마등 장면",
        hint = "2.8초마다 다음 장면으로 넘어가고, 누르면 바로 넘어가요. 모든 움직임은 1.1초 안에 끝나요.",
        entries = RecapMomentStyle.entries,
        applied = applied.recapMoment,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = key,
        onApply = { onApply(applied.copy(recapMoment = it)) },
    ) { previewing, _ ->
        CompositionLocalProvider(LocalMotionStyle provides applied.copy(recapMoment = previewing)) {
            Box(Modifier.testTag("${key}_preview")) { RecapMomentDemo() }
        }
    }
}

@Composable
private fun RecapMomentDemo() {
    val moments = remember { recapDemoMoments() }
    var index by remember { mutableIntStateOf(0) }
    // The real RecapView's own timer (the same delay), so the preview advances exactly like the screen; it stops at
    // the last moment instead of moving on.
    LaunchedEffect(index) {
        delay(RECAP_MILLIS)
        if (index < moments.lastIndex) index += 1
    }
    Column(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(12.dp)).background(LifeGameInk)) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clickable(role = Role.Button) { if (index < moments.lastIndex) index += 1 }
                .semantics { contentDescription = "다음 장면" },
        ) {
            RecapStage(moments, index)
        }
        Text(
            "${index + 1} / ${moments.size}",
            color = LifeMutedOnDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
    }
}

/**
 * The real certificate on a fake fresh record: seal and title reveal can be combined. Every replay is a new run id (a
 * fresh one-shot); "조용히 다시 열기" opens it the way an archived record or 주마등 다시 보기 does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CertificateRevealSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    val sealKey = MotionStyleStore.CERTIFICATE_SEAL_KEY
    val titleKey = MotionStyleStore.TITLE_REVEAL_KEY
    var seal by remember { mutableStateOf(applied.certificateSeal) }
    var title by remember { mutableStateOf(applied.titleReveal) }
    var quiet by remember { mutableStateOf(false) }
    var withPopup by remember { mutableStateOf(false) }
    var replay by remember { mutableIntStateOf(0) }
    val replayNow = { replay += 1 }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("인생기록증 도장 · 칭호 공개", "막 끝난 인생의 기록증을 처음 열 때 한 번만 펼쳐져요. 지난 기록에서는 다 찍힌 모습이에요.")
        ProfileComparePanel {
            ProfileCompareLabel("도장")
            ChoiceChips(CertificateSealStyle.entries, seal, { it.label }, { "${sealKey}_${it.name}" }, { seal = it; replayNow() })
            ProfileCompareDetail(seal.detail)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ReplayChip("${sealKey}_replay", replayNow) }
            ApplyRow(applied.certificateSeal == seal, seal.label, "${sealKey}_apply") { onApply(applied.copy(certificateSeal = seal)) }
            ProfileCompareLabel("칭호 공개")
            ChoiceChips(TitleRevealStyle.entries, title, { it.label }, { "${titleKey}_${it.name}" }, { title = it; replayNow() })
            ProfileCompareDetail(title.detail)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ReplayChip("${titleKey}_replay", replayNow) }
            ApplyRow(applied.titleReveal == title, title.label, "${titleKey}_apply") { onApply(applied.copy(titleReveal = title)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CertificateDemoToggle("조용히 다시 열기", quiet, "${sealKey}_quiet") { quiet = it; replayNow() }
                CertificateDemoToggle("칭호 팝업과 함께 보기", withPopup, "${titleKey}_popup") { withPopup = it; replayNow() }
            }
            ReducedMotionNote()
            CompositionLocalProvider(LocalMotionStyle provides applied.copy(certificateSeal = seal, titleReveal = title)) {
                Box(Modifier.testTag("${titleKey}_preview")) {
                    Box(Modifier.testTag("${sealKey}_preview")) {
                        key(replay) { CertificateDemo(runId = "demo-$replay", quiet = quiet, withPopup = withPopup) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateDemo(runId: String, quiet: Boolean, withPopup: Boolean) {
    val record = remember(runId) { certificateDemoRecord(runId) }
    // Like EndingFlowScreen: the reveal consumes "celebrate" once; a quiet opening starts consumed.
    var presented by remember(runId) { mutableStateOf(quiet) }
    Box(Modifier.fillMaxWidth().height(560.dp).clip(RoundedCornerShape(12.dp))) {
        CertificateView(
            record = record,
            onReplay = {},
            onDone = {},
            doneLabel = "홈으로",
            celebrate = !presented,
            onCelebrationConsumed = { presented = true },
            titleUnlock = if (withPopup) TitleUnlockUi(timesEarned = 1) else null,
        )
    }
}

// ---- shared bits ----------------------------------------------------------------------------------------

@Composable
private fun ProfileComparePanel(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComparePanel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun ProfileCompareDetail(text: String) {
    Text(text, color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable
private fun ProfileCompareLabel(text: String) {
    Text(text, color = ComparePaper, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

/** An on/off chip (a switch to accessibility), in the look of the stage-1 replay-mode chips. */
@Composable
private fun CertificateDemoToggle(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.heightIn(min = 44.dp).clip(shape)
            .border(1.dp, ComparePaper.copy(alpha = if (checked) 0.6f else 0.15f), shape)
            .background(if (checked) CompareGold.copy(alpha = 0.18f) else CompareNight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (checked) ComparePaper else ComparePaper.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ---- fake data ------------------------------------------------------------------------------------------

/** "바꿔 보기" walks through these hair styles. */
private val ProfileDemoHairCycle = listOf("hair_default", "hair_bob_brown", "hair_long_black", "hair_curly_brown")

/** Three fixed rolls (two dice per stat) the count-up preview steps through. */
private val ProfileDemoRolls = listOf(
    "3,4,2,5,6,1,1,4,5,4,2,2",
    "6,6,1,2,3,3,5,4,2,1,4,4",
    "1,3,3,4,3,4,1,4,3,4,3,4",
)

private const val PROFILE_DEMO_REROLLS = 3

private fun recapDemoMoments(): List<RecapMomentUi> = listOf(
    RecapMomentUi(
        key = "demo_recap_infant",
        periodName = "영아기",
        yearInPeriod = 1,
        title = "첫걸음",
        choiceLabel = "엄마 손을 놓고 걸어 본다",
        resultText = "세 걸음 만에 주저앉았지만, 다들 박수를 쳤다.",
        images = listOf(ImageCandidate("demo_recap_infant", "week10/scenes/img_week10_bg_infant.png")),
        avatar = AvatarLook(period = LifePeriod.INFANT),
    ),
    RecapMomentUi(
        key = "demo_recap_teen",
        periodName = "청소년기",
        yearInPeriod = 3,
        title = "밤새 쓴 편지",
        choiceLabel = "끝내 편지를 건넨다",
        resultText = "대답은 듣지 못했지만, 그날 밤은 오래 기억에 남았다.",
        images = listOf(ImageCandidate("demo_recap_teen", "week10/scenes/img_week10_bg_teen.png")),
        avatar = AvatarLook(period = LifePeriod.TEEN),
    ),
    RecapMomentUi(
        key = "demo_recap_old_age",
        periodName = "노년기",
        yearInPeriod = 5,
        title = "마당의 감나무",
        choiceLabel = "손주와 감을 딴다",
        resultText = "높은 가지의 감은 새들 몫으로 남겨 두었다.",
        images = listOf(ImageCandidate("demo_recap_old_age", "week10/scenes/img_week10_bg_old_age.png")),
        avatar = AvatarLook(period = LifePeriod.OLD_AGE),
    ),
)

private fun certificateDemoRecord(runId: String): LifeRecordUi = LifeRecordUi(
    runId = runId,
    playerName = "민서",
    title = TitleUi("demo_title", "끝까지 궁금했던 사람", "질문을 멈추지 않았다"),
    summary = EndingSummaryUi(
        headline = "평범하게 빛난 사람",
        endingSummary = "하루하루를 궁금해하며 살았다.",
        pathLine = "배우고 연구한 삶 · 지력 82",
        lifeLine = "민서 · 배우고 연구한 삶",
        death = null,
        endedAt = "노년기 5번째 해",
        educationCareer = emptyList(),
        familyLife = emptyList(),
        achievements = emptyList(),
        topStats = emptyList(),
        choices = emptyList(),
    ),
    avatar = AvatarLook(period = LifePeriod.OLD_AGE),
    finalPeriodName = "노년기",
    recap = emptyList(),
    backdrop = emptyList(),
    pathTitle = "배우고 연구한 삶",
    usesPhotoFace = false,
)
