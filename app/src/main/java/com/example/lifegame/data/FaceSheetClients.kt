package com.example.lifegame.data

import com.example.lifegame.domain.generation.GenerationClient

/**
 * The app's online-generation boundary. No provider credential is accepted or read here.
 *
 * An authenticated backend, upload/deletion policy, and operating limits must be supplied before
 * an online client can be enabled. Until then, the existing local/Fake path remains available.
 * The development provider client lives in src/debug and cannot be packaged in release builds.
 */
object FaceSheetClients {
    fun onlineClient(): GenerationClient? = null
}
