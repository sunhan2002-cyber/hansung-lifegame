package com.example.lifegame.ui

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.images.ScenePresentation
import org.json.JSONObject
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

    private fun invalidMetadata(change: (JSONObject) -> Unit): String {
        val root = JSONObject(metadata())
        val entries = root.getJSONArray("images")
        val source = (0 until entries.length()).map(entries::getJSONObject)
            .first { it.getString("assetPath") == original }
        change(source)
        return root.toString()
    }

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
        for (invalid in listOf(
            invalidMetadata { it.put("sourceSha256", "a".repeat(64)) },
            invalidMetadata { it.getJSONObject("portraitVariant").put("sha256", "b".repeat(64)) },
            invalidMetadata { it.getJSONObject("portraitVariant").put("assetPath", "presentation/portraits/missing.png") },
            invalidMetadata {
                val variant = it.getJSONObject("portraitVariant")
                variant.put("width", variant.getInt("width") + 1)
            },
            invalidMetadata { it.getJSONObject("portraitVariant").put("reviewStatus", "PENDING") },
            "malformed",
        )) {
            val loaded = PixelAssetLoader.load(context, listOf(candidate), 360, 800, ScenePresentation.parse(invalid))!!
            assertEquals(original, loaded.candidate.path)
        }
    }
}
