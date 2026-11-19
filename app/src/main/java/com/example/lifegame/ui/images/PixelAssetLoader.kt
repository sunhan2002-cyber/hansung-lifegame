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
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class LoadedPixelAsset(val candidate: ImageCandidate, val bitmap: Bitmap, val focal: SceneFocalPoint = SceneFocalPoint())

/** Bounded decoded-image cache; packaged source PNGs are never resized or overwritten. */
object PixelAssetLoader {
    @Volatile private var presentation: ScenePresentation? = null
    private val sourceHashes = ConcurrentHashMap<String, String>()
    // Called on the IO decoding path. Packaged assets are immutable during this process.
    private fun metadata(context: Context): ScenePresentation = presentation ?: synchronized(this) {
        presentation ?: runCatching {
            context.assets.open("image_presentation.json").bufferedReader().use { ScenePresentation.parse(it.readText()) }
        }.getOrDefault(ScenePresentation.EMPTY).also { presentation = it }
    }

    private fun assetHash(context: Context, path: String): String = sourceHashes.getOrPut(path) {
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open("images/$path").use { input ->
                val bytes = ByteArray(8192)
                while (true) {
                    val count = input.read(bytes)
                    if (count < 0) break
                    digest.update(bytes, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun load(
        context: Context,
        candidates: List<ImageCandidate>,
        width: Int,
        height: Int,
        presentationOverride: ScenePresentation? = null,
    ): LoadedPixelAsset? {
        val targetWidth = width.coerceIn(1, 2048)
        val targetHeight = height.coerceIn(1, 2048)
        val metadata = presentationOverride ?: metadata(context)
        for (candidate in candidates) {
            if (!isSafeImagePath(candidate.path)) continue
            val sourceHash = if (metadata.entry(candidate.path) != null) assetHash(context, candidate.path) else ""
            val variant = metadata.portraitVariant(candidate.path, sourceHash, width, height)
                ?.takeIf { it.sha256.equals(assetHash(context, it.assetPath), ignoreCase = true) }
            // A failed variant never skips the original or changes event identity.
            val attempts = listOfNotNull(variant?.let { candidate.copy(path = it.assetPath) }, candidate)
            for (actual in attempts) {
                val isVariant = actual.path != candidate.path
                val expectedSize = if (isVariant) "${variant?.width}x${variant?.height}" else "source"
                val key = "${actual.path}:$targetWidth:$targetHeight:$expectedSize"
                val focal = if (isVariant) requireNotNull(variant).focal else metadata.focal(candidate.path, sourceHash)
                val cached = cache.get(key)
                if (cached != null) {
                    return LoadedPixelAsset(actual, cached, focal)
                }
                val bitmap = runCatching {
                    val assetPath = "images/${actual.path}"
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                    if (isVariant && (variant == null || bounds.outWidth != variant.width || bounds.outHeight != variant.height)) {
                        return@runCatching null
                    }
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= targetWidth &&
                        bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2
                    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
                    context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
                }.getOrNull()
                if (bitmap != null) {
                    cache.put(key, bitmap)
                    return LoadedPixelAsset(actual, bitmap, focal)
                }
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
