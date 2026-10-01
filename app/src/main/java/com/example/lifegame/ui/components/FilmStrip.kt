package com.example.lifegame.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGamePicture
import com.example.lifegame.ui.theme.LifeGameSpecial
import com.example.lifegame.ui.theme.LifeGameSpecialPicture
import com.example.lifegame.ui.theme.LifeGameStatUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** 필름 한 컷. 같은 [key]면 같은 그림을 다시 쓴다. */
@Immutable
data class FilmShot(
    val key: String,
    val imageId: String,
    /** assets/images 아래 그림 파일 이름. 없거나 읽지 못하면 자리 표시 그림을 그린다. */
    val imageFile: String?,
    val isSpecial: Boolean,
)

/** 전환 중 필름의 순간 상태. 매 프레임 바뀌므로 그리기 단계에서만 읽는다. */
@Immutable
data class FilmMotion(
    /** 0 = 지금 컷, 1 = 다음 컷까지 한 칸 넘어간 상태 */
    val advance: Float = 0f,
    /** 0이 아니면 이 값을 시드로 입자 · 먼지 · 스크래치 · 깜빡임을 그린다 (지지직) */
    val noiseSeed: Int = 0,
    /** 왜곡 진폭 (dp). 0이면 왜곡 없음 */
    val distortion: Float = 0f,
    /** 물결이 흐르는 위치 (라디안) */
    val phase: Float = 0f,
    /** 필름 전체의 세로 떨림 (dp) */
    val jitterY: Float = 0f,
)

const val DISTORT_MAX_DP = 34f

private val StripHeight = 21.dp
private val SideBorder = 10.dp
private val HoleWidth = 10.dp
private val HoleHeight = 7.dp
private val HolePitch = 18.dp
private val GateCorner = 3.dp

// 왜곡 때 흰 번짐 줄: (그림 높이 대비 위치, 그림 폭 대비 길이)
private val Smears = listOf(
    0.256f to 0.35f, 0.368f to 0.59f, 0.472f to 0.44f, 0.592f to 0.71f, 0.652f to 0.32f,
    0.712f to 0.82f, 0.784f to 0.50f, 0.844f to 0.88f, 0.904f to 0.65f, 0.96f to 0.94f,
)

fun filmHeight(gateHeight: Dp): Dp = gateHeight + StripHeight * 2

/**
 * 위아래 구멍 띠 + 양옆 검은 테두리로 그림 한 장을 감싼 필름 (시안 7 C안).
 * [incoming]이 있으면 오른쪽에 다음 컷을 이어 붙이고 [FilmMotion.advance]만큼 왼쪽으로 넘긴다.
 */
@Composable
fun FilmStrip(
    current: FilmShot,
    incoming: FilmShot?,
    gateHeight: Dp,
    motion: () -> FilmMotion,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(filmHeight(gateHeight))) {
        val density = LocalDensity.current
        val pictureWidth = with(density) { (maxWidth - SideBorder * 2).roundToPx() }.coerceAtLeast(1)
        val pictureHeight = with(density) { gateHeight.roundToPx() }.coerceAtLeast(1)
        val currentPicture = rememberFilmPicture(current, pictureWidth, pictureHeight)
        val incomingPicture = incoming?.let { rememberFilmPicture(it, pictureWidth, pictureHeight) }

        Canvas(Modifier.fillMaxSize().clipToBounds()) {
            drawFilm(motion(), currentPicture, incomingPicture)
        }
    }
}

/** 팝업, "지금으로" 막대처럼 그림 없이 구멍 띠만 필요한 곳에 쓴다. */
@Composable
fun PerforationBand(
    modifier: Modifier = Modifier,
    holeWidth: Dp = HoleWidth,
    holeHeight: Dp = HoleHeight,
    pitch: Dp = HolePitch,
) {
    Canvas(modifier.clipToBounds()) {
        drawRect(LifeGameInk)
        val holeW = holeWidth.toPx()
        val holeH = holeHeight.toPx()
        val count = max(1, (size.width / pitch.toPx()).roundToInt())
        val step = size.width / count
        val y = (size.height - holeH) / 2
        repeat(count) { k ->
            drawRoundRect(
                color = LifeGamePaper,
                topLeft = Offset((step - holeW) / 2 + k * step, y),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(holeH * 0.2f),
            )
        }
    }
}

@Composable
private fun rememberFilmPicture(shot: FilmShot, width: Int, height: Int): ImageBitmap {
    val context = LocalContext.current
    val density = LocalDensity.current
    val captionPx = with(density) { 10.sp.toPx() }
    val placeholder = remember(shot.key, width, height) {
        placeholderPicture(shot, width, height, captionPx, density.density)
    }
    val loaded by produceState<ImageBitmap?>(null, shot.imageFile, width, height) {
        value = shot.imageFile?.let { file ->
            withContext(Dispatchers.IO) { loadAssetPicture(context, file, width, height) }
        }
    }
    return loaded ?: placeholder
}

