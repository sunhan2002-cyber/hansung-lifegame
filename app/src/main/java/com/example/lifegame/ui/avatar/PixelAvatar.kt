package com.example.lifegame.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifegame.domain.engine.bodyTypeFor
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.rememberPixelAsset

/**
 * 화면에 그릴 플레이어 모습. 외형(피부 · 머리 · 눈)은 저장된 [AvatarConfig],
 * 시기는 지금 시기, 체형은 게임 로직의 [bodyTypeFor] 결과를 그대로 쓴다. UI에서 체형을 따로 계산하지 않는다.
 */
data class AvatarLook(
    val skinToneId: String = AvatarOptions.skins.first().id,
    val hairId: String = AvatarOptions.hairs.first().id,
    val eyesId: String = AvatarOptions.eyes.first().id,
    val period: LifePeriod = LifePeriod.INFANT,
    val bodyType: BodyType = BodyType.STANDARD,
    /** 사진 기반 얼굴 시트가 준비된 경우(지금은 Fake 응답) 표시용 표식 */
    val usesPhotoFace: Boolean = false,
) {
    val description: String
        get() = "${period.koreanName()} ${bodyType.koreanName()} 체형, " +
            "${AvatarOptions.skin(skinToneId).label} 피부, ${AvatarOptions.hair(hairId).label}, ${AvatarOptions.eye(eyesId).label}"
}

fun avatarLookOf(config: AvatarConfig, period: LifePeriod, stats: Stats): AvatarLook = AvatarLook(
    skinToneId = AvatarOptions.skin(config.skinToneId).id,
    hairId = AvatarOptions.hair(config.hairId).id,
    eyesId = AvatarOptions.eye(config.eyesId).id,
    period = period,
    bodyType = bodyTypeFor(stats),
    usesPhotoFace = config.photoConsent && !config.faceSheetRef.isNullOrBlank(),
)

fun LifePeriod.koreanName(): String = when (this) {
    LifePeriod.INFANT -> "영아기"
    LifePeriod.CHILD -> "유년기"
    LifePeriod.TEEN -> "청소년기"
    LifePeriod.YOUNG_ADULT -> "청년기"
    LifePeriod.MIDDLE -> "중년기"
    LifePeriod.OLD_AGE -> "노년기"
}

fun BodyType.koreanName(): String = when (this) {
    BodyType.SLIM -> "마른"
    BodyType.STANDARD -> "보통"
    BodyType.MUSCULAR -> "근육"
}

data class AvatarOption(val id: String, val label: String)

/**
 * 사진 없이 고르는 기본 외형. id는 저장 모델(AvatarConfig)의 기본값과 같다.
 * 정원률 소재가 들어오면 같은 id의 PNG 레이어를 우선 쓴다 ([AvatarLayerAssets]).
 */
object AvatarOptions {
    val skins = listOf(
        AvatarOption("skin_default", "밝은"),
        AvatarOption("skin_tan", "구릿빛"),
        AvatarOption("skin_deep", "짙은"),
    )
    val hairs = listOf(
        AvatarOption("hair_default", "짧은 검은 머리"),
        AvatarOption("hair_bob_brown", "갈색 단발"),
        AvatarOption("hair_long_black", "긴 생머리"),
        AvatarOption("hair_curly_brown", "곱슬머리"),
    )
    val eyes = listOf(
        AvatarOption("eyes_default", "둥근 눈"),
        AvatarOption("eyes_big", "큰 눈"),
        AvatarOption("eyes_narrow", "가는 눈"),
    )

    fun skin(id: String) = skins.firstOrNull { it.id == id } ?: skins.first()
    fun hair(id: String) = hairs.firstOrNull { it.id == id } ?: hairs.first()
    fun eye(id: String) = eyes.firstOrNull { it.id == id } ?: eyes.first()
}

/**
 * 정원률 소재 연결 지점. 같은 시기의 레이어는 모두 같은 캔버스 크기 · 같은 발 위치로 그려서
 * 별도 좌표 없이 겹치면 된다(합의안, docs/week10/submissions/김재겸.md 참고).
 * 파일이 없거나 읽지 못하면 null을 돌려주고 [PixelAvatar]가 대신 그린다.
 */
