package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.inspector.WindowInspector
import androidx.test.platform.app.InstrumentationRegistry

/** Capture the app's top window on its UI thread, including a fullscreen Dialog. */
internal fun captureAppWindow(): Bitmap {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    if (Build.VERSION.SDK_INT < 29) {
        return requireNotNull(instrumentation.uiAutomation.takeScreenshot())
    }
    var bitmap: Bitmap? = null
    instrumentation.runOnMainSync {
        val root = WindowInspector.getGlobalWindowViews().last {
            it.isShown && it.width > 0 && it.height > 0
        }
        bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also {
            root.draw(Canvas(it))
        }
    }
    return requireNotNull(bitmap)
}
