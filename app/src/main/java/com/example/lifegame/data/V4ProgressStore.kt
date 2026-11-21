package com.example.lifegame.data

import android.content.Context
import android.content.SharedPreferences
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.RunNarrativeRecord
import com.example.lifegame.domain.model.RunExtensionState
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.TurnPhase
import java.util.UUID
import java.util.concurrent.CompletableFuture

data class V4SavedRun(
    val playerName: String,
    val state: PeriodRunState,
    val choices: List<RunChoiceRecord> = emptyList(),
    val pendingOutcome: SavedOutcome? = null,
    val avatar: AvatarConfig = AvatarConfig(),
    val awardedTitle: AwardedTitle? = null,
    val phase: TurnPhase? = null,
    val presentation: PresentationState = PresentationState(),
    val narrativeRecords: List<RunNarrativeRecord> = emptyList(),
    /** 시작 주사위의 원본 두 눈. null이면 v1~v4 저장으로 간주한다. */
    val startingRollRaw: String? = null,
    val startingDiceRule: String? = null,
    val rerollsRemaining: Int = 0,
    val extensionState: RunExtensionState = RunExtensionState(),
)

sealed interface V4LoadResult {
    data object Empty : V4LoadResult
    data class Loaded(val run: V4SavedRun) : V4LoadResult
    data object Invalid : V4LoadResult
}