object AvatarLayerAssets {
    fun layers(look: AvatarLook): List<ImageCandidate> {
        val period = look.period.name.lowercase()
        return listOf(
            ImageCandidate("avatar_body", "avatar/body/body_${period}_${look.bodyType.name.lowercase()}.png", "avatar"),
            ImageCandidate("avatar_skin", "avatar/face/skin_${period}_${look.skinToneId}.png", "avatar"),
            ImageCandidate("avatar_eyes", "avatar/face/eyes_${period}_${look.eyesId}.png", "avatar"),
            ImageCandidate("avatar_hair", "avatar/face/hair_${period}_${look.hairId}.png", "avatar"),
        )
    }
}

/**
 * 플레이어 캐릭터. 배경 없이 투명하게 그려져 사건 배경 위에 그대로 얹힌다.
 * 레이어 PNG가 모두 있으면 그것을 겹치고, 하나라도 없으면 도트 생성기로 그린다.
 */
@Composable
fun PlayerAvatar(look: AvatarLook, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .testTag("player_avatar")
            .semantics { contentDescription = "플레이어 모습: ${look.description}" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val height = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        val layers = AvatarLayerAssets.layers(look).map { candidate ->
            rememberPixelAsset(listOf(candidate), width, height)
        }
        if (layers.all { it != null }) {
            Box(Modifier.fillMaxSize().testTag("player_avatar_layers")) {
                layers.forEach { loaded ->
                    Image(
                        painter = BitmapPainter(loaded!!.bitmap.asImageBitmap(), filterQuality = FilterQuality.None),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                    )
                }
            }
        } else {
            PixelAvatar(look, Modifier.fillMaxSize())
        }
    }
}

/** 도트 생성기. 칸 좌표로 그린 뒤 성인 키 기준 칸 크기로 늘린다(발이 아래에 닿게). */
@Composable
fun PixelAvatar(look: AvatarLook, modifier: Modifier = Modifier) {
    val sprite = remember(look) { AvatarSprite.build(look) }
    Canvas(modifier.testTag("player_avatar_pixels")) {
        // 성인 키(가장 큰 칸 수)를 기준으로 배율을 고정해 어린 시기는 실제로 작게 보인다.
        val cell = minOf(size.width / AvatarSprite.WIDTH, size.height / AvatarSprite.MAX_HEIGHT)
        val left = (size.width - cell * AvatarSprite.WIDTH) / 2f
        val top = size.height - cell * sprite.height
        sprite.pixels.forEach { (point, color) ->
            drawRect(
                color = color,
                topLeft = Offset(left + point.first * cell, top + point.second * cell),
                size = Size(cell + 0.6f, cell + 0.6f),
            )
        }
    }
}

/** 칸 단위 도트 그림. pixels는 (x, y) → 색. */
class AvatarSprite private constructor(val height: Int, val pixels: Map<Pair<Int, Int>, Color>) {

