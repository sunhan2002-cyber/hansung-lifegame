package com.example.lifegame.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.example.lifegame.domain.generation.FaceSheetRequest
import com.example.lifegame.domain.generation.FutureLookRequest
import com.example.lifegame.domain.generation.GenerationClient
import com.example.lifegame.domain.generation.GenerationResult
import com.example.lifegame.domain.generation.RecapRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * 사진 → 얼굴 시트를 OpenAI 이미지 편집 API(POST /v1/images/edits)로 만든다 (2026-11-05 피드백 2번).
 *
 * - 사용자가 동의한 사진 한 장만 보낸다. 보내기 전에 긴 변 [MAX_PHOTO_SIDE]px JPEG로 줄인다(원본은 보내지 않는다).
 * - 함께 보내는 두 번째 그림은 게임 기본 캐릭터 머리 6개(화풍 · 칸 배치 참고용)다.
 * - 받은 PNG는 앱 내부 저장소(files/face_sheets)에만 저장하고 file:// 경로를 돌려준다. 원본 사진은 복사하지 않는다.
 * - 미래 모습 · 회고는 이 클라이언트가 만들지 않는다(기존 Fake 경로 유지).
 *
 * 키는 local.properties → BuildConfig로만 들어온다. 앱 안에 키가 들어가는 구조라 시연용이며,
 * 실제 배포라면 서버에서 키를 들고 대신 요청해야 한다.
 */
