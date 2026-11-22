package com.example.lifegame.data

import com.example.lifegame.domain.engine.MarriageContract
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.ChallengeCode
import com.example.lifegame.ui.ChallengeCodeResult
import com.example.lifegame.ui.ChallengeStart
import com.example.lifegame.ui.DiceRule
import com.example.lifegame.ui.Gender
import com.example.lifegame.ui.StartingDice
import com.example.lifegame.ui.StartingRoll
import com.example.lifegame.ui.StatRoll
import kotlin.random.Random
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Week14WooyoungGameLogicTest {
    @Test fun `all 36 eye pairs use their sum for all six stats`() {
        StartingDice.STAT_LABELS.forEach { label ->
            (1..6).forEach { first -> (1..6).forEach { second ->
                val result = StatRoll(label, first, second).let {
                    StartingRoll(List(6) { index ->
                        StatRoll(StartingDice.STAT_LABELS[index], first, second)
                    }).results.first { row -> row.label == label }
                }
                assertEquals(first + second, result.sum)
                assertEquals(first + second, result.increase)
                assertEquals(50 + first + second, result.finalValue)
            } }
        }
    }

    @Test fun `LG1 reproduces legacy values while LG2 uses sums`() {
        val raw = "1,3,1,4,6,6,1,1,2,5,3,4"
        val old = ChallengeStart(Gender.MALE, raw, 42, DiceRule.LEGACY_LG1)
        val current = old.copy(diceRule = DiceRule.SUM_LG2)
        assertTrue(ChallengeCode.encode(old).startsWith("LG1-"))
        assertTrue(ChallengeCode.encode(current).startsWith("LG2-"))
        assertEquals(44, StartingRoll.decode(raw, DiceRule.LEGACY_LG1)!!.results.first().finalValue)
        assertEquals(54, StartingRoll.decode(raw, DiceRule.SUM_LG2)!!.results.first().finalValue)
        assertEquals(ChallengeCodeResult.Valid(old), ChallengeCode.decode(ChallengeCode.encode(old)))
        assertEquals(ChallengeCodeResult.Valid(current), ChallengeCode.decode(ChallengeCode.encode(current)))
    }

    @Test fun `ten thousand seeded starts are deterministic and stay in new bounds`() {
        fun sample() = Random(20261120).let { random ->
            List(10_000) { StartingDice.roll(random).results.map { it.finalValue } }
        }
        val first = sample()
        assertEquals(first, sample())
        assertTrue(first.flatten().all { it in 52..62 })
        assertTrue(first.flatten().toSet().size > 8)
    }

    @Test fun `v1 to v4 shaped saves default to legacy rule without changing stored stats`() {
        val state = PeriodRunState("old-run", 7, LifePeriod.INFANT)
        val encoded = JSONObject(Week10ProgressCodec.encode(V4SavedRun("우영", state)))
            .put("saveFormatVersion", 4)
            .apply { remove("startingDiceRule"); remove("rerollsRemaining") }
            .toString()
        val restored = Week10ProgressCodec.decode(encoded)!!
        assertEquals(null, restored.startingDiceRule)
        assertEquals(0, restored.rerollsRemaining)
        assertEquals(state.stats, restored.state.stats)
    }

    @Test fun `new save preserves raw roll rule health and rerolls`() {
        val state = PeriodRunState("new-run", 9, LifePeriod.INFANT)
        val run = V4SavedRun("우영", state, startingRollRaw = "1,2,1,2,1,2,1,2,1,2,1,2", startingDiceRule = "SUM_LG2", rerollsRemaining = 2)
        assertEquals(run, Week10ProgressCodec.decode(Week10ProgressCodec.encode(run)))
    }

    @Test fun `wedding requires one partner and every prior stage`() {
        val ready = setOf(
            "dating", "engaged", "families_met", "wedding_ready", "partner_music",
            "met_music_band", "dating__path_hap_05", "partner_id_music_band",
        )
        MarriageContract.requireFirstWedding("love_wedding_music", ready, setOf("married"))
        assertTrue(runCatching {
            MarriageContract.requireFirstWedding("love_wedding_book", ready, setOf("married"))
        }.isFailure)
        assertTrue(runCatching {
            MarriageContract.requireFirstWedding("love_wedding_music", ready - "families_met", setOf("married"))
        }.isFailure)
        assertTrue(runCatching {
            MarriageContract.requireFirstWedding("love_wedding_music", ready + "married", setOf("married"))
        }.isFailure)
        assertTrue(runCatching {
            MarriageContract.requireFirstWedding("love_wedding_music", ready - "wedding_ready", setOf("married"))
        }.isFailure)
        assertTrue(runCatching {
            MarriageContract.requireFirstWedding("love_wedding_prep_01", ready, setOf("married"))
        }.isFailure)
    }

    @Test fun `partner id starts from the actual dating choice and becomes spouse only at wedding`() {
        val dating = MarriageContract.synchronizeIdentity(
            "path_hap_05",
            "A",
            setOf("dating", "partner_music", "met_music_band", "dating__path_hap_05"),
        )
        assertEquals("music_band", MarriageContract.currentPartnerId(dating))
        assertEquals(null, MarriageContract.spouseId(dating))

        val married = MarriageContract.synchronizeIdentity(
            "love_wedding_music",
            "A",
            dating + setOf("engaged", "families_met", "wedding_ready", "married"),
        )
        assertEquals("music_band", MarriageContract.spouseId(married))

        val brokenUp = MarriageContract.synchronizeIdentity(
            "love_conflict_music",
            "B",
            married - setOf("dating", "partner_music", "married"),
        )
        assertEquals(null, MarriageContract.currentPartnerId(brokenUp))
        assertEquals(null, MarriageContract.spouseId(brokenUp))
    }

    @Test fun `non wedding outcomes and already married saves remain untouched`() {
        MarriageContract.requireFirstWedding("adult_001", setOf("married"), emptySet())
        assertFalse(MarriageContract.partnerIds(setOf("married", "friend_old")).isNotEmpty())
    }
}
