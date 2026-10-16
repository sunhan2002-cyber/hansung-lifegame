package com.example.lifegame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.images.rememberPixelAsset
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGamePicture

/** Callers give this a 4:3 box; the whole subject remains visible, including portraits. */
@Composable
fun PixelAssetImage(candidates: List<ImageCandidate>, description: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.background(LifeGamePicture), contentAlignment = Alignment.Center) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val height = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        val loaded = rememberPixelAsset(candidates, width, height)
        if (loaded == null) {
            Text(candidates.lastOrNull()?.fallbackText ?: "이미지를 준비 중입니다", color = LifeGameInkSecondary)
        } else {
            Image(
                painter = BitmapPainter(loaded.bitmap.asImageBitmap(), filterQuality = FilterQuality.None),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
