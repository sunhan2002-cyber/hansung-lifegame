package com.example.lifegame.ui.avatar

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 그림 파일을 새로 만들지 않고 외형 색을 바꾼다(2026-11-13 김재겸 과제 6, 커스터마이징).
 *
 * - 머리 염색: 머리 레이어의 밝기 순서(윤곽선 → 바탕 → 반짝임)를 그대로 두고 고른 색의 어두운 쪽 ~ 밝은 쪽으로 옮긴다.
 *   검은 머리도 갈색 머리도 같은 규칙으로 물든다. 노년기 흰머리는 나이 표현이라 호출 쪽에서 염색하지 않는다.
 * - 옷 색: 몸 레이어(옷 · 신발 · 윤곽, 피부는 별도 레이어)에서 가장 넓게 칠해진 색 계열(보통 윗옷)만 고른 색으로 바꾼다.
 *   흰 티 · 윤곽선 같은 무채색과 밝기가 크게 다른 바지는 그대로 둔다.
 *   청년기처럼 셔츠와 청바지가 같은 파란 계열이고 밝기도 비슷하면 바지도 함께 바뀐다(제한 사항).
 *
 * 픽셀 배열(ARGB)만 다루는 순수 함수라서 JVM 단위 테스트로 검증한다.
 */
object AvatarRecolor {
    private const val HUE_BINS = 18
    /** 같은 계열로 보는 색상 차이(한 바퀴 = 1). 약 25도 */
    private const val HUE_TOLERANCE = 0.07f
    /** 같은 옷으로 보는 밝기 차이. 이보다 어둡거나 밝은 같은 계열(청바지 등)은 남긴다. */
    private const val LIGHT_TOLERANCE = 0.2f

    fun dyeHair(src: Bitmap, target: Int): Bitmap = transform(src) { dyeHairPixels(it, target) }

    fun recolorOutfit(src: Bitmap, targetHue: Float, targetSaturation: Float): Bitmap =
        transform(src) { recolorOutfitPixels(it, targetHue, targetSaturation) }

    private inline fun transform(src: Bitmap, change: (IntArray) -> Unit): Bitmap {
        val px = IntArray(src.width * src.height)
        src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)
        change(px)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, src.width, 0, 0, src.width, src.height)
        return out
    }

    /** 밝기 순서를 지키며 [target] 색으로 물들인다. 투명한 픽셀은 그대로. */
    fun dyeHairPixels(px: IntArray, target: Int) {
        var lo = 1f
        var hi = 0f
        for (p in px) {
            if (alpha(p) == 0) continue
            val l = luma(p)
            lo = min(lo, l)
            hi = max(hi, l)
        }
        if (hi < lo) return
        val range = max(hi - lo, 0.08f)
        val tr = red(target) / 255f
        val tg = green(target) / 255f
        val tb = blue(target) / 255f
        for (i in px.indices) {
            val p = px[i]
            val a = alpha(p)
            if (a == 0) continue
            val t = (luma(p) - lo) / range
            val f = 0.35f + 0.9f * t
            px[i] = argb(a, channel(tr * f), channel(tg * f), channel(tb * f))
        }
    }

    /** 가장 넓은 유채색 계열(보통 윗옷)만 [targetHue]로 바꾼다. 밝기는 그대로 두어 주름 · 그림자가 남는다. */
    fun recolorOutfitPixels(px: IntArray, targetHue: Float, targetSaturation: Float) {
        val bins = IntArray(HUE_BINS)
        val hsl = FloatArray(3)
        for (p in px) {
            if (alpha(p) < 128) continue
            toHsl(p, hsl)
            if (hsl[1] > 0.25f && hsl[2] > 0.12f && hsl[2] < 0.9f) bins[(hsl[0] * HUE_BINS).toInt().mod(HUE_BINS)]++
        }
        val best = bins.indices.maxByOrNull { bins[it] } ?: return
        if (bins[best] == 0) return
        val h0 = (best + 0.5f) / HUE_BINS
        val lights = ArrayList<Float>()
        for (p in px) {
            if (alpha(p) < 128) continue
            toHsl(p, hsl)
            if (hsl[1] > 0.25f && hsl[2] > 0.12f && hsl[2] < 0.9f && hueDistance(hsl[0], h0) < HUE_TOLERANCE) lights += hsl[2]
        }
        if (lights.isEmpty()) return
        lights.sort()
        val l0 = lights[lights.size / 2]
        for (i in px.indices) {
            val p = px[i]
            val a = alpha(p)
            if (a == 0) continue
            toHsl(p, hsl)
            if (hsl[1] > 0.18f && hueDistance(hsl[0], h0) < HUE_TOLERANCE && abs(hsl[2] - l0) < LIGHT_TOLERANCE) {
                val s = min(1f, hsl[1] * 0.5f + targetSaturation * 0.5f)
                px[i] = fromHsl(a, targetHue, s, hsl[2])
            }
        }
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b)
        return min(d, 1f - d)
    }

    private fun alpha(p: Int) = (p ushr 24) and 0xFF
    private fun red(p: Int) = (p shr 16) and 0xFF
    private fun green(p: Int) = (p shr 8) and 0xFF
    private fun blue(p: Int) = p and 0xFF
    private fun luma(p: Int) = (0.299f * red(p) + 0.587f * green(p) + 0.114f * blue(p)) / 255f
    private fun channel(v: Float) = (v.coerceIn(0f, 1f) * 255f).toInt()
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    /** HSL(색상 0~1, 채도, 밝기)로 바꾼다. */
    private fun toHsl(p: Int, out: FloatArray) {
        val r = red(p) / 255f
        val g = green(p) / 255f
        val b = blue(p) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        if (mx == mn) {
            out[0] = 0f; out[1] = 0f; out[2] = l
            return
        }
        val d = mx - mn
        val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f))
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } / 6f
        out[0] = h; out[1] = s; out[2] = l
    }

    private fun fromHsl(a: Int, h: Float, s: Float, l: Float): Int {
        if (s == 0f) {
            val v = channel(l)
            return argb(a, v, v, v)
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        fun hue(t0: Float): Float {
            var t = t0
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }
        return argb(a, channel(hue(h + 1f / 3f)), channel(hue(h)), channel(hue(h - 1f / 3f)))
    }
}
