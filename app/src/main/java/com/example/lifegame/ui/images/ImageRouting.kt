package com.example.lifegame.ui.images

import com.example.lifegame.domain.model.LifePeriod
import org.json.JSONObject

/** Paths are relative to assets/images; no game rules or stat-based inference live here. */
data class ImageCandidate(
    val imageId: String,
    val path: String,
    val usage: String = "scene",
    val fallbackText: String = "이미지를 준비 중입니다",
)

data class ImageRouting(
    val assets: Map<String, ImageCandidate> = emptyMap(),
    val periodPortraits: Map<LifePeriod, String> = emptyMap(),
    val statScenes: Map<String, String> = emptyMap(),
    val focusStats: Map<String, String> = emptyMap(),
    val eventStatTags: Map<String, List<String>> = emptyMap(),
) {
    fun portrait(period: LifePeriod?): List<ImageCandidate> =
        listOfNotNull(periodPortraits[period]?.let(assets::get))

    /** The loader tries every candidate, including when a present file cannot decode. */
    fun candidates(
        preferredImageId: String?,
        focusId: String?,
        period: LifePeriod?,
        legacyFiles: Map<String, String> = emptyMap(),
    ): List<ImageCandidate> = buildList {
        preferredImageId?.takeIf(String::isNotBlank)?.let { id ->
            val exact = assets[id] ?: legacyFiles[id]?.takeIf(::isSafeImagePath)
                ?.let { ImageCandidate(id, it) }
            exact?.let(::add)
        }
        focusStats[focusId]?.let(statScenes::get)?.let(assets::get)?.let(::add)
        addAll(portrait(period))
    }.distinctBy { it.path }

    /** Event art is preferred. A focus scene is used only when that event shares its tag. */
    fun eventCandidates(
        eventId: String,
        preferredImageId: String?,
        focusId: String?,
        period: LifePeriod?,
        legacyFiles: Map<String, String> = emptyMap(),
    ): List<ImageCandidate> {
        val tags = eventStatTags[eventId].orEmpty()
        val tag = focusStats[focusId]?.takeIf { it in tags } ?: tags.firstOrNull()
        return buildList {
            // Reuse the exact-art lookup, without its unconditional focus fallback.
            addAll(candidates(preferredImageId, null, null, legacyFiles))
            tag?.let(statScenes::get)?.let(assets::get)?.let(::add)
            addAll(portrait(period))
        }.distinctBy { it.path }
    }

    companion object {
        fun parse(json: String): ImageRouting {
            val root = JSONObject(json)
            require(root.getInt("schemaVersion") == 1) { "Unsupported image routing version" }
            val items = root.getJSONArray("assets")
            val assets = linkedMapOf<String, ImageCandidate>()
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val id = item.getString("imageId")
                val path = item.getString("assetPath")
                require(isSafeImagePath(path)) { "Invalid image path: $path" }
                require(id !in assets) { "Duplicate routed image: $id" }
                assets[id] = ImageCandidate(id, path, item.getString("usage"), item.getString("fallbackText"))
            }
            fun stringMap(key: String): Map<String, String> {
                val obj = root.getJSONObject(key)
                return obj.keys().asSequence().associateWith { obj.getString(it) }
            }
            val portraits = stringMap("periodPortraits").mapKeys { LifePeriod.valueOf(it.key) }
            val scenes = stringMap("statScenes")
            val focuses = stringMap("focusStats")
            require((portraits.values + scenes.values).all { it in assets }) { "Unregistered generic image" }
            require(focuses.values.all { it in scenes }) { "Unknown focus stat tag" }
            val eventTags = root.optJSONObject("eventStatTags")?.let { obj ->
                obj.keys().asSequence().associateWith { id ->
                    val tags = obj.getJSONArray(id)
                    List(tags.length()) { tags.getString(it) }.also { values ->
                        require(values.isNotEmpty() && values.distinct().size == values.size &&
                            values.all { it in scenes }) { "Invalid event image stat tags: $id" }
                    }
                }
            }.orEmpty()
            return ImageRouting(assets, portraits, scenes, focuses, eventTags)
        }
    }
}

fun isSafeImagePath(path: String): Boolean =
    path.endsWith(".png") && !path.startsWith("/") && '\\' !in path && ':' !in path &&
        path.split('/').all { it.isNotBlank() && it != "." && it != ".." }
