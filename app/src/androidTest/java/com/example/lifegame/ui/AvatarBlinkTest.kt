package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarAssetLookKey
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.avatar.eyeRows
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarBlinkTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun allAuthoredEyesBlinkAndReopenWithoutChangingTheRestOfTheSavedAppearance() {
        val phase = mutableFloatStateOf(0f)
        val look = mutableStateOf(AvatarLook())
        compose.setContent {
            val density = LocalDensity.current.density
            PlayerAvatar(look.value, Modifier.requiredSize((192 / density).dp, (312 / density).dp)
                .background(Color(0xFFE8DBC3)), ambientPhase = phase)
        }
        for (period in LifePeriod.entries) {
            for (eyes in listOf("eyes_default", "eyes_big", "eyes_narrow")) {
                val appearance = AvatarLook(period = period, eyesId = eyes)
                compose.runOnIdle { look.value = appearance; phase.floatValue = 0f }
                compose.waitUntil(10_000) {
                    runCatching { compose.onNodeWithTag("player_avatar_layers").fetchSemanticsNode().config[AvatarAssetLookKey] == appearance.description }.getOrDefault(false)
                }
                val open = capture()
                compose.runOnIdle { phase.floatValue = .28f }
                val closed = capture()
                var eyeChanges = 0
                val rows = eyeRows(appearance)
                for (y in 0 until open.height) for (x in 0 until open.width) {
                    if (open.getPixel(x, y) != closed.getPixel(x, y)) {
                        assertTrue("Changed outside eye band: $period/$eyes at $x,$y", y in rows)
                        eyeChanges++
                    }
                }
                assertTrue("No visible blink: $period/$eyes", eyeChanges > 3)
                compose.runOnIdle { phase.floatValue = .31f }
                val reopened = capture()
                assertTrue("Blink did not restore exact appearance: $period/$eyes", open.sameAs(reopened))
                if (period == LifePeriod.YOUNG_ADULT && eyes == "eyes_default") {
                    save(open, "open.png"); save(closed, "closed.png")
                }
                open.recycle(); closed.recycle(); reopened.recycle()
            }
        }
    }

    private fun capture(): Bitmap = compose.onNodeWithTag("player_avatar").captureToImage().asAndroidBitmap()
    private fun save(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = requireNotNull(context.getExternalFilesDir("avatar-blink")).also { it.mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
