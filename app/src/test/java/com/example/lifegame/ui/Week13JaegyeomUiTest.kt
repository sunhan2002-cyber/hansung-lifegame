package com.example.lifegame.ui

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.data.Week10ProgressCodec
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import com.example.lifegame.ui.avatar.AvatarRecolor
import com.example.lifegame.ui.screens.DOLJABI_EVENT_ID
import com.example.lifegame.ui.screens.doljabiCaption
import com.example.lifegame.ui.screens.doljabiPreviewAlreadyShown
import com.example.lifegame.ui.screens.shouldPlayDoljabiCutscene
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-13 김재겸 과제(화면 흐름 · 커스터마이징 · 확장 기능 표시)의 화면 쪽 규칙. */
class Week13JaegyeomUiTest {

    private fun asset(name: String) = File("src/main/assets/$name").readText()

    // ---- 과제 7: 선택지 라벨 마침표 ----

    @Test
    fun `choice labels lose only the trailing period`() {
        assertEquals("수능을 본다", displayChoiceLabel("수능을 본다."))
        assertEquals("수능을 본다", displayChoiceLabel("수능을 본다 "))
        assertEquals("일단 버틴다...", displayChoiceLabel("일단 버틴다..."))
        assertEquals("그냥…", displayChoiceLabel("그냥…"))
        assertEquals("\"제가 많이 긴장했나 봅니다.\"", displayChoiceLabel("\"제가 많이 긴장했나 봅니다.\""))
        assertEquals("정말?", displayChoiceLabel("정말?"))
    }

    @Test
    fun `no content choice label is shown with a trailing period`() {
        val events = V4ContentParser.parseDecisionEvents(asset("events.sample.json"))
        val shown = events.flatMap { event -> event.choices.map { event.eventId to displayChoiceLabel(it.label) } }
        assertTrue(shown.size > 900)
        val dotted = shown.filter { (_, label) -> label.endsWith(".") && !label.endsWith("...") }
        assertTrue("화면에 마침표로 끝나는 라벨: ${dotted.take(5)}", dotted.isEmpty())
    }

    // ---- 확장 기능 4: 같은 출발선 도전 코드 ----

    private val start = ChallengeStart(Gender.MALE, "1,3,6,6,2,2,5,1,4,4,3,3", seed = 1_731_500_000_123L)

    // ---- 2026-11-20 과제: 3D 눈과 능력치 줄 1:1 (값은 로직 결과를 그대로) ----

    @Test
    fun `starting stat rows carry the logic's eyes, sum, increase and final value`() {
        val roll = checkNotNull(StartingRoll.decode("3,4,2,5,6,1,1,4,5,4,2,2"))
        val rows = startingStatRows(roll)
        assertEquals(listOf("운동능력", "지력", "사회성", "경제력", "행복", "운"), rows.map { it.label })
        // 화면은 규칙을 다시 계산하지 않는다: 줄의 모든 숫자는 로직(StatRoll)의 값이다(규칙 변경은 김우영 담당).
        rows.zip(roll.rolls).forEach { (row, stat) ->
            assertEquals(stat.first, row.first)
            assertEquals(stat.second, row.second)
            assertEquals(stat.sum, row.sum)
            assertEquals(stat.delta, row.increase)
            assertEquals(stat.finalValue, row.finalValue)
            assertEquals(row.finalValue, row.base + row.increase)
            val shown = row.formula.replace('\u00A0', ' ')
            assertTrue(shown, shown.startsWith("${stat.first} + ${stat.second} = ${stat.sum} · ${row.base} "))
            assertTrue(shown, shown.endsWith(" = ${stat.finalValue}"))
            assertTrue(row.speech, row.speech.startsWith("${stat.label}, 주사위 ${stat.first}"))
            assertTrue(row.speech, row.speech.contains("합 ${stat.sum}") && row.speech.endsWith("${stat.finalValue}"))
        }
        // 11/20 통합지시서 예시의 모양(경제력 1 + 4 = 5 → +5 → 55)과 내림 · 그대로의 모양.
        val up = StartingStatRowUi("경제력", 1, 4, 5, 5, 50, 55)
        assertEquals("1 + 4 = 5 · 50 + 5 = 55", up.formula.replace('\u00A0', ' '))
        assertEquals("경제력, 주사위 1과 4, 합 5, 50에서 5 올라 55", up.speech)
        val down = StartingStatRowUi("지력", 4, 1, 5, -4, 50, 46)
        assertEquals("4 + 1 = 5 · 50 − 4 = 46", down.formula.replace('\u00A0', ' '))
        assertEquals("지력, 주사위 4와 1, 합 5, 50에서 4 내려 46", down.speech)
        assertEquals("행복, 주사위 3과 4, 합 7, 50 그대로 50", StartingStatRowUi("행복", 3, 4, 7, 0, 50, 50).speech)
        // 줄바꿈은 가운데 점에서만: 덩어리 안에는 일반 공백이 없다.
        assertEquals(listOf("1\u00A0+\u00A04\u00A0=\u00A05", "50\u00A0+\u00A05\u00A0=\u00A055"), up.formula.split(" · "))
    }

