package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.local.LanguagePreferences
import pl.dekrate.kofeino.tracker.data.local.ThemePreferences
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Display tests — Header & navigation =====

    @Test
    fun settingsScreen_displaysTitle() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLanguageSectionHeader() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.app_language))
            .assertIsDisplayed()
    }

    // ===== Language options display =====

    @Test
    fun settingsScreen_displaysSystemLanguageOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.language_system))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysEnglishOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.english))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysPolishOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.polish))
            .assertIsDisplayed()
    }

    // ===== Theme options display =====

    @Test
    fun settingsScreen_displaysThemeSectionHeader() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.app_theme))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSystemThemeOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_system))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLightThemeOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_light))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysDarkThemeOption() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_dark))
            .assertIsDisplayed()
    }

    // ===== About section display =====

    @Test
    fun settingsScreen_displaysAboutSectionHeader() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.about))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAppName() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()
    }

    // ===== Language selection tests =====

    @Test
    fun settingsScreen_clickingPolish_callsSetLanguage() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentLanguage = LanguagePreferences.LANGUAGE_EN)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.polish))
            .performClick()
        verify { fakeVm.setLanguage("pl") }
    }

    @Test
    fun settingsScreen_clickingEnglish_callsSetLanguage() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentLanguage = LanguagePreferences.LANGUAGE_PL)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.english))
            .performClick()
        verify { fakeVm.setLanguage("en") }
    }

    @Test
    fun settingsScreen_clickingSystemLanguage_callsSetLanguage() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentLanguage = LanguagePreferences.LANGUAGE_EN)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.language_system))
            .performClick()
        verify { fakeVm.setLanguage(LanguagePreferences.LANGUAGE_SYSTEM) }
    }

    // ===== Theme selection tests =====

    @Test
    fun settingsScreen_clickingSystemTheme_callsSetThemeMode() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentThemeMode = ThemePreferences.THEME_DARK)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_system))
            .performClick()
        verify { fakeVm.setThemeMode(ThemePreferences.THEME_SYSTEM) }
    }

    @Test
    fun settingsScreen_clickingLightTheme_callsSetThemeMode() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentThemeMode = ThemePreferences.THEME_DARK)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_light))
            .performClick()
        verify { fakeVm.setThemeMode(ThemePreferences.THEME_LIGHT) }
    }

    @Test
    fun settingsScreen_clickingDarkTheme_callsSetThemeMode() {
        val fakeVm = mockk<SettingsViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            SettingsUiState(currentThemeMode = ThemePreferences.THEME_SYSTEM)
        ) as StateFlow<SettingsUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.theme_dark))
            .performClick()
        verify { fakeVm.setThemeMode(ThemePreferences.THEME_DARK) }
    }

    // ===== Navigation test =====

    @Test
    fun settingsScreen_backButton_navigatesBack() {
        var navigated = false
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = { navigated = true },
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
        assert(navigated) { "Back button click should trigger onNavigateBack" }
    }

    // ===== Accessibility content description tests =====

    @Test
    fun settingsScreen_backButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_systemLanguageOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.language_system_description))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_englishOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.language_switch_description, context.getString(R.string.english))
        ).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_polishOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.language_switch_description, context.getString(R.string.polish))
        ).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_systemThemeOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.theme_system_description))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_lightThemeOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.theme_light_description))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_darkThemeOption_hasContentDescription() {
        val fakeVm = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.theme_dark_description))
            .assertIsDisplayed()
    }

    // ===== Utility =====

    private fun createFakeViewModel(
        state: SettingsUiState
    ): SettingsViewModel {
        val vm = mockk<SettingsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<SettingsUiState>
        return vm
    }
}
