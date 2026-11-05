package com.example.lifegame.ui

import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import org.json.JSONObject

/**
 * 선택과 별개로 해마다 흘러가는 일상 이야기 (2026-11-05 피드백 4번, yearly_stories.sample.json).
 * 능력치 · 플래그를 바꾸지 않는 연출 문장이라 엔진 상태와 저장 형식은 그대로다.
 * 같은 인생(runSeed)에서는 다시 열어도 같은 문장이 나온다.
 */
class YearlyStoryBook(private val texts: Map<Pair<LifePeriod, Int>, List<String>>) {

    fun lineFor(runSeed: Long, period: LifePeriod, year: Int): String? {
        val options = texts[period to year].orEmpty()
        if (options.isEmpty()) return null
        return options[Math.floorMod("$runSeed:${period.name}:$year:story".hashCode(), options.size)]
    }

    val isEmpty: Boolean get() = texts.isEmpty()

    companion object {
        val EMPTY = YearlyStoryBook(emptyMap())

        fun parse(json: String): YearlyStoryBook {
            val stories = JSONObject(json).getJSONArray("stories")
            val map = linkedMapOf<Pair<LifePeriod, Int>, List<String>>()
            for (i in 0 until stories.length()) {
                val item = stories.getJSONObject(i)
                val period = runCatching { LifePeriod.valueOf(item.getString("period")) }.getOrNull() ?: continue
                val year = item.optInt("year", 0).takeIf { it > 0 } ?: continue
                val array = item.optJSONArray("texts") ?: continue
                val lines = (0 until array.length()).map { array.getString(it).trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) map[period to year] = lines
            }
            return YearlyStoryBook(map)
        }
    }
}

/** 엔진이 영아기 · 노년기에 해마다 남기는 틀 문장. 이야기 문장이 있는 해에는 이것 대신 이야기를 보여 준다. */
private val FILLER_RECORD = Regex("""^(영아기|노년기) \d+번째 해, (세상을 처음 배웠다|지나온 선택을 돌아보았다)\.$""")

/**
 * 기록 줄을 시간 순서(오래된 것 → 최신)로 만든다. 화면은 최신을 위에 보이도록 뒤집어 쓴다.
 * 이야기가 있으면 그 해의 첫 줄로 끼워 넣고, 같은 해의 틀 문장은 뺀다.
 */
fun buildLogLines(
    moments: List<LifeMoment>,
    periods: List<PeriodDefinition>,
    stories: YearlyStoryBook = YearlyStoryBook.EMPTY,
    runSeed: Long = 0L,
): List<LogLineUi> {
    val names = periods.associate { it.period to it.displayName }
    fun label(period: LifePeriod, year: Int) = "${names[period] ?: period.name} · ${year}번째 해"
    if (stories.isEmpty) {
        return moments.mapIndexed { index, moment ->
            LogLineUi(key = "$index", label = label(moment.period, moment.yearInPeriod), text = moment.text)
        }
    }
    val lines = mutableListOf<LogLineUi>()
    var lastYear: Pair<LifePeriod, Int>? = null
    moments.forEachIndexed { index, moment ->
        val year = moment.period to moment.yearInPeriod
        val story = stories.lineFor(runSeed, moment.period, moment.yearInPeriod)
        if (year != lastYear) {
            story?.let { lines += LogLineUi(key = "story-${moment.period.name}-${moment.yearInPeriod}", label = label(moment.period, moment.yearInPeriod), text = it) }
            lastYear = year
        }
        if (story != null && FILLER_RECORD.matches(moment.text)) return@forEachIndexed
        lines += LogLineUi(key = "$index", label = label(moment.period, moment.yearInPeriod), text = moment.text)
    }
    return lines
}
