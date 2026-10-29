package com.example.lifegame.ui

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.components.HealthLevel
import com.example.lifegame.ui.components.healthLevelOf
import com.example.lifegame.ui.images.ImageRouting
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 10주차: 건강 색 구간 · 완료 기록 → 기록증 · 주마등 · 칭호 수집 변환 */
class LifeRecordUiTest {
    private val assets = File("src/main/assets")
    private val periods = V4ContentParser.parsePeriods(File(assets, "periods.sample.json").readText())
    private val routing = ImageRouting.parse(File(assets, "image_routing.json").readText())
    private val content = RecordContent(periods = periods, imageRouting = routing)

    private fun choice(id: String, period: LifePeriod, year: Int, image: String? = null) = RunChoiceRecord(
        eventId = id,
        choiceId = "${id}_a",
        outcomeId = "${id}_o",
        period = period,
        yearInPeriod = year,
        eventTitle = "사건 $id",
        choiceLabel = "선택 $id",
        resultText = "결과 $id",
        momentImageId = image,
    )

    private fun record(
        runId: String = "run-1",
        state: PeriodRunState = PeriodRunState(
            runId = runId,
            runSeed = 1L,
            period = LifePeriod.OLD_AGE,
            stats = Stats(health = 40, fitness = 75, intelligence = 60),
            lifeFacts = LifeFacts(educationLevel = "대학 진학", careerTrack = "코치", maritalStatus = "결혼", childrenCount = 2),
            isEnding = true,
            isDead = true,
            endingPeriod = LifePeriod.OLD_AGE,
        ),
        choices: List<RunChoiceRecord> = emptyList(),
        title: AwardedTitle? = AwardedTitle("title_coach", "끝까지 뛴 코치", "운동장을 떠나지 않았다"),
        name: String = "재겸",
    ) = CompletedLifeRecord(
        runId = runId,
        playerName = name,
        summary = "요약",
        endingId = "ending_x",
        endingTitle = "운동장의 노인",
        endingSummary = "평생 공을 찼다.",
        awardedTitle = title,
        finalState = state,
        choices = choices,
        avatar = AvatarConfig(skinToneId = "skin_tan", hairId = "hair_curly_brown", eyesId = "eyes_big"),
    )

    @Test
    fun healthGaugeColorBoundaries() {
        assertEquals(HealthLevel.GOOD, healthLevelOf(100))
        assertEquals(HealthLevel.GOOD, healthLevelOf(80))
        assertEquals(HealthLevel.CAUTION, healthLevelOf(79))
        assertEquals(HealthLevel.CAUTION, healthLevelOf(60))
        assertEquals(HealthLevel.DANGER, healthLevelOf(59))
        assertEquals(HealthLevel.DANGER, healthLevelOf(0))
    }

    @Test fun recapUsesBodySnapshotForEachChoiceInsteadOfFinalBody() {
        val choices = listOf(
            choice("slim", LifePeriod.TEEN, 1).copy(bodyType = BodyType.SLIM),
            choice("fit", LifePeriod.YOUNG_ADULT, 1).copy(bodyType = BodyType.MUSCULAR),
            choice("old-save", LifePeriod.MIDDLE, 1),
        )
        val ui = buildLifeRecordUi(record(choices = choices), content)
        assertEquals(listOf(BodyType.SLIM, BodyType.MUSCULAR, BodyType.STANDARD), ui.recap.map { it.avatar?.bodyType })
        assertEquals(listOf(LifePeriod.TEEN, LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE), ui.recap.map { it.avatar?.period })
    }

    @Test
    fun certificateUsesSavedTitleFactsAndFinalLook() {
        val ui = buildLifeRecordUi(record(), content)
        assertEquals("끝까지 뛴 코치", ui.title?.text)
        assertEquals("운동장의 노인", ui.summary.headline)
        assertEquals(LifePeriod.OLD_AGE, ui.avatar.period)
        assertEquals(BodyType.MUSCULAR, ui.avatar.bodyType)
        assertEquals("skin_tan", ui.avatar.skinToneId)
        assertTrue(ui.summary.educationCareer.any { it.value == "대학 진학" })
        assertTrue(ui.summary.familyLife.any { it.value.contains("2") })
        // 운동 경로는 신규 성취 분위기 배경을 쓰며 특정 직업을 그림으로 단정하지 않는다.
        assertEquals(listOf("img_week10_ending_achievement"), ui.backdrop.map { it.imageId })
    }

    @Test
    fun recapKeepsRealChoicesInTimeOrderWithoutDuplicates() {
        val choices = listOf(
            choice("e1", LifePeriod.INFANT, 1, image = "img_stat_health_001"),
            choice("e2", LifePeriod.CHILD, 2),
            choice("e1", LifePeriod.INFANT, 1, image = "img_stat_health_001"),
            choice("e3", LifePeriod.TEEN, 4, image = "img_stat_intelligence_001"),
        )
        val recap = recapMomentsUi(choices, content)
        assertEquals(listOf("사건 e1", "사건 e2", "사건 e3"), recap.map { it.title })
        assertEquals("청소년기 · 4번째 해", recap.last().place)
        assertEquals("img_stat_intelligence_001", recap.last().images.first().imageId)
        // 사람이 그려진 기본 초상은 주마등 배경으로 쓰지 않는다(같은 인물을 따로 세운다).
        assertTrue(recap.all { moment -> moment.images.none { it.usage == "character" } })
    }

