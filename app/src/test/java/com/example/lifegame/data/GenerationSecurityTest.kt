package com.example.lifegame.data

import com.example.lifegame.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Release safety checks run in both debug and release unit-test variants. */
class GenerationSecurityTest {
    @Test
    fun buildConfigCannotCarryProviderCredentials() {
        val credentialFields = BuildConfig::class.java.declaredFields.map { it.name }.filter {
            it.contains("API_KEY", ignoreCase = true) || it.contains("SECRET", ignoreCase = true)
        }
        assertFalse("Provider credentials must stay outside Android builds", credentialFields.isNotEmpty())
    }

    @Test
    fun productionCompositionDoesNotEnableAnUnauthenticatedProviderClient() {
        assertNull(FaceSheetClients.onlineClient())
    }

    @Test
    fun providerExperimentCannotBeLoadedInRelease() {
        if (BuildConfig.DEBUG) return
        val directClient = runCatching {
            Class.forName("com.example.lifegame.data.OpenAiFaceSheetClient")
        }.getOrNull()
        assertNull("The provider experiment must remain in src/debug", directClient)
    }
}
