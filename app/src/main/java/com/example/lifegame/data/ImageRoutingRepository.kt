package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.ui.images.ImageRouting

class ImageRoutingRepository(private val context: Context) {
    fun load(): ImageRouting = runCatching {
        context.assets.open("image_routing.json").bufferedReader(Charsets.UTF_8).use {
            ImageRouting.parse(it.readText())
        }
    }.getOrElse { ImageRouting() }
}