class OpenAiFaceSheetClient(
    context: Context,
    private val apiKey: String,
    private val model: String,
) : GenerationClient {
    private val app = context.applicationContext
    private val running = ConcurrentHashMap<String, HttpURLConnection>()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()

    override fun createFaceSheet(request: FaceSheetRequest): GenerationResult<String> {
        if (!request.consent) return GenerationResult.Failure("photo consent is required")
        cancelled.remove(request.runId)
        val photo = runCatching { readPhoto(Uri.parse(request.localPhotoRef)) }.getOrNull()
            ?: return GenerationResult.Failure("사진을 읽지 못했습니다.")
        val style = runCatching { app.assets.open(STYLE_REFERENCE).use { it.readBytes() } }.getOrNull()
        val parts = buildList {
            add(MultipartPart.Field("model", model))
            add(MultipartPart.Field("prompt", FACE_SHEET_PROMPT))
            add(MultipartPart.Field("size", "1536x1024"))
            add(MultipartPart.Field("quality", "medium"))
            add(MultipartPart.Field("background", "transparent"))
            add(MultipartPart.Field("output_format", "png"))
            add(MultipartPart.Field("n", "1"))
            add(MultipartPart.File("image[]", "photo.jpg", "image/jpeg", photo))
            if (style != null) add(MultipartPart.File("image[]", "style_reference.png", "image/png", style))
        }
        val boundary = "lifegame-${System.nanoTime()}"
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        running[request.runId] = connection
        return try {
            connection.outputStream.use { it.write(Multipart.encode(boundary, parts)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (request.runId in cancelled) return GenerationResult.Cancelled
            when (val parsed = FaceSheetResponse.parse(code, body)) {
                is FaceSheetResponse.Image -> {
                    val file = saveSheet(Base64.decode(parsed.base64, Base64.DEFAULT))
                    GenerationResult.Success(Uri.fromFile(file).toString())
                }
                is FaceSheetResponse.Error -> GenerationResult.Failure(parsed.message, parsed.retryable)
            }
        } catch (timeout: SocketTimeoutException) {
            GenerationResult.TimedOut
        } catch (error: IOException) {
            if (request.runId in cancelled) GenerationResult.Cancelled
            else GenerationResult.Failure("생성 서버에 연결하지 못했습니다.", retryable = true)
        } finally {
            running.remove(request.runId)
            connection.disconnect()
        }
    }

    override fun createFutureLook(request: FutureLookRequest): GenerationResult<String> =
        GenerationResult.Failure("not supported by this client")

    override fun createRecap(request: RecapRequest): GenerationResult<String> =
        GenerationResult.Failure("not supported by this client")

    override fun cancel(runId: String) {
        cancelled += runId
        running.remove(runId)?.disconnect()
    }

    /** EXIF 회전을 바로잡고 긴 변을 줄인 JPEG. */
    private fun readPhoto(uri: Uri): ByteArray {
        val resolver = app.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        require(longest > 0) { "unreadable photo" }
        var sample = 1
        while (longest / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("unreadable photo")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = minOf(1f, MAX_PHOTO_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
    }

    private fun saveSheet(png: ByteArray): File {
        val dir = File(app.filesDir, SHEET_DIR).apply { mkdirs() }
        return File(dir, "face_sheet_${System.currentTimeMillis()}.png").apply { writeBytes(png) }
    }

    companion object {
        const val ENDPOINT = "https://api.openai.com/v1/images/edits"
        const val STYLE_REFERENCE = "images/avatar_age/face_sheet_style_reference.png"
        const val SHEET_DIR = "face_sheets"
        const val MAX_PHOTO_SIDE = 1024
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 150_000

        /** 3열 × 2행: 위 줄 영아 · 유년 · 청소년, 아래 줄 청년 · 중년 · 노년. [com.example.lifegame.ui.avatar.PhotoFaceSheet]가 같은 순서로 자른다. */
        val FACE_SHEET_PROMPT = """
            Create a pixel-art face sheet of the person in the first image for a life-simulation game.
            Draw exactly six head-only portraits of the SAME person at different ages, in a 3-column by 2-row grid,
            each centered in its own equal cell with empty space around it.
            Top row, left to right: baby (about 1 year old), child (about 8), teenager (about 15).
            Bottom row, left to right: young adult (about 25), middle-aged (about 50), elderly (about 75).
            Front-facing, calm friendly expression. Head and hair only: no neck, no shoulders, no body, no text, no frames.
            Keep the person's recognizable features at every age: face shape, skin tone, hairstyle and hair color,
            eyebrows, and glasses or facial hair if they have them.
            Make the aging clearly visible: the middle-aged face has some grey hair and smile lines;
            the elderly face has white or grey thinning hair, forehead wrinkles, eye bags and softer, sagging cheeks.
            Style: crisp 64x64-style pixel art scaled up with hard pixel edges, a dark outline, a limited palette,
            and soft shading lit from the upper left so the face looks rounded.
            The second image shows only the grid layout and the art style of the game's default character.
            Do not copy that character's face or hair. Transparent background.
        """.trimIndent().replace("\n", " ")
    }
}

/** multipart/form-data 본문. 테스트하기 쉽게 Android 의존 없이 만든다. */
sealed interface MultipartPart {
    data class Field(val name: String, val value: String) : MultipartPart
    class File(val name: String, val fileName: String, val contentType: String, val bytes: ByteArray) : MultipartPart
}

object Multipart {
    fun encode(boundary: String, parts: List<MultipartPart>): ByteArray {
        val out = ByteArrayOutputStream()
        fun line(text: String = "") = out.write("$text\r\n".toByteArray(Charsets.UTF_8))
        parts.forEach { part ->
            line("--$boundary")
            when (part) {
                is MultipartPart.Field -> {
                    line("Content-Disposition: form-data; name=\"${part.name}\"")
                    line()
                    line(part.value)
                }
                is MultipartPart.File -> {
                    line("Content-Disposition: form-data; name=\"${part.name}\"; filename=\"${part.fileName}\"")
                    line("Content-Type: ${part.contentType}")
                    line()
                    out.write(part.bytes)
                    line()
                }
            }
        }
        line("--$boundary--")
        return out.toByteArray()
    }
}

/** 이미지 편집 API 응답: data[0].b64_json 또는 error.message. */
sealed interface FaceSheetResponse {
    data class Image(val base64: String) : FaceSheetResponse
    data class Error(val message: String, val retryable: Boolean) : FaceSheetResponse

    companion object {
        fun parse(code: Int, body: String): FaceSheetResponse {
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (code in 200..299) {
                val b64 = json?.optJSONArray("data")?.optJSONObject(0)?.optString("b64_json").orEmpty()
                return if (b64.isNotBlank()) Image(b64) else Error("생성 결과에 그림이 없습니다.", retryable = true)
            }
            val detail = json?.optJSONObject("error")?.optString("message").orEmpty()
            val message = when (code) {
                401 -> "생성 서버 키가 올바르지 않습니다."
                429 -> "요청이 많거나 사용 한도를 넘었습니다. 잠시 뒤 다시 시도해 주세요."
                400 -> if (detail.contains("safety", ignoreCase = true) || detail.contains("moderation", ignoreCase = true)) {
                    "이 사진으로는 얼굴을 만들 수 없습니다. 다른 사진을 골라 주세요."
                } else {
                    "생성 요청이 거절되었습니다."
                }
                else -> "생성 서버 오류($code)."
            }
            return Error(message, retryable = code == 429 || code >= 500)
        }
    }
}