    @Test
    fun `challenge code round trips gender dice and seed`() {
        val code = ChallengeCode.encode(start)
        assertTrue(code, code.startsWith("LG2-M-136622514433-"))
        assertEquals(ChallengeCodeResult.Valid(start), ChallengeCode.decode(code))
        assertEquals("소문자 · 공백도 받는다", ChallengeCodeResult.Valid(start), ChallengeCode.decode(" ${code.lowercase()} "))
    }

    @Test
    fun `broken challenge codes explain what is wrong`() {
        val code = ChallengeCode.encode(start)
        fun message(input: String) = (ChallengeCode.decode(input) as ChallengeCodeResult.Invalid).message
        assertEquals("코드를 입력해 주세요.", message("   "))
        assertEquals("인생게임 도전 코드가 아니거나 지원하지 않는 형식입니다.", message("ABC-123"))
        assertEquals("인생게임 도전 코드가 아니거나 지원하지 않는 형식입니다.", message(code.replaceFirst("LG2", "LG9")))
        // 주사위 한 자리를 잘못 옮겨 적으면 검사 자리가 맞지 않는다.
        val typo = code.replaceFirst("136622514433", "136622514434")
        assertEquals("코드 일부가 틀렸습니다. 다시 확인해 주세요.", message(typo))
    }

