package com.example.lifegame.ui

import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-06 통합지시서 요구 4(기록 구분 · 복원 순서) · 요구 7(선택지 보조 태그 분리). */
class IterationFeaturesTest {

    private val stories = YearlyStoryBook.parse(File("src/main/assets/yearly_stories.sample.json").readText())
    private val periods = listOf(
        PeriodDefinition(LifePeriod.INFANT, order = 0, years = 5, displayName = "영아기"),
        PeriodDefinition(LifePeriod.CHILD, order = 1, years = 5, displayName = "유년기"),
    )
    private val moments = listOf(
        LifeMoment(LifePeriod.INFANT, 1, "영아기 1번째 해, 세상을 처음 배웠다."),
        LifeMoment(LifePeriod.INFANT, 1, "건반을 두드리자 박수가 터졌다."),
        LifeMoment(LifePeriod.INFANT, 2, "영아기 2번째 해, 세상을 처음 배웠다."),
        LifeMoment(LifePeriod.CHILD, 1, "친구와 블록을 쌓았다."),
        LifeMoment(LifePeriod.CHILD, 1, "선생님이 칭찬해 주었다."),
    )
    private val outcomes = setOf("건반을 두드리자 박수가 터졌다.", "선생님이 칭찬해 주었다.")

    @Test
    fun choiceResultsAndYearlyStoriesAreTaggedApart() {
        val lines = buildLogLines(moments, periods, stories, runSeed = 11L, outcomeTexts = outcomes)
        assertEquals(
            listOf(LogKind.STORY, LogKind.CHOICE, LogKind.STORY, LogKind.STORY, LogKind.RECORD, LogKind.CHOICE),
            lines.map { it.kind },
        )
        assertEquals("건반을 두드리자 박수가 터졌다.", lines[1].text)
        assertEquals("친구와 블록을 쌓았다.", lines[4].text)
    }

    @Test
    fun rebuildingAfterRestoreKeepsTheSameOrderWithoutDuplicates() {
        val first = buildLogLines(moments, periods, stories, runSeed = 11L, outcomeTexts = outcomes)
        // 저장 → 복원은 같은 moments · runSeed를 돌려준다. 다시 만들어도 순서 · 문장 · 키가 같다.
        val restored = buildLogLines(moments.toList(), periods, stories, runSeed = 11L, outcomeTexts = outcomes)
        assertEquals(first, restored)
        assertEquals(first.size, first.map { it.key }.toSet().size)
        // 해마다 이야기는 한 번만, 그 해 첫 줄로 들어간다.
        val storyYears = first.filter { it.kind == LogKind.STORY }.map { it.label }
        assertEquals(storyYears.distinct(), storyYears)
        // 화면은 최신이 위: 뒤집으면 마지막 선택 결과가 맨 앞이다.
        assertEquals("선생님이 칭찬해 주었다.", first.asReversed().first().text)
    }

    @Test
    fun yearlyStoriesNeverClaimMajorLifeFacts() {
        // 자동 이야기가 결혼 · 취업 · 합격을 사실로 단정하지 않는다(통합지시서 요구 4).
        val banned = listOf("결혼했", "결혼식을 올", "취업했", "입사했", "합격했", "합격 통지")
        LifePeriod.entries.forEach { period ->
            (1..5).forEach { year ->
                (0L until 40L).mapNotNull { stories.lineFor(it, period, year) }.toSet().forEach { line ->
                    banned.forEach { word -> assertTrue("$period $year: $line", !line.contains(word)) }
                }
            }
        }
    }

    @Test
    fun choiceHintSplitsLikelihoodFromStatTags() {
        val choice = DecisionChoiceUi("c", "A", "봉사를 나간다", hint = "가능성 높음 · 사회성 · 행복")
        assertEquals("가능성 높음", choice.likelihood)
        assertEquals(listOf("사회성", "행복"), choice.statTags)
        val single = DecisionChoiceUi("d", "B", "쉰다", hint = "건강 · 행복")
        assertNull(single.likelihood)
        assertEquals(listOf("건강", "행복"), single.statTags)
        val empty = DecisionChoiceUi("e", "C", "그냥 둔다", hint = "")
        assertNull(empty.likelihood)
        assertEquals(emptyList<String>(), empty.statTags)
    }
}
