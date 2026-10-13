package com.example.lifegame.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.pow

// 시안 7 전환 타이밍을 v4 화면(자동 진행 · 결정)에서도 같이 쓰려고 모아 둔 것.

/** 해 넘김 지지직: 넘어가다 40% 근처에서 덜컥 걸린 뒤 마저 넘어간다 (시간 비율 → 넘어간 비율). */
private val NormalAdvanceKeys = listOf(
    0f to 0f, 0.36f to 0.4167f, 0.42f to 0.3972f, 0.47f to 0.4333f,
    0.52f to 0.4083f, 0.58f to 0.4167f, 1f to 1f,
)

/** 1배속 해 넘김 길이 (ms). 배속이 올라가면 나눠서 짧게 쓴다. */
const val NOISE_ADVANCE_MILLIS = 340

/** 돌발 왜곡 길이 (ms). 강조 연출이라 배속과 상관없이 그대로 쓴다 (R-23). */
const val DISTORT_MILLIS = 680

internal fun easeInOut(t: Float): Float = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

internal fun easeOut(t: Float): Float = 1 - (1 - t).pow(3)

internal fun normalAdvance(p: Float): Float {
    for (i in 0 until NormalAdvanceKeys.lastIndex) {
        val (p0, v0) = NormalAdvanceKeys[i]
        val (p1, v1) = NormalAdvanceKeys[i + 1]
        if (p <= p1) {
            val local = (p - p0) / (p1 - p0)
            val eased = if (i == 0 || i == NormalAdvanceKeys.lastIndex - 1) easeInOut(local) else local
            return v0 + (v1 - v0) * eased
        }
    }
    return 1f
}

/** 왜곡 세기 곡선: 빠르게 최대로 올라가 잠깐 버티다 가라앉는다. */
internal fun distortEnvelope(p: Float): Float = when {
    p < 0.18f -> easeOut(p / 0.18f)
    p < 0.42f -> 1f
    else -> 1f - easeInOut((p - 0.42f) / 0.58f)
}

/** 시스템 애니메이션이 꺼져 있으면 지지직 · 왜곡을 건너뛴다. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
