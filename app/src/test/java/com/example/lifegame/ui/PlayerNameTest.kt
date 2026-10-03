package com.example.lifegame.ui

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.LifeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerNameTest {

    @Test
    fun blankNameFallsBackToDefault() {
        assertEquals(DEFAULT_PLAYER_NAME, normalizePlayerName(""))
        assertEquals(DEFAULT_PLAYER_NAME, normalizePlayerName("   "))
    }

    @Test
    fun nameIsTrimmedAndLimited() {
        assertEquals("선한", normalizePlayerName("  선한  "))
        assertEquals("김 선한", normalizePlayerName("김   선한"))
        val long = normalizePlayerName("가나다라마바사아자차카타파하")
        assertEquals(PLAYER_NAME_MAX_LENGTH, long.length)
    }

    @Test
    fun nameCallUsesAWhenLastSyllableHasFinalConsonant() {
        assertEquals("선한아", nameCallOf("선한"))
        assertEquals("재겸아", nameCallOf("재겸"))
    }

    @Test
    fun nameCallUsesYaWhenLastSyllableHasNoFinalConsonant() {
        assertEquals("민지야", nameCallOf("민지"))
        assertEquals("플레이어야", nameCallOf(DEFAULT_PLAYER_NAME))
    }

    @Test
    fun nonHangulNameIsCalledAsIs() {
        assertEquals("Alex", nameCallOf("Alex"))
        assertEquals("", nameCallOf(""))
    }

    @Test
    fun eventAndChoicesHaveNoPlaceholdersAfterReplacement() {
        val event = GameEvent(
            eventId = "test_001",
            stage = LifeStage.INFANT,
            title = "{name}의 하루",
            text = "엄마: \"{nameCall}~ 비행기 들어갑니다~\" 간호사: \"{name} 아기가 또 웃네요.\"",
            imageId = "img_test",
            choices = listOf(
                Choice(choiceId = "A", label = "{name}답게 웃는다", resultText = "{nameCall}, 잘했어."),
            ),
        )

        val replaced = event.withPlayerName("선한", nameCallOf("선한"))

        assertEquals("선한의 하루", replaced.title)
        assertTrue(replaced.text.contains("선한아~"))
        assertTrue(replaced.text.contains("선한 아기가"))
        assertEquals("선한답게 웃는다", replaced.choices.single().label)
        assertEquals("선한아, 잘했어.", replaced.choices.single().resultText)
        val all = listOf(replaced.title, replaced.text) + replaced.choices.flatMap { listOf(it.label, it.resultText) }
        assertFalse(all.any { it.contains("{name") })
    }

    @Test
    fun endingHasNoPlaceholdersAfterReplacement() {
        val ending = Ending(endingId = "e1", title = "{name}의 인생", summary = "{nameCall}, 수고했어.", priority = 1)
        val replaced = ending.withPlayerName("민지", nameCallOf("민지"))
        assertEquals("민지의 인생", replaced.title)
        assertEquals("민지야, 수고했어.", replaced.summary)
    }

    /** 콘텐츠에 {name}, {nameCall} 말고 다른 치환 표기가 있으면 화면에 그대로 남으므로 막는다. */
    @Test
    fun contentUsesOnlySupportedPlaceholders() {
        val assets = File("src/main/assets")
        listOf("events.sample.json", "endings.sample.json").forEach { fileName ->
            val text = File(assets, fileName).readText(Charsets.UTF_8)
            val unsupported = Regex("\\{[A-Za-z_]+\\}").findAll(text)
                .map { it.value }
                .filterNot { it == "{name}" || it == "{nameCall}" }
                .toSet()
            assertTrue("$fileName has unsupported placeholders: $unsupported", unsupported.isEmpty())
        }
    }
}
