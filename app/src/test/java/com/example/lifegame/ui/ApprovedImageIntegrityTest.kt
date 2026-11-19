package com.example.lifegame.ui

import com.example.lifegame.ui.preview.Week7ImageSamples
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이미지 크기 · 지문 검증(2026-11-16 프론트 마무리 과제 1).
 *
 * `Week7ImageSamplePreviewTest`가 모든 샘플을 1448×1086으로 기대해 실패했다. main `e16faf5`가 승인되지 않은 그림을
 * 720×540으로 시연용 재압축했고, `083d7ed`가 승인 그림만 원본으로 되돌렸기 때문이다. 크기 기준을 낮추지 않고,
 * 그림마다 "있어야 할 정확한 크기"를 근거 파일에서 가져와 비교한다.
 * - 승인 그림: 승인 manifest의 SHA-256과 바이트가 같고(재압축 금지) 등록 크기와 같아야 한다(SHA · manifest는 읽기만 한다).
 * - 등록 그림: `image_ids.sample.json`에 적힌 크기와 실제 PNG 크기가 같아야 한다(`scripts/validate_images.py`와 같은 규칙).
 * - 미리보기 샘플: 위 기준으로 하나도 빠짐없이 정확한 크기여야 한다. 등록되지 않은 컷신은 승인 원본 크기여야 한다.
 */
class ApprovedImageIntegrityTest {
    private val images = File("src/main/assets/images")
    private val docs = File("../docs/assignments")
    private val registry: Map<String, Pair<Int, Int>> = JSONArray(File("src/main/assets/image_ids.sample.json").readText()).let { array ->
        List(array.length()) { array.getJSONObject(it) }
            .filter { it.has("width") && it.has("height") }
            .associate { it.getString("fileName") to (it.getInt("width") to it.getInt("height")) }
    }
    private val filesByName: Map<String, File> by lazy {
        val byName = images.walkTopDown().filter { it.isFile && it.extension == "png" }.associateBy { it.name }.toMutableMap()
        // Archived art may share a filename with its replacement. Validate the file actually routed in the app.
        val routed = JSONObject(File("src/main/assets/image_routing.json").readText()).getJSONArray("assets")
        repeat(routed.length()) { index ->
            val entry = routed.getJSONObject(index)
            val file = File(images, entry.getString("assetPath"))
            if (file.isFile) byName[file.name] = file
        }
        byName
    }

    private fun size(file: File): Pair<Int, Int> {
        val header = file.inputStream().use { it.readNBytes(24) }
        val signature = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        assertTrue("${file.name} is not a PNG", header.copyOfRange(0, 4).contentEquals(signature))
        fun int(at: Int) = ((header[at].toInt() and 0xff) shl 24) or ((header[at + 1].toInt() and 0xff) shl 16) or
            ((header[at + 2].toInt() and 0xff) shl 8) or (header[at + 3].toInt() and 0xff)
        return int(16) to int(20)
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun `approved group images keep the approved bytes and their registered size`() {
        val manifest = JSONArray(File(docs, "20261031/이미지그룹_승인manifest.json").readText())
        assertTrue("approved manifest must retain the original approved set", manifest.length() >= 15)
        repeat(manifest.length()) { index ->
            val entry = manifest.getJSONObject(index)
            assertEquals("APPROVED", entry.getString("approvalStatus"))
            val file = File(images, entry.getString("assetPath"))
            assertTrue(file.path, file.isFile)
            // 승인 그림은 재압축하지 않은 승인 바이트 그대로다(크기는 그림마다 다르다: 1448×1086 · 1280×960 · 300×225).
            assertEquals(entry.getString("imageId"), entry.getString("sha256"), sha256(file))
            registry[file.name]?.let { assertEquals("registered size of ${file.name}", it, size(file)) }
            assertTrue(file.name, size(file).let { (w, h) -> w * 3 == h * 4 })
        }
    }

    @Test
    fun `doljabi cutscene images keep their approved bytes and original size`() {
        val grasp = JSONObject(File(docs, "20261113/submissions/정원률_돌잡이_manifest.json").readText())
        val graspFile = File(images, grasp.getString("assetPath"))
        assertEquals(grasp.getString("sha256"), sha256(graspFile))
        assertEquals(grasp.getInt("width") to grasp.getInt("height"), size(graspFile))
        assertEquals(ORIGINAL, size(graspFile))
        val intro = File(images, grasp.getString("sourceAssetPath"))
        assertEquals(grasp.getString("preservedSourceSha256"), sha256(intro))
        assertEquals(ORIGINAL, size(intro))
    }

    @Test
    fun `every registered image has exactly its registered size`() {
        // 등록만 되고 아직 파일이 없는 자리(대체 문구로 표시)는 validate_images.py처럼 건너뛰고, 실제 파일은 모두 비교한다.
        val packaged = registry.filterKeys(filesByName::containsKey)
        assertTrue(packaged.size >= 380)
        val mismatches = packaged.mapNotNull { (name, expected) ->
            size(filesByName.getValue(name)).takeIf { it != expected }
                ?.let { "$name ${it.first}x${it.second} != ${expected.first}x${expected.second}" }
        }
        assertTrue(mismatches.take(10).joinToString(), mismatches.isEmpty())
        // 등록 크기는 모두 4:3이다.
        assertTrue(registry.values.all { (w, h) -> w * 3 == h * 4 })
    }

    @Test
    fun `every design preview sample has its exact expected size`() {
        assertEquals(80, Week7ImageSamples.size)
        val counts = Week7ImageSamples.map { sample ->
            val file = File(images, sample.imageFile)
            assertTrue(sample.imageFile, file.isFile)
            val expected = expectedSampleSize(sample.imageFile, registry)
            assertEquals(sample.imageFile, expected, size(file))
            expected
        }.groupingBy { it }.eachCount()
        assertEquals(Week7ImageSamples.size, counts.values.sum())
        assertTrue("all preview samples must match their registered or original dimensions", counts.keys.all { it == ORIGINAL || it == PACKAGED })
    }

    companion object {
        val ORIGINAL = 1448 to 1086
        val PACKAGED = 720 to 540

        /** 미리보기 샘플이 가져야 할 크기: 등록 크기, 등록되지 않은 컷신 · 디자인 원본은 승인 원본 크기. */
        fun expectedSampleSize(imageFile: String, registry: Map<String, Pair<Int, Int>>): Pair<Int, Int> =
            registry[imageFile.substringAfterLast('/')] ?: ORIGINAL
    }
}
