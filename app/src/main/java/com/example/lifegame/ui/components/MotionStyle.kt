package com.example.lifegame.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Which variant of each presentation effect is used. Every variant still obeys "움직임 줄이기":
 * with reduced motion the effects rest in their final, static state.
 *
 * The debug-only "연출 비교" screen lets the team try the variants side by side and apply one to the real
 * app; [MotionStyle.Default] is what ships. Stage 1 (press · highlight · entrance) was chosen on 2026-11-26 and
 * stage 2 on 2026-11-27, both in "연출 비교" (stage 2 kept every recommended variant). On 2026-11-27 the main-button
 * highlight became PULSE_SHINE (the breathing glow plus the shine band) at the user's request.
 * Each field is read only by the area named in the comment above it.
 */
data class MotionStyle(
    // 1단계 · 로비와 버튼
    val press: PressStyle = PressStyle.BOUNCE_INK,
    val highlight: HighlightStyle = HighlightStyle.PULSE_SHINE,
    val entrance: EntranceStyle = EntranceStyle.STAMP_STAGGER,
    // 2단계 · 플레이 화면 (play)
    val choiceReveal: ChoiceRevealStyle = ChoiceRevealStyle.RISE,
    val choicePick: ChoicePickStyle = ChoicePickStyle.LIFT,
    val statChange: StatChangeStyle = StatChangeStyle.ROLL,
    val bigResult: BigResultStyle = BigResultStyle.THUD,
    // 2단계 · 해 · 시기 전환 (time)
    val ageStamp: AgeStampStyle = AgeStampStyle.BIG_STAMP,
    val periodTransition: PeriodTransitionStyle = PeriodTransitionStyle.PAGE_TURN,
    // 2단계 · 칭호 팝업 · 로비 디테일 (lobby2)
    val titleUnlock: TitleUnlockStyle = TitleUnlockStyle.DROP_SPIN,
    val footerIcon: FooterIconStyle = FooterIconStyle.CHARACTER,
    val newBadge: NewBadgeStyle = NewBadgeStyle.POP_PING,
    val lobbyTime: LobbyTimeStyle = LobbyTimeStyle.LIVING,
    // 2단계 · 새 인생 · 엔딩 (bookends)
    val profileEntrance: ProfileEntranceStyle = ProfileEntranceStyle.STAMP_RISE,
    val appearancePop: AppearancePopStyle = AppearancePopStyle.WAVE,
    val statCountUp: StatCountUpStyle = StatCountUpStyle.TICK,
    val recapMoment: RecapMomentStyle = RecapMomentStyle.SURFACE,
    val certificateSeal: CertificateSealStyle = CertificateSealStyle.STAMP,
    val titleReveal: TitleRevealStyle = TitleRevealStyle.GLOW,
) {
    companion object { val Default = MotionStyle() }
}

enum class PressStyle(val label: String, val detail: String) {
    CLASSIC("기본", "살짝 작아지고 기본 물결"),
    BOUNCE("탄성", "놓을 때 통 튕김 + 기본 물결"),
    BOUNCE_INK("탄성 + 잉크", "튕김 + 누른 곳에서 잉크가 번짐"),
}

enum class HighlightStyle(val label: String, val detail: String) {
    NONE("없음", "강조 없음"),
    SHINE("빛줄기", "몇 초마다 빛이 대각선으로 지나감"),
    SHINE_GLOW("빛줄기 + 테두리", "빛줄기와 은은하게 빛나는 테두리"),
    PULSE("숨쉬는 빛", "버튼 주변 빛이 천천히 커졌다 작아짐"),
    PULSE_SHINE("숨쉬는 빛 + 빛줄기", "숨쉬는 빛에 더해, 숨 두 번마다 빛줄기가 대각선으로 지나감"),
}

enum class EntranceStyle(val label: String, val detail: String) {
    NONE("없음", "바로 표시"),
    STAGGER("순차 등장", "요소가 차례로 떠오름"),
    STAMP_STAGGER("타이틀 도장 + 순차", "타이틀 글자가 도장처럼 찍히고 나머지가 차례로 떠오름"),
}

// ---- play · 선택지 + 스탯 (DecisionScreen, PlayTopBar) ----

