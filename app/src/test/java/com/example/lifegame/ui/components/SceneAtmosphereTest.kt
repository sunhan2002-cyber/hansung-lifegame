package com.example.lifegame.ui.components

import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.SceneFocalPoint
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SceneAtmosphereTest {
    private fun source(id: String, path: String, width: Int = 720, height: Int = 540, focal: SceneFocalPoint = SceneFocalPoint()) =
        SceneImageSource(ImageCandidate(id, path), width, height, focal)

    @Test fun threeInspectedEventsSelectTheirOwnLightingIncludingThePortraitVariant() {
        assertEquals(AtmosphereKind.CLASSROOM_DAY, sceneAtmosphereFor("teen_024", source("img_teen_wrong_answer_001", "followup_20261031/img_teen_wrong_answer_001.png"))?.kind)
        for (path in listOf("followup_20261031/img_pov_first_card_001.png", "presentation/portraits/img_pov_first_card_001_portrait_v1.png")) {
            assertEquals(AtmosphereKind.DESK_DAY, sceneAtmosphereFor("adult_072", source("img_pov_first_card_001", path))?.kind)
        }
        assertEquals(AtmosphereKind.DESK_NIGHT, sceneAtmosphereFor("adult_067", source("img_adult_sunday_night_001", "followup_20261031/img_adult_sunday_night_001.png"))?.kind)
    }

    @Test fun missingUninspectedOrFallbackArtDoesNotInheritTheRequestedEventsLighting() {
        val night = source("img_adult_sunday_night_001", "followup_20261031/img_adult_sunday_night_001.png")
        assertNull(sceneAtmosphereFor("adult_067", null))
        assertNull(sceneAtmosphereFor("teen_024", night))
        assertNull(sceneAtmosphereFor("other_event", night))
        assertNull(sceneAtmosphereFor("adult_067", night.copy(candidate = night.candidate.copy(path = "generic/night.png"))))
    }

    @Test fun cameraOverscanCoversEveryEdgeForEveryPhaseAndLoopsWithoutJumping() {
        for (kind in AtmosphereKind.entries) {
            for (step in 0..1000) {
                val pose = sceneCameraPose(kind, step / 1000f)
                val margin = (pose.scale - 1f) / 2f
                assertTrue("$kind camera plus nearest depth plane must cover horizontal edges", abs(pose.x) + .007f * pose.scale < margin)
                assertTrue("$kind vertical edge", abs(pose.y) < margin)
                assertTrue("Three-percent overscan covers independent foreground drift", pose.scale in 1f..1.031f)
            }
            val first = sceneCameraPose(kind, 0f)
            val last = sceneCameraPose(kind, 1f)
            assertEquals(first.x, last.x, .000001f)
            assertEquals(first.y, last.y, .000001f)
            assertEquals(first.scale, last.scale, .000001f)
        }
    }

    @Test fun sourceAnchorsFollowFocalCropRatherThanAssumingTheWindowIsScreenCentred() {
        val art = source("img_teen_wrong_answer_001", "followup_20261031/img_teen_wrong_answer_001.png", 1448, 1086, SceneFocalPoint(.62f, .5f))
        val profile = requireNotNull(sceneAtmosphereFor("teen_024", art))
        val focal = profile.anchor(.62f, .5f, 360, 800)
        assertEquals(180f, focal.x, .001f)
        assertEquals(400f, focal.y, .001f)
        assertTrue("The source window is offscreen after the teacher-focused portrait crop", profile.anchor(.16f, .23f, 360, 800).x < 0f)
    }
}
