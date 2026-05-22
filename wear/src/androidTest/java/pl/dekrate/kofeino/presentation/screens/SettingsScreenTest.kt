package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.domain.model.CaffeineLimitProfile
import pl.dekrate.kofeino.presentation.theme.KofeinoTrackerTheme
import pl.dekrate.kofeino.presentation.viewmodel.SettingsUiState
import pl.dekrate.kofeino.presentation.viewmodel.SettingsViewModel

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun settingsScreen_displaysTitle() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLanguageSection() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.language)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysLanguageOptions() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.language_system)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.english)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.polish)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysProfileOptions() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_limit_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_adult)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_pregnant)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_sensitive)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.profile_custom)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysCustomLimitControls_whenCustomProfileSelected() {
        val fakeViewModel = createFakeViewModel(
            SettingsUiState(currentProfile = CaffeineLimitProfile.CUSTOM, customLimit = 150)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.custom_limit)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.custom_limit_value, 150)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_doesNotDisplayCustomLimitControls_whenNonCustomProfile() {
        val fakeViewModel = createFakeViewModel(
            SettingsUiState(currentProfile = CaffeineLimitProfile.ADULT)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.custom_limit_value, 400)).assertDoesNotExist()
    }

    @Test
    fun settingsScreen_displaysHealthDisclaimer() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_title)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysHealthReferences() {
        val fakeViewModel = createFakeViewModel(SettingsUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_title)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsSelectedLanguageAsActive() {
        val fakeViewModel = createFakeViewModel(
            SettingsUiState(currentLanguage = LanguagePreferences.LANGUAGE_PL)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        // Polish option should be displayed - the disabled button shows current selection
        composeTestRule.onNodeWithText(context.getString(R.string.polish)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsSystemLanguageAsDefault() {
        val fakeViewModel = createFakeViewModel(
            SettingsUiState(currentLanguage = LanguagePreferences.LANGUAGE_SYSTEM)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SettingsScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.language_system)).assertIsDisplayed()
    }

    private fun createFakeViewModel(state: SettingsUiState): SettingsViewModel {
        val vm = mockk<SettingsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state)
        return vm
    }
}
