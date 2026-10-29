package com.example.lifegame.data

import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class Week10ProgressCodecTest {
    private val state = PeriodRunState(
        runId = "run-10", runSeed = 1010L, period = LifePeriod.MIDDLE, yearInPeriod = 2,
        selectedFocusId = "focus_middle_health", stats = Stats(health = 72, wealth = 81),
        flags = linkedSetOf("married", "home_owned"),
        lifeFacts = LifeFacts(maritalStatus = "MARRIED", housingType = "OWNED", wealthBand = "HIGH"),
        dominantFocusStat = StatKey.HEALTH, recentChoiceIds = listOf("event:A"),
        moments = listOf(LifeMoment(LifePeriod.MIDDLE, 1, "기억", "img_moment")),
    )
    private val choice = RunChoiceRecord(
        "event", "A", "success", LifePeriod.MIDDLE, 1, "중요한 선택", "도전", "성공했다", "img_moment",
    )
    private val outcome = SavedOutcome(
        "event", "A", "success", "도전", "성공했다", Stats(), state.stats,
        LifeFacts(), state.lifeFacts, false,
    )

    @Test fun currentRunRoundTripKeepsResultHistoryAvatarAndTitle() {
        val run = V4SavedRun(
            "우영", state, listOf(choice), outcome,
            AvatarConfig("skin_2", "hair_3", "eyes_1", "local://portrait", "local://sheet", true),
            AwardedTitle("title_health", "건강 장인", "꾸준히 관리했다"),
            TurnPhase.DECISION,
        )
        assertEquals(run, Week10ProgressCodec.decode(Week10ProgressCodec.encode(run)))
    }

    @Test fun completedLivesRoundTripKeepsAtLeastThreeRecords() {
        val records = (1..3).map { index ->
            CompletedLifeRecord(
                runId = "life-$index", playerName = "플레이어 $index", summary = "요약 $index",
                endingId = "ending_$index", endingTitle = "결말 $index", endingSummary = "본문 $index",
                awardedTitle = AwardedTitle("title_$index", "칭호 $index", "설명"),
                finalState = state.copy(runId = "life-$index"), choices = listOf(choice),
            )
        }
        assertEquals(records, Week10ProgressCodec.decodeCompletedLives(Week10ProgressCodec.encodeCompletedLives(records)))
    }

    @Test fun openedPathsSurviveSaveAndLegacyOutcomesRemainReadable() {
        val saved = V4SavedRun("검사", state, pendingOutcome = outcome.copy(openedEvents = listOf("거실 연주회")))
        val raw = Week10ProgressCodec.encode(saved)
        assertEquals(listOf("거실 연주회"), Week10ProgressCodec.decode(raw)?.pendingOutcome?.openedEvents)
        val legacy = org.json.JSONObject(raw)
        legacy.getJSONObject("pendingOutcome").remove("openedEvents")
        assertEquals(emptyList<String>(), Week10ProgressCodec.decode(legacy.toString())?.pendingOutcome?.openedEvents)
    }

    @Test fun corruptOrFutureSaveFailsSafely() {
        assertNull(Week10ProgressCodec.decode("not-json"))
        assertNull(Week10ProgressCodec.decode("""{"saveFormatVersion":999,"state":{}}"""))
        assertTrue(Week10ProgressCodec.decodeCompletedLives("broken").isEmpty())
    }

    @Test fun versionTwoSaveMigratesWithNewFieldsDefaulted() {
        val v2 = """{
          "saveFormatVersion":2,"playerName":"이전 사용자",
          "state":{"runId":"old","runSeed":2,"period":"CHILD","year":1,
          "stats":{"health":50,"fitness":50,"intelligence":50,"social":50,"wealth":50,"happiness":50,"luck":50},
          "flags":[],"moments":[],"ending":false,"dead":false}
        }""".trimIndent()
        val migrated = requireNotNull(Week10ProgressCodec.decode(v2))
        assertEquals("이전 사용자", migrated.playerName)
        assertTrue(migrated.choices.isEmpty())
        assertNull(migrated.pendingOutcome)
        assertEquals(AvatarConfig(), migrated.avatar)
    }
}
