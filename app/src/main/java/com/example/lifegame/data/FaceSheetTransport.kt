package com.example.lifegame.data

import java.io.ByteArrayOutputStream
import org.json.JSONObject

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
