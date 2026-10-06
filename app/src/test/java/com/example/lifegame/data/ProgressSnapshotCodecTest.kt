package com.example.lifegame.data

import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressSnapshotCodecTest {
    @Test
    fun encodeAndDecode_preservesCompleteProgress() {
        val before = Stats()
        val after = Stats(
            health = 61,
            fitness = 62,
            intelligence = 63,
            social = 64,
            wealth = 65,
            happiness = 66,
            luck = 67,
        )
        val saved = SavedGame(
            characterId = 2,
            playerName = "김우영",
            progress = GameProgress(
                runId = "run-save-test",
                contentVersion = "week7",
                saveFormatVersion = 1,
                selectedImageRef = "character_2",
                stage = LifeStage.TEEN,
                currentEventOccurrenceId = "run-save-test:teen_event_12:3",
                screenState = GameScreenState.CHOICE_RESULT,
                stats = after,
                flags = setOf("club_member", "has_friend"),
                completedEventIds = setOf("teen_event_10", "teen_event_12"),
                handledOccurrenceIds = setOf("run-save-test:teen_event_12:3"),
                choiceHistory = listOf(
                    ChoiceHistory(
                        eventOccurrenceId = "run-save-test:teen_event_12:3",
                        eventId = "teen_event_12",
                        choiceId = "B",
                        beforeStats = before,
                        afterStats = after,
                        resultText = "저장할 결과",
                    ),
                ),
                specialEventCount = 2,
                stageEventCount = 4,
            ),
        )

        val restored = ProgressSnapshotCodec.decode(ProgressSnapshotCodec.encode(saved))

        assertEquals(saved, restored)
    }

    @Test
    fun decode_usesDefaultNameForLegacySnapshot() {
        val saved = SavedGame(
            characterId = 1,
            playerName = "김우영",
            progress = GameProgress(
                runId = "legacy-run",
                currentEventOccurrenceId = "legacy-run:infant_event:1",
            ),
        )
        val legacySnapshot = JSONObject(ProgressSnapshotCodec.encode(saved))
            .apply { remove("playerName") }
            .toString()

        assertEquals(
            saved.copy(playerName = "플레이어"),
            ProgressSnapshotCodec.decode(legacySnapshot),
        )
    }

    @Test
    fun decode_returnsNullForCorruptedOrUnsupportedSave() {
        assertNull(ProgressSnapshotCodec.decode("not-json"))
        val valid = ProgressSnapshotCodec.encode(
            SavedGame(
                characterId = 1,
                progress = GameProgress(
                    runId = "unsupported-run",
                    currentEventOccurrenceId = "unsupported-run:infant_event:1",
                ),
            ),
        )
        assertNull(
            ProgressSnapshotCodec.decode(
                valid.replace("\"saveFormatVersion\":1", "\"saveFormatVersion\":99"),
            ),
        )
    }
}