    companion object {
        const val WIDTH = 16
        const val MAX_HEIGHT = 26

        private data class Proportion(val torso: Int, val legs: Int)

        private fun proportionOf(period: LifePeriod) = when (period) {
            LifePeriod.INFANT -> Proportion(torso = 4, legs = 3)
            LifePeriod.CHILD -> Proportion(torso = 5, legs = 5)
            LifePeriod.TEEN -> Proportion(torso = 7, legs = 7)
            else -> Proportion(torso = 7, legs = 8)
        }

        private data class Outfit(val top: Color, val bottom: Color, val shoes: Color, val collar: Color? = null)

        private fun outfitOf(period: LifePeriod) = when (period) {
            LifePeriod.INFANT -> Outfit(Color(0xFFF4E3A1), Color(0xFFF4E3A1), Color(0xFFF4E3A1))
            LifePeriod.CHILD -> Outfit(Color(0xFFD9573F), Color(0xFF3F5E9C), Color(0xFF2F2A26))
            LifePeriod.TEEN -> Outfit(Color(0xFF2E3B5E), Color(0xFF55595F), Color(0xFF1F1C1A), collar = Color(0xFFF2F2EE))
            LifePeriod.YOUNG_ADULT -> Outfit(Color(0xFF8DB3D9), Color(0xFF34435E), Color(0xFF4A3B30))
            LifePeriod.MIDDLE -> Outfit(Color(0xFF4B4F57), Color(0xFF3A3530), Color(0xFF231F1C), collar = Color(0xFFE9E6DD))
            LifePeriod.OLD_AGE -> Outfit(Color(0xFFC9B38A), Color(0xFF6A5340), Color(0xFF3B3029))
        }

        private fun skinColor(id: String) = when (id) {
            "skin_tan" -> Color(0xFFD9A273)
            "skin_deep" -> Color(0xFF8D5B3B)
            else -> Color(0xFFF2C9A0)
        }

        private fun hairColor(id: String, period: LifePeriod): Color = when {
            period == LifePeriod.OLD_AGE -> Color(0xFFCFCBC4)
            id == "hair_bob_brown" || id == "hair_curly_brown" -> Color(0xFF6B4226)
            else -> Color(0xFF2B2321)
        }

        private fun Color.shade(factor: Float) = Color(red * factor, green * factor, blue * factor, alpha)

        fun build(look: AvatarLook): AvatarSprite {
            val p = proportionOf(look.period)
            val outfit = outfitOf(look.period)
            val skin = skinColor(look.skinToneId)
            val hair = hairColor(look.hairId, look.period)
            val px = linkedMapOf<Pair<Int, Int>, Color>()
            fun put(x: Int, y: Int, c: Color) {
                if (x in 0 until WIDTH && y >= 0) px[x to y] = c
            }
            fun rect(x0: Int, x1: Int, y0: Int, y1: Int, c: Color) {
                for (y in y0..y1) for (x in x0..x1) put(x, y, c)
            }
            fun clear(x0: Int, x1: Int, y0: Int, y1: Int) {
                for (y in y0..y1) for (x in x0..x1) px.remove(x to y)
            }

            val hy = 2 // 머리 위쪽 행. 위 두 줄은 머리카락 자리.
            val infant = look.period == LifePeriod.INFANT
            val long = look.hairId == "hair_long_black"

            // 1) 등 뒤 긴 머리 (몸보다 먼저)
            if (long && !infant) rect(3, 12, hy + 2, hy + 10, hair.shade(0.85f))

            // 2) 몸: 체형에 따라 몸통 · 팔 너비가 다르다
            val t0 = hy + 9
            val (bodyL, bodyR, armW) = when (look.bodyType) {
                BodyType.SLIM -> Triple(5, 10, 1)
                BodyType.STANDARD -> Triple(4, 11, 2)
                BodyType.MUSCULAR -> Triple(3, 12, 2)
            }
            rect(bodyL, bodyR, t0, t0 + p.torso - 1, outfit.top)
            // 오른쪽 그늘
            rect(bodyR, bodyR, t0, t0 + p.torso - 1, outfit.top.shade(0.82f))
            if (look.bodyType == BodyType.MUSCULAR) {
                // 가슴 윤곽과 허리로 갈수록 좁아지는 몸통
                rect(bodyL + 2, bodyR - 2, t0 + 2, t0 + 2, outfit.top.shade(0.88f))
                clear(bodyL, bodyL, t0 + p.torso - 1, t0 + p.torso - 1)
                clear(bodyR, bodyR, t0 + p.torso - 1, t0 + p.torso - 1)
            }
            if (look.period == LifePeriod.MIDDLE && look.bodyType == BodyType.STANDARD) {
                rect(bodyL + 1, bodyR - 1, t0 + p.torso - 2, t0 + p.torso - 1, outfit.top.shade(0.92f))
            }
            outfit.collar?.let { collar -> rect(7, 8, t0, t0 + 1, collar) }
            // 팔: 소매 + 손
            // 팔은 어깨 한 칸 아래에서 시작해 어깨선이 둥글게 보이게 한다. 근육 체형은 팔뚝이 굵다.
            val armTop = t0 + 1
            val armLeft = (bodyL - armW) until bodyL
            val armRight = (bodyR + 1)..(bodyR + armW)
            for (x in armLeft) rect(x, x, armTop, t0 + p.torso - 1, outfit.top.shade(0.9f))
            for (x in armRight) rect(x, x, armTop, t0 + p.torso - 1, outfit.top.shade(0.78f))
            if (look.bodyType == BodyType.MUSCULAR) {
                // 이두 부분만 한 칸 더 굵게
                rect(bodyL - armW - 1, bodyL - armW - 1, armTop + 1, armTop + 2, outfit.top.shade(0.9f))
                rect(bodyR + armW + 1, bodyR + armW + 1, armTop + 1, armTop + 2, outfit.top.shade(0.78f))
            }
            for (x in armLeft) put(x, t0 + p.torso, skin)
            for (x in armRight) put(x, t0 + p.torso, skin.shade(0.9f))

            // 3) 다리와 신발
            val l0 = t0 + p.torso
            val (legL, legR) = when (look.bodyType) {
                BodyType.SLIM -> 6 to 9
                BodyType.STANDARD -> 5 to 10
                BodyType.MUSCULAR -> 4 to 11
            }
            val mid = (legL + legR) / 2
            rect(legL, legR, l0, l0 + p.legs - 2, outfit.bottom)
            if (!infant) rect(mid, mid, l0 + 1, l0 + p.legs - 2, outfit.bottom.shade(0.7f))
            rect(legL, mid - 1, l0 + p.legs - 1, l0 + p.legs - 1, if (infant) skin else outfit.shoes)
            rect(mid + 1, legR, l0 + p.legs - 1, l0 + p.legs - 1, if (infant) skin else outfit.shoes)
            if (infant) put(mid, l0 + p.legs - 1, outfit.bottom)

            // 4) 목 · 얼굴
            rect(7, 8, hy + 8, hy + 8, skin.shade(0.9f))
            rect(5, 10, hy, hy, skin)
            rect(4, 11, hy + 1, hy + 6, skin)
            rect(5, 10, hy + 7, hy + 7, skin)
            rect(11, 11, hy + 1, hy + 6, skin.shade(0.9f))
            // 눈
            val eye = Color(0xFF2A2522)
            when (look.eyesId) {
                "eyes_big" -> {
                    rect(6, 6, hy + 3, hy + 4, eye); rect(9, 9, hy + 3, hy + 4, eye)
                    put(6, hy + 3, Color(0xFFF7F7F2)); put(9, hy + 3, Color(0xFFF7F7F2))
                }
                "eyes_narrow" -> { rect(5, 6, hy + 4, hy + 4, eye); rect(9, 10, hy + 4, hy + 4, eye) }
                else -> { put(6, hy + 4, eye); put(9, hy + 4, eye) }
            }
            // 볼과 입
            if (look.period == LifePeriod.INFANT || look.period == LifePeriod.CHILD) {
                put(5, hy + 5, Color(0xFFE79A92)); put(10, hy + 5, Color(0xFFE79A92))
            }
            rect(7, 8, hy + 6, hy + 6, skin.shade(0.72f))
            if (look.period == LifePeriod.OLD_AGE) {
                // 주름 한 줄
                put(5, hy + 3, skin.shade(0.82f)); put(10, hy + 3, skin.shade(0.82f))
            }

            // 5) 앞머리
            if (infant) {
                rect(7, 8, hy - 1, hy - 1, hair)
                put(8, hy - 2, hair)
            } else {
                rect(5, 10, hy - 1, hy - 1, hair)
                rect(4, 11, hy, hy + 1, hair)
                when (look.hairId) {
                    "hair_bob_brown" -> { rect(3, 4, hy, hy + 6, hair); rect(11, 12, hy, hy + 6, hair.shade(0.85f)) }
                    "hair_long_black" -> { rect(3, 4, hy, hy + 8, hair); rect(11, 12, hy, hy + 8, hair.shade(0.85f)) }
                    "hair_curly_brown" -> {
                        rect(5, 10, hy - 2, hy - 2, hair)
                        rect(3, 12, hy - 1, hy + 1, hair)
                        rect(3, 3, hy + 2, hy + 4, hair); rect(12, 12, hy + 2, hy + 4, hair.shade(0.85f))
                        listOf(4 to hy - 1, 7 to hy - 2, 10 to hy - 1, 6 to hy, 9 to hy).forEach { (x, y) -> put(x, y, hair.shade(1.25f)) }
                    }
                    else -> { put(4, hy + 2, hair); put(11, hy + 2, hair.shade(0.85f)) }
                }
            }

            val height = px.keys.maxOf { it.second } + 1
            return AvatarSprite(height, px)
        }
    }
}