/** 본문이 끝나고 선택지가 처음 나타날 때 (play). */
enum class ChoiceRevealStyle(val label: String, val detail: String) {
    NONE("없음", "본문이 끝나면 선택지가 한꺼번에 나타나요"),
    RISE("차례로 떠오름", "카드가 한 장씩 아래에서 떠오르고, 자리 잡을 때 ABC 표식이 톡 찍혀요"),
    DEAL("한 장씩 내려놓기", "카드를 테이블에 놓듯 옆에서 미끄러져 들어와 살짝 튕기며 멈춰요"),
    INK("먹물 번짐", "ABC 표식에서 먹물이 번지듯 카드가 차올라요"),
}

/** 선택지를 고른 순간 (play). */
enum class ChoicePickStyle(val label: String, val detail: String) {
    NONE("없음", "누르면 바로 결과 화면으로 바뀌어요"),
    LIFT("금빛 들어올림", "고른 카드가 금빛으로 번쩍이며 살짝 떠오르고, 나머지는 아래로 떨어져요"),
    STAMP("도장 쾅", "고른 카드에 금색 '선택' 도장이 찍히고, 나머지는 흐려지며 가라앉아요"),
    MORPH("결과로 이어짐", "고른 카드가 결과 카드 자리로 미끄러져 결과로 바뀌어요"),
}

/** 결과의 능력치 변화 칩과 건강 게이지 (play). */
enum class StatChangeStyle(val label: String, val detail: String) {
    NONE("없음", "변화 칩만 잠깐 떠올랐다 사라지고, 건강 게이지는 바로 바뀌어요"),
    ROLL("숫자 굴림", "칩 숫자가 0부터 굴러 오르고 칩 속 막대가 차올라요. 깎이면 붉게 흔들리고, 건강 게이지도 차오르거나 깎여요"),
    FLOAT("떠오르는 숫자", "+5는 위로 떠오르고 −3은 아래로 떨어지며 사라져요. 건강 게이지 끝에서도 같은 숫자가 떠요"),
}

/** 결과가 클 때만 더하는 연출 (play). */
enum class BigResultStyle(val label: String, val detail: String) {
    NONE("없음", "결과가 크든 작든 같은 연출이에요"),
    THUD("쿵 울림", "큰 결과에만 화면이 짧게 흔들리고 진동이 세져요. 좋은 일은 금빛, 나쁜 일은 붉은 그늘이 번져요"),
    GLOW("빛 번짐", "흔들림 없이 결과 카드 둘레에 금빛·붉은빛이 번지고 진동만 세져요"),
}

// ---- time · 해 · 시기 전환 (PeriodTrack, PlayHost overlay) ----

/**
 * 해가 바뀔 때 시기선 위 표시 (time). 나이는 만들지 않는다: 도장에는 화면에 이미 있는
 * 'N번째 해'(PeriodTrack의 yearLabel)만 찍는다(2026-11-26 사용자 결정).
 */
enum class AgeStampStyle(val label: String, val detail: String) {
    NONE("없음", "해 표시 없이 시기선 점이 바로 옮겨 가요"),
    SEAL("해 도장", "시기선 점 위에 붉은 ‘N번째 해’ 도장이 쾅 찍혔다가 사라져요"),
    BIG_STAMP("큰 글자 도장", "‘3번째 해’가 한 글자씩 크게 찍힌 뒤 시기선 점 속으로 쏙 들어가요"),
    ROLL("숫자 넘김", "시기선 위 작은 표에서 해 숫자가 위로 넘어가며 바뀌어요"),
}

/** 시기가 바뀔 때 화면 전환 (time). */
enum class PeriodTransitionStyle(val label: String, val detail: String) {
    NONE("없음", "지금처럼 새 장 표지가 바로 나와요"),
    FILM_ADVANCE("필름 넘김", "지난 시기의 끝 장면이 지지직 한 칸 넘어가고 새 장면이 열려요"),
    PAGE_TURN("책장 넘김", "지난 시기의 마지막 장이 오른쪽 아래 모서리부터 넘어가며 새 장이 드러나요"),
}

