package com.example.lifegame.ui.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import com.example.lifegame.domain.model.LifePeriod
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * AI가 만든 얼굴 시트(3열 × 2행 머리 6개)를 시기별로 잘라 기본 몸 · 피부 레이어 위에 얹는다.
 * 결과는 레이어와 같은 192×312 캔버스라 [PlayerAvatar]가 기존 레이어처럼 최근접 확대로 그린다.
 */
object PhotoFaceSheet {
    const val CANVAS_WIDTH = 192
    const val CANVAS_HEIGHT = 312

    /** 시기별 기본 머리 자리(avatar_v4 캔버스 좌표). scripts/build_aged_avatar_layers.py의 Face 계산값이다. */
    data class HeadBox(val top: Int, val left: Int, val right: Int, val chin: Int, val centerX: Int)

    private val heads = mapOf(
        LifePeriod.INFANT to HeadBox(top = 138, left = 67, right = 124, chin = 218, centerX = 95),
        LifePeriod.CHILD to HeadBox(top = 90, left = 66, right = 125, chin = 166, centerX = 96),
        LifePeriod.TEEN to HeadBox(top = 30, left = 63, right = 129, chin = 110, centerX = 95),
        LifePeriod.YOUNG_ADULT to HeadBox(top = 14, left = 57, right = 135, chin = 118, centerX = 96),
        LifePeriod.MIDDLE to HeadBox(top = 20, left = 61, right = 131, chin = 106, centerX = 95),
        LifePeriod.OLD_AGE to HeadBox(top = 30, left = 61, right = 131, chin = 122, centerX = 95),
    )

    fun headBox(period: LifePeriod): HeadBox = heads.getValue(period)

    /** 시트 칸 순서: 위 줄 영아 · 유년 · 청소년, 아래 줄 청년 · 중년 · 노년 (OpenAiFaceSheetClient 프롬프트와 같다). */
    fun cellOf(period: LifePeriod): Int = when (period) {
        LifePeriod.INFANT -> 0
        LifePeriod.CHILD -> 1
        LifePeriod.TEEN -> 2
        LifePeriod.YOUNG_ADULT -> 3
        LifePeriod.MIDDLE -> 4
        LifePeriod.OLD_AGE -> 5
    }

    private val sheets = LruCache<String, Bitmap>(2)
    private val composed = LruCache<String, Bitmap>(12)

    /** file:// 참조만 실제 파일로 본다. Fake 응답(fake://)이나 지워진 파일이면 null. */
    fun pathOf(ref: String?): String? {
        if (ref.isNullOrBlank() || !ref.startsWith("file:")) return null
        val path = runCatching { java.net.URI(ref).path }.getOrNull() ?: return null
        return path.takeIf { java.io.File(it).isFile }
    }

    /** 몸 → 피부(턱 아래만) → AI 머리 순서로 그린 캔버스. 읽지 못하면 null(호출 쪽이 기본 레이어로 그린다). */
    fun compose(context: Context, look: AvatarLook, sheetPath: String): Bitmap? {
        val key = "$sheetPath|${look.period}|${look.bodyType}|${look.skinToneId}"
        composed.get(key)?.let { return it }
        val sheet = sheets.get(sheetPath) ?: BitmapFactory.decodeFile(sheetPath)?.also { sheets.put(sheetPath, it) } ?: return null
        val head = headTile(sheet, cellOf(look.period)) ?: return null
        val box = heads.getValue(look.period)
        val period = look.period.name.lowercase()
        val body = asset(context, "images/avatar_v4/body/body_${period}_${look.bodyType.name.lowercase()}.png") ?: return null
        val skin = asset(context, "images/avatar_v4/face/skin_${period}_${look.skinToneId}_${look.bodyType.name.lowercase()}.png") ?: return null

        val out = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(body, 0f, 0f, null)
        // 기본 얼굴 · 귀는 AI 머리로 바꾸고, 목 · 팔 · 손 피부만 남긴다.
        val below = Rect(0, box.chin - 1, CANVAS_WIDTH, CANVAS_HEIGHT)
        canvas.drawBitmap(skin, below, below, null)

        // 머리 자리(귀 포함 폭, 머리카락 위 ~ 턱)에 비율을 지켜 맞춘다. 발 쪽 기준선은 턱이다.
        val targetW = (box.right - box.left) * 1.08f
        val targetH = (box.chin - box.top + 6).toFloat()
        val scale = minOf(targetW / head.width, targetH / head.height)
        val w = (head.width * scale).roundToInt().coerceAtLeast(1)
        val h = (head.height * scale).roundToInt().coerceAtLeast(1)
        val small = downscale(head, w, h)
        crispAlpha(small)
        canvas.drawBitmap(small, (box.centerX - w / 2).toFloat(), (box.chin + 3 - h).toFloat(), Paint())
        composed.put(key, out)
        return out
    }

    /** 칸 하나에서 머리만 잘라 낸다. 배경이 투명하지 않게 왔으면 칸 가장자리 색을 배경으로 보고 지운다. */
    fun headTile(sheet: Bitmap, cell: Int): Bitmap? {
        val cellW = sheet.width / 3
        val cellH = sheet.height / 2
        val x0 = (cell % 3) * cellW
        val y0 = (cell / 3) * cellH
        val pixels = IntArray(cellW * cellH)
        sheet.getPixels(pixels, 0, cellW, x0, y0, cellW, cellH)
        val opaqueBackground = listOf(0, cellW - 1, (cellH - 1) * cellW, cellW * cellH - 1).all { (pixels[it] ushr 24) > 200 }
        if (opaqueBackground) {
            val bg = pixels[0]
            for (i in pixels.indices) if (colorDistance(pixels[i], bg) < BACKGROUND_TOLERANCE) pixels[i] = 0
        }
        var minX = cellW; var minY = cellH; var maxX = -1; var maxY = -1
        for (y in 0 until cellH) for (x in 0 until cellW) {
            if ((pixels[y * cellW + x] ushr 24) >= 128) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX || maxY < minY) return null
        val w = maxX - minX + 1
        val h = maxY - minY + 1
        // 너무 작은 조각(잡티)은 머리로 보지 않는다
        if (w < cellW / 8 || h < cellH / 8) return null
        val tile = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        tile.setPixels(pixels, minY * cellW + minX, cellW, 0, 0, w, h)
        return tile
    }

    /** 반씩 줄여 가며 축소해 도트가 뭉개지지 않게 한다(한 번에 크게 줄이면 계단 현상이 생긴다). */
    private fun downscale(src: Bitmap, w: Int, h: Int): Bitmap {
        var current = src
        while (current.width / 2 >= w && current.height / 2 >= h) {
            current = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
        }
        return Bitmap.createScaledBitmap(current, w, h, true)
    }

    private fun crispAlpha(bitmap: Bitmap) {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in px.indices) {
            val a = px[i] ushr 24
            px[i] = if (a < 128) 0 else (px[i] or (0xFF shl 24))
        }
        bitmap.setPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun colorDistance(a: Int, b: Int): Int =
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))

    private const val BACKGROUND_TOLERANCE = 48

    private fun asset(context: Context, path: String): Bitmap? = runCatching {
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inScaled = false }) }
    }.getOrNull()
}
