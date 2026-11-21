package com.example.lifegame.debug

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.StartingRoll
import com.example.lifegame.ui.screens.DicePanel
import com.example.lifegame.ui.theme.LifeGamePaper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.domain.model.EventImageExposureStatus
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.PlayContent
import com.example.lifegame.ui.PlayPhase
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.restoredDecisionUi
import com.example.lifegame.ui.shortName
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_DICE = "3,4,2,5,6,1,1,4,5,4,2,2"

/** Debug APK only. No current/completed life store, network request, seed or reward is touched. */
class ExperiencePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = intent.getStringExtra("scene") ?: "home"
        if (requested !in setOf("home", "dice", "teen_024", "adult_072", "adult_067")) { finish(); return }
        val motion = intent.getBooleanExtra("motion", true)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false)) {
                    if (requested == "home") HomeScreen(null, emptyList(), {}, {})
                    else if (requested == "dice") {
                        // 2026-11-20: 고정 굴림(경제력 1 + 4 포함)으로 프로필 주사위 패널만 측정한다. 저장 · 난수는 건드리지 않는다.
                        Box(Modifier.fillMaxSize().background(LifeGamePaper).verticalScroll(rememberScrollState()).padding(16.dp)) {
                            DicePanel(
                                roll = StartingRoll.decode(PREVIEW_DICE),
                                rollNonce = 1,
                                tendency = null,
                                rerollsLeft = 2,
                                onRoll = {},
                            )
                        }
                    }
                    else {
                        var state by remember { mutableStateOf<PeriodPlayUiState?>(null) }
                        LaunchedEffect(requested) { state = withContext(Dispatchers.IO) { scene(requested) } }
                        state?.let { DecisionScreen(it, onChoose = {}, onContinue = {}) }
                    }
                }
            }
        }
    }

    private fun scene(eventId: String): PeriodPlayUiState {
        val repository = ContentRepository(this)
        val events = repository.loadDecisionEvents()
        val periods = repository.loadPeriodDefinitions()
        val event = events.first { it.eventId == eventId }
        val period = event.periods.minOf { it }
        val definition = periods.first { it.period == period }
        val content = PlayContent(periods = periods, focuses = emptyList(), decisions = events.associateBy { it.eventId },
            imageFiles = emptyMap(), imageRouting = ImageRoutingRepository(this).load())
        val decision = restoredDecisionUi(event, content, null, period).copy(
            text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
            choices = event.choices.take(3).mapIndexed { index, choice -> DecisionChoiceUi(choice.choiceId, ('A' + index).toString(), choice.label, hint = "") },
        )
        return PeriodPlayUiState(
            runKey = "read-only-preview", playerName = "재겸", phase = PlayPhase.DECISION,
            periodName = definition.displayName, chapterLabel = "제${definition.order}장", periodYears = definition.years,
            periodIntro = "", yearInPeriod = 1, periodMarks = periods.sortedBy { it.order }.map { PeriodMarkUi(it.shortName(), 0f, it.period == period) },
            previousPeriodName = null, stats = Stats(health = 72), focus = null, focusOptions = emptyList(), sceneImageId = "", sceneFile = null,
            log = emptyList(), yearKey = eventId, decision = decision, outcome = null, isDead = false, autoPlaying = false, speed = 1,
            presentation = PresentationState(exposedEventId = eventId, eventImageExposure = EventImageExposureStatus.SHOWN),
        )
    }
}