// ---- lobby2 · 칭호 팝업 · 로비 디테일 (CertificateView overlay, HomeScreen) ----

/** 인생기록증이 처음 열릴 때의 칭호 획득 팝업 (lobby2). */
enum class TitleUnlockStyle(val label: String, val detail: String) {
    NONE("없음", "팝업 없이 기록증 메달만 살짝 반짝여요"),
    DROP_SPIN("떨어져 돌며 안착", "메달이 위에서 떨어져 빙글 돌다 통 튀며 자리 잡고, 빛이 천천히 숨 쉬어요"),
    STAMP("도장 쾅", "메달이 크게 내려와 도장처럼 찍히고, 금빛 잉크가 번지며 카드가 살짝 흔들려요"),
    FLIP_REVEAL("뒤집어 공개", "어두운 뒷면이 바르르 떨다 휙 뒤집히며 금빛 앞면과 반짝이가 터져요"),
}

/** 로비 아래 버튼(지난 인생 · 칭호 수집 · 설정) 아이콘 (lobby2). */
enum class FooterIconStyle(val label: String, val detail: String) {
    NONE("없음", "아이콘은 가만히, 버튼만 눌려요"),
    WIGGLE("흔들흔들", "누르면 세 아이콘이 장난감처럼 좌우로 흔들려요"),
    CHARACTER("아이콘마다 다르게", "책은 펼쳤다 닫히고, 메달은 대롱대롱, 톱니는 한 칸 돌아요"),
}

/** 칭호 수집 버튼의 새 칭호 금색 점 (lobby2). */
enum class NewBadgeStyle(val label: String, val detail: String) {
    NONE("없음", "금색 점만 가만히 떠 있어요"),
    POP_PING("톡 + 물결", "로비가 열릴 때 점이 톡 튀어나오고 물결이 한 번 퍼져요"),
    BREATH("숨쉬는 점", "점 둘레의 빛이 메인 버튼처럼 천천히 숨 쉬어요"),
}

/** 기기 시각을 따르는 로비 하늘 (lobby2). */
enum class LobbyTimeStyle(val label: String, val detail: String) {
    NONE("항상 같게", "시간과 상관없이 지금 로비 그대로예요"),
    GRADE("하늘빛 물들기", "아침은 맑게, 노을은 주황빛, 밤은 푸르게 물들고 빛도 해처럼 옮겨 가요"),
    LIVING("별과 반딧불", "하늘빛에 더해 아침엔 빛줄기, 노을엔 불티, 밤엔 별과 반딧불이 떠요"),
}

// ---- bookends · 새 인생 · 엔딩 (CharacterSelectScreen, EndingFlowScreen) ----

/** 새 인생(프로필) 화면 등장 (bookends). */
enum class ProfileEntranceStyle(val label: String, val detail: String) {
    NONE("없음", "바로 표시"),
    RISE("차례로 떠오름", "제목부터 섹션이 차례로 살짝 떠오름"),
    STAMP_RISE("제목 도장 + 차례로", "'새 인생' 글자가 도장처럼 찍히고 섹션이 뒤따라 떠오름"),
    UNFOLD("종이 펼침", "섹션이 접힌 종이처럼 위쪽 끝을 축으로 펼쳐짐"),
}

/** 모습 옵션을 바꿨을 때 네 시기 아바타 (bookends). */
enum class AppearancePopStyle(val label: String, val detail: String) {
    NONE("없음", "모습이 그냥 바뀜"),
    BOUNCE("통 튀기", "네 모습이 발끝을 축으로 찌그러졌다 동시에 통 튐"),
    WAVE("나이 따라 물결", "통 튀는 움직임이 영아기부터 노년기까지 물결처럼 지나감"),
    INK("잉크 번짐", "모습은 그대로, 뒤에서 잉크가 동그랗게 번졌다 사라짐"),
}

/** 주사위가 멈춘 뒤 능력치 숫자 (bookends). */
enum class StatCountUpStyle(val label: String, val detail: String) {
    NONE("없음", "최종 값이 바로 보임"),
    TICK("숫자 세기", "주사위가 멈추면 능력치 숫자가 위에서부터 차례로 올라가고 통 멈춤"),
    SLOT("슬롯 릴", "숫자 자리가 슬롯머신처럼 굴러가다 줄마다 딱 멈춤"),
    STAMP("결과 도장", "세지 않고, 줄마다 최종 숫자가 도장처럼 쾅 찍힘"),
}

