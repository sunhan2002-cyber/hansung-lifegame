package com.example.lifegame.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LoadedPixelAsset(val candidate: ImageCandidate, val bitmap: Bitmap)

/** Bounded decoded-image cache; packaged source PNGs are never resized or overwritten. */
object PixelAssetLoader {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun load(context: Context, candidates: List<ImageCandidate>, width: Int, height: Int): LoadedPixelAsset? {
        val targetWidth = width.coerceIn(1, 2048)
        val targetHeight = height.coerceIn(1, 2048)
        for (candidate in candidates) {
            if (!isSafeImagePath(candidate.path)) continue
            val key = "${candidate.path}:$targetWidth:$targetHeight"
            val cached = cache.get(key)
            if (cached != null) return LoadedPixelAsset(candidate, cached)
            val bitmap = runCatching {
                val assetPath = "images/${candidate.path}"
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth &&
                    bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2
                // Also bound decoding of unusually large originals before drawing into a view.
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
            if (bitmap != null) {
                cache.put(key, bitmap)
                return LoadedPixelAsset(candidate, bitmap)
            }
        }
        return null
    }
}

@Composable
fun rememberPixelAsset(candidates: List<ImageCandidate>, width: Int, height: Int): LoadedPixelAsset? {
    val context = LocalContext.current
    var loaded by remember(candidates, width, height) { mutableStateOf<LoadedPixelAsset?>(null) }
    LaunchedEffect(candidates, width, height) {
        loaded = withContext(Dispatchers.IO) { PixelAssetLoader.load(context, candidates, width, height) }
    }
    return loaded
}
