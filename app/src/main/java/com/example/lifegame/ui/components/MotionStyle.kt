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
 * The debug-only "연출 비교" screen lets the team try the variants side by side and apply one
 * to the real lobby; [MotionStyle.Default] is what ships (chosen on 2026-11-26 with that screen).
 */
data class MotionStyle(
    val press: PressStyle = PressStyle.BOUNCE_INK,
    val highlight: HighlightStyle = HighlightStyle.PULSE,
    val entrance: EntranceStyle = EntranceStyle.STAMP_STAGGER,
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
}

enum class EntranceStyle(val label: String, val detail: String) {
    NONE("없음", "바로 표시"),
    STAGGER("순차 등장", "요소가 차례로 떠오름"),
    STAMP_STAGGER("타이틀 도장 + 순차", "타이틀 글자가 도장처럼 찍히고 나머지가 차례로 떠오름"),
}

val LocalMotionStyle = staticCompositionLocalOf { MotionStyle.Default }

internal class MotionStyleStore(context: Context) {
    val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): MotionStyle = MotionStyle(
        press = enumOr(preferences.getString(PRESS_KEY, null), MotionStyle.Default.press),
        highlight = enumOr(preferences.getString(HIGHLIGHT_KEY, null), MotionStyle.Default.highlight),
        entrance = enumOr(preferences.getString(ENTRANCE_KEY, null), MotionStyle.Default.entrance),
    )

    fun write(style: MotionStyle) {
        preferences.edit()
            .putString(PRESS_KEY, style.press.name)
            .putString(HIGHLIGHT_KEY, style.highlight.name)
            .putString(ENTRANCE_KEY, style.entrance.name)
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

    companion object {
        const val PREFERENCES_NAME = "motion_style"
        const val PRESS_KEY = "press"
        const val HIGHLIGHT_KEY = "highlight"
        const val ENTRANCE_KEY = "entrance"
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
