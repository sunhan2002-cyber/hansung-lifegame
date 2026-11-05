package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PhotoFaceSheet
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.avatar.PlayerPortrait
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 사진 → AI 얼굴 시트(2026-11-05 피드백 2번)의 기기 쪽 처리: 시트 자르기 · 배경 지우기 · 몸 위에 얹기 · 화면 표시.
 * 실제 API 호출은 하지 않는다. 시트는 테스트 자산(test_face_sheet.png, 이미지 생성 모델이 만든 가상 인물)이다.
 */
@RunWith(AndroidJUnit4::class)
class PhotoFaceSheetTest {
    @get:Rule val compose = createComposeRule()

    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun sheetFile(name: String, opaque: Boolean = false): File {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("test_face_sheet.png").use { it.readBytes() }
        val file = File(target.filesDir, "face_sheets/$name").apply { parentFile?.mkdirs() }
        if (!opaque) {
            file.writeBytes(bytes)
        } else {
            val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(AndroidColor.rgb(236, 232, 224))
                drawBitmap(source, 0f, 0f, null)
            }
            file.outputStream().use { flat.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return file
    }

    @Test
    fun onlyExistingLocalFilesCountAsAFaceSheet() {
        val file = sheetFile("ref_check.png")
        assertEquals(file.path, PhotoFaceSheet.pathOf(Uri.fromFile(file).toString()))
        assertNull(PhotoFaceSheet.pathOf("fake://face-sheet/profile"))
        assertNull(PhotoFaceSheet.pathOf("file:///data/missing/face.png"))
        assertNull(PhotoFaceSheet.pathOf(null))
    }

    @Test
    fun everyPeriodGetsItsOwnHeadOnTheBody() {
        for (opaque in listOf(false, true)) {
            val file = sheetFile(if (opaque) "opaque.png" else "transparent.png", opaque)
            LifePeriod.entries.forEach { period ->
                val look = AvatarLook(period = period, bodyType = BodyType.STANDARD)
                val out = PhotoFaceSheet.compose(target, look, file.path)
                assertNotNull("$period opaque=$opaque", out)
                assertEquals(PhotoFaceSheet.CANVAS_WIDTH, out!!.width)
                assertEquals(PhotoFaceSheet.CANVAS_HEIGHT, out.height)
                val box = PhotoFaceSheet.headBox(period)
                // 머리 가운데는 칠해져 있고, 머리 칸 바깥(캔버스 왼쪽 위 모서리)은 비어 있다.
                assertEquals("$period head", 255, AndroidColor.alpha(out.getPixel(box.centerX, (box.top + box.chin) / 2)))
                assertEquals("$period corner", 0, AndroidColor.alpha(out.getPixel(2, 2)))
                // 배경을 지운 머리 조각은 칸 전체보다 작다(배경째로 붙지 않았다).
                val sheet = BitmapFactory.decodeFile(file.path)
                val tile = PhotoFaceSheet.headTile(sheet, PhotoFaceSheet.cellOf(period))
                assertNotNull(tile)
                assertTrue("$period tile ${tile!!.width}", tile.width < sheet.width / 3 * 0.95)
            }
        }
    }

    @Test
    fun avatarAndPortraitShowThePhotoFaceForEveryPeriod() {
        val file = sheetFile("screen.png")
        compose.setContent {
            Column(Modifier.background(Color(0xFFF4F1EA))) {
                Row {
                    LifePeriod.entries.forEach { period ->
                        PlayerAvatar(AvatarLook(period = period, photoFaceSheet = file.path, usesPhotoFace = true), Modifier.size(64.dp, 104.dp))
                    }
                }
                Row {
                    LifePeriod.entries.forEach { period ->
                        PlayerPortrait(AvatarLook(period = period, photoFaceSheet = file.path, usesPhotoFace = true), Modifier.size(64.dp, 72.dp))
                    }
                }
                Row {
                    LifePeriod.entries.forEach { period ->
                        PlayerPortrait(AvatarLook(period = period), Modifier.size(64.dp, 72.dp))
                    }
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("player_avatar_photo", useUnmergedTree = true).fetchSemanticsNodes().size == 12
        }
        File(target.getExternalFilesDir(null), "photo_face_sheet_avatars.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