    // ---- 과제 6: 커스터마이징 색 ----

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun luma(p: Int) = 0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)

    @Test
    fun `hair dye keeps outline to highlight order and transparency`() {
        val px = intArrayOf(0, argb(0x20, 0x20, 0x20), argb(0x60, 0x60, 0x60), argb(0xA0, 0xA0, 0xA0), argb(0x60, 0x60, 0x60, 128))
        AvatarRecolor.dyeHairPixels(px, argb(0xD9, 0xB4, 0x5A))
        assertEquals("투명한 픽셀은 그대로", 0, px[0])
        assertTrue("어두운 순서 유지", luma(px[1]) < luma(px[2]) && luma(px[2]) < luma(px[3]))
        assertTrue("금발 쪽(빨강 > 파랑)으로 물든다", ((px[3] shr 16) and 0xFF) > (px[3] and 0xFF))
        assertEquals("반투명은 알파를 지킨다", 128, (px[4] ushr 24) and 0xFF)
    }

    @Test
    fun `outfit color changes only the main garment color`() {
        val red = argb(0xD9, 0x57, 0x3F)
        val gray = argb(0x80, 0x80, 0x80)
        val darkRed = argb(0x4A, 0x18, 0x10) // 같은 계열이지만 훨씬 어두운 바지 · 신발
        val px = IntArray(20) { red } + IntArray(5) { gray } + IntArray(5) { darkRed }
        AvatarRecolor.recolorOutfitPixels(px, targetHue = 0.36f, targetSaturation = 0.55f)
        val top = px[0]
        assertTrue("윗옷이 초록이 된다", ((top shr 8) and 0xFF) > ((top shr 16) and 0xFF))
        assertEquals("무채색(흰 티 · 윤곽)은 그대로", gray, px[20])
        assertEquals("밝기가 크게 다른 같은 계열은 그대로", darkRed, px[25])
    }

    // ---- 확장 기능: 회상 · 계기 카드 · 칭호 목표 · NPC (신기훈 #76 실제 콘텐츠) ----

    private val extensions = ScenarioExtensions.parse(asset("scenario_extensions.sample.json"))
    private val periods = V4ContentParser.parsePeriods(asset("periods.sample.json"))

    private fun record(eventId: String, choiceId: String, label: String, period: LifePeriod = LifePeriod.CHILD, title: String = eventId) =
        RunChoiceRecord(eventId, choiceId, "${choiceId.lowercase()}_primary", period, 1, title, label, "결과", null)

    @Test
    fun `extension content is read`() {
        assertTrue(extensions.recallSlots.size >= 10)
        assertTrue(extensions.titleGoals.size >= 40)
        assertTrue(extensions.npcs.isNotEmpty())
        assertTrue(extensions.turningPoints.values.flatten().size >= 10)
    }

    @Test
    fun `recall appears only with the real choice and only once per life`() {
        val slot = extensions.recallSlots.first { it.slotId == "recall_dol_music" }
        val shownIn = slot.showIn.filter { it != ScenarioExtensions.ENDING && !it.startsWith("auto_") }
        val flags = slot.requiredFlags
        assertNull("기록이 없으면 만들지 않는다", extensions.recallForEvent(shownIn.first(), emptyList(), flags))
        val other = record(slot.sourceEventId, "Z", "다른 선택")
        assertNull("다른 선택이면 만들지 않는다", extensions.recallForEvent(shownIn.first(), listOf(other), flags))
        val dol = record(slot.sourceEventId, slot.sourceChoiceIds.first(), "피아노 장난감을 잡는다")
        assertEquals(slot.line, extensions.recallForEvent(shownIn.first(), listOf(dol), flags)?.text)
        if (shownIn.size > 1) {
            val passed = record(shownIn.first(), "A", "지나온 사건")
            assertNull("같은 회상은 사건에서 한 번만", extensions.recallForEvent(shownIn[1], listOf(dol, passed), flags))
        }
        val ending = extensions.recallsForEnding(listOf(dol), flags)
        assertEquals(listOf("recall_dol_music"), ending.map { it.slotId })
    }

    @Test
    fun `turning point cards use the actual choice label and stop at three`() {
        val card = extensions.turningPoints.getValue("EDUCATION").first { it.cardId == "tp_edu_high_school" }
        val chose = record(card.eventId, "A", "인문계.", LifePeriod.TEEN, "고등학교 원서")
        val cards = extensions.turningPointsFor(listOf(chose), card.requiredFlags, periods)
        assertEquals(listOf("tp_edu_high_school"), cards.map { it.cardId })
        assertTrue(cards.single().text, cards.single().text.contains("'인문계'"))
        assertEquals("청소년기", cards.single().periodName)
        assertTrue("기록이 없으면 카드를 만들지 않는다", extensions.turningPointsFor(emptyList(), card.requiredFlags, periods).isEmpty())
        val all = extensions.turningPoints.values.flatten()
        val many = all.map { record(it.eventId, "A", "고른 길") }
        val picked = extensions.turningPointsFor(many, all.flatMap { it.requiredFlags }.toSet(), periods)
        assertTrue("2~3장: ${picked.size}", picked.size in 2..3)
    }

    @Test
    fun `returning person needs the relation flag and the first meeting`() {
        val npc = extensions.npcs.first { it.npcId == "npc_transfer_child" }
        val again = npc.appearances.first { it.type == "EVENT" }
        val met = record(npc.originEventIds.first(), npc.originChoiceIds.firstOrNull() ?: "A", "같이 먹는다", LifePeriod.CHILD, "혼자 밥 먹는 전학생")
        val flags = npc.relationFlags + again.requiredFlags
        assertNull("관계 플래그가 없으면 없다", extensions.npcForEvent(again.id, listOf(met), emptySet(), periods))
        assertNull("처음 만난 기록이 없으면 없다", extensions.npcForEvent(again.id, emptyList(), flags, periods))
        val notice = requireNotNull(extensions.npcForEvent(again.id, listOf(met), flags, periods))
        assertEquals("혼자 밥 먹는 전학생", notice.originTitle)
        val metAgain = extensions.npcsMetAgain(listOf(met, record(again.id, "A", "반갑다", LifePeriod.YOUNG_ADULT, "다시 만남")), flags, periods)
        assertEquals(listOf("다시 만남"), metAgain.single { it.npcId == npc.npcId }.metAgainTitles)
    }

    // ---- 과제 4: 결과 화면의 선택 당시 시기 · 사실 변화 ----

    @Test
    fun `result screen keeps the period of the choice after the calendar moved on`() {
        val defs = listOf(
            PeriodDefinition(LifePeriod.CHILD, order = 1, years = 5, displayName = "유년기"),
            PeriodDefinition(LifePeriod.TEEN, order = 2, years = 5, displayName = "청소년기"),
        )
        val moved = PeriodRunState(runId = "run", runSeed = 1L, period = LifePeriod.TEEN, yearInPeriod = 1)
        val decision = DecisionUi("child_008", com.example.lifegame.domain.model.DecisionKind.INCIDENT, "첫 용돈", "본문", "", null, emptyList())
        val ui = buildPeriodPlayState(
            step = TurnStep(state = moved, phase = TurnPhase.AUTO_PROGRESS),
            content = PlayContent(periods = defs, focuses = emptyList(), decisions = emptyMap(), imageFiles = emptyMap()),
            playerName = "재겸",
            outcome = OutcomeUi("고른 길", "결과", emptyList(), endsLife = false),
            decision = decision,
            decisionYear = 5,
            outcomePeriod = LifePeriod.CHILD,
        )
        assertEquals("유년기", ui.periodName)
        assertEquals(5, ui.yearInPeriod)
        assertEquals(LifePeriod.CHILD, ui.imagePeriod)
        assertEquals(LifePeriod.CHILD, ui.avatar.period)
    }

    @Test
    fun `restored result recomputes new facts from the flags before the choice`() {
        val catalog = LifeFactCatalog(parseLifeFacts(asset("life_facts.sample.json")))
        val newFlag = "vocational_high"
        assertNotNull("특성화고 플래그가 생애 사실 카탈로그에 있어야 한다", catalog.entry(newFlag))
        val before = PeriodRunState(runId = "run", runSeed = 1L, period = LifePeriod.TEEN, flags = setOf("gender_female"))
        val after = before.copy(flags = before.flags + newFlag)
        val live = factChanges(buildLifeFacts(before, catalog), buildLifeFacts(after, catalog))
        assertTrue("선택으로 새 사실이 생기는 예: $newFlag", live.isNotEmpty())
        val snapshot = SavedOutcome(
            "teen_019", "B", "b_primary", "특성화고", "결과", before.stats, after.stats,
            before.lifeFacts, after.lifeFacts, false, beforeFlags = before.flags,
        )
        assertEquals("이어하기 뒤에도 같은 사실 변화", live, restoredFactChanges(after, snapshot, catalog))
        assertTrue(
            "선택 전 플래그가 없던 옛 저장은 예전처럼 계산한다(변화가 사라질 수 있음)",
            restoredFactChanges(after, snapshot.copy(beforeFlags = null), catalog).size <= live.size,
        )
    }

    // ---- 저장 형식: 새 칸은 왕복되고 옛 저장은 기본값으로 읽힌다 ----

    @Test
    fun `avatar colors and before flags survive the save and old saves read defaults`() {
        val state = PeriodRunState(runId = "run", runSeed = 7L, period = LifePeriod.TEEN, flags = setOf("a", "b"))
        val outcome = SavedOutcome("e", "A", "a1", "고른 길", "결과", Stats(), Stats(), LifeFacts(), LifeFacts(), false, beforeFlags = setOf("a"))
        val run = V4SavedRun(
            playerName = "재겸",
            state = state,
            pendingOutcome = outcome,
            avatar = AvatarConfig(hairColorId = "hair_color_blond", outfitColorId = "outfit_green"),
        )
        val restored = requireNotNull(Week10ProgressCodec.decode(Week10ProgressCodec.encode(run)))
        assertEquals("hair_color_blond", restored.avatar.hairColorId)
        assertEquals("outfit_green", restored.avatar.outfitColorId)
        assertEquals(setOf("a"), restored.pendingOutcome?.beforeFlags)

        val legacy = JSONObject(Week10ProgressCodec.encode(run))
        legacy.getJSONObject("avatar").apply { remove("hairColorId"); remove("outfitColorId") }
        legacy.getJSONObject("pendingOutcome").remove("beforeFlags")
        val old = requireNotNull(Week10ProgressCodec.decode(legacy.toString()))
        assertEquals("hair_color_natural", old.avatar.hairColorId)
        assertEquals("outfit_original", old.avatar.outfitColorId)
        assertNull(old.pendingOutcome?.beforeFlags)
    }

    // ---- 과제 5: 돌잡이 컷신 재생 조건 ----

    @Test
    fun `doljabi cutscene plays only before the first decision until it is finished`() {
        assertTrue(shouldPlayDoljabiCutscene(DOLJABI_EVENT_ID, hasOutcome = false, status = CutsceneStatus.NOT_STARTED))
        assertTrue("재생 중 앱이 꺼졌으면 처음부터 다시", shouldPlayDoljabiCutscene(DOLJABI_EVENT_ID, false, CutsceneStatus.PLAYING))
        assertFalse(shouldPlayDoljabiCutscene(DOLJABI_EVENT_ID, false, CutsceneStatus.COMPLETED))
        assertFalse(shouldPlayDoljabiCutscene(DOLJABI_EVENT_ID, false, CutsceneStatus.SKIPPED))
        assertFalse("결과를 보는 중이면 재생하지 않는다", shouldPlayDoljabiCutscene(DOLJABI_EVENT_ID, true, CutsceneStatus.NOT_STARTED))
        assertFalse("다른 사건 앞에서는 재생하지 않는다", shouldPlayDoljabiCutscene("child_008", false, CutsceneStatus.NOT_STARTED))
        assertTrue(doljabiPreviewAlreadyShown(DOLJABI_EVENT_ID, CutsceneStatus.SKIPPED))
        assertFalse(doljabiPreviewAlreadyShown("child_008", CutsceneStatus.SKIPPED))
        assertEquals("오늘은 재겸의 첫 생일이다.", doljabiCaption("재겸"))
        assertEquals("오늘은 첫 생일이다.", doljabiCaption(""))
    }
}
