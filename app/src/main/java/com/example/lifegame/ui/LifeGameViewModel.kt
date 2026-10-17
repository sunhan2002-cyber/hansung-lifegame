package com.example.lifegame.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ProgressStore
import com.example.lifegame.data.SavedGame
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.domain.engine.GameSession
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep

class LifeGameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(application)
    private val progressStore = ProgressStore(application)
    private val v4ProgressStore = V4ProgressStore(application)
    private var session: GameSession? = null

    var uiState by mutableStateOf(LifeGameUiState(hasSaveData = progressStore.hasSaveData()))
        private set

    fun startNewGame(characterId: Int, playerName: String) {
        val name = normalizePlayerName(playerName)
        val nameCall = nameCallOf(name)
        // 세션에 넘기기 전에 {name}, {nameCall}을 바꿔 두면 사건 · 선택지 · 결과 · 기록 · 결말 어디에도 남지 않는다.
        val events = repository.loadEvents().map { it.withPlayerName(name, nameCall) }
        val endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) }
        if (events.isEmpty() || endings.none { it.isDefault }) {
            session = null
            uiState = LifeGameUiState(
                characterId = characterId,
                playerName = name,
                nameCall = nameCall,
                contentError = "사건 또는 기본 결말 데이터를 불러오지 못했습니다.",
            )
            return
        }

        session = GameSession(events = events, endings = endings)
        val started = session!!.start()
        uiState = LifeGameUiState(
            characterId = characterId,
            hasSaveData = true,
            playerName = name,
            nameCall = nameCall,
            progress = started.progress,
            currentEvent = started.currentEvent,
            ending = started.ending,
            eventsById = events.associateBy { it.eventId },
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
        persistSession()
    }

    fun continueGame(): GameScreenState? {
        val savedGame = progressStore.load() ?: return clearInvalidSave()
        val name = normalizePlayerName(savedGame.playerName)
        val nameCall = nameCallOf(name)
        val events = repository.loadEvents().map { it.withPlayerName(name, nameCall) }
        val endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) }
        val restoredSession = runCatching {
            GameSession(events = events, endings = endings).also {
                it.restore(savedGame.progress)
            }
        }.getOrElse { return clearInvalidSave() }

        session = restoredSession
        uiState = LifeGameUiState(
            characterId = savedGame.characterId,
            playerName = name,
            nameCall = nameCall,
            hasSaveData = true,
            eventsById = events.associateBy { it.eventId },
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
        syncFromSession()
        return uiState.progress?.screenState
    }

    fun choose(choice: Choice): Boolean {
        val event = uiState.currentEvent ?: return false
        val result = session?.choose(choice) ?: return false
        uiState = uiState.copy(
            progress = result.progress,
            lastResult = UiChoiceResult(event.eventId, choice, result),
        )
        persistSession()
        return true
    }

    fun proceed(): Boolean {
        val hasNextEvent = session?.proceed() ?: return false
        syncFromSession()
        persistSession()
        return !hasNextEvent
    }

    fun finish() {
        session?.finish()
        syncFromSession()
        persistSession()
    }

    fun reset() {
        session = null
        progressStore.clear()
        uiState = LifeGameUiState(hasSaveData = false)
    }

    // ---- v4: 홈 → 장 표지(초점) → 자동 진행 → 결정 · 돌발 ----
    // v4 진행 상태는 로컬 V4ProgressStore에 저장해 앱을 껐다 켜도 이어갈 수 있다.

    private var playSession: PeriodGameSession? = null
    private var playContent: PlayContent? = null
    private var playStep: TurnStep? = null
    private var playerNameForPlay: String = DEFAULT_PLAYER_NAME
    private var characterImageIdForPlay: String? = null
    private var shownDecision: DecisionUi? = null
    private var decisionYear: Int? = null
    private var outcome: OutcomeUi? = null
    private var autoPlaying = true
    private var speed = PLAY_SPEEDS.first()
    private var died = false

    var play by mutableStateOf<PeriodPlayUiState?>(null)
        private set

    var playError by mutableStateOf<String?>(null)
        private set

    var finishedLives by mutableStateOf<List<FinishedLifeUi>>(emptyList())
        private set

    /** 프로필 만들기의 타고난 기질. characters.sample.json에서 읽는다. */
    val starters: List<StarterUi> by lazy {
        runCatching {
            getApplication<Application>().assets.open(CHARACTERS_FILE).bufferedReader().use { it.readText() }
        }.map(::parseStarters).getOrElse { emptyList() }
    }

    /** 홈 화면에 보여 줄 진행 중인 인생. 끝났거나 없으면 null. */
    val lifeCard: LifeCardUi?
        get() = play?.takeIf { it.phase != PlayPhase.ENDED }?.let { current ->
            LifeCardUi(
                playerName = current.playerName,
                place = if (current.phase == PlayPhase.CHAPTER) {
                    "${current.periodName} · 초점 고르는 중"
                } else {
                    current.place
                },
                focusLabel = current.focus?.label,
                stats = current.stats,
                lastLine = current.log.lastOrNull()?.text,
            )
        } ?: v4ProgressStore.load()?.let { saved ->
            val content = repository.loadV4Content()
            val period = content.periods.firstOrNull { it.period == saved.state.period }
            val focus = content.focuses.firstOrNull { it.focusId == saved.state.selectedFocusId }
            LifeCardUi(
                playerName = saved.playerName,
                place = if (saved.state.selectedFocusId == null) {
                    "${period?.displayName ?: saved.state.period.name} · 초점 고르는 중"
                } else {
                    "${period?.displayName ?: saved.state.period.name} · ${saved.state.yearInPeriod}번째 해"
                },
                focusLabel = focus?.label,
                stats = saved.state.stats,
                lastLine = saved.state.moments.lastOrNull()?.text,
            )
        }

    fun startNewLife(profile: NewLifeProfile) {
        val name = normalizePlayerName(profile.name)
        val nameCall = nameCallOf(name)
        val content = repository.loadV4Content()
        if (content.periods.isEmpty() || content.focuses.isEmpty()) {
            clearPlay()
            playError = "시기 또는 초점 데이터를 불러오지 못했습니다."
            return
        }
        // 엔진에 넘기기 전에 이름을 바꿔 두면 결정 · 결과 · 기록 문장 어디에도 {name}이 남지 않는다.
        val focuses = content.focuses.map { it.withPlayerName(name, nameCall) }
        val decisions = content.decisions.map { it.withPlayerName(name, nameCall) }
        val newSession = PeriodGameSession(
            periods = content.periods,
            focuses = focuses,
            decisions = decisions,
            endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) },
            lifeFactRules = content.lifeFactRules,
            engine = com.example.lifegame.domain.engine.GameEngine(
                skipInfantFocus = true,
                guaranteeYearlyIncident = true,
            ),
        )
        val starter = starters.firstOrNull { it.starterId == profile.starterId }
        val step = runCatching {
            val first = newSession.start().state
            // 타고난 기질과 성별은 시작 능력치 · 플래그로 넣는다. 남자는 청년기에 입영 통지서를 받는다.
            val startFlags = buildSet {
                addAll(starter?.startFlags.orEmpty())
                add(profile.gender.flag)
                if (profile.gender == Gender.MALE) add(MILITARY_SERVICE_FLAG)
            }
            newSession.restore(
                first.copy(
                    stats = starter?.let { first.stats.plusClamped(it.statDelta) } ?: first.stats,
                    flags = first.flags + startFlags,
                ),
            )
        }.getOrElse {
            clearPlay()
            playError = "새 인생을 시작하지 못했습니다."
            return
        }

        playSession = newSession
        playContent = PlayContent(
            periods = content.periods,
            focuses = focuses,
            decisions = decisions.associateBy { it.eventId },
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
        playerNameForPlay = name
        characterImageIdForPlay = starter?.imageId
        shownDecision = null
        decisionYear = null
        outcome = null
        autoPlaying = true
        speed = PLAY_SPEEDS.first()
        died = false
        playError = null
        v4ProgressStore.clear()
        applyStep(step)
    }

    fun continueLife() {
        val saved = v4ProgressStore.load() ?: run {
            playError = "저장된 인생이 없습니다. 새 인생을 시작해 주세요."
            return
        }
        val content = repository.loadV4Content()
        val name = normalizePlayerName(saved.playerName)
        val nameCall = nameCallOf(name)
        val focuses = content.focuses.map { it.withPlayerName(name, nameCall) }
        val decisions = content.decisions.map { it.withPlayerName(name, nameCall) }
        playContent = PlayContent(content.periods, focuses, decisions.associateBy { it.eventId }, repository.loadImageAssets().filter { it.fileName.isNotBlank() }.associate { it.imageId to it.fileName })
        playSession = PeriodGameSession(
            content.periods,
            focuses,
            decisions,
            engine = com.example.lifegame.domain.engine.GameEngine(
                skipInfantFocus = true,
                guaranteeYearlyIncident = true,
            ),
        )
        playerNameForPlay = name
        characterImageIdForPlay = saved.state.flags.firstNotNullOfOrNull { flag ->
            when (flag) {
                "starter_loud" -> "img_baby_loud_001"
                "starter_curious" -> "img_baby_curious_001"
                "starter_quirky" -> "img_baby_quirky_001"
                else -> null
            }
        }
        shownDecision = null; outcome = null; decisionYear = null; died = saved.state.isDead; playError = null
        applyStep(playSession!!.restoreSaved(saved.state))
    }

    fun selectFocus(focusId: String) {
        val current = playSession ?: return
        runStep { current.selectFocus(focusId) }
    }

    /** 자동 진행 타이머가 한 해마다 부른다. 결정이 걸리면 엔진이 멈춘 상태를 돌려준다. */
    fun advanceYear() {
        if (play?.phase != PlayPhase.AUTO) return
        val current = playSession ?: return
        runStep { current.advance() }
    }

    /** "다음 결정까지": 결정 · 장 표지 · 끝이 나올 때까지 해를 한 번에 넘긴다. */
    fun skipToNextDecision() {
        if (play?.phase != PlayPhase.AUTO) return
        val current = playSession ?: return
        runStep {
            var step = current.advance()
            var guard = 0
            while (step.phase == TurnPhase.AUTO_PROGRESS && guard < MAX_SKIP_YEARS) {
                step = current.advance()
                guard += 1
            }
            step
        }
    }

    fun chooseDecision(choiceId: String) {
        val current = playSession ?: return
        val decision = play?.decision ?: return
        if (outcome != null) return
        val before = playStep?.state?.stats ?: return
        val label = decision.choices.firstOrNull { it.choiceId == choiceId }?.label ?: return
        val resolution = runCatching { current.choose(choiceId) }.getOrElse {
            playError = "선택을 반영하지 못했습니다."
            return
        }
        shownDecision = decision
        died = died || resolution.step.state.isDead
        outcome = OutcomeUi(
            choiceLabel = label,
            text = resolution.outcome.resultText,
            changes = statChanges(before, resolution.step.state.stats),
            endsLife = resolution.step.state.isDead,
        )
        playStep = resolution.step
        publishPlay()
    }

    /** 결과를 읽고 나서 다음 해 · 장 표지 · 끝으로 넘어간다. */
    fun continueAfterOutcome() {
        if (outcome == null) return
        outcome = null
        shownDecision = null
        decisionYear = null
        publishPlay()
    }

    fun togglePlaying() {
        autoPlaying = !autoPlaying
        publishPlay()
    }

    fun setSpeed(value: Int) {
        if (value !in PLAY_SPEEDS) return
        speed = value
        publishPlay()
    }

    /** 끝난 인생을 "지난 인생"에 남기고 홈으로 돌아간다. */
    fun finishLife() {
        val ended = play?.takeIf { it.phase == PlayPhase.ENDED } ?: return
        val summary = if (ended.isDead) {
            "${ended.periodName}에 생을 마쳤다"
        } else {
            "${ended.periodName}까지 살았다"
        }
        finishedLives = listOf(FinishedLifeUi(ended.runKey, ended.playerName, summary)) + finishedLives
        clearPlay()
        v4ProgressStore.clear()
    }

    private inline fun runStep(block: () -> TurnStep) {
        val step = runCatching(block).getOrElse {
            playError = "다음 해를 불러오지 못했습니다."
            return
        }
        applyStep(step)
    }

    private fun applyStep(step: TurnStep) {
        playStep = step
        died = died || step.state.isDead
        decisionYear = if (step.phase == TurnPhase.DECISION) step.state.yearInPeriod else null
        publishPlay()
    }

    private fun publishPlay() {
        val step = playStep ?: return
        val content = playContent ?: return
        play = buildPeriodPlayState(
            step = step,
            content = content,
            playerName = playerNameForPlay,
            outcome = outcome,
            decision = shownDecision.takeIf { outcome != null },
            decisionYear = decisionYear,
            autoPlaying = autoPlaying,
            speed = speed,
            isDead = died,
            characterImageId = characterImageIdForPlay,
        )
        v4ProgressStore.save(V4SavedRun(playerNameForPlay, step.state))
    }

    private fun clearPlay() {
        playSession = null
        playContent = null
        playStep = null
        shownDecision = null
        decisionYear = null
        outcome = null
        died = false
        play = null
    }

    private fun syncFromSession() {
        val sessionState = session?.state ?: return
        uiState = uiState.copy(
            progress = sessionState.progress,
            currentEvent = sessionState.currentEvent,
            lastResult = sessionState.lastResult?.let { result ->
                UiChoiceResult(
                    eventId = result.progress.choiceHistory.lastOrNull()?.eventId.orEmpty(),
                    choice = requireNotNull(sessionState.selectedChoice),
                    engineResult = result,
                )
            },
            ending = sessionState.ending,
        )
    }

    private fun persistSession() {
        val characterId = uiState.characterId ?: return
        val progress = session?.state?.progress ?: return
        progressStore.save(
            SavedGame(
                characterId = characterId,
                playerName = uiState.playerName,
                progress = progress,
            ),
        )
        uiState = uiState.copy(hasSaveData = true)
    }

    private fun clearInvalidSave(): GameScreenState? {
        progressStore.clear()
        session = null
        uiState = LifeGameUiState(
            hasSaveData = false,
            contentError = "저장된 진행 상태를 복원하지 못했습니다. 새 게임을 시작해 주세요.",
        )
        return null
    }

    private companion object {
        const val CHARACTERS_FILE = "characters.sample.json"
        const val MAX_SKIP_YEARS = 200
    }
}