class V4ProgressStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE))

    private val persistence = ProgressPersistence(commitLatest = { prefs.edit().commit() })
    val saveState get() = persistence.state
    private var knownValidRaw: String? = null
    private var failedEdit: EditPlan? = null

    fun hasSave(): Boolean = prefs.contains(KEY)

    /** Immediate readable checkpoint; saveState/awaitDurable distinguish it from disk completion. */
    fun save(run: V4SavedRun, completed: CompletedLifeRecord? = null): Boolean = synchronized(prefs) {
        val raw = Week10ProgressCodec.encode(run)
        val editor = EditPlan()
        preserveInvalidCurrent(editor)
        completed?.let { appendCompletedLife(editor, it) }
        editor.putString(KEY, raw)
        publish(editor).also { if (it) knownValidRaw = raw }
    }

    fun loadResult(): V4LoadResult {
        val value = currentRaw() ?: return V4LoadResult.Empty
        val raw = value as? String ?: return V4LoadResult.Invalid
        return Week10ProgressCodec.decode(raw)?.let {
            knownValidRaw = raw
            V4LoadResult.Loaded(it)
        } ?: V4LoadResult.Invalid
    }

    fun load(): V4SavedRun? = (loadResult() as? V4LoadResult.Loaded)?.run

    /** Explicit clear preserves an unreadable/future slot instead of destroying its source bytes. */
    fun clear(): Boolean = synchronized(prefs) {
        val editor = EditPlan()
        preserveInvalidCurrent(editor)
        knownValidRaw = null
        publish(editor.remove(KEY))
    }

    /** The archive update and active-slot removal become one atomic SharedPreferences edit. */
    fun finish(record: CompletedLifeRecord): Boolean = synchronized(prefs) {
        val editor = EditPlan()
        preserveInvalidCurrent(editor)
        appendCompletedLife(editor, record)
        knownValidRaw = null
        publish(editor.remove(KEY))
    }

    /** Caller may await on a worker/test thread; never block the Android main thread on get(). */
    fun awaitDurable(): CompletableFuture<Boolean> = synchronized(prefs) {
        // Navigation must not implicitly dismiss an observed failure. Retry is a distinct action.
        if (saveState.value is SaveState.Failed) CompletableFuture.completedFuture(false)
        else persistence.confirmChanges()
    }
    fun retrySave() = synchronized(prefs) {
        val edit = failedEdit
        if (edit != null) publish(edit) else { persistence.confirmChanges(); true }
    }

    fun loadCompletedLives(): List<CompletedLifeRecord> =
        (readRecords(PENDING_COMPLETED_KEY) + readRecords(COMPLETED_KEY)).distinctBy { it.runId }

    private fun readRecords(key: String): List<CompletedLifeRecord> = (rawPreference(key) as? String)
        ?.let(Week10ProgressCodec::decodeCompletedLives).orEmpty()

    private fun readArchive(raw: Any?): Week10ProgressCodec.CompletedLivesRead = when (raw) {
        null -> Week10ProgressCodec.CompletedLivesRead()
        is String -> Week10ProgressCodec.readCompletedLives(raw)
        else -> Week10ProgressCodec.CompletedLivesRead(writable = false)
    }

    /** true means accepted in memory; disk success is exposed separately in saveState. */
    fun saveCompletedLife(record: CompletedLifeRecord): Boolean = synchronized(prefs) {
        val editor = EditPlan()
        appendCompletedLife(editor, record)
        publish(editor)
    }

    /** runId upsert, future archives, and partially damaged records retain their original contract. */
    private fun appendCompletedLife(editor: EditPlan, record: CompletedLifeRecord) {
        val raw = rawPreference(COMPLETED_KEY)
        val read = readArchive(raw)
        val pendingRaw = rawPreference(PENDING_COMPLETED_KEY)
        val pendingRead = readArchive(pendingRaw)
        if (pendingRaw != null && (!pendingRead.writable || pendingRead.damagedEntries > 0)) {
            editor.putValue("completedLivesRecovery_${System.currentTimeMillis()}_${UUID.randomUUID()}", pendingRaw)
        }
        // Preserve unreadable/future data intact and put new lives in a separate readable slot.
        if (!read.writable) {
            val pending = listOf(record) + pendingRead.records.filterNot { it.runId == record.runId }
            editor.putString(PENDING_COMPLETED_KEY, Week10ProgressCodec.encodeCompletedLives(pending))
            return
        }
        val updated = listOf(record) + (pendingRead.records + read.records).distinctBy { it.runId }
            .filterNot { it.runId == record.runId }
        if (read.damagedEntries > 0 && raw != null) {
            editor.putValue("completedLivesRecovery_${System.currentTimeMillis()}_${UUID.randomUUID()}", raw)
        }
        editor.remove(PENDING_COMPLETED_KEY).putString(COMPLETED_KEY, Week10ProgressCodec.encodeCompletedLives(updated))
    }

    private fun preserveInvalidCurrent(editor: EditPlan) {
        val raw = rawPreference(KEY) ?: return
        if (raw !is String || (raw !== knownValidRaw && Week10ProgressCodec.decode(raw) == null)) {
            editor.putValue("${RECOVERY_PREFIX}${System.currentTimeMillis()}_${UUID.randomUUID()}", raw)
        }
    }

    private fun currentRaw(): Any? = rawPreference(KEY)

    private fun rawPreference(key: String): Any? = try {
        prefs.getString(key, null)
    } catch (_: ClassCastException) {
        prefs.all[key]
    }

    private fun publish(editor: EditPlan): Boolean = try {
        editor.applyTo(prefs)
        failedEdit = null
        persistence.confirmChanges()
        true
    } catch (_: Exception) {
        // Retain the exact, idempotent set/remove operations, including recovery copies. A retry
        // republishes those bytes, never invokes an engine choice or recalculates its rewards.
        failedEdit = editor
        persistence.reportFailure()
        false
    }

    private class EditPlan {
        private val changes = linkedMapOf<String, Any?>()
        fun putString(key: String, value: String): EditPlan = apply { changes[key] = value }
        fun putValue(key: String, value: Any): EditPlan = apply {
            changes[key] = if (value is Set<*>) value.toSet() else value
        }
        fun remove(key: String): EditPlan = apply { changes[key] = null }
        fun applyTo(preferences: SharedPreferences) {
            val target = preferences.edit()
            changes.forEach { (key, value) ->
                when (value) {
                    null -> target.remove(key)
                    is String -> target.putString(key, value)
                    is Int -> target.putInt(key, value)
                    is Long -> target.putLong(key, value)
                    is Float -> target.putFloat(key, value)
                    is Boolean -> target.putBoolean(key, value)
                    is Set<*> -> target.putStringSet(key, value.map { it as String }.toSet())
                    else -> error("Unsupported preference value")
                }
            }
            target.apply()
        }
    }

    private companion object {
        const val KEY = "savedRun"
        const val COMPLETED_KEY = "completedLives"
        const val PENDING_COMPLETED_KEY = "completedLivesPending"
        const val RECOVERY_PREFIX = "savedRunRecovery_"
    }
}
