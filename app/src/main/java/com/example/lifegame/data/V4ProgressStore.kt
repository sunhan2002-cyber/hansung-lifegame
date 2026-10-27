package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.TurnPhase

data class V4SavedRun(
    val playerName: String,
    val state: PeriodRunState,
    val choices: List<RunChoiceRecord> = emptyList(),
    val pendingOutcome: SavedOutcome? = null,
    val avatar: AvatarConfig = AvatarConfig(),
    val awardedTitle: AwardedTitle? = null,
    val phase: TurnPhase? = null,
)

sealed interface V4LoadResult {
    data object Empty : V4LoadResult
    data class Loaded(val run: V4SavedRun) : V4LoadResult
    data object Invalid : V4LoadResult
}

class V4ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
    fun hasSave(): Boolean = prefs.contains(KEY)
    fun save(run: V4SavedRun) = prefs.edit().putString(KEY, Week10ProgressCodec.encode(run)).apply()
    fun loadResult(): V4LoadResult {
        val raw = prefs.getString(KEY, null) ?: return V4LoadResult.Empty
        return Week10ProgressCodec.decode(raw)?.let(V4LoadResult::Loaded) ?: V4LoadResult.Invalid
    }

    fun load(): V4SavedRun? = (loadResult() as? V4LoadResult.Loaded)?.run
    fun clear() = prefs.edit().remove(KEY).apply()

    fun loadCompletedLives(): List<CompletedLifeRecord> = prefs.getString(COMPLETED_KEY, null)
        ?.let(Week10ProgressCodec::decodeCompletedLives)
        .orEmpty()

    /** runId 기준 upsert라 종료 연출 재시도·프로세스 복원으로 같은 인생이 중복되지 않는다. */
    fun saveCompletedLife(record: CompletedLifeRecord) {
        val updated = listOf(record) + loadCompletedLives().filterNot { it.runId == record.runId }
        prefs.edit().putString(COMPLETED_KEY, Week10ProgressCodec.encodeCompletedLives(updated)).apply()
    }

    private companion object {
        const val KEY = "savedRun"
        const val COMPLETED_KEY = "completedLives"
    }
}