/** 그림이 준비되기 전 자리: 단색 바탕, 아래쪽 바닥 면, 왼쪽 아래 이미지 ID. */
private fun placeholderPicture(
    shot: FilmShot,
    width: Int,
    height: Int,
    captionPx: Float,
    density: Float,
): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val base = if (shot.isSpecial) LifeGameSpecialPicture else LifeGamePicture
    val ground = if (shot.isSpecial) Color(0xFFE6D4D9) else Color(0xFFE2E5E9)
    canvas.drawColor(base.toArgb())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = ground.toArgb()
    canvas.drawRect(0f, height * 0.62f, width.toFloat(), height.toFloat(), paint)
    paint.color = LifeGameInk.copy(alpha = 0.6f).toArgb()
    paint.textSize = captionPx
    paint.letterSpacing = 0.08f
    val margin = 10f * density
    canvas.drawText(shot.imageId.uppercase(), margin, height - margin, paint)
    return bitmap.asImageBitmap()
}

/** assets/images/<fileName>을 필름 칸 크기에 맞춰 가운데를 잘라 읽는다. */
private fun loadAssetPicture(context: Context, fileName: String, width: Int, height: Int): ImageBitmap? =
    runCatching {
        val source = context.assets.open("images/$fileName").use(BitmapFactory::decodeStream)
            ?: return@runCatching null
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).roundToInt().coerceAtLeast(width),
            (source.height * scale).roundToInt().coerceAtLeast(height),
            true,
        )
        Bitmap.createBitmap(
            scaled,
            (scaled.width - width) / 2,
            (scaled.height - height) / 2,
            width,
            height,
        ).asImageBitmap()
    }.getOrNull()

private fun DrawScope.drawFilm(m: FilmMotion, current: ImageBitmap, incoming: ImageBitmap?) {
    val strip = StripHeight.toPx()
    val side = SideBorder.toPx()
    val gateHeight = size.height - strip * 2
    val trackX = -m.advance * size.width

    translate(top = m.jitterY.dp.toPx()) {
        // 떨릴 때 위아래가 비지 않도록 필름 바탕을 넉넉하게 칠한다
        drawRect(LifeGameInk, topLeft = Offset(0f, -size.height), size = Size(size.width, size.height * 3))
        drawPerforations(trackX, strip, gateHeight)
        drawPicture(current, Offset(trackX + side, strip), m, distort = true)
        if (incoming != null) {
            drawPicture(incoming, Offset(trackX + size.width + side, strip), m, distort = false)
        }
        val flash = (m.distortion / DISTORT_MAX_DP - 0.6f) / 0.4f
        if (flash > 0f) {
            drawRect(Color.White.copy(alpha = 0.12f * flash), Offset(0f, strip), Size(size.width, gateHeight))
        }
    }
}

private fun DrawScope.drawPerforations(trackX: Float, strip: Float, gateHeight: Float) {
    val holeW = HoleWidth.toPx()
    val holeH = HoleHeight.toPx()
    // 컷 하나에 구멍이 정수 개 들어가야 한 컷 넘긴 뒤에도 구멍 위치가 이어진다
    val count = max(1, (size.width / HolePitch.toPx()).roundToInt())
    val pitch = size.width / count
    val shift = trackX % pitch
    val corner = CornerRadius(1.5.dp.toPx())
    val topY = (strip - holeH) / 2
    val bottomY = strip + gateHeight + (strip - holeH) / 2
    for (k in -1..count) {
        val x = (pitch - holeW) / 2 + k * pitch + shift
        drawRoundRect(LifeGamePaper, Offset(x, topY), Size(holeW, holeH), corner)
        drawRoundRect(LifeGamePaper, Offset(x, bottomY), Size(holeW, holeH), corner)
    }
}

private fun DrawScope.drawPicture(image: ImageBitmap, topLeft: Offset, m: FilmMotion, distort: Boolean) {
    if (topLeft.x > size.width || topLeft.x + image.width < 0f) return
    if (distort && m.distortion > 0.01f) {
        drawDistorted(image, topLeft, m)
        return
    }
    val rect = Rect(topLeft, Size(image.width.toFloat(), image.height.toFloat()))
    val gate = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(GateCorner.toPx()))) }
    clipPath(gate) {
        drawImage(image, topLeft = topLeft)
        if (m.noiseSeed != 0) drawNoise(rect, m.noiseSeed)
    }
}

