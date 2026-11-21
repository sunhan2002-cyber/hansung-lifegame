package com.example.lifegame.ui

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.images.ScenePresentation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortraitVariantTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val original = "followup_20261031/img_pov_first_card_001.png"
    private val portrait = "presentation/portraits/img_pov_first_card_001_portrait_v1.png"
    private val candidate = ImageCandidate("img_pov_first_card_001", original)
    private fun metadata() = context.assets.open("image_presentation.json").bufferedReader().use { it.readText() }

    @Test fun portraitUsesActualDerivedPathButLandscapeAndIdentityKeepOriginalContract() {
        val tall = PixelAssetLoader.load(context, listOf(candidate), 360, 800)!!
        assertEquals(portrait, tall.candidate.path)
        assertEquals(candidate.imageId, tall.candidate.imageId)
        assertEquals(candidate.usage, tall.candidate.usage)
        assertTrue(tall.bitmap.height > tall.bitmap.width)
        val wide = PixelAssetLoader.load(context, listOf(candidate), 800, 360)!!
        assertEquals(original, wide.candidate.path)
        assertEquals(4f / 3f, wide.bitmap.width.toFloat() / wide.bitmap.height, .01f)
    }

    @Test fun staleMissingAndIncompatibleVariantsReturnOriginalBeforeOtherCandidates() {
        val sourceHash = "e8b321c9fffff153bf5a4a47eef2c67a67097db7acf14f218013ad79b33a2121"
        val variantHash = "91490db70d899eae008069ff82dded8cc100ba89d654cb02c599b2f90e6cf011"
        val json = metadata()
        for (invalid in listOf(
            json.replace(sourceHash, "a".repeat(64)),
            json.replace(variantHash, "b".repeat(64)),
            json.replace(portrait, "presentation/portraits/missing.png"),
            json.replace("\"width\": 941", "\"width\": 942"),
            json.replace("AI_REVIEWED_PRESENTATION", "PENDING"),
            "malformed",
        )) {
            val loaded = PixelAssetLoader.load(context, listOf(candidate), 360, 800, ScenePresentation.parse(invalid))!!
            assertEquals(original, loaded.candidate.path)
        }
    }
}
