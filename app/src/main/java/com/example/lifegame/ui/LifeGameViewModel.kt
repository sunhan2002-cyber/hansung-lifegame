package com.example.lifegame.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ContentLoadResult
import com.example.lifegame.data.PlayableContent
import com.example.lifegame.data.SaveState
import com.example.lifegame.data.ProgressStore
import com.example.lifegame.data.SavedGame
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.data.V4LoadResult
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.GameSession
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.engine.PeriodSessionFactory
import com.example.lifegame.domain.engine.SessionStartProfile
import com.example.lifegame.domain.engine.RunExtensionApi
import com.example.lifegame.domain.engine.CurrentLifeResetCoordinator
import com.example.lifegame.domain.engine.CurrentLifeResetResult
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.RunNarrativeKind
import com.example.lifegame.domain.model.RunNarrativeRecord
import com.example.lifegame.domain.model.RunExtensionState
import com.example.lifegame.domain.model.ChallengeCodeState
import com.example.lifegame.domain.model.TitleGoalState
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.PhotoFlowStatus
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.domain.model.ChoiceApplicationGuard
import com.example.lifegame.domain.model.appendOnce
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import com.example.lifegame.domain.engine.bodyTypeFor
import com.example.lifegame.data.FaceSheetClients
import com.example.lifegame.domain.generation.FaceSheetRequest
import com.example.lifegame.domain.generation.GenerationClient
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
import kotlinx.coroutines.flow.collect

/** 진행 중 인생 리셋의 저장 확인 단계 */
enum class LifeResetProgress { WAITING, DONE, FAILED }

class LifeGameViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ContentRepository = ContentRepository(application),
    private val v4ProgressStore: V4ProgressStore = V4ProgressStore(application),
) : AndroidViewModel(application) {
    private val progressStore = ProgressStore(application)
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
    private var startingRollRawForRun: String? = null
    private var startingDiceRuleForRun: DiceRule = DiceRule.LEGACY_LG1
    private var rerollsRemainingForRun: Int = 0
    private var extensionStateForRun = RunExtensionState()
    private var characterImageIdForPlay: String? = null
    /** 고른 선택지 기록. 결말 화면의 "주요 선택"에 쓴다. */
    private val choiceLog = mutableListOf<ChoiceRecordUi>()
    private val runChoiceHistory = mutableListOf<RunChoiceRecord>()
    private var narrativeRecords = emptyList<RunNarrativeRecord>()
    private var presentationForRun = PresentationState()
    private var profilePhotoPresentation = PresentationState()
    private val choiceApplicationGuard = ChoiceApplicationGuard()
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

    var contentWarning by mutableStateOf<String?>(null)
        private set
    var saveWarning by mutableStateOf<String?>(null)
        private set
    val saveState get() = v4ProgressStore.saveState
    val hasStoredRun: Boolean get() = v4ProgressStore.hasSave()
    fun retrySave() = v4ProgressStore.retrySave()
    fun awaitSave() = v4ProgressStore.awaitDurable()

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
    private var faceRequestId: String? = null

    /** Online generation remains disabled until an authenticated backend is configured. */
    private val realFaceClient: GenerationClient? by lazy { FaceSheetClients.onlineClient() }

    /** 프로필 화면 문구(동의 · 안내)를 실제 전송 기준으로 바꿀지 */
    val usesRealFaceGeneration: Boolean get() = realFaceClient != null
    private var savedCompletedRunId: String? = null
    private val resetCoordinator = CurrentLifeResetCoordinator()

    /**
     * 진행 중 인생만 원자적으로 폐기한다. 완료 기록·칭호·설정은 건드리지 않는다.
     * 저장 삭제가 실패하면 화면과 세션을 그대로 둬 다음 실행에서도 복구할 수 있다.
     */
    fun resetCurrentLife(): Boolean {
        return when (resetCoordinator.reset(v4ProgressStore::clearCurrentLifeAtomically) {
            futureJob?.cancel()
            faceJob?.cancel()
            clearPlay()
        }) {
            CurrentLifeResetResult.SUCCESS -> {
                playError = null
                true
            }
            CurrentLifeResetResult.STORAGE_FAILURE -> {
                playError = "진행 데이터를 정리하지 못했습니다. 기존 인생을 보존했습니다."
                false
            }
            CurrentLifeResetResult.ALREADY_RUNNING -> false
        }
    }

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
            extensions = scenarioExtensions,
            automaticEvents = automaticEventRules.associateBy { it.event.eventId },
        )
    }

    /** 성향 목록 (characters.sample.json). 주사위 결과와 가장 닮은 성향의 플래그 · 아기 그림을 쓴다. */
    val starters: List<StarterUi> by lazy {
        runCatching {
            getApplication<Application>().assets.open(CHARACTERS_FILE).bufferedReader().use { it.readText() }
        }.map(::parseStarters).getOrElse { emptyList() }
    }

    /** 해마다의 일상 이야기. 파일이 없거나 깨져도 게임은 기존 기록만으로 진행한다. */
    private val yearlyStories: YearlyStoryBook by lazy {
        runCatching {
            getApplication<Application>().assets.open(YEARLY_STORIES_FILE).bufferedReader().use { it.readText() }
        }.map(YearlyStoryBook::parse).getOrElse { YearlyStoryBook.EMPTY }
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
        yearlyStories = yearlyStories,
        extensions = scenarioExtensions,
        automaticEvents = automaticEventRules.associateBy { it.event.eventId },
    )

    /** 회상 · 계기 카드 · 칭호 목표 · NPC 재등장(신기훈 #76). 없거나 깨지면 확장 표시만 빠진다. */
    private val scenarioExtensions: ScenarioExtensions by lazy {
        runCatching {
            getApplication<Application>().assets.open(SCENARIO_EXTENSIONS_FILE).bufferedReader().use { it.readText() }
        }.map(ScenarioExtensions::parse).getOrElse { ScenarioExtensions.EMPTY }
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
                avatar = current.avatar,
            )
        } ?: v4ProgressStore.load()?.let { saved ->
            val content = (repository.loadPlayableContent() as? ContentLoadResult.Ready)?.value?.content
                ?: return@let null
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
        if (!canPersistNextAction()) return
        val bundle = requirePlayableContent() ?: return
        val content = bundle.content
        val name = normalizePlayerName(profile.name)
        val nameCall = nameCallOf(name)
        startingRollRawForRun = profile.startingRollRaw
        startingDiceRuleForRun = profile.diceRule
        rerollsRemainingForRun = profile.rerollsRemaining.coerceIn(0, StartingDice.REROLLS)
        extensionStateForRun = RunExtensionState(
            challengeCode = ChallengeCodeState(
                input = if (profile.startingRollRaw != null && profile.runSeed != null) {
                    ChallengeCode.encode(ChallengeStart(profile.gender, profile.startingRollRaw, profile.runSeed))
                } else "",
                genderFlag = profile.gender.flag,
                startingRollRaw = profile.startingRollRaw,
                seed = profile.runSeed,
                valid = profile.startingRollRaw != null && profile.runSeed != null,
                diceRuleVersion = profile.diceRule.name,
            ),
        )
        // 엔진에 넘기기 전에 이름을 바꿔 두면 결정 · 결과 · 기록 문장 어디에도 {name}이 남지 않는다.
        val focuses = content.focuses.map { it.withPlayerName(name, nameCall) }
        val decisions = content.decisions.map { it.withPlayerName(name, nameCall) }
        val sessionFactory = PeriodSessionFactory(
            periods = content.periods,
            focuses = focuses,
            decisions = decisions,
            endings = bundle.endings.map { it.withPlayerName(name, nameCall) },
            lifeFactRules = content.lifeFactRules,
            automaticEvents = content.automaticEvents,
            automaticEventRules = bundle.automaticEventRules,
        )
        val starter = starters.firstOrNull { it.starterId == profile.starterId }
        val started = runCatching {
            sessionFactory.start(SessionStartProfile(
                genderFlag = profile.gender.flag,
                starterId = profile.starterId,
                starterDelta = starter?.statDelta ?: com.example.lifegame.domain.model.StatDelta(),
                starterFlags = starter?.startFlags.orEmpty(),
                rolledDelta = profile.rolledDelta,
                startingRollRaw = profile.startingRollRaw,
                runSeed = profile.runSeed,
            ))
        }.getOrElse {
            clearPlay()
            playError = "새 인생을 시작하지 못했습니다."
            return
        }
        val newSession = started.session
        val step = started.step
        profile.startingRollRaw?.let { raw ->
            extensionStateForRun = extensionStateForRun.copy(
                challengeCode = ChallengeCodeState(
                    input = ChallengeCode.encode(ChallengeStart(profile.gender, raw, step.state.runSeed)),
                    genderFlag = profile.gender.flag,
                    startingRollRaw = raw,
                    seed = step.state.runSeed,
                    valid = true,
                    diceRuleVersion = profile.diceRule.name,
                ),
            )
        }

        playSession = newSession
        playContent = playContentOf(content.periods, focuses, decisions)
        choiceLog.clear()
        runChoiceHistory.clear()
        narrativeRecords = emptyList()
        presentationForRun = profilePhotoPresentation
        profilePhotoPresentation = PresentationState()
        choiceApplicationGuard.clear()
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
        // The first checkpoint atomically archives any unreadable previous slot before replacing it.
        applyStep(step)
    }

    fun continueLife() {
        if (!canPersistNextAction()) return
        val saved = when (val loaded = v4ProgressStore.loadResult()) {
            V4LoadResult.Empty -> {
                playError = "저장된 인생이 없습니다. 새 인생을 시작해 주세요."
                return
            }
            V4LoadResult.Invalid -> {
                clearPlay()
                playError = "저장된 인생을 읽을 수 없어 원본을 보존했습니다. 앱을 업데이트하거나 새 인생을 시작할 수 있습니다."
                return
            }
            is V4LoadResult.Loaded -> loaded.run
        }
        val bundle = requirePlayableContent() ?: return
        val content = bundle.content
        val name = normalizePlayerName(saved.playerName)
        val nameCall = nameCallOf(name)
        val focuses = content.focuses.map { it.withPlayerName(name, nameCall) }
        val decisions = content.decisions.map { it.withPlayerName(name, nameCall) }
        playContent = playContentOf(content.periods, focuses, decisions)
        choiceLog.clear()
        runChoiceHistory.clear()
        runChoiceHistory += saved.choices
        narrativeRecords = saved.narrativeRecords
        startingRollRawForRun = saved.startingRollRaw
        startingDiceRuleForRun = saved.startingDiceRule?.let {
            runCatching { DiceRule.valueOf(it) }.getOrNull()
        } ?: DiceRule.LEGACY_LG1
        rerollsRemainingForRun = saved.rerollsRemaining.coerceIn(0, StartingDice.REROLLS)
        extensionStateForRun = saved.extensionState
        presentationForRun = saved.presentation
        choiceLog += saved.choices.map { record ->
            val periodName = content.periods.firstOrNull { it.period == record.period }?.displayName
                ?: record.period.name
            ChoiceRecordUi(periodName, record.yearInPeriod, record.eventTitle, displayChoiceLabel(record.choiceLabel))
        }
        pendingOutcomeSnapshot = saved.pendingOutcome
        avatarForPlay = withReadablePhoto(saved.avatar)
        awardedTitleForRun = saved.awardedTitle
        endingForRun = null
        currentRecord = null
        futureLook = null
        savedCompletedRunId = null
        playSession = PeriodSessionFactory(
            content.periods,
            focuses,
            decisions,
            endings = bundle.endings.map { it.withPlayerName(name, nameCall) },
            lifeFactRules = content.lifeFactRules,
            automaticEvents = content.automaticEvents,
            automaticEventRules = bundle.automaticEventRules,
        ).create()
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
                choiceLabel = displayChoiceLabel(snapshot.choiceLabel),
                text = snapshot.resultText,
                changes = statChanges(snapshot.beforeStats, snapshot.afterStats),
                endsLife = snapshot.endsLife,
                openedEvents = snapshot.openedEvents,
                factChanges = restoredFactChanges(saved.state, snapshot, factCatalog),
            )
        }
        // 결과 화면에서 꺼졌다면 그 사건 머리도 되살린다. 없으면 결정 화면이 비어 보인다.
        shownDecision = saved.pendingOutcome?.let { snapshot ->
            playContent?.decisions?.get(snapshot.eventId)?.let { event ->
                restoredDecisionUi(
                    event,
                    playContent!!,
                    saved.state.selectedFocusId,
                    snapshot.period ?: saved.state.period,
                    savedEventImageId = snapshot.eventImageId,
                )
            }
        }
        if (outcome != null && shownDecision == null) {
            // 콘텐츠가 바뀌어 사건을 찾지 못하면 결과 확인을 건너뛰고 다음으로 넘어간다(이미 반영된 결과는 그대로).
            outcome = null
            pendingOutcomeSnapshot = null
        }
        decisionYear = saved.pendingOutcome?.yearInPeriod
            ?: saved.choices.lastOrNull()?.yearInPeriod.takeIf { outcome != null }
        died = saved.state.isDead; playError = null
        val restored = runCatching { playSession!!.restoreSaved(saved.state) }.getOrElse {
            clearPlay()
            playError = "이 버전에서 인생을 이어갈 수 없어 저장 원본을 보존했습니다."
            return
        }
        applyStep(withRestoredAutomaticEvent(restored))
    }

    private fun requirePlayableContent(): PlayableContent? = when (val loaded = repository.loadPlayableContent()) {
        is ContentLoadResult.Ready -> loaded.value.also { contentWarning = null }
        is ContentLoadResult.Failed -> {
            clearPlay()
            contentWarning = CONTENT_ERROR_MESSAGE
            playError = CONTENT_ERROR_MESSAGE
            null
        }
    }

    private fun canPersistNextAction(): Boolean {
        if (saveState.value !is SaveState.Failed) return true
        saveWarning = SAVE_ERROR_MESSAGE
        return false
    }

    /**
     * 자동 사건 카드를 보던 중 앱이 꺼졌다면 같은 카드를 다시 보여 준다. 엔진은 다시 적용하지 않는다
     * (같은 진행 단계 키가 이미 저장돼 있어 [com.example.lifegame.domain.engine.AutomaticEventPicker]가 다시 고르지 않는다).
     */
    private fun withRestoredAutomaticEvent(step: TurnStep): TurnStep {
        if (step.phase != TurnPhase.AUTO_PROGRESS || step.automaticEvent != null || outcome != null) return step
        val last = step.state.automaticEventRecords.lastOrNull() ?: return step
        val shownLast = last.triggerToken == com.example.lifegame.domain.engine.AutomaticEventPicker.stageToken(step.state) &&
            step.state.moments.lastOrNull()?.text == last.text
        return if (shownLast) step.copy(automaticEvent = last, recordText = last.text) else step
    }

    /** Required play data is checked at entry; record-only views can still display saved text. */
    private val automaticEventRules: List<com.example.lifegame.domain.engine.AutomaticEventRule> by lazy {
        repository.loadAutomaticEventRules()
    }

    fun selectFocus(focusId: String) {
        if (!canPersistNextAction()) return
        val current = playSession ?: return
        runStep { current.selectFocus(focusId) }
    }

    /** 자동 진행 타이머가 한 해마다 부른다. 결정이 걸리면 엔진이 멈춘 상태를 돌려준다. */
    fun advanceYear() {
        if (!canPersistNextAction()) return
        if (play?.phase != PlayPhase.AUTO) return
        val current = playSession ?: return
        runStep { current.advance() }
    }

    /** "다음 결정까지": 결정 · 장 표지 · 끝이 나올 때까지 해를 한 번에 넘긴다. */
    fun skipToNextDecision() {
        if (!canPersistNextAction()) return
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
        if (!canPersistNextAction()) return
        val current = playSession ?: return
        val decision = play?.decision ?: return
        if (outcome != null) return
        val beforeState = playStep?.state ?: return
        val before = beforeState.stats
        val content = playContent ?: return
        val label = decision.choices.firstOrNull { it.choiceId == choiceId }?.label ?: return
        val inputId = "${beforeState.runId}:${decision.eventId}:$choiceId"
        if (!choiceApplicationGuard.tryBegin(inputId)) return
        val resolution = runCatching { current.choose(choiceId) }.getOrElse {
            choiceApplicationGuard.release(inputId)
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
            bodyType = bodyTypeFor(afterState.stats),
            eventImageId = decision.imageId.takeIf(String::isNotBlank),
        )
        runChoiceHistory += choiceRecord
        val displayId = "${beforeState.runId}:${decision.eventId}:$choiceId:${resolution.outcome.outcomeId}"
        narrativeRecords = narrativeRecords.appendOnce(
            RunNarrativeRecord(
                recordId = displayId,
                kind = RunNarrativeKind.CHOICE_RESULT,
                period = beforeState.period,
                yearInPeriod = beforeState.yearInPeriod,
                text = resolution.outcome.resultText,
                eventId = decision.eventId,
                choiceId = choiceId,
            ),
        )
        presentationForRun = presentationForRun.requestResultAnimation(displayId)
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
            eventImageId = decision.imageId.takeIf(String::isNotBlank),
            period = beforeState.period,
            yearInPeriod = beforeState.yearInPeriod,
            beforeFlags = beforeState.flags,
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
        if (!canPersistNextAction()) return
        if (outcome == null) return
        outcome = null
        pendingOutcomeSnapshot = null
        shownDecision = null
        decisionYear = null
        publishPlay()
    }

    /** 화면 연출 확인만 저장한다. 엔진 선택이나 보상 적용을 다시 호출하지 않는다. */
    fun markEventImageShown(eventId: String) {
        if (!canPersistNextAction()) return
        presentationForRun = presentationForRun.markEventImageShown(eventId)
        publishPlay()
    }

    /** 애니메이션 재생 완료만 기록한다. 능력치 변화는 이미 엔진에서 한 번 적용된 상태다. */
    fun markResultAnimationConsumed(displayId: String) {
        if (!canPersistNextAction()) return
        presentationForRun = presentationForRun.consumeResultAnimation(displayId)
        publishPlay()
    }

    /**
     * 컷신 연출 상태만 저장한다. 끝난(완료 · 건너뜀) 컷신은 다시 바꾸지 않아, 자동 끝과 탭이 겹치거나 화면이 다시 그려져도
     * 한 번만 기록된다. 새 인생은 [PresentationState] 기본값(시작 전)에서 다시 시작한다.
     */
    fun setCutsceneStatus(status: CutsceneStatus) {
        if (!canPersistNextAction()) return
        val current = presentationForRun.cutscene
        if (current == status || current == CutsceneStatus.COMPLETED || current == CutsceneStatus.SKIPPED) return
        presentationForRun = presentationForRun.copy(cutscene = status)
        publishPlay()
    }

    fun setPhotoFlowStatus(status: PhotoFlowStatus, failureReason: String? = null) {
        if (!canPersistNextAction()) return
        profilePhotoPresentation = profilePhotoPresentation.updatePhotoFlow(status, failureReason)
        if (playSession != null) {
            presentationForRun = presentationForRun.updatePhotoFlow(status, failureReason)
            publishPlay()
        }
    }

    fun togglePlaying() {
        if (!canPersistNextAction()) return
        autoPlaying = !autoPlaying
        publishPlay()
    }

    fun setSpeed(value: Int) {
        if (!canPersistNextAction()) return
        if (value !in PLAY_SPEEDS) return
        speed = value
        publishPlay()
    }

    /**
     * 끝난 인생을 "지난 인생"에 남기고 현재 슬롯을 비운다.
     * 결말에 닿을 때 이미 같은 runId로 저장했으므로 여기서는 같은 기록을 다시 덮어쓸 뿐이다(중복 없음).
     */
    fun finishLife() {
        if (!canPersistNextAction()) return
        play?.takeIf { it.phase == PlayPhase.ENDED } ?: return
        val finalState = playStep?.state ?: return
        val record = completedRecordFor(finalState)
        if (!v4ProgressStore.finish(record)) return
        completedLives = v4ProgressStore.loadCompletedLives()
        futureJob?.cancel()
        clearPlay()
        currentRecord = null
        futureLook = null
    }

    /** 진행 중 인생 리셋이 디스크 확인을 기다리는 중인지. 이 동안 화면은 "정리 중"을 보여 주고 진행 저장을 하지 않는다. */
    var lifeResetPending by mutableStateOf(false)
        private set

    /** 리셋을 하지 못했을 때 보여 줄 안내. 지금 인생은 그대로 남아 있다. */
    var lifeResetError by mutableStateOf<String?>(null)
        private set

    fun clearLifeResetError() {
        lifeResetError = null
    }

    /**
     * 진행 중 "인생 리셋" 1단계(2026-11-20 과제). 현재 진행 슬롯만 지운다. 완료한 지난 인생 · 칭호 · 화면 설정은 그대로다.
     * 화면 상태는 디스크 저장이 확인될 때까지 유지하고([completeLifeReset]), 실패하면 되돌린다([cancelLifeReset]).
     * 연타 · 중복 호출은 한 번만 처리한다. 결말에 닿은 인생은 리셋하지 않는다(완료 기록 중복 방지).
     * 저장 계층의 리셋 규칙(김우영 담당)이 들어오면 이 함수만 그쪽을 부르면 된다.
     */
    fun startLifeReset(): Boolean {
        if (lifeResetPending) return true
        val current = play ?: return false
        if (current.phase == PlayPhase.ENDED) return false
        if (!canPersistNextAction()) {
            lifeResetError = LIFE_RESET_ERROR_MESSAGE
            return false
        }
        if (!v4ProgressStore.clear()) {
            lifeResetError = LIFE_RESET_ERROR_MESSAGE
            return false
        }
        lifeResetRevision = (saveState.value as? SaveState.Pending)?.revision ?: 0L
        lifeResetPending = true
        return true
    }

    private var lifeResetRevision = Long.MAX_VALUE

    /** 리셋이 만든 저장 요청이 디스크에 기록됐는지. 그 전의 저장 완료 표시는 리셋 완료로 보지 않는다. */
    fun lifeResetProgress(): LifeResetProgress = when (val state = saveState.value) {
        is SaveState.Durable -> if (state.revision >= lifeResetRevision) LifeResetProgress.DONE else LifeResetProgress.WAITING
        is SaveState.Failed -> if (state.revision >= lifeResetRevision) LifeResetProgress.FAILED else LifeResetProgress.WAITING
        else -> LifeResetProgress.WAITING
    }

    /** 리셋 2단계: 디스크에서 슬롯이 지워진 것이 확인되면 현재 인생 화면 상태를 비운다. 대기 중 자동 진행도 함께 사라진다. */
    fun completeLifeReset() {
        if (!lifeResetPending) return
        futureJob?.cancel()
        cancelRunningFaceRequest()
        clearPlay()
        currentRecord = null
        futureLook = null
        playError = null
        lifeResetPending = false
    }

    /** 리셋을 확정하지 못했으면 지금 인생을 다시 저장해 그대로 둔다. */
    fun cancelLifeReset() {
        if (!lifeResetPending) return
        lifeResetPending = false
        lifeResetError = LIFE_RESET_ERROR_MESSAGE
        publishPlay()
    }

    /** 홈의 지난 인생 / 기록 화면에서 다시 여는 기록 */
    fun recordFor(runId: String): LifeRecordUi? =
        completedLives.firstOrNull { it.runId == runId }?.let { buildLifeRecordUi(it, recordContent) }

    fun titleCollection(): TitleCollectionUi =
        buildTitleCollection(
            runCatching { repository.loadTitles() }.getOrElse { emptyList() },
            completedLives,
            scenarioExtensions.titleGoals,
        )

    /** 화면 설정만 담는 저장소. 게임 진행 저장(진행 · 완료 기록)과 섞지 않는다. */
    private val uiPrefs = application.getSharedPreferences(UI_PREFS_NAME, android.content.Context.MODE_PRIVATE)

    /** 칭호 목록에서 "다음 판에서 노리기"로 고른 칭호. 이 기기에서만 기억한다. */
    var titleTargetId by mutableStateOf(runCatching { uiPrefs.getString(TITLE_TARGET_KEY, null) }.getOrNull())
        private set

    fun setTitleTarget(titleId: String?) {
        titleTargetId = titleId
        uiPrefs.edit().apply { if (titleId == null) remove(TITLE_TARGET_KEY) else putString(TITLE_TARGET_KEY, titleId) }.apply()
    }

    /** 새 인생 화면에 보여 줄, 아직 받지 못한 목표 칭호 */
    fun titleTarget(): TitleEntryUi? =
        titleTargetId?.let { id -> titleCollection().entries.firstOrNull { it.titleId == id && !it.earned } }

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
        cancelRunningFaceRequest()
        if (!consent) {
            faceSheet = GenerationUi(photoRef, GenerationStatus.FALLBACK, "동의하지 않아 기본 모습으로 진행합니다.")
            return
        }
        discardUnusedFaceSheet()
        faceSheet = GenerationUi(photoRef, GenerationStatus.LOADING)
        val real = realFaceClient
        if (real != null) {
            val requestId = "profile-${System.nanoTime()}"
            faceRequestId = requestId
            faceJob = viewModelScope.launch {
                val result = withTimeoutOrNull(REAL_FACE_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        real.createFaceSheet(FaceSheetRequest(runId = requestId, localPhotoRef = photoRef, consent = true))
                    }
                } ?: GenerationResult.TimedOut.also { real.cancel(requestId) }
                if (faceRequestId != requestId) return@launch
                faceRequestId = null
                faceSheet = when (result) {
                    is GenerationResult.Success -> GenerationUi(
                        photoRef,
                        GenerationStatus.READY,
                        "사진으로 얼굴을 만들었습니다. 시기마다 이 얼굴로 나이 들어 갑니다.",
                        value = result.value,
                    )
                    is GenerationResult.Failure -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "${result.reason} 기본 모습으로도 시작할 수 있습니다.")
                    GenerationResult.TimedOut -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "응답이 너무 늦어 멈췄습니다. 다시 시도하거나 기본 모습으로 시작할 수 있습니다.")
                    GenerationResult.Cancelled -> GenerationUi(photoRef, GenerationStatus.FALLBACK, "취소해서 기본 모습으로 진행합니다.")
                }
            }
            return
        }
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
                    "온라인 얼굴 생성은 아직 연결되지 않았습니다. 사진은 전송하지 않고 고른 기본 외형으로 보여 줍니다.",
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
        cancelRunningFaceRequest()
        faceSheet = GenerationUi(key, GenerationStatus.FALLBACK, "취소해서 기본 모습으로 진행합니다.")
    }

    /** 사진 지우기: 진행 중인 요청을 끊고, 아직 어떤 인생에도 쓰지 않은 생성 결과 파일도 지운다. */
    fun clearFaceSheet() {
        cancelRunningFaceRequest()
        discardUnusedFaceSheet()
        faceSheet = null
    }

    private fun cancelRunningFaceRequest() {
        faceJob?.cancel()
        faceRequestId?.let { id -> realFaceClient?.cancel(id) }
        faceRequestId = null
    }

    private fun discardUnusedFaceSheet() {
        val path = com.example.lifegame.ui.avatar.PhotoFaceSheet.pathOf(faceSheet?.value) ?: return
        val inUse = avatarForPlay.faceSheetRef == faceSheet?.value ||
            completedLives.any { it.avatar.faceSheetRef == faceSheet?.value }
        if (!inUse) runCatching { java.io.File(path).delete() }
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

    /** 공유 URI 접근이 만료됐으면 사진 참조를 지우고 기본 외형으로 이어 간다. 이미 만든 얼굴 시트는 원본 사진 없이도 쓴다. */
    private fun withReadablePhoto(avatar: AvatarConfig): AvatarConfig {
        val ref = avatar.portraitRef ?: return avatar
        if (com.example.lifegame.ui.avatar.PhotoFaceSheet.pathOf(avatar.faceSheetRef) != null) return avatar
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
        // 자동 사건은 엔진의 automaticEventRecords에 따로 저장된다. 연도 이야기로 한 번 더 남기지 않는다(선택 · 이야기와 섞지 않음).
        if (step.phase == TurnPhase.AUTO_PROGRESS && step.automaticEvent == null && !step.recordText.isNullOrBlank()) {
            val recordId = "${step.state.runId}:year:${step.state.period}:${step.state.yearInPeriod}:${step.state.moments.size}"
            narrativeRecords = narrativeRecords.appendOnce(
                RunNarrativeRecord(
                    recordId = recordId,
                    kind = RunNarrativeKind.YEARLY_STORY,
                    period = step.state.period,
                    yearInPeriod = step.state.yearInPeriod,
                    text = step.recordText,
                ),
            )
        }
        if (step.phase == TurnPhase.DECISION) {
            presentationForRun = presentationForRun.requestEventImage(step.pendingDecision?.eventId)
        }
        publishPlay()
    }

    private fun publishPlay() {
        // 인생 리셋 중에는 지우는 중인 인생을 다시 저장하지 않는다(늦게 도착한 화면 콜백이 슬롯을 되살리지 않게).
        if (lifeResetPending) return
        val step = playStep ?: return
        val content = playContent ?: return
        val ending = if (step.phase == TurnPhase.ENDING) endingFor(step.state) else null
        if (step.phase == TurnPhase.ENDING && awardedTitleForRun == null) {
            awardedTitleForRun = resolveTitleFor(step.state)
        }
        refreshExtensionState(step.state)
        var completedForCheckpoint: CompletedLifeRecord? = null
        if (step.phase == TurnPhase.ENDING) {
            val existing = completedLives.firstOrNull { it.runId == step.state.runId }
                ?.takeIf { savedCompletedRunId == step.state.runId }
            val record = existing ?: completedRecordFor(step.state).also {
                completedForCheckpoint = it
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
            presentation = presentationForRun,
            outcomePeriod = pendingOutcomeSnapshot?.period?.takeIf { outcome != null },
            runChoices = runChoiceHistory.toList(),
        )
        val accepted = v4ProgressStore.save(
            V4SavedRun(
                playerName = playerNameForPlay,
                state = step.state,
                choices = runChoiceHistory.toList(),
                pendingOutcome = pendingOutcomeSnapshot,
                avatar = avatarForPlay,
                awardedTitle = awardedTitleForRun,
                phase = step.phase,
                presentation = presentationForRun,
                narrativeRecords = narrativeRecords,
                startingRollRaw = startingRollRawForRun,
                startingDiceRule = startingDiceRuleForRun.name,
                rerollsRemaining = rerollsRemainingForRun,
                extensionState = extensionStateForRun,
            ),
            completed = completedForCheckpoint,
        )
        completedForCheckpoint?.takeIf { accepted }?.let {
            savedCompletedRunId = it.runId
            completedLives = v4ProgressStore.loadCompletedLives()
        }
    }

    private fun refreshExtensionState(state: PeriodRunState) {
        val goal = awardedTitleForRun?.let { awarded ->
            scenarioExtensions.titleGoals[awarded.titleId]?.let {
                TitleGoalState(it.titleId, it.goal, it.hint, completed = false)
            }
        } ?: extensionStateForRun.titleGoal
        extensionStateForRun = RunExtensionApi.snapshot(
            choices = runChoiceHistory,
            recallCandidateEventIds = scenarioExtensions.recallSlots.map { it.sourceEventId },
            catalystCandidateEventIds = scenarioExtensions.turningPoints.values.flatten().map { it.eventId },
            relationshipFlagCatalog = scenarioExtensions.npcs.flatMap { it.relationFlags }.toSet(),
            currentFlags = state.flags,
            titleGoal = goal,
            challengeCode = extensionStateForRun.challengeCode,
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
        narrativeRecords = emptyList()
        startingRollRawForRun = null
        startingDiceRuleForRun = DiceRule.LEGACY_LG1
        rerollsRemainingForRun = 0
        extensionStateForRun = RunExtensionState()
        presentationForRun = PresentationState()
        profilePhotoPresentation = PresentationState()
        choiceApplicationGuard.clear()
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

    init {
        viewModelScope.launch {
            saveState.collect { state ->
                saveWarning = if (state is SaveState.Failed) {
                    SAVE_ERROR_MESSAGE
                } else null
            }
        }
        // Warm the immutable asset bundle off the UI thread. Immediate play requests share the
        // same cache and still receive an explicit failure rather than silently empty content.
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.loadPlayableContent() }
            contentWarning = if (result is ContentLoadResult.Failed) CONTENT_ERROR_MESSAGE else null
        }
    }

    private companion object {
        const val CHARACTERS_FILE = "characters.sample.json"
        const val LIFE_FACTS_FILE = "life_facts.sample.json"
        const val SCENARIO_EXTENSIONS_FILE = "scenario_extensions.sample.json"
        const val AUTO_EVENTS_FILE = "auto_events.sample.json"
        const val UI_PREFS_NAME = "lifegame_ui_prefs"
        const val TITLE_TARGET_KEY = "title_target"
        const val YEARLY_STORIES_FILE = "yearly_stories.sample.json"
        /** 실제 이미지 생성은 30초~1분 넘게 걸릴 수 있다. 이보다 길면 끊고 기본 모습을 권한다. */
        const val REAL_FACE_TIMEOUT_MS = 150_000L
        const val MAX_SKIP_YEARS = 200
        const val CONTENT_ERROR_MESSAGE = "게임 데이터를 불러오지 못했습니다. 저장된 인생은 보존되어 있습니다."
        const val LIFE_RESET_ERROR_MESSAGE = "인생을 정리하지 못했어요. 진행 중인 인생은 그대로 있어요."
        const val SAVE_ERROR_MESSAGE = "진행 상황을 기기에 저장하지 못했습니다. 공간을 확인한 뒤 다시 저장해 주세요."
    }
}