/** 주마등 한 장면 (bookends). */
enum class RecapMomentStyle(val label: String, val detail: String) {
    NONE("지금처럼", "장면이 서서히 바뀜"),
    SURFACE("기억 떠오름", "그림이 살짝 다가오며 자리 잡고 글줄이 차례로 떠오름"),
    DEVELOP("사진 현상", "빛바랜 사진이 제 색을 찾고 글이 잉크처럼 위에서부터 배어 나옴"),
    LANTERN("주마등 회전", "장면이 등불 돌듯 옆으로 지나가고 따뜻한 빛이 한 번 스침"),
}

/** 인생기록증이 처음 열릴 때 도장 (bookends). */
enum class CertificateSealStyle(val label: String, val detail: String) {
    NONE("도장 없음", "지금처럼 금빛 선과 메달만"),
    STAMP("도장 쾅", "카드가 떠오른 뒤 인주 도장이 사진 모서리에 쾅 찍히고 잉크 테가 퍼짐"),
    INK_BLEED("인주 번짐", "카드와 본문이 은은히 나타나고 도장이 가운데부터 종이에 스며듦"),
}

/** 기록증 안 칭호 공개. 칭호 획득 팝업(titleUnlock)이 보일 때는 쉰다 (bookends). */
enum class TitleRevealStyle(val label: String, val detail: String) {
    NONE("지금처럼", "메달이 기록증 빛과 함께 돌아 들어옴"),
    GLOW("금빛 숨결", "메달이 자리 잡을 때 메달과 칭호 뒤로 금빛이 한 번 부풀었다 가라앉음"),
    SPIN_STAMP("메달 회전 + 칭호 도장", "메달이 한 바퀴 돌아 내려앉고 칭호 글자가 하나씩 도장처럼 찍힘"),
}

val LocalMotionStyle = staticCompositionLocalOf { MotionStyle.Default }

internal class MotionStyleStore(context: Context) {
    val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Missing or unknown stored names fall back to [MotionStyle.Default], field by field. */
    fun read(): MotionStyle {
        val d = MotionStyle.Default
        return MotionStyle(
            press = enumOr(preferences.getString(PRESS_KEY, null), d.press),
            highlight = enumOr(preferences.getString(HIGHLIGHT_KEY, null), d.highlight),
            entrance = enumOr(preferences.getString(ENTRANCE_KEY, null), d.entrance),
            choiceReveal = enumOr(preferences.getString(CHOICE_REVEAL_KEY, null), d.choiceReveal),
            choicePick = enumOr(preferences.getString(CHOICE_PICK_KEY, null), d.choicePick),
            statChange = enumOr(preferences.getString(STAT_CHANGE_KEY, null), d.statChange),
            bigResult = enumOr(preferences.getString(BIG_RESULT_KEY, null), d.bigResult),
            ageStamp = enumOr(preferences.getString(AGE_STAMP_KEY, null), d.ageStamp),
            periodTransition = enumOr(preferences.getString(PERIOD_TRANSITION_KEY, null), d.periodTransition),
            titleUnlock = enumOr(preferences.getString(TITLE_UNLOCK_KEY, null), d.titleUnlock),
            footerIcon = enumOr(preferences.getString(FOOTER_ICON_KEY, null), d.footerIcon),
            newBadge = enumOr(preferences.getString(NEW_BADGE_KEY, null), d.newBadge),
            lobbyTime = enumOr(preferences.getString(LOBBY_TIME_KEY, null), d.lobbyTime),
            profileEntrance = enumOr(preferences.getString(PROFILE_ENTRANCE_KEY, null), d.profileEntrance),
            appearancePop = enumOr(preferences.getString(APPEARANCE_POP_KEY, null), d.appearancePop),
            statCountUp = enumOr(preferences.getString(STAT_COUNT_UP_KEY, null), d.statCountUp),
            recapMoment = enumOr(preferences.getString(RECAP_MOMENT_KEY, null), d.recapMoment),
            certificateSeal = enumOr(preferences.getString(CERTIFICATE_SEAL_KEY, null), d.certificateSeal),
            titleReveal = enumOr(preferences.getString(TITLE_REVEAL_KEY, null), d.titleReveal),
        )
    }

