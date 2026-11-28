package com.example.lifegame.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

data class ExperiencePreferences(
    val motionEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
) {
    fun reduceMotion(systemAnimatorScale: Float): Boolean = !motionEnabled || systemAnimatorScale <= 0f
}

val LocalExperiencePreferences = compositionLocalOf { ExperiencePreferences() }
internal val LocalSystemAnimatorScale = compositionLocalOf<Float?> { null }

/** Only these three presentation preferences are stored here; no game or photo data. */
internal class ExperiencePreferencesStore(context: Context) {
    val preferences: SharedPreferences = context.getSharedPreferences("lifegame_ui_prefs", Context.MODE_PRIVATE)

    fun read() = ExperiencePreferences(
        motionEnabled = preferences.getBoolean(MOTION_KEY, true),
        hapticsEnabled = preferences.getBoolean(HAPTICS_KEY, true),
        soundEnabled = preferences.getBoolean(SOUND_KEY, false),
    )

    fun write(value: ExperiencePreferences) {
        preferences.edit()
            .putBoolean(MOTION_KEY, value.motionEnabled)
            .putBoolean(HAPTICS_KEY, value.hapticsEnabled)
            .putBoolean(SOUND_KEY, value.soundEnabled)
            .apply()
    }

    companion object {
        const val MOTION_KEY = "experience_motion_enabled"
        const val HAPTICS_KEY = "experience_haptics_enabled"
        const val SOUND_KEY = "experience_sound_enabled"
    }
}

@Composable
internal fun ProvideExperiencePreferences(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { ExperiencePreferencesStore(context) }
    var preferences by remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> preferences = store.read() }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        preferences = store.read()
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val animatorScale = rememberSystemAnimatorScale()
    CompositionLocalProvider(
        LocalExperiencePreferences provides preferences,
        LocalSystemAnimatorScale provides animatorScale,
        content = content,
    )
}

/** Observe runtime accessibility/developer-setting changes without restarting the activity. */
@Composable
internal fun rememberSystemAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    fun readScale() = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    var scale by remember(resolver) { mutableFloatStateOf(readScale()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = readScale() }
        }
        val registered = runCatching {
            resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        }.isSuccess
        scale = readScale()
        onDispose { if (registered) resolver.unregisterContentObserver(observer) }
    }
    return scale
}

/** A slow 0..1 phase. Removing the transition stops its frame work when the screen is inactive. */
@Composable
fun rememberAmbientPhase(enabled: Boolean = true): State<Float> {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduceMotion = rememberReduceMotion()
    val restingPhase = remember { mutableFloatStateOf(0f) }
    if (!enabled || reduceMotion || lifecycleState != Lifecycle.State.RESUMED) return restingPhase
    return rememberInfiniteTransition(label = "ambient_scene").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambient_phase",
    )
}

private const val PRESS_SCALE_THRESHOLD = 0.0005f

/** Presentation-only modifier: callers retain their existing clickable, guards and semantics. */
@Composable
internal fun Modifier.experiencePress(
    interactions: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val bounce = LocalMotionStyle.current.press != PressStyle.CLASSIC
    val down = enabled && pressed && !reduceMotion
    val scale = animateFloatAsState(
        if (down) pressedScale else 1f,
        // With a bouncy press style the release uses a low damping ratio so the button springs
        // slightly past its resting size and back, like a soft game button. The overshoot is under 1%,
        // so the visibility threshold must be far below the 0.01 default or the spring stops before it.
        animationSpec = when {
            reduceMotion -> snap()
            !bounce -> spring(dampingRatio = 0.8f, stiffness = 450f, visibilityThreshold = PRESS_SCALE_THRESHOLD)
            down -> spring(dampingRatio = 0.8f, stiffness = 650f, visibilityThreshold = PRESS_SCALE_THRESHOLD)
            else -> spring(dampingRatio = 0.35f, stiffness = 420f, visibilityThreshold = PRESS_SCALE_THRESHOLD)
        },
        visibilityThreshold = PRESS_SCALE_THRESHOLD,
        label = "experience_press",
    )
    // Read frame values only in the layer, without recomposing the button's text/content.
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** The existing clickable remains the only event source for touch, keyboard and accessibility. */
@Composable
internal fun rememberExperienceClick(onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    val preferences by rememberUpdatedState(LocalExperiencePreferences.current)
    val action by rememberUpdatedState(onClick)
    return remember(view) {
        {
            runCatching {
                if (preferences.hapticsEnabled) {
                    // No IGNORE_* flags: Android respects both the system and the view's setting.
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                if (preferences.soundEnabled && Settings.System.getInt(
                        view.context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0,
                    ) != 0
                ) {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
            action()
        }
    }
}

@Composable
fun ExperienceSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { ExperiencePreferencesStore(context) }
    val preferences = LocalExperiencePreferences.current
    val systemReduced = (LocalSystemAnimatorScale.current ?: rememberSystemAnimatorScale()) <= 0f
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("experience_settings"),
        title = { Text("화면·소리 설정") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExperienceToggle("화면 움직임", "그림의 잔잔한 움직임과 버튼 효과", preferences.motionEnabled, "experience_motion") {
                    store.write(store.read().copy(motionEnabled = it))
                }
                if (systemReduced) Text("기기에서 애니메이션을 꺼 두어 움직임을 줄이고 있어요.", style = MaterialTheme.typography.bodySmall)
                ExperienceToggle("버튼 진동", "선택할 때 짧은 진동", preferences.hapticsEnabled, "experience_haptics") {
                    store.write(store.read().copy(hapticsEnabled = it))
                }
                ExperienceToggle("버튼 소리", "기기의 터치 소리 설정을 따라요", preferences.soundEnabled, "experience_sound") {
                    store.write(store.read().copy(soundEnabled = it))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun ExperienceToggle(label: String, description: String, checked: Boolean, tag: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag(tag)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