    @Test
    fun recapPrefersMajorScenesAndSpreadsTheRestAcrossTheLife() {
        val many = (1..20).map { choice("e$it", if (it <= 10) LifePeriod.CHILD else LifePeriod.MIDDLE, (it % 5) + 1) }
        val withScene = many.mapIndexed { i, c -> if (i == 15) c.copy(momentImageId = "img_stat_wealth_001") else c }
        val recap = recapMomentsUi(withScene, content, limit = 4) { period -> com.example.lifegame.ui.avatar.AvatarLook(period = period) }
        assertEquals(4, recap.size)
        assertTrue("장면이 기록된 사건은 꼭 들어간다", recap.any { it.title == "사건 e16" })
        assertEquals("처음과 끝을 포함한다", "사건 e1", recap.first().title)
        assertEquals("사건 e20", recap.last().title)
        // 그림 없는 사건은 빈 무대 위에 그 시기의 같은 인물을 세운다.
        assertEquals(LifePeriod.CHILD, recap.first().avatar?.period)
        assertFalse(recap.first().isFirstPerson(null))
        // 사건 전용 그림이 실제로 보이면 1인칭 장면으로 보고 얼굴을 올리지 않는다.
        val scene = recap.first { it.title == "사건 e16" }
        assertTrue(scene.isFirstPerson(scene.images.first()))
        assertFalse(scene.isFirstPerson(routing.assets.getValue("img_stat_health_001").copy(imageId = "img_stat_health_001")))
    }

    @Test
    fun lifePathFollowsTheEnginesPathEnding() {
        val smart = Stats(intelligence = 99, health = 60)
        assertEquals(LifePathKind.HEALTH, lifePathOf("ending_path_hea_peaks", emptySet(), smart))
        assertEquals(LifePathKind.LUCK, lifePathOf("ending_path_luk_jackpot", emptySet(), smart))
        // 경로 결말이 아니면 능력치 · 경로 사건으로 고른다.
        assertEquals(LifePathKind.INTELLIGENCE, lifePathOf("ending_default", emptySet(), smart))
        assertEquals(LifePathKind.INTELLIGENCE, lifePathOf(null, emptySet(), smart))
    }

    @Test
    fun spreadEvenlyKeepsOrderAndEnds() {
        assertEquals(listOf(1, 4, 7, 10), spreadEvenly((1..10).toList(), 4))
        assertEquals(listOf(1, 2), spreadEvenly(listOf(1, 2), 5))
    }

    @Test
    fun missingTitleAndEarlyDeathAreShownAsTheyHappened() {
        val teen = PeriodRunState(
            runId = "early",
            runSeed = 2L,
            period = LifePeriod.TEEN,
            yearInPeriod = 3,
            stats = Stats(health = 0),
            isEnding = true,
            isDead = true,
            endingPeriod = LifePeriod.TEEN,
            lifeFacts = LifeFacts(deathPeriod = LifePeriod.TEEN, deathCause = "건강 악화"),
            moments = listOf(com.example.lifegame.domain.model.LifeMoment(LifePeriod.TEEN, 3, "쓰러졌다")),
        )
        val ui = buildLifeRecordUi(record(runId = "early", state = teen, title = null), content)
        assertNull(ui.title)
        assertEquals(true, ui.summary.death?.early)
        assertTrue(ui.summary.headline.contains("청소년기"))
        assertEquals(LifePeriod.TEEN, ui.avatar.period)
        val row = record(runId = "early", state = teen, title = null).toFinishedLifeUi(periods)
        assertEquals("청소년기에 생을 마침", row.endedAt)
    }

    @Test
    fun titleCollectionShowsEarnedFirstAndKeepsRetiredSnapshots() {
        val titles = listOf(
            LifeTitle("title_a", "가", "설명 가", priority = 10),
            LifeTitle("title_b", "나", "설명 나", priority = 30),
            LifeTitle("title_default", "평범한 사람", "기본", priority = 0, isDefault = true),
        )
        val lives = listOf(
            record(runId = "r1", title = AwardedTitle("title_a", "가(그때 문구)", "그때 설명"), name = "하나"),
            record(runId = "r2", title = AwardedTitle("title_old", "사라진 칭호", "옛 설명"), name = "둘"),
            record(runId = "r3", title = AwardedTitle("title_a", "가(그때 문구)", "그때 설명"), name = "셋"),
        )
        val collection = buildTitleCollection(titles, lives)
        assertEquals(4, collection.total)
        assertEquals(2, collection.earnedCount)
        assertTrue(collection.entries.take(2).all { it.earned })
        val a = collection.entries.first { it.titleId == "title_a" }
        assertEquals("가(그때 문구)", a.text)
        assertEquals(listOf("하나", "셋"), a.earnedBy)
        assertTrue(collection.entries.any { it.titleId == "title_old" && it.earned })
        // 받지 않은 칭호는 우선순위 순서대로 뒤에 남는다.
        assertEquals(listOf("title_b", "title_default"), collection.entries.filterNot { it.earned }.map { it.titleId })
        assertFalse(collection.entries.first { it.titleId == "title_b" }.earned)
    }
}