/** 지지직: 깜빡임 + 입자 + 먼지 + 세로 스크래치. 시드가 바뀔 때마다 다른 자리에 그린다. */
private fun DrawScope.drawNoise(rect: Rect, seed: Int) {
    val rnd = Random(seed)
    drawRect(Color.White.copy(alpha = 0.05f + rnd.nextFloat() * 0.15f), rect.topLeft, rect.size)
    val dot = 1.dp.toPx()
    repeat(240) {
        val s = dot * (1f + rnd.nextFloat() * 1.4f)
        val alpha = 0.15f + rnd.nextFloat() * 0.3f
        val color = if (rnd.nextFloat() < 0.55f) LifeGameInk.copy(alpha = alpha) else Color.White.copy(alpha = alpha)
        drawRect(color, Offset(rect.left + rnd.nextFloat() * rect.width, rect.top + rnd.nextFloat() * rect.height), Size(s, s))
    }
    repeat(5) {
        val w = dot * (2f + rnd.nextFloat() * 4.4f)
        val h = dot * (1.6f + rnd.nextFloat() * 3.2f)
        drawOval(
            LifeGameInk.copy(alpha = 0.6f),
            Offset(rect.left + 4 * dot + rnd.nextFloat() * (rect.width - 8 * dot), rect.top + 6 * dot + rnd.nextFloat() * (rect.height - 12 * dot)),
            Size(w, h),
        )
    }
    repeat(3) {
        val light = rnd.nextFloat() < 0.7f
        val top = if (rnd.nextFloat() < 0.5f) 0f else rnd.nextFloat() * rect.height * 0.4f
        val color = if (light) Color.White.copy(alpha = 0.4f + rnd.nextFloat() * 0.4f) else LifeGameInk.copy(alpha = 0.25f + rnd.nextFloat() * 0.2f)
        drawRect(
            color,
            Offset(rect.left + 4 * dot + rnd.nextFloat() * (rect.width - 8 * dot), rect.top + top),
            Size(if (rnd.nextFloat() < 0.3f) 1.5f * dot else dot, rect.height * (0.4f + rnd.nextFloat() * 0.6f)),
        )
    }
}

/**
 * 돌발 사건 왜곡: 그림을 3dp 띠로 잘라 띠마다 sin 값만큼 옆으로 밀어 그린다.
 * 아래로 갈수록 크게 휘고, 띠 3개는 찢기듯 더 크게 튄다. 모든 Android 버전에서 같은 결과가 나온다.
 */
private fun DrawScope.drawDistorted(image: ImageBitmap, topLeft: Offset, m: FilmMotion) {
    val amp = m.distortion.dp.toPx()
    val strength = (m.distortion / DISTORT_MAX_DP).coerceIn(0f, 1f)
    val height = image.height.toFloat()
    val tearRandom = Random(floor(m.phase * 1.2f).toInt() + 11)
    val tears = if (strength > 0.25f) {
        List(3) {
            Triple(
                tearRandom.nextFloat() * height,
                (6f + tearRandom.nextFloat() * 16f).dp.toPx(),
                (if (tearRandom.nextFloat() < 0.5f) -1f else 1f) * (0.6f + tearRandom.nextFloat() * 0.8f) * amp,
            )
        }
    } else {
        emptyList()
    }
    val waveLong = 46.dp.toPx()
    val waveShort = 19.dp.toPx()
    fun offsetAt(y: Float): Float {
        val a = amp * (0.18f + 0.82f * (y / height).pow(1.2f))
        var o = a * sin(2f * PI.toFloat() * y / waveLong + m.phase) +
            0.3f * a * sin(2f * PI.toFloat() * y / waveShort + m.phase * 1.7f + 1.1f)
        for ((tearY, tearH, dx) in tears) if (y >= tearY && y < tearY + tearH) o += dx
        return o
    }

    val slice = max(1, 3.dp.toPx().roundToInt())
    var y = 0
    while (y < image.height) {
        val sliceHeight = min(slice, image.height - y)
        val overlap = if (y + sliceHeight < image.height) 1 else 0
        drawImage(
            image = image,
            srcOffset = IntOffset(0, y),
            srcSize = IntSize(image.width, sliceHeight),
            dstOffset = IntOffset((topLeft.x + offsetAt(y.toFloat())).roundToInt(), (topLeft.y + y).roundToInt()),
            dstSize = IntSize(image.width, sliceHeight + overlap),
        )
        y += slice
    }

    if (strength > 0.05f) {
        val line = 1.dp.toPx()
        for ((fy, fw) in Smears) {
            val sy = topLeft.y + fy * height
            val w = fw * image.width
            val x = topLeft.x + max(0f, 30.dp.toPx() + offsetAt(fy * height) * 1.5f)
            drawRect(LifeGameStatUp.copy(alpha = 0.45f * strength), Offset(x - 4 * line, sy), Size(w, 2.5f * line))
            drawRect(LifeGameSpecial.copy(alpha = 0.45f * strength), Offset(x + 4 * line, sy), Size(w, 2.5f * line))
            drawRect(Color.White.copy(alpha = 0.9f * strength), Offset(x, sy), Size(w, 1.5f * line))
        }
    }
}
