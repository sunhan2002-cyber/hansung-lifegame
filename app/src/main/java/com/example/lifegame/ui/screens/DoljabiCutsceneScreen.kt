package com.example.lifegame.ui.screens

import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.SceneImage
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameSerif
import kotlinx.coroutines.delay

/** 돌잡이 컷신 앞에 오는 첫 결정. 재생 조건은 연도가 아니라 이 사건 ID로 건다(신기훈 11/09 설계 4장). */
const val DOLJABI_EVENT_ID = "infant_007"

internal const val CUTSCENE_FADE_MILLIS = 200L
internal const val CUTSCENE_HOLD_MILLIS = 1_800L

/** 정원률 #75 새 그림: 여러 물건이 놓인 상 위에서 아기 손이 한 물건을 잡는 순간. */
private val DoljabiImage = ImageCandidate(
    imageId = "img_doljabi_grasp_001",
    path = "cutscenes/week11/img_doljabi_grasp_001.png",
    usage = "scene",
)

/** 컷신 그림 낭독 문구. 그림에 보이는 그대로(정원률 manifest의 accessibilityDescription)다. */
const val DOLJABI_IMAGE_DESCRIPTION =
    "첫 생일 돌상에 피아노 장난감과 지폐 한 장이 놓여 있고, 아기의 손이 판사봉을 잡는다. 뒤에서 가족들이 지켜본다."

/** 기다리는 결정이 돌잡이이고 컷신이 아직 끝나지 않았으면(재생 중 앱 종료 포함) 컷신을 보여 준다. */
fun shouldPlayDoljabiCutscene(eventId: String?, hasOutcome: Boolean, status: CutsceneStatus): Boolean =
    eventId == DOLJABI_EVENT_ID && !hasOutcome &&
        (status == CutsceneStatus.NOT_STARTED || status == CutsceneStatus.PLAYING)

/** 컷신을 끝낸(완료 · 건너뜀) 뒤의 돌잡이 결정은 같은 그림을 선노출로 한 번 더 보여 주지 않는다(설계 2장 4번). */
fun doljabiPreviewAlreadyShown(eventId: String?, status: CutsceneStatus): Boolean =
    eventId == DOLJABI_EVENT_ID && (status == CutsceneStatus.COMPLETED || status == CutsceneStatus.SKIPPED)

/** 컷신 캡션. 이름이 비면 이름 없이 쓴다. */
fun doljabiCaption(playerName: String): String =
    if (playerName.isBlank()) "오늘은 첫 생일이다." else "오늘은 ${playerName}의 첫 생일이다."

/**
 * 새 인생의 첫 생일 돌잡이 컷신(2026-11-13 김재겸 과제 5). 고정 1컷 · 캡션 1줄 · 2초(0.2초 나타남 + 1.8초 유지).
 * 그림은 4:3 전체를 보여 준다. 한 번 누르면 완료, "건너뛰기"(또는 뒤로가기)면 건너뜀.
 * 움직임 줄이기나 TalkBack이 켜져 있으면 저절로 넘기지 않고 "계속" 버튼을 둔다.
 * 연출 상태만 바꾸고 능력치 · 플래그 · 보상은 건드리지 않는다. 보상은 다음 돌잡이 선택에서 한 번 적용된다.
 */
@Composable
fun DoljabiCutsceneScreen(
    playerName: String,
    onStart: () -> Unit,
    onFinish: (CutsceneStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current
    val talkBack = remember {
        (context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
            ?.isTouchExplorationEnabled == true
    }
    val autoAdvance = !reduceMotion && !talkBack
    val currentFinish by rememberUpdatedState(onFinish)
    var finished by remember { mutableStateOf(false) }
    // 자동 끝 · 탭 · 건너뛰기가 겹쳐도 한 번만 알린다.
    val finish: (CutsceneStatus) -> Unit = { status ->
        if (!finished) {
            finished = true
            currentFinish(status)
        }
    }
    val fade = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) { onStart() }
    LaunchedEffect(Unit) {
        if (!reduceMotion) fade.animateTo(1f, tween(CUTSCENE_FADE_MILLIS.toInt()))
        if (autoAdvance) {
            delay(CUTSCENE_HOLD_MILLIS)
            finish(CutsceneStatus.COMPLETED)
        }
    }
    BackHandler { finish(CutsceneStatus.SKIPPED) }

    Box(
        modifier
            .fillMaxSize()
            .background(LifeGameInk)
            .testTag("doljabi_cutscene")
            .pointerInput(Unit) { detectTapGestures { finish(CutsceneStatus.COMPLETED) } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .alpha(fade.value),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SceneImage(
                shot = FilmShot(
                    key = "doljabi_cutscene",
                    imageId = DoljabiImage.imageId,
                    imageFile = DoljabiImage.path,
                    isSpecial = false,
                    candidates = listOf(DoljabiImage),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .semantics {
                        // 그림과 같은 정보를 읽는다(정원률 manifest accessibilityDescription). 예전 문구("그중 하나로 뻗는다")는
                        // 승인 전 그림(intro) 기준이라, 판사봉을 잡은 지금 그림과 달라 화면 낭독 사용자만 다른 장면을 들었다.
                        contentDescription = DOLJABI_IMAGE_DESCRIPTION
                    }
                    .testTag("doljabi_cutscene_image"),
                fitWholeImage = true,
            )
            Text(
                text = doljabiCaption(playerName),
                color = LifeGamePaper,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("doljabi_cutscene_caption"),
            )
            if (!autoAdvance) {
                InkButton(
                    text = "계속",
                    onClick = { finish(CutsceneStatus.COMPLETED) },
                    modifier = Modifier.testTag("doljabi_cutscene_continue"),
                )
            }
        }
        // 컷신을 가리지 않도록 오른쪽 위에 작은 글자 버튼으로 둔다(터치 영역은 48dp).
        Text(
            text = "건너뛰기",
            color = LifeGamePaper,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LifeGamePaper.copy(alpha = 0.14f))
                .clickable(role = Role.Button, onClick = { finish(CutsceneStatus.SKIPPED) })
                .heightIn(min = 48.dp)
                .padding(horizontal = 18.dp, vertical = 13.dp)
                .testTag("doljabi_cutscene_skip"),
        )
    }
}
