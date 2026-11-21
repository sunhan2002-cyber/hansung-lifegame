package com.example.lifegame.data

import java.security.MessageDigest

enum class ContentFailureKind { READ, PARSE, VALIDATION }

/** Diagnostics identify the asset and failure class; authored text and player data are not logged. */
data class ContentIssue(
    val asset: String,
    val kind: ContentFailureKind,
    val code: String,
    val sha256: String? = null,
)

sealed interface ContentLoadResult<out T> {
    data class Ready<T>(val value: T, val fingerprints: Map<String, String>) : ContentLoadResult<T>
    data class Failed(val issues: List<ContentIssue>) : ContentLoadResult<Nothing>
}

fun <T> ContentLoadResult<T>.valueOrNull(): T? = (this as? ContentLoadResult.Ready<T>)?.value

/** Identity is intentional: the same JSON can have different typed views without cache collisions. */
class ContentAsset<T>(
    val fileName: String,
    val parse: (String) -> T,
    val validate: (T) -> String? = { null },
)

/** One immutable APK asset read, one parse per typed view, including failures. */
class CachedContentLoader(private val readText: (String) -> String) {
    private data class TextAsset(val text: String, val hash: String)
    private val texts = mutableMapOf<String, ContentLoadResult<TextAsset>>()
    private val parsed = mutableMapOf<ContentAsset<*>, ContentLoadResult<*>>()
    private val observedIssues = linkedSetOf<ContentIssue>()

    val issues: List<ContentIssue> @Synchronized get() = observedIssues.toList()
    val fingerprints: Map<String, String>
        @Synchronized get() = texts.mapNotNull { (name, result) ->
            result.valueOrNull()?.let { name to it.hash }
        }.toMap()

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T> load(asset: ContentAsset<T>): ContentLoadResult<T> {
        parsed[asset]?.let { return it as ContentLoadResult<T> }
        val text = texts.getOrPut(asset.fileName) {
            try {
                val value = readText(asset.fileName)
                val hash = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                ContentLoadResult.Ready(TextAsset(value, hash), mapOf(asset.fileName to hash))
            } catch (_: Exception) {
                ContentLoadResult.Failed(listOf(ContentIssue(asset.fileName, ContentFailureKind.READ, "UNREADABLE_ASSET")))
            }
        }
        val result = when (text) {
            is ContentLoadResult.Failed -> text
            is ContentLoadResult.Ready -> {
                try {
                    val value = asset.parse(text.value.text)
                    val invalid = asset.validate(value)
                    if (invalid == null) ContentLoadResult.Ready(value, text.fingerprints)
                    else ContentLoadResult.Failed(listOf(ContentIssue(asset.fileName, ContentFailureKind.VALIDATION, invalid, text.value.hash)))
                } catch (_: Exception) {
                    ContentLoadResult.Failed(listOf(ContentIssue(asset.fileName, ContentFailureKind.PARSE, "INVALID_ASSET", text.value.hash)))
                }
            }
        }
        parsed[asset] = result
        if (result is ContentLoadResult.Failed) observedIssues += result.issues
        return result
    }
}
