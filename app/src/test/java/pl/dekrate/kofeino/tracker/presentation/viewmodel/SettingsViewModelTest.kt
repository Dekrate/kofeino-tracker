package pl.dekrate.kofeino.tracker.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences

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

    private lateinit var preferences: DataStorePreferences
    private lateinit var viewModel: SettingsViewModel

    /** Simulates the DataStore-backed reactive streams. */
    private val languageFlow = MutableStateFlow(DataStorePreferences.DEFAULT_LANGUAGE)
    private val themeFlow = MutableStateFlow(DataStorePreferences.DEFAULT_THEME)

    @Before
    fun setup() {
        preferences = mockk(relaxed = true)
        every { preferences.observeLanguage() } returns languageFlow
        every { preferences.observeThemeMode() } returns themeFlow
        every { preferences.getLanguage() } returns DataStorePreferences.DEFAULT_LANGUAGE
        every { preferences.getThemeMode() } returns DataStorePreferences.DEFAULT_THEME
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should use default language`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle() // let init coroutines complete

        val state = viewModel.uiState.value
        assertEquals(DataStorePreferences.DEFAULT_LANGUAGE, state.currentLanguage)
        assertFalse("languageChanged should be false initially", state.languageChanged)
    }

    @Test
    fun `initial state should reflect saved language preference`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL

        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pl", state.currentLanguage)
        assertFalse(state.languageChanged)
    }

    @Test
    fun `initial state should use default theme mode`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataStorePreferences.DEFAULT_THEME, state.currentThemeMode)
        assertFalse("themeChanged should be false initially", state.themeChanged)
    }

    @Test
    fun `initial state should reflect saved theme preference`() = runTest {
        themeFlow.value = DataStorePreferences.THEME_DARK
        every { preferences.getThemeMode() } returns DataStorePreferences.THEME_DARK

        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataStorePreferences.THEME_DARK, state.currentThemeMode)
        assertFalse(state.themeChanged)
    }

    // ===== Language change tests =====

    @Test
    fun `setLanguage to pl should update currentLanguage`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pl", state.currentLanguage)
    }

    @Test
    fun `setLanguage to pl should persist preference`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        coVerify { preferences.setLanguage("pl") }
    }

    @Test
    fun `setLanguage to en should update currentLanguage`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("en")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("en", state.currentLanguage)
    }

    @Test
    fun `setLanguage to en should persist preference`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("en")
        advanceUntilIdle()

        coVerify { preferences.setLanguage("en") }
    }

    @Test
    fun `setLanguage should set languageChanged flag`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        assertTrue("languageChanged should be true after change", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same language should not update languageChanged`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        // First change
        viewModel.setLanguage("pl")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.languageChanged)

        // Same language again — no-op keeps it true (already changed)
        viewModel.setLanguage("pl")
        advanceUntilIdle()
        assertTrue("Should remain true after repeat call", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same language should not persist again`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()
        viewModel.setLanguage("pl")
        advanceUntilIdle()

        coVerify(exactly = 1) { preferences.setLanguage("pl") }
    }

    // ===== Language — system default =====

    @Test
    fun `setLanguage to system empty should persist empty string`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM)
        advanceUntilIdle()

        coVerify { preferences.setLanguage("") }
        assertEquals("", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `setLanguage to system when already system is no-op`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.languageChanged)
        coVerify(exactly = 0) { preferences.setLanguage(any()) }
    }

    // ===== Theme change tests =====

    @Test
    fun `setThemeMode to dark should update currentThemeMode`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        assertEquals(DataStorePreferences.THEME_DARK, viewModel.uiState.value.currentThemeMode)
    }

    @Test
    fun `setThemeMode to dark should persist preference`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        coVerify { preferences.setThemeMode(DataStorePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode with same mode should not update themeChanged`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.themeChanged)

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()
        assertTrue("Should remain true after repeat call", viewModel.uiState.value.themeChanged)
    }

    @Test
    fun `setThemeMode with same mode should not persist again`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()
        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferences.setThemeMode(DataStorePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode should set themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        assertTrue("themeChanged should be true after change", viewModel.uiState.value.themeChanged)
    }

    // ===== Consume flags tests =====

    @Test
    fun `consumeLanguageChanged should reset languageChanged flag`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.languageChanged)

        viewModel.consumeLanguageChanged()
        assertFalse("languageChanged should be false after consume", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `consumeThemeChanged should reset themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.themeChanged)

        viewModel.consumeThemeChanged()
        assertFalse("themeChanged should be false after consume", viewModel.uiState.value.themeChanged)
    }

    // ===== Flow / Turbine tests =====

    @Test
    fun `uiState should emit initial state then emit on setLanguage`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.uiState.test {
            // Skip initial (consumed above via advanceUntilIdle)
            val initial = viewModel.uiState.value
            assertEquals("en", initial.currentLanguage)
            assertFalse(initial.languageChanged)

            viewModel.setLanguage("pl")
            advanceUntilIdle()

            val updated = viewModel.uiState.value
            assertEquals("pl", updated.currentLanguage)
            assertTrue(updated.languageChanged)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeated setLanguage should keep languageChanged true`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.uiState.test {
            var current = viewModel.uiState.value
            assertEquals("en", current.currentLanguage)

            viewModel.setLanguage("pl")
            advanceUntilIdle()
            current = viewModel.uiState.value
            assertTrue(current.languageChanged)
            assertEquals("pl", current.currentLanguage)

            viewModel.setLanguage("en")
            advanceUntilIdle()
            current = viewModel.uiState.value
            assertTrue(current.languageChanged)
            assertEquals("en", current.currentLanguage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState should emit on setThemeMode`() = runTest {
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = viewModel.uiState.value
            assertEquals(DataStorePreferences.DEFAULT_THEME, initial.currentThemeMode)
            assertFalse(initial.themeChanged)

            viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
            advanceUntilIdle()

            val updated = viewModel.uiState.value
            assertEquals(DataStorePreferences.THEME_DARK, updated.currentThemeMode)
            assertTrue(updated.themeChanged)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Edge cases =====

    @Test
    fun `setLanguage with same en when already en is no-op`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("en")
        advanceUntilIdle()

        assertEquals("en", viewModel.uiState.value.currentLanguage)
        assertFalse(viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same pl when already pl is no-op`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        assertEquals("pl", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `switching back to original language after change should re-trigger`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()
        assertEquals("pl", viewModel.uiState.value.currentLanguage)
        assertTrue(viewModel.uiState.value.languageChanged)

        viewModel.setLanguage("en")
        advanceUntilIdle()
        assertEquals("en", viewModel.uiState.value.currentLanguage)
        assertTrue(viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `LANGUAGE_PL constant should match DataStorePreferences`() {
        assertEquals("pl", DataStorePreferences.LANGUAGE_PL)
    }

    @Test
    fun `LANGUAGE_EN constant should match DataStorePreferences`() {
        assertEquals("en", DataStorePreferences.LANGUAGE_EN)
    }

    @Test
    fun `LANGUAGE_SYSTEM constant should be empty string`() {
        assertEquals("", DataStorePreferences.LANGUAGE_SYSTEM)
    }

    @Test
    fun `THEME constants should match DataStorePreferences`() {
        assertEquals("system", DataStorePreferences.THEME_SYSTEM)
        assertEquals("light", DataStorePreferences.THEME_LIGHT)
        assertEquals("dark", DataStorePreferences.THEME_DARK)
    }
}
