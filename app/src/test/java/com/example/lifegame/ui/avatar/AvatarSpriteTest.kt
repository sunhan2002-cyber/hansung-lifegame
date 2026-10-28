package com.example.lifegame.ui.avatar

import androidx.compose.ui.graphics.toArgb
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Stats
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarSpriteTest {

    @Test
    fun packagedAppearancePartsChangeIndependently() {
        val base = AvatarLook(period = LifePeriod.YOUNG_ADULT)
        val paths = AvatarLayerAssets.layers(base).map { it.path }
        val edits = listOf(
            base.copy(skinToneId = "skin_deep") to setOf(1),
            base.copy(hairId = "hair_long_black") to setOf(3),
            base.copy(eyesId = "eyes_narrow") to setOf(2),
            base.copy(bodyType = BodyType.MUSCULAR) to setOf(0, 1),
        )
        edits.forEach { (look, expectedChangedLayers) ->
            val next = AvatarLayerAssets.layers(look).map { it.path }
            assertEquals(expectedChangedLayers, paths.indices.filter { paths[it] != next[it] }.toSet())
        }
        assertEquals(listOf("avatar_body", "avatar_skin", "avatar_eyes", "avatar_hair"),
            AvatarLayerAssets.layers(base).map { it.imageId })
    }

    private fun widthOf(sprite: AvatarSprite): Int {
        val xs = sprite.pixels.keys.map { it.first }
        return xs.max() - xs.min() + 1
    }

    @Test
    fun everyPeriodBodyAndLookCombinationBuilds() {
        LifePeriod.entries.forEach { period ->
            BodyType.entries.forEach { body ->
                AvatarOptions.skins.forEach { skin ->
                    AvatarOptions.hairs.forEach { hair ->
                        AvatarOptions.eyes.forEach { eyes ->
                            val sprite = AvatarSprite.build(AvatarLook(skin.id, hair.id, eyes.id, period, body))
                            assertTrue(sprite.height in 1..AvatarSprite.MAX_HEIGHT)
                            assertTrue(sprite.pixels.keys.all { it.first in 0 until AvatarSprite.WIDTH && it.second >= 0 })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun characterGrowsTallerUntilAdulthood() {
        val heights = LifePeriod.entries.map { AvatarSprite.build(AvatarLook(period = it)).height }
        assertTrue("영아 < 유년 < 청소년 <= 청년: $heights", heights[0] < heights[1] && heights[1] < heights[2] && heights[2] <= heights[3])
    }

    @Test
    fun bodyTypesAreVisiblyDifferentInEveryPeriod() {
        LifePeriod.entries.forEach { period ->
            val slim = AvatarSprite.build(AvatarLook(period = period, bodyType = BodyType.SLIM))
            val standard = AvatarSprite.build(AvatarLook(period = period, bodyType = BodyType.STANDARD))
            val muscular = AvatarSprite.build(AvatarLook(period = period, bodyType = BodyType.MUSCULAR))
            assertNotEquals(slim.pixels, standard.pixels)
            assertNotEquals(standard.pixels, muscular.pixels)
            assertTrue("$period 근육 체형이 가장 넓다", widthOf(muscular) > widthOf(slim))
        }
    }

    @Test
    fun skinHairAndEyesChangeThePicture() {
        val base = AvatarLook(period = LifePeriod.YOUNG_ADULT)
        val sprite = AvatarSprite.build(base).pixels
        assertNotEquals(sprite, AvatarSprite.build(base.copy(skinToneId = "skin_deep")).pixels)
        assertNotEquals(sprite, AvatarSprite.build(base.copy(hairId = "hair_curly_brown")).pixels)
        assertNotEquals(sprite, AvatarSprite.build(base.copy(eyesId = "eyes_big")).pixels)
    }

    @Test
    fun lookUsesSavedAppearanceAndGameBodyTypeRule() {
        val config = AvatarConfig(skinToneId = "skin_tan", hairId = "hair_long_black", eyesId = "eyes_narrow")
        val strong = avatarLookOf(config, LifePeriod.TEEN, Stats(fitness = 75))
        assertEquals(BodyType.MUSCULAR, strong.bodyType)
        assertEquals("skin_tan", strong.skinToneId)
        assertEquals(BodyType.SLIM, avatarLookOf(config, LifePeriod.TEEN, Stats(health = 30, fitness = 50)).bodyType)
        assertEquals(BodyType.STANDARD, avatarLookOf(config, LifePeriod.TEEN, Stats(health = 60, fitness = 50)).bodyType)
        // 알 수 없는 id(이전 버전 저장 등)는 기본 외형으로 돌아간다.
        assertEquals("hair_default", avatarLookOf(AvatarConfig(hairId = "hair_???"), LifePeriod.TEEN, Stats()).hairId)
    }

    /** AVATAR_PREVIEW_DIR가 있을 때만 6시기×3체형 픽셀 목록을 남긴다(디자인 확인용, 그림은 별도 스크립트로 그린다). */
    @Test
    fun writePreviewPixelsWhenRequested() {
        val dir = System.getenv("AVATAR_PREVIEW_DIR") ?: return
        val looks = listOf(
            AvatarLook(),
            AvatarLook("skin_tan", "hair_bob_brown", "eyes_big"),
            AvatarLook("skin_deep", "hair_curly_brown", "eyes_narrow"),
            AvatarLook("skin_default", "hair_long_black", "eyes_default"),
        )
        File(dir).mkdirs()
        val out = StringBuilder()
        looks.forEachIndexed { lookIndex, look ->
            LifePeriod.entries.forEachIndexed { col, period ->
                BodyType.entries.forEachIndexed { row, body ->
                    val sprite = AvatarSprite.build(look.copy(period = period, bodyType = body))
                    sprite.pixels.forEach { (point, color) ->
                        out.append("$lookIndex $col $row ${sprite.height} ${point.first} ${point.second} ${Integer.toHexString(color.toArgb())}").append('\n')
                    }
                }
            }
        }
        File(dir, "avatar_pixels.txt").writeText(out.toString())
    }
}
