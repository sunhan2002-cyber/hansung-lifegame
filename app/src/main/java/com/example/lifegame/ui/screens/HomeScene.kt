package com.example.lifegame.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.LifeCardUi
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.RecapMomentUi
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.SceneImage
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.ImageRouting
import com.example.lifegame.ui.images.rememberPixelAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class HomeScene(val images: List<ImageCandidate>, val avatar: AvatarLook?, val moment: RecapMomentUi? = null)
internal val HomeSceneImageKey = SemanticsPropertyKey<String>("HomeSceneImage")

/** The lobby's artwork is dark beneath both system bars; other screens retain their own style. */
@Composable
internal fun HomeSystemBarAppearance() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.hostActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && previousStatus != null && previousNavigation != null) {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }
}

private fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.hostActivity()
    else -> null
}

@Composable
internal fun rememberHomeScene(card: LifeCardUi?, record: LifeRecordUi?): HomeScene {
    val context = LocalContext.current
    var routing by remember(context) { mutableStateOf(ImageRouting()) }
    LaunchedEffect(context) {
        routing = withContext(Dispatchers.IO) { ImageRoutingRepository(context).load() }
    }
    val period = card?.avatar?.period ?: LifePeriod.YOUNG_ADULT
    val ordinaryScene = routing.candidates(null, null, period).filterNot { it.usage == "character" }
    if (card != null) return HomeScene(ordinaryScene, card.avatar)
    if (record != null) {
        val moment = record.recap.lastOrNull { it.images.isNotEmpty() }
        if (moment != null) return HomeScene(moment.images, moment.avatar, moment)
        if (record.backdrop.isNotEmpty()) return HomeScene(record.backdrop, record.avatar)
    }
    // A generic preview never invents a completed life or a player's identity.
    return HomeScene(ordinaryScene, AvatarLook(period = LifePeriod.YOUNG_ADULT))
}

@Composable
internal fun HomeSceneBackground(scene: HomeScene, phase: State<Float>, modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds().testTag("home_scene").semantics {
        this[HomeSceneImageKey] = scene.images.firstOrNull()?.imageId.orEmpty()
    }) {
        SceneImage(FilmShot("home-${scene.images.firstOrNull()?.imageId}", scene.images.firstOrNull()?.imageId.orEmpty(),
            scene.images.firstOrNull()?.path, false, scene.images),
            Modifier.fillMaxSize().graphicsLayer {
                val angle = phase.value * 2f * PI.toFloat()
                scaleX = 1.035f
                scaleY = 1.035f
                translationX = sin(angle) * size.width * 0.009f
                translationY = (cos(angle) - 1f) * size.height * 0.003f
            })
        Canvas(Modifier.fillMaxSize()) {
            val angle = phase.value * 2f * PI.toFloat()
            drawRect(Brush.radialGradient(listOf(Color(0xFFFFDEA0).copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * (0.82f + sin(angle) * 0.025f), size.height * 0.16f), radius = size.maxDimension * 0.65f))
            // Fixed visual seeds only; the game's random stream is never consumed here.
            repeat(12) { index ->
                val progress = (phase.value + index * 0.0833f) % 1f
                val x = ((index * 0.217f) % 1f) * size.width + sin(angle + index) * 5.dp.toPx()
                val y = size.height * (0.72f - progress * 0.65f)
                val alpha = sin(progress * PI.toFloat()).coerceAtLeast(0f) * 0.32f
                drawCircle(Color(0xFFFFE7B7).copy(alpha = alpha), (0.8f + index % 3 * 0.3f).dp.toPx(), Offset(x, y))
            }
        }
    }
}

@Composable
internal fun HomeSceneAvatar(scene: HomeScene, phase: State<Float>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        val density = LocalDensity.current
        val needsViewpointResolution = scene.images.any { it.usage == "pov" || scene.moment?.isFirstPerson(it) == true }
        val shown = if (needsViewpointResolution) {
            rememberPixelAsset(scene.images, with(density) { maxWidth.roundToPx() }, with(density) { maxHeight.roundToPx() })?.candidate
        } else scene.images.firstOrNull()
        // Resolve the actual fallback before compositing, keeping authored first-person viewpoints intact.
        val showAvatar = scene.avatar != null && (scene.moment == null || (shown != null && !scene.moment.isFirstPerson(shown))) && shown?.usage != "pov"
        if (showAvatar) {
            Canvas(Modifier.fillMaxSize()) {
                val breath = sin(phase.value * 2f * PI.toFloat())
                val width = size.width * (0.29f + breath * 0.004f)
                drawOval(Color.Black.copy(alpha = 0.24f), Offset((size.width - width) / 2f, size.height - 15.dp.toPx()), Size(width, 12.dp.toPx()))
            }
            Box(Modifier.padding(bottom = 10.dp).height(maxHeight * 0.86f).aspectRatio(0.62f)
                .testTag("home_avatar").graphicsLayer {
                    val breath = sin(phase.value * 2f * PI.toFloat())
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = 1f + breath * 0.009f
                    translationY = -breath * 0.7.dp.toPx()
                }) { PlayerAvatar(requireNotNull(scene.avatar), Modifier.fillMaxSize(), ambientPhase = phase) }
        }
    }
}
