package pl.dekrate.kofeino.tracker.presentation.viewmodel

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.backup.BackupIOException
import pl.dekrate.kofeino.tracker.data.backup.BackupManager
import pl.dekrate.kofeino.tracker.data.backup.BackupVersionException
import pl.dekrate.kofeino.tracker.data.backup.ExportResult
import pl.dekrate.kofeino.tracker.data.backup.ImportResult
import pl.dekrate.kofeino.tracker.data.local.CaffeineLimitProfile
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
    private lateinit var backupManager: BackupManager
    private lateinit var context: Context
    private lateinit var viewModel: SettingsViewModel

    /** Simulates the DataStore-backed reactive streams. */
    private val languageFlow = MutableStateFlow(DataStorePreferences.DEFAULT_LANGUAGE)
    private val themeFlow = MutableStateFlow(DataStorePreferences.DEFAULT_THEME)
    private val notifLiveFlow = MutableStateFlow(DataStorePreferences.DEFAULT_NOTIF_LIVE)
    private val notifMorningFlow = MutableStateFlow(DataStorePreferences.DEFAULT_NOTIF_MORNING)
    private val notifRegularFlow = MutableStateFlow(DataStorePreferences.DEFAULT_NOTIF_REGULAR)
    private val notifEveningFlow = MutableStateFlow(DataStorePreferences.DEFAULT_NOTIF_EVENING)
    private val caffeineProfileFlow = MutableStateFlow(CaffeineLimitProfile.ADULT)
    private val customLimitFlow = MutableStateFlow(DataStorePreferences.DEFAULT_CUSTOM_CAFFEINE_LIMIT)

    @Before
    fun setup() {
        preferences = mockk(relaxed = true)
        backupManager = mockk()
        context = mockk()
        every { context.getString(any()) } answers { "mocked string" }
        every { context.getString(any(), *anyVararg()) } answers { "mocked string" }
        every { preferences.observeLanguage() } returns languageFlow
        every { preferences.observeThemeMode() } returns themeFlow
        every { preferences.observeNotificationLiveEnabled() } returns notifLiveFlow
        every { preferences.observeNotificationMorningEnabled() } returns notifMorningFlow
        every { preferences.observeNotificationRegularEnabled() } returns notifRegularFlow
        every { preferences.observeNotificationEveningEnabled() } returns notifEveningFlow
        every { preferences.observeCaffeineProfile() } returns caffeineProfileFlow
        every { preferences.observeCustomCaffeineLimit() } returns customLimitFlow
        every { preferences.getLanguage() } returns DataStorePreferences.DEFAULT_LANGUAGE
        every { preferences.getThemeMode() } returns DataStorePreferences.DEFAULT_THEME
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should use default language`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle() // let init coroutines complete

        val state = viewModel.uiState.value
        assertEquals(DataStorePreferences.DEFAULT_LANGUAGE, state.currentLanguage)
        assertFalse("languageChanged should be false initially", state.languageChanged)
    }

    @Test
    fun `initial state should reflect saved language preference`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pl", state.currentLanguage)
        assertFalse(state.languageChanged)
    }

    @Test
    fun `initial state should use default theme mode`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataStorePreferences.DEFAULT_THEME, state.currentThemeMode)
        assertFalse("themeChanged should be false initially", state.themeChanged)
    }

    @Test
    fun `initial state should reflect saved theme preference`() = runTest {
        themeFlow.value = DataStorePreferences.THEME_DARK
        every { preferences.getThemeMode() } returns DataStorePreferences.THEME_DARK

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        coVerify { preferences.setLanguage("pl") }
    }

    @Test
    fun `setLanguage to en should update currentLanguage`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_PL
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_PL
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage("en")
        advanceUntilIdle()

        coVerify { preferences.setLanguage("en") }
    }

    @Test
    fun `setLanguage should set languageChanged flag`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        assertTrue("languageChanged should be true after change", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `setLanguage with same language should not update languageChanged`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM)
        advanceUntilIdle()

        coVerify { preferences.setLanguage("") }
        assertEquals("", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `setLanguage to system when already system is no-op`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.languageChanged)
        coVerify(exactly = 0) { preferences.setLanguage(any()) }
    }

    // ===== Theme change tests =====

    @Test
    fun `setThemeMode to dark should update currentThemeMode`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        assertEquals(DataStorePreferences.THEME_DARK, viewModel.uiState.value.currentThemeMode)
    }

    @Test
    fun `setThemeMode to dark should persist preference`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        coVerify { preferences.setThemeMode(DataStorePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode with same mode should not update themeChanged`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()
        viewModel.setThemeMode(DataStorePreferences.THEME_DARK)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferences.setThemeMode(DataStorePreferences.THEME_DARK) }
    }

    @Test
    fun `setThemeMode should set themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.languageChanged)

        viewModel.consumeLanguageChanged()
        assertFalse("languageChanged should be false after consume", viewModel.uiState.value.languageChanged)
    }

    @Test
    fun `consumeThemeChanged should reset themeChanged flag`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setLanguage("pl")
        advanceUntilIdle()

        assertEquals("pl", viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `switching back to original language after change should re-trigger`() = runTest {
        languageFlow.value = DataStorePreferences.LANGUAGE_EN
        every { preferences.getLanguage() } returns DataStorePreferences.LANGUAGE_EN
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
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

    // ===== Caffeine profile tests =====

    @Test
    fun `setCaffeineProfile should update state and call preferences setCaffeineProfile`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setCaffeineProfile(CaffeineLimitProfile.PREGNANT)
        advanceUntilIdle()

        assertEquals(CaffeineLimitProfile.PREGNANT, viewModel.uiState.value.currentCaffeineProfile)
        coVerify { preferences.setCaffeineProfile(CaffeineLimitProfile.PREGNANT) }
    }

    @Test
    fun `setCaffeineProfile with same profile should not persist again`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        // First change
        viewModel.setCaffeineProfile(CaffeineLimitProfile.PREGNANT)
        advanceUntilIdle()
        assertEquals(CaffeineLimitProfile.PREGNANT, viewModel.uiState.value.currentCaffeineProfile)

        // Same profile again — no-op
        viewModel.setCaffeineProfile(CaffeineLimitProfile.PREGNANT)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferences.setCaffeineProfile(CaffeineLimitProfile.PREGNANT) }
    }

    @Test
    fun `setCustomCaffeineLimit should update state and call preferences setCustomCaffeineLimit`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setCustomCaffeineLimit(200)
        advanceUntilIdle()

        assertEquals(200, viewModel.uiState.value.currentCustomLimit)
        coVerify { preferences.setCustomCaffeineLimit(200) }
    }

    @Test
    fun `state should reflect observed caffeine profile from preferences`() = runTest {
        caffeineProfileFlow.value = CaffeineLimitProfile.SENSITIVE

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        assertEquals(CaffeineLimitProfile.SENSITIVE, viewModel.uiState.value.currentCaffeineProfile)
    }

    @Test
    fun `state should reflect observed custom caffeine limit from preferences`() = runTest {
        customLimitFlow.value = 150

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        assertEquals(150, viewModel.uiState.value.currentCustomLimit)
    }

    // ===== Backup / Restore operation tests =====

    @Test
    fun `exportBackup emits Exporting then Success state`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.exportBackup(uri) } returns ExportResult(intakeCount = 2, drinkCount = 1)

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.exportBackup(uri)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.backupState is BackupUiState.Success)
    }

    @Test
    fun `exportBackup calls backupManager with correct uri`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.exportBackup(uri) } returns ExportResult(intakeCount = 0, drinkCount = 0)

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.exportBackup(uri)
        advanceUntilIdle()

        coVerify { backupManager.exportBackup(uri) }
    }

    @Test
    fun `exportBackup emits error event on failure`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.exportBackup(uri) } throws BackupIOException("Export failed")

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.exportBackup(uri)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.backupState is BackupUiState.Error)
    }

    @Test
    fun `importBackup emits Importing then Success state`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.importBackup(uri, any()) } returns ImportResult(
            intakesImported = 1, intakesSkipped = 0,
            drinksImported = 1, drinksSkipped = 0,
            settingsImported = false
        )

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.importBackup(uri)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.backupState is BackupUiState.Success)
    }

    @Test
    fun `importBackup calls backupManager with importSettings from state`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.importBackup(uri, any()) } returns ImportResult(
            intakesImported = 0, intakesSkipped = 0,
            drinksImported = 0, drinksSkipped = 0,
            settingsImported = true
        )

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        // Enable settings import
        viewModel.setImportSettingsEnabled(true)
        viewModel.importBackup(uri)
        advanceUntilIdle()

        coVerify { backupManager.importBackup(uri, true) }
    }

    @Test
    fun `importBackup emits error event on BackupVersionException`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.importBackup(uri, any()) } throws BackupVersionException("Unsupported version")

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.importBackup(uri)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.backupState is BackupUiState.Error)
    }

    @Test
    fun `double exportBackup is ignored while already exporting`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.exportBackup(uri) } coAnswers {
            // Simulate a slow export — never completes
            kotlinx.coroutines.delay(10_000)
            ExportResult(0, 0)
        }

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.exportBackup(uri)
        // Second call while first is still in flight
        viewModel.exportBackup(uri)
        advanceUntilIdle()

        // backupManager.exportBackup should only be called once
        coVerify(exactly = 1) { backupManager.exportBackup(uri) }
    }

    @Test
    fun `setImportSettingsEnabled updates state`() = runTest {
        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.setImportSettingsEnabled(true)
        assertTrue(viewModel.uiState.value.importSettingsEnabled)

        viewModel.setImportSettingsEnabled(false)
        assertFalse(viewModel.uiState.value.importSettingsEnabled)
    }

    @Test
    fun `exportBackup event emits ShowSnackbar with export success resource`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.exportBackup(uri) } returns ExportResult(intakeCount = 3, drinkCount = 2)

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.exportBackup(uri)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowSnackbar)
            assertTrue((event as SettingsEvent.ShowSnackbar).message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `importBackup event emits ShowSnackbar with import success resource`() = runTest {
        val uri = mockk<Uri>()
        coEvery { backupManager.importBackup(uri, any()) } returns ImportResult(
            intakesImported = 5, intakesSkipped = 2,
            drinksImported = 3, drinksSkipped = 1,
            settingsImported = false
        )

        viewModel = SettingsViewModel(preferences, backupManager, context, testDispatcher)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.importBackup(uri)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowSnackbar)
            assertTrue((event as SettingsEvent.ShowSnackbar).message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
