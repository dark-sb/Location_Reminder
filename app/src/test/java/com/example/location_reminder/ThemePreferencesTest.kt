package com.example.location_reminder

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.location_reminder.data.preferences.AppTheme
import com.example.location_reminder.ui.theme.CompletedContainerDark
import com.example.location_reminder.ui.theme.CompletedContainerLight
import com.example.location_reminder.ui.theme.CompletedGreenDark
import com.example.location_reminder.ui.theme.CompletedGreenLight
import com.example.location_reminder.ui.theme.DarkBackground
import com.example.location_reminder.ui.theme.LightBackground
import com.example.location_reminder.ui.theme.completedContainer
import com.example.location_reminder.ui.theme.completedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemePreferencesTest {

    @Test
    fun testAppThemeEnumValues() {
        assertEquals(2, AppTheme.values().size)
        assertEquals("LIGHT", AppTheme.LIGHT.name)
        assertEquals("DARK", AppTheme.DARK.name)

        assertEquals(AppTheme.LIGHT, AppTheme.valueOf("LIGHT"))
        assertEquals(AppTheme.DARK, AppTheme.valueOf("DARK"))
    }

    @Test
    fun testThemeSafeFallback() {
        val fallback = try {
            AppTheme.valueOf("UNKNOWN_THEME")
        } catch (e: IllegalArgumentException) {
            AppTheme.DARK
        }
        assertEquals(AppTheme.DARK, fallback)
    }

    @Test
    fun testThemeColorTokens_darkAndLightDistinct() {
        assertNotEquals(DarkBackground, LightBackground)
        assertNotEquals(CompletedContainerDark, CompletedContainerLight)
        assertNotEquals(CompletedGreenDark, CompletedGreenLight)
    }

    @Test
    fun testColorSchemeCompletedExtensions() {
        val lightScheme = lightColorScheme(
            background = LightBackground
        )
        val darkScheme = darkColorScheme(
            background = DarkBackground
        )

        assertEquals(CompletedContainerLight, lightScheme.completedContainer)
        assertEquals(CompletedGreenLight, lightScheme.completedContent)

        assertEquals(CompletedContainerDark, darkScheme.completedContainer)
        assertEquals(CompletedGreenDark, darkScheme.completedContent)
    }
}
