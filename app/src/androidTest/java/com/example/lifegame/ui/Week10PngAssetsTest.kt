package com.example.lifegame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import com.example.lifegame.ui.avatar.AvatarAssetLookKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PlayerAvatar
import org.junit.Rule
import org.junit.Test
import java.io.File

class Week10PngAssetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun growthAndBuildChangesPublishOneCompleteAppearance() {
        var look by mutableStateOf(AvatarLook())
        compose.setContent { PlayerAvatar(look, Modifier.size(100.dp, 180.dp)) }
        for (period in LifePeriod.entries) for (build in BodyType.entries) {
            val next = AvatarLook(period = period, bodyType = build)
            compose.runOnIdle { look = next }
            compose.waitUntil(15000) {
                runCatching {
                    compose.onNodeWithTag("player_avatar_layers")
                        .assert(SemanticsMatcher.expectValue(AvatarAssetLookKey, next.description))
                }.isSuccess
            }
        }
    }

    @Test fun everyGeneratedAppearanceLoadsWithoutTheCodeFallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (period in LifePeriod.entries) for (build in BodyType.entries)
            for (skin in com.example.lifegame.ui.avatar.AvatarOptions.skins)
                for (hair in com.example.lifegame.ui.avatar.AvatarOptions.hairs)
                    for (eye in com.example.lifegame.ui.avatar.AvatarOptions.eyes) {
                        val look = AvatarLook(skin.id, hair.id, eye.id, period, build)
                        com.example.lifegame.ui.avatar.AvatarLayerAssets.layers(look).forEach { candidate ->
                            org.junit.Assert.assertTrue(candidate.path, candidate.path.startsWith("avatar_v4/"))
                            val image = com.example.lifegame.ui.images.PixelAssetLoader.load(context, listOf(candidate), 192, 312)
                            org.junit.Assert.assertNotNull(candidate.path, image)
                            org.junit.Assert.assertEquals(192, image!!.bitmap.width)
                            org.junit.Assert.assertEquals(312, image.bitmap.height)
                        }
                    }
    }

    @Test fun sixPeriodsAndThreeBuildsLoadAndRemainAligned() {
        compose.setContent {
            Column(Modifier.background(Color(0xFFFFEACA))) {
                LifePeriod.entries.forEach { period ->
                    Row {
                        listOf(BodyType.SLIM, BodyType.STANDARD, BodyType.MUSCULAR).forEach {
                            PlayerAvatar(AvatarLook(period = period, bodyType = it), Modifier.size(100.dp, 100.dp))
                        }
                    }
                }
            }
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("player_avatar_layers").fetchSemanticsNodes().size == 18
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "all_18_bodies.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun youngAdultThreeBodiesLoadRealPngs() {
        listOf(BodyType.SLIM, BodyType.STANDARD, BodyType.MUSCULAR).forEach { build ->
            val look = AvatarLook(period = LifePeriod.YOUNG_ADULT, bodyType = build)
            com.example.lifegame.ui.avatar.AvatarLayerAssets.layers(look).forEach { candidate ->
                org.junit.Assert.assertTrue(candidate.path, candidate.path.startsWith("avatar_v4/"))
                val loaded = com.example.lifegame.ui.images.PixelAssetLoader.load(
                    InstrumentationRegistry.getInstrumentation().targetContext, listOf(candidate), 192, 312)
                org.junit.Assert.assertNotNull(candidate.path, loaded)
                org.junit.Assert.assertEquals(192, loaded!!.bitmap.width)
                org.junit.Assert.assertEquals(312, loaded.bitmap.height)
            }
        }
        compose.setContent {
            Row(Modifier.background(Color(0xFFFFEACA))) {
                listOf(BodyType.SLIM, BodyType.STANDARD, BodyType.MUSCULAR).forEach {
                    PlayerAvatar(AvatarLook(period = LifePeriod.YOUNG_ADULT, bodyType = it), Modifier.size(100.dp, 180.dp))
                }
            }
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("player_avatar_layers").fetchSemanticsNodes().size == 3
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "pilot_three_bodies.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
