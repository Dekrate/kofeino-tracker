package pl.dekrate.kofeino.tracker.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.tracker.data.local.LanguagePreferences
import pl.dekrate.kofeino.tracker.data.local.ThemePreferences

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = object : TestWatcher() {
        override fun starting(description: Description?) {
            Dispatchers.setMain(testDispatcher)
        }
        override fun finished(description: Description?) {
            Dispatchers.resetMain()
        }
    }

    private lateinit var languagePreferences: LanguagePreferences
    private lateinit var themePreferences: ThemePreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        languagePreferences = mockk(relaxed = true)
        every { languagePreferences.getLanguage() } returns LanguagePreferences.DEFAULT_LANGUAGE
        themePreferences = mockk(relaxed = true)
        every { themePreferences.getThemeMode() } returns ThemePreferences.DEFAULT_THEME_MODE
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should use default language`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        val state = viewModel.uiState.value
        assertEquals(LanguagePreferences.DEFAULT_LANGUAGE, state.currentLanguage)
        assertFalse("languageChanged should be false initially", state.languageChanged)
    }

    @Test
    fun `initial state should reflect saved language preference`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_PL

        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        val state = viewModel.uiState.value
        assertEquals("pl", state.currentLanguage)
        assertFalse(state.languageChanged)
    }

    @Test
    fun `initial state should use default theme mode`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        val state = viewModel.uiState.value
        assertEquals(ThemePreferences.DEFAULT_THEME_MODE, state.currentThemeMode)
        assertFalse("themeChanged should be false initially", state.themeChanged)
    }

    @Test
    fun `initial state should reflect saved theme preference`() = runTest {
        every { themePreferences.getThemeMode() } returns ThemePreferences.THEME_DARK

        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        val state = viewModel.uiState.value
        assertEquals(ThemePreferences.THEME_DARK, state.currentThemeMode)
        assertFalse(state.themeChanged)
    }

    // ===== Language change tests =====

    @Test
    fun `setLanguage to pl should update currentLanguage`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")

        val state = viewModel.uiState.value
        assertEquals("pl", state.currentLanguage)
    }

    @Test
    fun `setLanguage to pl should persist preference`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")

        verify { languagePreferences.setLanguage("pl") }
    }

    @Test
    fun `setLanguage to en should update currentLanguage`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_PL

        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("en")

        val state = viewModel.uiState.value
        assertEquals("en", state.currentLanguage)
    }

    @Test
    fun `setLanguage to en should persist preference`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("en")

        verify { languagePreferences.setLanguage("en") }
    }

    @Test
    fun `setLanguage should set languageChanged flag`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")

        assertTrue("languageChanged should be true after change", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same language should not update languageChanged`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        // First change
        viewModel.setLanguage("pl")
        assertTrue(viewModel.uiState.value.languageChanged)

        // Same language again — no-op keeps it true (already changed)
        viewModel.setLanguage("pl")
        assertTrue("Should remain true after repeat call", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same language should not persist again`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")
        viewModel.setLanguage("pl")

        // Should only have been called once
        verify(exactly = 1) { languagePreferences.setLanguage("pl") }
    }

    // ===== Language — system default =====

    @Test
    fun `setLanguage to system empty should persist empty string`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage(LanguagePreferences.LANGUAGE_SYSTEM)

        verify { languagePreferences.setLanguage("") }
        assertEquals("", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `setLanguage to system when already system is no-op`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_SYSTEM
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage(LanguagePreferences.LANGUAGE_SYSTEM)

        assertFalse(viewModel.uiState.value.languageChanged)
        verify(exactly = 0) { languagePreferences.setLanguage(any()) }
    }

    // ===== Theme change tests =====

    @Test
    fun `setThemeMode to dark should update currentThemeMode`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)

        assertEquals(ThemePreferences.THEME_DARK, viewModel.uiState.value.currentThemeMode)
    }

    @Test
    fun `setThemeMode to dark should persist preference`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)

        verify { themePreferences.setThemeMode(ThemePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode to light should update and persist`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_LIGHT)

        assertEquals(ThemePreferences.THEME_LIGHT, viewModel.uiState.value.currentThemeMode)
        verify { themePreferences.setThemeMode(ThemePreferences.THEME_LIGHT) }
    }

    @Test
    fun `setThemeMode to system should update and persist`() = runTest {
        every { themePreferences.getThemeMode() } returns ThemePreferences.THEME_DARK
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_SYSTEM)

        assertEquals(ThemePreferences.THEME_SYSTEM, viewModel.uiState.value.currentThemeMode)
        verify { themePreferences.setThemeMode(ThemePreferences.THEME_SYSTEM) }
    }

    @Test
    fun `setThemeMode with same mode should not update themeChanged`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)
        assertTrue(viewModel.uiState.value.themeChanged)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)
        assertTrue("Should remain true after repeat call", viewModel.uiState.value.themeChanged)
    }

    @Test
    fun `setThemeMode with same mode should not persist again`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)
        viewModel.setThemeMode(ThemePreferences.THEME_DARK)

        verify(exactly = 1) { themePreferences.setThemeMode(ThemePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode should set themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)

        assertTrue("themeChanged should be true after change", viewModel.uiState.value.themeChanged)
    }

    // ===== Consume flags tests =====

    @Test
    fun `consumeLanguageChanged should reset languageChanged flag`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")
        assertTrue(viewModel.uiState.value.languageChanged)

        viewModel.consumeLanguageChanged()
        assertFalse("languageChanged should be false after consume", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `consumeThemeChanged should reset themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setThemeMode(ThemePreferences.THEME_DARK)
        assertTrue(viewModel.uiState.value.themeChanged)

        viewModel.consumeThemeChanged()
        assertFalse("themeChanged should be false after consume", viewModel.uiState.value.themeChanged)
    }

    // ===== Flow / Turbine tests =====

    @Test
    fun `uiState should emit initial state then emit on setLanguage`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("en", initial.currentLanguage)
            assertFalse(initial.languageChanged)

            viewModel.setLanguage("pl")

            val updated = awaitItem()
            assertEquals("pl", updated.currentLanguage)
            assertTrue(updated.languageChanged)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeated setLanguage should keep languageChanged true`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.setLanguage("pl")
            val afterFirst = awaitItem()
            assertTrue(afterFirst.languageChanged)
            assertEquals("pl", afterFirst.currentLanguage)

            viewModel.setLanguage("en")
            val afterSecond = awaitItem()
            assertTrue(afterSecond.languageChanged)
            assertEquals("en", afterSecond.currentLanguage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState should emit on setThemeMode`() = runTest {
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(ThemePreferences.THEME_SYSTEM, initial.currentThemeMode)
            assertFalse(initial.themeChanged)

            viewModel.setThemeMode(ThemePreferences.THEME_DARK)

            val updated = awaitItem()
            assertEquals(ThemePreferences.THEME_DARK, updated.currentThemeMode)
            assertTrue(updated.themeChanged)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Edge cases =====

    @Test
    fun `setLanguage with same en when already en is no-op`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        // Initial language is "en", so calling setLanguage("en") should be a no-op
        viewModel.setLanguage("en")

        // State should be unchanged
        assertEquals("en", viewModel.uiState.value.currentLanguage)
        assertFalse(viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same pl when already pl is no-op`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.setLanguage("pl")

        assertEquals("pl", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `switching back to original language after change should re-trigger`() = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(languagePreferences, themePreferences)

        viewModel.uiState.test {
            awaitItem() // initial en

            viewModel.setLanguage("pl")
            val afterPl = awaitItem()
            assertEquals("pl", afterPl.currentLanguage)
            assertTrue(afterPl.languageChanged)

            viewModel.setLanguage("en")
            val afterEn = awaitItem()
            assertEquals("en", afterEn.currentLanguage)
            assertTrue(afterEn.languageChanged)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LANGUAGE_PL constant should match LanguagePreferences`() {
        assertEquals("pl", LanguagePreferences.LANGUAGE_PL)
    }

    @Test
    fun `LANGUAGE_EN constant should match LanguagePreferences`() {
        assertEquals("en", LanguagePreferences.LANGUAGE_EN)
    }

    @Test
    fun `LANGUAGE_SYSTEM constant should be empty string`() {
        assertEquals("", LanguagePreferences.LANGUAGE_SYSTEM)
    }

    @Test
    fun `THEME constants should match ThemePreferences`() {
        assertEquals("system", ThemePreferences.THEME_SYSTEM)
        assertEquals("light", ThemePreferences.THEME_LIGHT)
        assertEquals("dark", ThemePreferences.THEME_DARK)
    }
}
