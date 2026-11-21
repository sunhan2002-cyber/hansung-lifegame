package com.example.lifegame.ui.images

import org.json.JSONObject
import kotlin.math.max

/** A presentation adjustment never changes the source artwork or its approval status. */
data class SceneFocalPoint(val x: Float = .5f, val y: Float = .5f) {
    init { require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) }
}

data class CoverPlacement(val left: Float, val top: Float, val width: Float, val height: Float)

/** Keep the focal point near the viewport centre, clamped so no empty edge is exposed. */
fun coverPlacement(sourceWidth: Int, sourceHeight: Int, width: Int, height: Int, focal: SceneFocalPoint = SceneFocalPoint()): CoverPlacement {
    require(sourceWidth > 0 && sourceHeight > 0 && width > 0 && height > 0)
    val scale = max(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
    val scaledWidth = max(width.toFloat(), sourceWidth * scale)
    val scaledHeight = max(height.toFloat(), sourceHeight * scale)
    return CoverPlacement(
        (width / 2f - focal.x * scaledWidth).coerceIn(width - scaledWidth, 0f),
        (height / 2f - focal.y * scaledHeight).coerceIn(height - scaledHeight, 0f),
        scaledWidth, scaledHeight,
    )
}

data class PortraitVariant(
    val assetPath: String,
    val sha256: String,
    val width: Int,
    val height: Int,
    val focal: SceneFocalPoint = SceneFocalPoint(),
)

data class ScenePresentationEntry(
    val sourceSha256: String,
    val focal: SceneFocalPoint,
    val portraitVariant: PortraitVariant? = null,
)

class ScenePresentation private constructor(private val entries: Map<String, ScenePresentationEntry>) {
    fun entry(path: String): ScenePresentationEntry? = entries[path]
    fun focal(path: String, actualSha256: String): SceneFocalPoint =
        entries[path]?.takeIf { it.sourceSha256.equals(actualSha256, ignoreCase = true) }?.focal ?: SceneFocalPoint()

    /** A variant is presentation-only; it never replaces the legacy source or its approval. */
    fun portraitVariant(path: String, actualSourceSha256: String, width: Int, height: Int): PortraitVariant? =
        entries[path]?.takeIf {
            width > 0 && height > width * 1.35 &&
                it.sourceSha256.equals(actualSourceSha256, ignoreCase = true)
        }?.portraitVariant

    companion object {
        val EMPTY = ScenePresentation(emptyMap())

        /** Optional metadata must fail closed to the original central crop. */
        fun parse(text: String): ScenePresentation = runCatching {
            val root = JSONObject(text)
            require(root.getInt("schemaVersion") == 1)
            val items = root.getJSONArray("images")
            val entries = linkedMapOf<String, ScenePresentationEntry>()
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val path = item.getString("assetPath")
                val hash = item.getString("sourceSha256")
                require(isSafeImagePath(path) && path !in entries && hash.matches(Regex("[a-fA-F0-9]{64}")))
                val variant = item.optJSONObject("portraitVariant")?.let { value ->
                    // Invalid optional variants fall back to the source, including its valid focal point.
                    runCatching {
                        val variantPath = value.getString("assetPath")
                        val variantHash = value.getString("sha256")
                        val width = value.getInt("width")
                        val height = value.getInt("height")
                        require(isSafeImagePath(variantPath) && variantPath.startsWith("presentation/portraits/"))
                        require(variantHash.matches(Regex("[a-fA-F0-9]{64}")))
                        require(width in 1..4096 && height in 1..4096 && height > width * 1.35)
                        require(value.getString("reviewStatus") == "AI_REVIEWED_PRESENTATION")
                        require(value.getString("reviewerType") == "AI")
                        require(value.getString("reviewer").isNotBlank() && value.getString("reviewEvidence").isNotBlank())
                        PortraitVariant(variantPath, variantHash, width, height, SceneFocalPoint(
                            value.optDouble("focalX", .5).toFloat(), value.optDouble("focalY", .5).toFloat(),
                        ))
                    }.getOrNull()
                }
                entries[path] = ScenePresentationEntry(hash, SceneFocalPoint(item.getDouble("focalX").toFloat(), item.getDouble("focalY").toFloat()), variant)
            }
            ScenePresentation(entries)
        }.getOrDefault(EMPTY)
    }
}
