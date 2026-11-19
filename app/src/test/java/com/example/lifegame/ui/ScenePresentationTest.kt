package com.example.lifegame.ui

import com.example.lifegame.ui.images.SceneFocalPoint
import com.example.lifegame.ui.images.ScenePresentation
import com.example.lifegame.ui.images.coverPlacement
import org.junit.Assert.*
import org.junit.Test

class ScenePresentationTest {
    private fun variantMetadata(overrides: String = "") = """{"schemaVersion":1,"images":[{
        "assetPath":"scene.png","sourceSha256":"${"a".repeat(64)}","focalX":0.7,"focalY":0.5,
        "portraitVariant":{"assetPath":"presentation/portraits/scene.png","sha256":"${"b".repeat(64)}",
        "width":941,"height":1672,"reviewStatus":"AI_REVIEWED_PRESENTATION","reviewerType":"AI",
        "reviewer":"Codex AI","reviewEvidence":"review.json"$overrides}}]}"""

    @Test fun portraitVariantRequiresSourceMatchAndPortraitViewport() {
        val metadata = ScenePresentation.parse(variantMetadata())
        val hash = "a".repeat(64)
        assertNotNull(metadata.portraitVariant("scene.png", hash, 360, 800))
        assertNull(metadata.portraitVariant("scene.png", "c".repeat(64), 360, 800))
        assertNull(metadata.portraitVariant("scene.png", hash, 800, 360))
        assertNull(metadata.portraitVariant("scene.png", hash, 400, 540))
        assertNull(metadata.portraitVariant("scene.png", hash, 0, 800))
    }

    @Test fun malformedOrUnreviewedVariantRetainsOriginalFocalPoint() {
        val valid = variantMetadata()
        for (invalid in listOf(
            valid.replace("AI_REVIEWED_PRESENTATION", "PENDING"),
            valid.replace("presentation/portraits/scene.png", "../scene.png"),
            valid.replace("\"width\":941", "\"width\":1672"),
            valid.replace("\"reviewer\":\"Codex AI\"", "\"reviewer\":\"\""),
            valid.replace("\"reviewerType\":\"AI\"", "\"reviewerType\":\"HUMAN\""),
        )) {
            val metadata = ScenePresentation.parse(invalid)
            assertNull(metadata.portraitVariant("scene.png", "a".repeat(64), 360, 800))
            assertEquals(.7f, metadata.focal("scene.png", "a".repeat(64)).x)
        }
    }

    @Test fun centralCropRemainsIdentical() {
        val crop = coverPlacement(1200, 800, 360, 800)
        assertEquals(-420f, crop.left, .01f)
        assertEquals(0f, crop.top, .01f)
    }

    @Test fun focalCropAlwaysCoversViewportIncludingExtremePositions() {
        for ((w, h) in listOf(360 to 800, 800 to 360, 640 to 640)) {
            for (x in listOf(0f, .3f, .62f, 1f)) for (y in listOf(0f, .5f, 1f)) {
                val crop = coverPlacement(1448, 1086, w, h, SceneFocalPoint(x, y))
                assertTrue(crop.left <= 0 && crop.top <= 0)
                assertTrue(crop.left + crop.width >= w && crop.top + crop.height >= h)
            }
        }
    }

    @Test fun changedSourceAndMalformedMetadataUseOriginalCrop() {
        val hash = "a".repeat(64)
        val json = """{"schemaVersion":1,"images":[{"assetPath":"scene.png","sourceSha256":"$hash","focalX":0.7,"focalY":0.5}]}"""
        assertEquals(.7f, ScenePresentation.parse(json).focal("scene.png", hash).x)
        assertEquals(.5f, ScenePresentation.parse(json).focal("scene.png", "b".repeat(64)).x)
        for (invalid in listOf(json.replace("0.7", "1.7"), json.replace("scene.png", "../scene.png"), "{}")) {
            assertEquals(.5f, ScenePresentation.parse(invalid).focal("scene.png", hash).x)
        }
    }
}