    fun write(style: MotionStyle) {
        preferences.edit()
            .putString(PRESS_KEY, style.press.name)
            .putString(HIGHLIGHT_KEY, style.highlight.name)
            .putString(ENTRANCE_KEY, style.entrance.name)
            .putString(CHOICE_REVEAL_KEY, style.choiceReveal.name)
            .putString(CHOICE_PICK_KEY, style.choicePick.name)
            .putString(STAT_CHANGE_KEY, style.statChange.name)
            .putString(BIG_RESULT_KEY, style.bigResult.name)
            .putString(AGE_STAMP_KEY, style.ageStamp.name)
            .putString(PERIOD_TRANSITION_KEY, style.periodTransition.name)
            .putString(TITLE_UNLOCK_KEY, style.titleUnlock.name)
            .putString(FOOTER_ICON_KEY, style.footerIcon.name)
            .putString(NEW_BADGE_KEY, style.newBadge.name)
            .putString(LOBBY_TIME_KEY, style.lobbyTime.name)
            .putString(PROFILE_ENTRANCE_KEY, style.profileEntrance.name)
            .putString(APPEARANCE_POP_KEY, style.appearancePop.name)
            .putString(STAT_COUNT_UP_KEY, style.statCountUp.name)
            .putString(RECAP_MOMENT_KEY, style.recapMoment.name)
            .putString(CERTIFICATE_SEAL_KEY, style.certificateSeal.name)
            .putString(TITLE_REVEAL_KEY, style.titleReveal.name)
            .apply()
    }

    /**
     * Back to [MotionStyle.Default]. Removes keys one by one: Editor.clear() notifies no listener on
     * API 26–29, so ProvideMotionStyle would keep the old style there.
     */
    fun reset() {
        val editor = preferences.edit()
        preferences.all.keys.forEach(editor::remove)
        editor.apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    /**
     * The store keys double as the comparison screen's tag prefixes:
     * `<key>_<VARIANT>`, `<key>_replay`, `<key>_apply` (extra controls: `<key>_<control>`).
     */
    companion object {
        const val PREFERENCES_NAME = "motion_style"
        const val PRESS_KEY = "press"
        const val HIGHLIGHT_KEY = "highlight"
        const val ENTRANCE_KEY = "entrance"
        // play
        const val CHOICE_REVEAL_KEY = "choice_reveal"
        const val CHOICE_PICK_KEY = "choice_pick"
        const val STAT_CHANGE_KEY = "stat_change"
        const val BIG_RESULT_KEY = "big_result"
        // time
        const val AGE_STAMP_KEY = "age_stamp"
        const val PERIOD_TRANSITION_KEY = "period_transition"
        // lobby2
        const val TITLE_UNLOCK_KEY = "title_unlock"
        const val FOOTER_ICON_KEY = "footer_icon"
        const val NEW_BADGE_KEY = "new_badge"
        const val LOBBY_TIME_KEY = "lobby_time"
        // bookends
        const val PROFILE_ENTRANCE_KEY = "profile_entrance"
        const val APPEARANCE_POP_KEY = "appearance_pop"
        const val STAT_COUNT_UP_KEY = "stat_count_up"
        const val RECAP_MOMENT_KEY = "recap_moment"
        const val CERTIFICATE_SEAL_KEY = "certificate_seal"
        const val TITLE_REVEAL_KEY = "title_reveal"
    }
}

/** Debug builds may override the shipped style from the comparison screen; release always uses the default. */
@Composable
internal fun ProvideMotionStyle(allowOverride: Boolean, content: @Composable () -> Unit) {
    if (!allowOverride) {
        content()
        return
    }
    val context = LocalContext.current
    val store = remember(context) { MotionStyleStore(context) }
    var style by remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> style = store.read() }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        style = store.read()
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    CompositionLocalProvider(LocalMotionStyle provides style, content = content)
}
