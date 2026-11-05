package com.abhisheksati.mydoc.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N-20 kept honest: `setDynamicColor` wrote a preference that no screen could reach, and
 * `AppDesignSystemTheme` defaulted its `dynamicColor` parameter to the opposite of what the app
 * actually ships, so every preview rendered a Material You palette the user never sees. The dead
 * writer is gone and the default now matches the runtime value.
 *
 * A Composable's default cannot be read from a JVM test, so this asserts the half that can: nothing
 * in the settings layer can turn dynamic colour on any more. The other half — that a preview renders
 * the brand palette — is a visual property and is verified on screen, not here.
 */
class SettingsRepositorySurfaceTest {

    private fun declaredMethodNames(): Set<String> =
        SettingsRepository::class.java.declaredMethods.map { it.name }.toSet()

    private fun declaredFieldNames(): Set<String> =
        SettingsRepository::class.java.declaredFields.map { it.name }.toSet()

    @Test
    fun noWriterCanTurnDynamicColourOnBehindTheBrandPalette() {
        assertFalse(
            "setDynamicColor came back: ${declaredMethodNames()}",
            declaredMethodNames().contains("setDynamicColor")
        )
        assertFalse(
            "the unread dynamicColor flow came back: ${declaredFieldNames()}",
            declaredFieldNames().contains("_dynamicColor")
        )
    }

    @Test
    fun theTogglesThatAreActuallyOnScreenAreStillExposed() {
        // Guards the removal: this class must keep losing dead members, not live ones.
        val methods = declaredMethodNames()
        listOf("setShowHiddenFiles", "setGlassEffectMode", "setCheckUpdatesOnLaunch").forEach {
            assertTrue("$it is missing from SettingsRepository", methods.contains(it))
        }
    }
}
