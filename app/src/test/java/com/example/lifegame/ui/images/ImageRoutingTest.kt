package com.example.lifegame.ui.images

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.PendingDecision
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.ui.RecordContent
import com.example.lifegame.ui.buildDecisionUi
import com.example.lifegame.ui.doljabiOutcomeImageId
import com.example.lifegame.ui.recapMomentsUi
import com.example.lifegame.ui.restoredDecisionUi
import org.json.JSONArray
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import com.example.lifegame.ui.PlayContent
import com.example.lifegame.ui.buildPeriodPlayState
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ImageRoutingTest {
    private fun manifest(): String = listOf(
        File("src/main/assets/image_routing.json"), File("app/src/main/assets/image_routing.json"),
    ).first { it.isFile }.readText()

    @Test fun `event then explicit focus then actual period portrait are ordered without random samples`() {
        val routing = ImageRouting.parse(manifest())
        val result = routing.candidates("img_pov_exam_paper_001", "focus_teen_study", LifePeriod.TEEN)
        assertEquals(listOf("img_pov_exam_paper_001", "img_stat_intelligence_001", "img_week10_bg_teen", "img_portrait_default_teen_001"),
            result.map { it.imageId })
        assertEquals("design_preview/week7/img_pov_exam_paper_001.png", result.first().path)
    }

    @Test fun `unknown focus and image do not choose an unrelated stat scene`() {
        val routing = ImageRouting.parse(manifest())
        assertEquals(listOf("img_week10_bg_child", "img_portrait_default_child_001"),
            routing.candidates("unknown", "new_story_focus", LifePeriod.CHILD).map { it.imageId })
        assertTrue(routing.candidates("unknown", "unknown", null).isEmpty())
    }

    @Test fun `career and tied family effects use authored theme rather than stat ranking`() {
        val routing = ImageRouting.parse(manifest())
        assertEquals("img_stat_wealth_001", routing.candidates(null, "focus_young_adult_career", null).first().imageId)
        assertEquals("img_stat_social_001", routing.candidates(null, "focus_old_age_family", null).first().imageId)
        assertEquals(LifePeriod.entries.toSet(), routing.periodPortraits.keys)
    }

    @Test fun `recorded teen death uses teen portrait even if calendar state is older`() {
        val routing = ImageRouting.parse(manifest())
        val state = PeriodRunState("run", 1L, LifePeriod.OLD_AGE, isEnding = true,
            isDead = true, endingPeriod = LifePeriod.TEEN)
        val ui = buildPeriodPlayState(TurnStep(state, TurnPhase.ENDING),
            PlayContent(emptyList(), emptyList(), emptyMap(), emptyMap(), imageRouting = routing), "플레이어")
        assertEquals(LifePeriod.TEEN, ui.imagePeriod)
        assertEquals("img_portrait_default_teen_001", ui.portraitImages.single().imageId)
        assertEquals(state.stats, ui.stats)
    }

    private fun assetDir(): File = listOf(File("src/main/assets"), File("app/src/main/assets"))
        .first { it.isDirectory }

    @Test fun `real pending events reach the matching generic art for all seven stat themes`() {
        val routing = ImageRouting.parse(manifest())
        val events = V4ContentParser.parseDecisionEvents(File(assetDir(), "events.sample.json").readText())
            .associateBy { it.eventId }
        val catalog = JSONArray(File(assetDir(), "image_ids.sample.json").readText())
        val files = (0 until catalog.length()).associate { index ->
            val item = catalog.getJSONObject(index)
            item.getString("imageId") to item.getString("fileName")
        }
        val cases = listOf(
            Triple("adult_058", "focus_middle_health", "health"),
            Triple("teen_009", "focus_teen_fitness", "fitness"),
            Triple("teen_007", "focus_teen_study", "intelligence"),
            Triple("teen_011", "focus_teen_club", "social"),
            Triple("teen_015", "focus_teen_part_time", "wealth"),
            Triple("teen_041", "focus_teen_rest", "happiness"),
            Triple("adult_019", "focus_young_adult_adventure", "luck"),
        )
        // 전용 그림이 실제 파일로 있는 사건은 "대기 사건"이 아니다. 예시 사건에 전용 그림이 연결되면(2026-11-11 김재겸 64그룹 연결로
        // adult_058이 그렇다) 같은 능력치 태그를 가진, 아직 전용 그림이 없는 다른 실제 사건으로 같은 대체 규칙을 확인한다.
        fun hasDedicatedArt(eventId: String): Boolean =
            routing.candidates(events.getValue(eventId).povImageId, null, null, files)
                .any { File(assetDir(), "images/${it.path}").isFile }
        for ((preferred, focus, tag) in cases) {
            val id = preferred.takeUnless(::hasDedicatedArt)
                ?: events.keys.sorted().firstOrNull { tag in routing.eventStatTags[it].orEmpty() && !hasDedicatedArt(it) }
            // 시연용 전수 라우팅에서는 모든 사건에 전용 PNG가 있으므로 generic fallback 대상이 없다.
            if (id == null) {
                assertTrue(hasDedicatedArt(preferred))
                continue
            }
            val event = events.getValue(id)
            val period = event.periods.first()
            val pending = PendingDecision(id, event.kind, event.title, event.text, emptyList(), event.povImageId)
            val state = PeriodRunState("run", 1L, period, selectedFocusId = focus)
            val ui = buildPeriodPlayState(TurnStep(state, TurnPhase.DECISION, pendingDecision = pending),
                PlayContent(emptyList(), emptyList(), events, files, imageRouting = routing), "플레이어")
            val candidates = ui.decision!!.images
            // Simulates missing event files using the actual packaged file paths, not just ID order.
            val ready = candidates.first { File(assetDir(), "images/${it.path}").isFile }
            assertEquals(id, "img_stat_${tag}_001", ready.imageId)
            assertEquals(state, state.copy(stats = ui.stats))
        }
    }

    @Test fun `dedicated event image wins over the matching focus scene`() {
        val routing = ImageRouting.parse(manifest())
        val result = routing.eventCandidates("teen_042", "img_pov_exam_paper_001",
            "focus_teen_study", LifePeriod.TEEN)
        assertEquals("img_pov_exam_paper_001", result.first().imageId)
        assertEquals("img_stat_intelligence_001", result[1].imageId)
    }

    @Test fun `wooyoung pilot event resolves to its packaged image before generic fallbacks`() {
        val routing = ImageRouting.parse(manifest())
        val result = routing.eventCandidates("adult_002", "img_adult_pro_offer_001",
            "focus_young_adult_career", LifePeriod.YOUNG_ADULT)
        val pilot = result.first()
        assertEquals("img_adult_pro_offer_001", pilot.imageId)
        assertEquals("followup_20261031/img_adult_pro_offer_001.png", pilot.path)
        assertTrue(File(assetDir(), "images/${pilot.path}").isFile)
    }

    @Test fun `doljabi decision uses neutral art before a choice`() {
        val routing = ImageRouting.parse(manifest())
        val event = V4ContentParser.parseDecisionEvents(File(assetDir(), "events.sample.json").readText())
            .first { it.eventId == "infant_007" }
        val ui = buildDecisionUi(PendingDecision(event.eventId, event.kind, event.title, event.text, emptyList(), event.povImageId),
            event, Stats(), emptyMap(), imageRouting = routing, period = LifePeriod.INFANT)
        val expected = "followup_20261120/img_infant_toys_001.png"
        assertEquals(expected, ui.images.first().path)
        assertTrue(File(assetDir(), "images/$expected").isFile)
        val restored = restoredDecisionUi(event,
            PlayContent(emptyList(), emptyList(), mapOf(event.eventId to event), emptyMap(), imageRouting = routing),
            focusId = null, period = LifePeriod.INFANT)
        assertEquals(expected, restored.images.first().path)
    }

    @Test fun `doljabi result art follows the confirmed choice even for older saved image ids`() {
        val routing = ImageRouting.parse(manifest())
        val event = V4ContentParser.parseDecisionEvents(File(assetDir(), "events.sample.json").readText())
            .first { it.eventId == "infant_007" }
        val content = PlayContent(emptyList(), emptyList(), mapOf(event.eventId to event), emptyMap(), imageRouting = routing)
        val priorImageIds = listOf(null, "img_infant_toys_001", "img_doljabi_grasp_001")
        for (choiceId in listOf("A", "B", "C")) {
            val expectedId = if (choiceId == "B") "img_doljabi_grasp_001" else "img_infant_toys_001"
            val expectedPath = if (choiceId == "B") {
                "cutscenes/week11/img_doljabi_grasp_001.png"
            } else {
                "followup_20261120/img_infant_toys_001.png"
            }
            for (priorImageId in priorImageIds) {
                val imageId = doljabiOutcomeImageId(event.eventId, choiceId, priorImageId)
                val restored = restoredDecisionUi(event, content, null, LifePeriod.INFANT, savedEventImageId = imageId)
                assertEquals("$choiceId / $priorImageId", expectedId, restored.imageId)
                assertEquals("$choiceId / $priorImageId", expectedPath, restored.images.first().path)
                assertTrue(File(assetDir(), "images/$expectedPath").isFile)
                assertTrue("결과 복원은 다시 선택할 수 없다", restored.choices.isEmpty())
            }
        }
    }

    @Test fun `doljabi recap uses the actual choice without changing old records`() {
        val routing = ImageRouting.parse(manifest())
        val content = RecordContent(emptyList(), imageRouting = routing)
        val record = RunChoiceRecord("infant_007", "B", "b_primary", LifePeriod.INFANT, 1, "돌잡이", "판사봉을 잡는다", "결과")
        for (choiceId in listOf("A", "B", "C")) {
            for (priorImageId in listOf(null, "img_infant_toys_001", "img_doljabi_grasp_001")) {
                val choice = record.copy(choiceId = choiceId, eventImageId = priorImageId)
                val recap = recapMomentsUi(listOf(choice), content).single()
                val expected = if (choiceId == "B") "img_doljabi_grasp_001" else "img_infant_toys_001"
                assertEquals("$choiceId / $priorImageId", expected, recap.eventImageId)
                assertEquals(expected, recap.images.first().imageId)
                assertEquals(priorImageId, choice.eventImageId)
            }
        }
    }

    @Test fun `other events retain their saved outcome artwork`() {
        val routing = ImageRouting.parse(manifest())
        val event = V4ContentParser.parseDecisionEvents(File(assetDir(), "events.sample.json").readText())
            .first { it.eventId == "teen_042" }
        val content = PlayContent(emptyList(), emptyList(), mapOf(event.eventId to event), emptyMap(), imageRouting = routing)
        val savedId = "img_special_auto_door_001"
        val restored = restoredDecisionUi(event, content, null, LifePeriod.TEEN,
            savedEventImageId = doljabiOutcomeImageId(event.eventId, "B", savedId))
        assertEquals(savedId, restored.imageId)
        assertEquals(savedId, restored.images.first().imageId)
        assertNull(doljabiOutcomeImageId(event.eventId, "B", null))
        val record = RunChoiceRecord(event.eventId, "B", "b_primary", LifePeriod.TEEN, 2, event.title, "선택", "결과", eventImageId = savedId)
        assertEquals(savedId, recapMomentsUi(listOf(record), RecordContent(emptyList(), imageRouting = routing)).single().eventImageId)
    }

    @Test fun `unrelated focus does not replace a health event with study art`() {
        val routing = ImageRouting.parse(manifest())
        assertEquals("img_stat_health_001", routing.eventCandidates("adult_058", null,
            "focus_teen_study", LifePeriod.MIDDLE).first().imageId)
        assertEquals(listOf("img_week10_bg_child", "img_portrait_default_child_001"), routing.eventCandidates(
            "child_002", null, "focus_child_study", LifePeriod.CHILD).map { it.imageId })
    }

    @Test fun `shared event chooses the selected focus only among its declared themes`() {
        val routing = ImageRouting.parse(manifest())
        assertEquals("img_stat_intelligence_001", routing.eventCandidates("teen_032", null,
            "focus_teen_study", LifePeriod.TEEN).first().imageId)
        assertEquals("img_stat_happiness_001", routing.eventCandidates("teen_032", null,
            "focus_teen_rest", LifePeriod.TEEN).first().imageId)
        assertEquals("img_stat_social_001", routing.eventCandidates("teen_032", null,
            "focus_teen_club", LifePeriod.TEEN).first().imageId)
    }

    @Test fun `pending snapshot without image id still preserves event specific art`() {
        val routing = ImageRouting.parse(manifest())
        val event = V4ContentParser.parseDecisionEvents(File(assetDir(), "events.sample.json").readText())
            .first { it.eventId == "teen_042" }
        val ui = buildDecisionUi(PendingDecision(event.eventId, event.kind, event.title, event.text, emptyList()),
            event, Stats(), emptyMap(), imageRouting = routing, focusId = "focus_teen_study", period = LifePeriod.TEEN)
        assertEquals("img_pov_exam_paper_001", ui.images.first().imageId)
    }

    @Test fun `generic scene failure still leaves the actual period portrait as next fallback`() {
        val routing = ImageRouting.parse(manifest())
        assertEquals(listOf("img_stat_intelligence_001", "img_week10_bg_teen", "img_portrait_default_teen_001"),
            routing.eventCandidates("teen_007", null, "focus_teen_study", LifePeriod.TEEN).map { it.imageId })
    }

    @Test fun `unknown event stat tags are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            val json = org.json.JSONObject(manifest())
            json.getJSONObject("eventStatTags").put("adult_058", JSONArray().put("missing_tag"))
            ImageRouting.parse(json.toString())
        }
    }

    @Test fun `unsafe asset paths are rejected before any file read`() {
        assertFalse(isSafeImagePath("../secret.png"))
        assertFalse(isSafeImagePath("C:/secret.png"))
        assertFalse(isSafeImagePath("images\\secret.png"))
        assertFalse(isSafeImagePath("/secret.png"))
        assertTrue(isSafeImagePath("week8/generic/image.png"))
        val bad = manifest().replace("week8/generic/", "../")
        assertThrows(IllegalArgumentException::class.java) { ImageRouting.parse(bad) }
    }
}
