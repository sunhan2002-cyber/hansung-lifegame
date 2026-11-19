package com.example.lifegame.data

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class CachedContentLoaderTest {
    @Test
    fun sharedTextIsReadOnceAndTypedViewsCannotCollide() {
        var reads = 0
        var parses = 0
        val loader = CachedContentLoader { reads++; "abc" }
        val text = ContentAsset("asset.json", { raw: String -> parses++; raw })
        val length = ContentAsset("asset.json", String::length)
        val first = loader.load(text) as ContentLoadResult.Ready
        assertSame(first, loader.load(text))
        assertEquals("abc", first.value)
        assertEquals(3, loader.load(length).valueOrNull())
        assertEquals(1, reads)
        assertEquals(1, parses)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", first.fingerprints["asset.json"])
    }

    @Test
    fun readParseAndValidationFailuresRemainDistinctAndDoNotLeakText() {
        val missing = CachedContentLoader { throw IOException("private contents") }
            .load(ContentAsset("missing.json", String::length)) as ContentLoadResult.Failed
        val malformed = CachedContentLoader { "private contents" }
            .load(ContentAsset("broken.json", { _: String -> error("private contents") })) as ContentLoadResult.Failed
        val invalid = CachedContentLoader { "[]" }
            .load(ContentAsset("empty.json", { _: String -> emptyList<String>() }, { "EMPTY_REQUIRED_LIST" })) as ContentLoadResult.Failed
        assertEquals(ContentFailureKind.READ, missing.issues.single().kind)
        assertNull(missing.issues.single().sha256)
        assertEquals(ContentFailureKind.PARSE, malformed.issues.single().kind)
        assertNotNull(malformed.issues.single().sha256)
        assertEquals(ContentFailureKind.VALIDATION, invalid.issues.single().kind)
        assertFalse(listOf(missing, malformed, invalid).toString().contains("private contents"))
    }

    @Test
    fun failedImmutableAssetIsNotRepeatedlyReadOrParsed() {
        var reads = 0
        val loader = CachedContentLoader { reads++; "broken" }
        val asset = ContentAsset("bad.json", { raw: String -> raw.toInt() })
        val result = loader.load(asset)
        repeat(8) { assertSame(result, loader.load(asset)) }
        assertEquals(1, reads)
        assertEquals(1, loader.issues.size)
    }

    private fun repository(overrides: Map<String, String> = emptyMap(), reads: MutableMap<String, Int> = mutableMapOf()) =
        ContentRepository(CachedContentLoader { name ->
            reads[name] = (reads[name] ?: 0) + 1
            overrides[name] ?: File("src/main/assets", name).readText()
        })

    @Test
    fun actualPlayableBundleIsValidatedAndStableAcrossStartAndRecordReads() {
        val reads = mutableMapOf<String, Int>()
        val repository = repository(reads = reads)
        val first = repository.loadPlayableContent() as ContentLoadResult.Ready
        assertEquals(372, first.value.content.decisions.size)
        assertEquals(66, first.value.endings.size)
        assertTrue(first.value.automaticEventRules.isNotEmpty())
        repeat(5) {
            assertSame(first, repository.loadPlayableContent())
            repository.loadV4Content()
            repository.loadEndings()
            repository.loadAutomaticEventRules()
        }
        assertTrue(reads.values.all { it == 1 })
        assertEquals(reads.keys, first.fingerprints.keys)
        assertTrue(repository.issues.isEmpty())
    }

    @Test
    fun mandatoryCorruptAndEmptyContentCannotBecomeAPlayableBundle() {
        for ((file, replacement) in listOf("events.sample.json" to "broken", "periods.sample.json" to "[]", "endings.sample.json" to "[]")) {
            val repository = repository(mapOf(file to replacement))
            val result = repository.loadPlayableContent() as ContentLoadResult.Failed
            assertTrue(result.issues.any { it.asset == file })
            // Compatibility display APIs remain safe while entry points receive an explicit failure.
            repository.loadV4Content()
            repository.loadEndings()
        }
    }

    @Test
    fun optionalImageManifestFailureDoesNotBlockTextPlayButIsObservable() {
        val repository = repository(mapOf("image_ids.sample.json" to "broken"))
        assertTrue(repository.loadPlayableContent() is ContentLoadResult.Ready)
        assertTrue(repository.loadImageAssets().isEmpty())
        assertTrue(repository.issues.any { it.asset == "image_ids.sample.json" })
    }
}
