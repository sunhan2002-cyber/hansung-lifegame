package com.example.lifegame.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ProgressStore
import com.example.lifegame.data.SavedGame
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.data.V4LoadResult
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.GameSession
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import com.example.lifegame.domain.engine.bodyTypeFor
import com.example.lifegame.domain.generation.FaceSheetRequest
import com.example.lifegame.domain.generation.FakeGenerationClient
import com.example.lifegame.domain.generation.FakeGenerationMode
import com.example.lifegame.domain.generation.FutureLookRequest
import com.example.lifegame.domain.generation.GenerationResult
import com.example.lifegame.domain.generation.IMAGE_GENERATION_TIMEOUT_MS
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.avatar.avatarLookOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    /** 고른 선택지 기록. 결말 화면의 "주요 선택"에 쓴다. */
    private val choiceLog = mutableListOf<ChoiceRecordUi>()
    private val runChoiceHistory = mutableListOf<RunChoiceRecord>()
    private var pendingOutcomeSnapshot: SavedOutcome? = null
    private var avatarForPlay = AvatarConfig()
    private var awardedTitleForRun: AwardedTitle? = null
    /** 끝난 판의 결말 (runId 기준으로 한 번만 고른다) */
    private var endingForRun: Pair<String, Ending?>? = null
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

    /** 완료 인생은 김우영 저장소(V4ProgressStore)가 원본이다. 화면은 여기서 읽기만 한다. */
    var completedLives by mutableStateOf(v4ProgressStore.loadCompletedLives())
        private set

    val finishedLives: List<FinishedLifeUi>
        get() = completedLives.map { it.toFinishedLifeUi(recordContent.periods) }

    /** 지금 끝난 인생의 기록증 · 주마등 (결말에 닿는 순간 저장된 기록에서 만든다) */
    var currentRecord by mutableStateOf<LifeRecordUi?>(null)
        private set

    /** 미래 모습 생성 상태. 생성은 Fake이며 실패 · 시간 초과 · 취소는 기기 안 그림으로 대체한다. */
    var futureLook by mutableStateOf<GenerationUi?>(null)
        private set

    /** 프로필의 사진 → 얼굴 시트 생성 상태 */
    var faceSheet by mutableStateOf<GenerationUi?>(null)
        private set

    private var scenarioState by mutableStateOf(FakeScenario.SUCCESS)

    /** 개발용: Fake 생성 응답 시나리오(성공 · 지연 · 실패 · 시간 초과 · 취소). 바꾸면 받은 미래 모습을 비워 다시 요청할 수 있다. */
    var generationScenario: FakeScenario
        get() = scenarioState
        set(value) {
            scenarioState = value
            futureJob?.cancel()
            futureLook = null
        }

    private var futureJob: Job? = null
    private var faceJob: Job? = null
    private var savedCompletedRunId: String? = null

    private val recordContent: RecordContent by lazy {
        val v4 = repository.loadV4Content()
        RecordContent(
            periods = v4.periods,
            decisions = v4.decisions.associateBy { it.eventId },
            factCatalog = factCatalog,
            imageRouting = com.example.lifegame.data.ImageRoutingRepository(getApplication<Application>()).load(),
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
    }

    /** 프로필 만들기의 타고난 기질. characters.sample.json에서 읽는다. */
    val starters: List<StarterUi> by lazy {
        runCatching {
            getApplication<Application>().assets.open(CHARACTERS_FILE).bufferedReader().use { it.readText() }
        }.map(::parseStarters).getOrElse { emptyList() }
    }

    /** 플래그 → 인생 사실 (life_facts.sample.json). 없으면 빈 카탈로그로 기본 문구만 보인다. */
    private val factCatalog: LifeFactCatalog by lazy {
        runCatching {
            getApplication<Application>().assets.open(LIFE_FACTS_FILE).bufferedReader().use { it.readText() }
        }.map { LifeFactCatalog(parseLifeFacts(it)) }.getOrElse { LifeFactCatalog.EMPTY }
    }

    private fun playContentOf(
        periods: List<com.example.lifegame.domain.model.PeriodDefinition>,
        focuses: List<com.example.lifegame.domain.model.Focus>,
        decisions: List<com.example.lifegame.domain.model.DecisionEvent>,
    ) = PlayContent(
        periods = periods,
        focuses = focuses,
        decisions = decisions.associateBy { it.eventId },
        imageFiles = repository.loadImageAssets()
            .filter { it.fileName.isNotBlank() }
            .associate { it.imageId to it.fileName },
        imageRouting = com.example.lifegame.data.ImageRoutingRepository(getApplication<Application>()).load(),
        factCatalog = factCatalog,
        lookup = FlagLookup.from(decisions, factCatalog),
    )

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
                avatar = current.avatar,
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
                avatar = avatarLookOf(saved.avatar, saved.state.period, saved.state.stats),
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
        playContent = playContentOf(content.periods, focuses, decisions)
        choiceLog.clear()
        runChoiceHistory.clear()
        pendingOutcomeSnapshot = null
        avatarForPlay = profile.avatar
        awardedTitleForRun = null
        endingForRun = null
        currentRecord = null
        futureLook = null
        faceSheet = null
        savedCompletedRunId = null
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
        val saved = when (val loaded = v4ProgressStore.loadResult()) {
            V4LoadResult.Empty -> {
                playError = "저장된 인생이 없습니다. 새 인생을 시작해 주세요."
                return
            }
            V4LoadResult.Invalid -> {
                v4ProgressStore.clear()
                playError = "저장 데이터가 손상되었거나 새 버전과 호환되지 않습니다. 새 인생을 시작해 주세요."
                return
            }
            is V4LoadResult.Loaded -> loaded.run
        }
        val content = repository.loadV4Content()
        val name = normalizePlayerName(saved.playerName)
        val nameCall = nameCallOf(name)
        val focuses = content.focuses.map { it.withPlayerName(name, nameCall) }
        val decisions = content.decisions.map { it.withPlayerName(name, nameCall) }
        playContent = playContentOf(content.periods, focuses, decisions)
        choiceLog.clear()
        runChoiceHistory.clear()
        runChoiceHistory += saved.choices
        choiceLog += saved.choices.map { record ->
            val periodName = content.periods.firstOrNull { it.period == record.period }?.displayName
                ?: record.period.name
            ChoiceRecordUi(periodName, record.yearInPeriod, record.eventTitle, record.choiceLabel)
        }
        pendingOutcomeSnapshot = saved.pendingOutcome
        avatarForPlay = withReadablePhoto(saved.avatar)
        awardedTitleForRun = saved.awardedTitle
        endingForRun = null
        currentRecord = null
        futureLook = null
        savedCompletedRunId = null
        playSession = PeriodGameSession(
            content.periods,
            focuses,
            decisions,
            endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) },
            lifeFactRules = content.lifeFactRules,
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
        shownDecision = null
        outcome = saved.pendingOutcome?.let { snapshot ->
            OutcomeUi(
                choiceLabel = snapshot.choiceLabel,
                text = snapshot.resultText,
                changes = statChanges(snapshot.beforeStats, snapshot.afterStats),
                endsLife = snapshot.endsLife,
                openedEvents = snapshot.openedEvents,
                factChanges = factChanges(
                    buildLifeFacts(saved.state.copy(stats = snapshot.beforeStats, lifeFacts = snapshot.beforeFacts), factCatalog),
                    buildLifeFacts(saved.state.copy(stats = snapshot.afterStats, lifeFacts = snapshot.afterFacts), factCatalog),
                ),
            )
        }
        // 결과 화면에서 꺼졌다면 그 사건 머리도 되살린다. 없으면 결정 화면이 비어 보인다.
        shownDecision = saved.pendingOutcome?.let { snapshot ->
            playContent?.decisions?.get(snapshot.eventId)?.let { event ->
                restoredDecisionUi(event, playContent!!, saved.state.selectedFocusId, saved.state.period)
            }
        }
        if (outcome != null && shownDecision == null) {
            // 콘텐츠가 바뀌어 사건을 찾지 못하면 결과 확인을 건너뛰고 다음으로 넘어간다(이미 반영된 결과는 그대로).
            outcome = null
            pendingOutcomeSnapshot = null
        }
        decisionYear = saved.choices.lastOrNull()?.yearInPeriod.takeIf { outcome != null }
        died = saved.state.isDead; playError = null
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
        val beforeState = playStep?.state ?: return
        val before = beforeState.stats
        val content = playContent ?: return
        val label = decision.choices.firstOrNull { it.choiceId == choiceId }?.label ?: return
        val resolution = runCatching { current.choose(choiceId) }.getOrElse {
            playError = "선택을 반영하지 못했습니다."
            return
        }
        shownDecision = decision
        died = died || resolution.step.state.isDead
        val afterState = resolution.step.state
        val periodName = content.periods.firstOrNull { it.period == beforeState.period }?.displayName
            ?: beforeState.period.name
        choiceLog += ChoiceRecordUi(periodName, beforeState.yearInPeriod, decision.title, label)
        val choiceRecord = RunChoiceRecord(
            eventId = decision.eventId,
            choiceId = choiceId,
            outcomeId = resolution.outcome.outcomeId,
            period = beforeState.period,
            yearInPeriod = beforeState.yearInPeriod,
            eventTitle = decision.title,
            choiceLabel = label,
            resultText = resolution.outcome.resultText,
            momentImageId = content.decisions[decision.eventId]?.momentImageId,
        )
        runChoiceHistory += choiceRecord
        val openedEvents = if (afterState.isEnding) emptyList() else newlyOpenedEvents(
            before = beforeState.flags,
            after = afterState.flags,
            current = beforeState.period,
            decisions = content.decisions.values,
            periods = content.periods,
        )
        pendingOutcomeSnapshot = SavedOutcome(
            eventId = decision.eventId,
            choiceId = choiceId,
            outcomeId = resolution.outcome.outcomeId,
            choiceLabel = label,
            resultText = resolution.outcome.resultText,
            beforeStats = beforeState.stats,
            afterStats = afterState.stats,
            beforeFacts = beforeState.lifeFacts,
            afterFacts = afterState.lifeFacts,
            endsLife = afterState.isDead,
            openedEvents = openedEvents,
        )
        outcome = OutcomeUi(
            choiceLabel = label,
            text = resolution.outcome.resultText,
            changes = statChanges(before, afterState.stats),
            endsLife = afterState.isDead,
            factChanges = factChanges(
                buildLifeFacts(beforeState, factCatalog),
                buildLifeFacts(afterState, factCatalog),
            ),
            openedEvents = openedEvents,
        )
        playStep = resolution.step
        publishPlay()
    }

    /** 결과를 읽고 나서 다음 해 · 장 표지 · 끝으로 넘어간다. */
    fun continueAfterOutcome() {
        if (outcome == null) return
        outcome = null
        pendingOutcomeSnapshot = null
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

    /**
     * 끝난 인생을 "지난 인생"에 남기고 현재 슬롯을 비운다.
     * 결말에 닿을 때 이미 같은 runId로 저장했으므로 여기서는 같은 기록을 다시 덮어쓸 뿐이다(중복 없음).
     */
    fun finishLife() {
        play?.takeIf { it.phase == PlayPhase.ENDED } ?: return
        val finalState = playStep?.state ?: return
        saveCompletedLife(finalState)
        futureJob?.cancel()
        clearPlay()
        currentRecord = null
        futureLook = null
        v4ProgressStore.clear()
    }

    /** 홈의 지난 인생 / 기록 화면에서 다시 여는 기록 */
    fun recordFor(runId: String): LifeRecordUi? =
        completedLives.firstOrNull { it.runId == runId }?.let { buildLifeRecordUi(it, recordContent) }

    fun titleCollection(): TitleCollectionUi =
        buildTitleCollection(runCatching { repository.loadTitles() }.getOrElse { emptyList() }, completedLives)

    /** 미래 모습을 요청한다. 같은 인생에서 이미 받았으면 다시 부르지 않는다(재진입 · 회전에도 한 번). */
    fun requestFutureLook(runId: String) {
        val current = futureLook
        if (current != null && current.key == runId && current.status != GenerationStatus.IDLE) return
        val record = completedLives.firstOrNull { it.runId == runId } ?: return
        futureJob?.cancel()
        futureLook = GenerationUi(runId, GenerationStatus.LOADING)
        val scenario = generationScenario
        val client = FakeGenerationClient(scenario.mode)
        val state = record.finalState
        futureJob = viewModelScope.launch {
            val result = withTimeoutOrNull(scenario.timeoutMillis) {
                withContext(Dispatchers.IO) {
                    delay(scenario.latencyMillis)
                    client.createFutureLook(
                        FutureLookRequest(
                            runId = runId,
                            avatar = record.avatar,
                            period = state.endingPeriod ?: state.period,
                            bodyType = bodyTypeFor(state.stats),
                            facts = state.lifeFacts,
                            stats = state.stats,
                        ),
                    )
                }
            } ?: GenerationResult.TimedOut
            futureLook = futureLookResult(runId, result)
        }
    }

    fun cancelFutureLook() {
        val key = futureLook?.key ?: return
        if (futureLook?.status != GenerationStatus.LOADING) return
        futureJob?.cancel()
        futureLook = GenerationUi(key, GenerationStatus.FALLBACK, "취소해서 기본 그림으로 보여 줍니다.")
    }

    /** 사진 → 얼굴 시트. 동의가 없으면 부르지 않는다. 결과와 상관없이 기본 캐릭터 진행은 막지 않는다. */
    fun requestFaceSheet(photoRef: String, consent: Boolean) {
        faceJob?.cancel()
        if (!consent) {
            faceSheet = GenerationUi(photoRef, GenerationStatus.FALLBACK, "동의하지 않아 기본 모습으로 진행합니다.")
            return
        }
        faceSheet = GenerationUi(photoRef, GenerationStatus.LOADING)
        val scenario = generationScenario
        val client = FakeGenerationClient(scenario.mode)
        faceJob = viewModelScope.launch {
            val result = withTimeoutOrNull(scenario.timeoutMillis) {
                withContext(Dispatchers.IO) {
                    delay(scenario.latencyMillis)
                    client.createFaceSheet(FaceSheetRequest(runId = "profile", localPhotoRef = photoRef, consent = true))
                }
            } ?: GenerationResult.TimedOut
            faceSheet = when (result) {
                is GenerationResult.Success -> GenerationUi(
                    photoRef,
                    GenerationStatus.READY,
                    "얼굴 시트 예시 응답을 받았습니다. 실제 생성 서버는 아직 연결되지 않아 화면에는 고른 기본 외형으로 보입니다.",
                    value = result.value,
                )
                is GenerationResult.Failure -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "얼굴을 만들지 못했습니다. 기본 모습으로 진행합니다.")
                GenerationResult.TimedOut -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "응답이 늦어 기본 모습으로 진행합니다.")
                GenerationResult.Cancelled -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "취소해서 기본 모습으로 진행합니다.")
            }
        }
    }

    fun cancelFaceSheet() {
        val key = faceSheet?.key ?: return
        if (faceSheet?.status != GenerationStatus.LOADING) return
        faceJob?.cancel()
        faceSheet = GenerationUi(key, GenerationStatus.FALLBACK, "취소해서 기본 모습으로 진행합니다.")
    }

    fun clearFaceSheet() {
        faceJob?.cancel()
        faceSheet = null
    }

    private fun futureLookResult(runId: String, result: GenerationResult<String>): GenerationUi = when (result) {
        is GenerationResult.Success -> GenerationUi(
            runId,
            GenerationStatus.READY,
            "생성 서버 연결 전이라 예시 응답을 받아, 기기 안 그림으로 보여 줍니다.",
            value = result.value,
        )
        is GenerationResult.Failure -> GenerationUi(runId, GenerationStatus.FALLBACK, "미래 모습을 만들지 못해 기본 그림으로 보여 줍니다.")
        GenerationResult.TimedOut -> GenerationUi(runId, GenerationStatus.FALLBACK, "응답이 늦어 기본 그림으로 보여 줍니다.")
        GenerationResult.Cancelled -> GenerationUi(runId, GenerationStatus.FALLBACK, "취소해서 기본 그림으로 보여 줍니다.")
    }

    /** 결말에 닿은 상태를 완료 기록으로 만든다. 결말 · 칭호는 runId마다 한 번 고른 값을 쓴다. */
    private fun completedRecordFor(finalState: PeriodRunState): CompletedLifeRecord {
        val ending = endingFor(finalState)
        val title = awardedTitleForRun ?: resolveTitleFor(finalState)?.also { awardedTitleForRun = it }
        val periodName = playContent?.periods?.firstOrNull { it.period == (finalState.endingPeriod ?: finalState.period) }
            ?.displayName ?: finalState.period.name
        val summary = ending?.title ?: if (finalState.isDead) "${periodName}에 생을 마쳤다" else "${periodName}까지 살았다"
        return CompletedLifeRecord(
            runId = finalState.runId,
            playerName = playerNameForPlay,
            summary = title?.let { "$summary · ${it.text}" } ?: summary,
            endingId = ending?.endingId,
            endingTitle = ending?.title,
            endingSummary = ending?.summary,
            awardedTitle = title,
            finalState = finalState,
            choices = runChoiceHistory.toList(),
            avatar = avatarForPlay,
        )
    }

    /** runId 기준 upsert. 결말 화면을 다시 열거나 홈으로 가도 같은 기록 하나만 남는다. */
    private fun saveCompletedLife(finalState: PeriodRunState): CompletedLifeRecord {
        val record = completedRecordFor(finalState)
        v4ProgressStore.saveCompletedLife(record)
        savedCompletedRunId = record.runId
        completedLives = v4ProgressStore.loadCompletedLives()
        return record
    }

    /** 공유 URI 접근이 만료됐으면 사진 참조를 지우고 기본 외형으로 이어 간다. */
    private fun withReadablePhoto(avatar: AvatarConfig): AvatarConfig {
        val ref = avatar.portraitRef ?: return avatar
        val readable = runCatching {
            getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(ref))?.use { true } ?: false
        }.getOrDefault(false)
        if (readable) return avatar
        android.widget.Toast.makeText(
            getApplication(),
            "사진에 다시 접근할 수 없어 기본 모습으로 이어 갑니다.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
        return avatar.copy(portraitRef = null, faceSheetRef = null, photoConsent = false)
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
        val ending = if (step.phase == TurnPhase.ENDING) endingFor(step.state) else null
        if (step.phase == TurnPhase.ENDING && awardedTitleForRun == null) {
            awardedTitleForRun = resolveTitleFor(step.state)
        }
        if (step.phase == TurnPhase.ENDING) {
            // 결말에 닿는 순간 완료 기록을 저장한다. 주마등 도중 앱이 꺼져도 기록은 남고, 다시 저장해도 runId로 덮어쓴다.
            val record = if (savedCompletedRunId == step.state.runId) {
                completedLives.firstOrNull { it.runId == step.state.runId } ?: saveCompletedLife(step.state)
            } else {
                saveCompletedLife(step.state)
            }
            if (currentRecord?.runId != record.runId) currentRecord = buildLifeRecordUi(record, recordContent)
        }
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
            choiceLog = choiceLog.toList(),
            endingTitle = ending?.title,
            endingSummary = ending?.summary,
            avatar = avatarForPlay,
            endingId = ending?.endingId,
        )
        v4ProgressStore.save(
            V4SavedRun(
                playerName = playerNameForPlay,
                state = step.state,
                choices = runChoiceHistory.toList(),
                pendingOutcome = pendingOutcomeSnapshot,
                avatar = avatarForPlay,
                awardedTitle = awardedTitleForRun,
                phase = step.phase,
            ),
        )
    }

    /**
     * 끝난 판의 결말을 결말 데이터의 조건 · 우선순위로 고른다 (기존 결말 규칙 그대로).
     * 백엔드의 v4 결말 판정(PR #30)이 들어오면 그 값을 쓰도록 바꾼다.
     */
    private fun endingFor(state: com.example.lifegame.domain.model.PeriodRunState): Ending? {
        endingForRun?.takeIf { it.first == state.runId }?.let { return it.second }
        val name = playerNameForPlay
        val nameCall = nameCallOf(name)
        val ending = runCatching {
            val endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) }
            val result = GameEngine().resolveEndingDecision(state, endings)
            android.util.Log.d("LifeGameEnding", "run=${state.runId} reason=${result.reason} ending=${result.selected.endingId}")
            result.selected
        }.getOrNull()
        endingForRun = state.runId to ending
        return ending
    }

    private fun resolveTitleFor(state: com.example.lifegame.domain.model.PeriodRunState): AwardedTitle? =
        runCatching { GameEngine().resolveTitle(state, repository.loadTitles()) }.getOrNull()

    private fun clearPlay() {
        playSession = null
        playContent = null
        playStep = null
        shownDecision = null
        decisionYear = null
        outcome = null
        died = false
        choiceLog.clear()
        runChoiceHistory.clear()
        pendingOutcomeSnapshot = null
        avatarForPlay = AvatarConfig()
        awardedTitleForRun = null
        endingForRun = null
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
        const val LIFE_FACTS_FILE = "life_facts.sample.json"
        const val MAX_SKIP_YEARS = 200
    }
}
