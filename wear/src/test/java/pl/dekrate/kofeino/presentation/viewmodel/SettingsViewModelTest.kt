package pl.dekrate.kofeino.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.data.local.CaffeinePreferences
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.domain.model.CaffeineLimitProfile

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
    private lateinit var caffeinePreferences: CaffeinePreferences
    private lateinit var viewModel: SettingsViewModel

    private val languageFlow = MutableStateFlow(LanguagePreferences.LANGUAGE_SYSTEM)
    private val caffeineLimitFlow = MutableStateFlow(400)

    @Before
    fun setup() {
        languagePreferences = mockk(relaxed = true)
        caffeinePreferences = mockk(relaxed = true)

        every { languagePreferences.languageFlow } returns languageFlow
        every { caffeinePreferences.limitFlow } returns caffeineLimitFlow
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_SYSTEM
        every { caffeinePreferences.getProfile() } returns CaffeineLimitProfile.ADULT
        every { caffeinePreferences.getCustomLimit() } returns CaffeinePreferences.DEFAULT_CUSTOM_LIMIT

        viewModel = SettingsViewModel(languagePreferences, caffeinePreferences)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state has ADULT profile`(): Unit = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CaffeineLimitProfile.ADULT, state.currentProfile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state has system language`(): Unit = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LanguagePreferences.LANGUAGE_SYSTEM, state.currentLanguage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state has default custom limit`(): Unit = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CaffeinePreferences.DEFAULT_CUSTOM_LIMIT, state.customLimit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Profile mutation tests =====

    @Test
    fun `setProfile ADULT updates profile`(): Unit = runTest {
        viewModel.setProfile(CaffeineLimitProfile.ADULT)
        verify { caffeinePreferences.setProfile(CaffeineLimitProfile.ADULT) }
    }

    @Test
    fun `setProfile PREGNANT updates profile`(): Unit = runTest {
        viewModel.setProfile(CaffeineLimitProfile.PREGNANT)
        verify { caffeinePreferences.setProfile(CaffeineLimitProfile.PREGNANT) }
    }

    @Test
    fun `setProfile SENSITIVE updates profile`(): Unit = runTest {
        viewModel.setProfile(CaffeineLimitProfile.SENSITIVE)
        verify { caffeinePreferences.setProfile(CaffeineLimitProfile.SENSITIVE) }
    }

    @Test
    fun `setProfile CUSTOM updates profile`(): Unit = runTest {
        viewModel.setProfile(CaffeineLimitProfile.CUSTOM)
        verify { caffeinePreferences.setProfile(CaffeineLimitProfile.CUSTOM) }
    }

    // ===== Custom limit tests =====

    @Test
    fun `setCustomLimit 150 saves and reflects`(): Unit = runTest {
        viewModel.setCustomLimit(150)
        verify { caffeinePreferences.setCustomLimit(150) }
    }

    @Test
    fun `setCustomLimit clamps to min value`(): Unit = runTest {
        viewModel.setCustomLimit(0)
        verify { caffeinePreferences.setCustomLimit(CaffeinePreferences.MIN_CUSTOM_LIMIT) }
    }

    @Test
    fun `setCustomLimit clamps to max value`(): Unit = runTest {
        viewModel.setCustomLimit(5000)
        verify { caffeinePreferences.setCustomLimit(CaffeinePreferences.MAX_CUSTOM_LIMIT) }
    }

    // ===== Language mutation tests =====

    @Test
    fun `setLanguage pl updates language`(): Unit = runTest {
        viewModel.setLanguage(LanguagePreferences.LANGUAGE_PL)
        verify { languagePreferences.setLanguage(LanguagePreferences.LANGUAGE_PL) }
    }

    @Test
    fun `setLanguage en updates language`(): Unit = runTest {
        viewModel.setLanguage(LanguagePreferences.LANGUAGE_EN)
        verify { languagePreferences.setLanguage(LanguagePreferences.LANGUAGE_EN) }
    }

    @Test
    fun `setLanguage system updates language`(): Unit = runTest {
        viewModel.setLanguage(LanguagePreferences.LANGUAGE_SYSTEM)
        verify { languagePreferences.setLanguage(LanguagePreferences.LANGUAGE_SYSTEM) }
    }

    // ===== Reactive state tests =====

    @Test
    fun `state reacts to caffeinePreferences limitFlow emission`(): Unit = runTest {
        every { caffeinePreferences.getProfile() } returns CaffeineLimitProfile.PREGNANT
        every { caffeinePreferences.getCustomLimit() } returns 200

        viewModel.uiState.test {
            // Skip initial emission
            awaitItem()

            caffeineLimitFlow.value = 200
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(CaffeineLimitProfile.PREGNANT, state.currentProfile)
            assertEquals(200, state.customLimit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reacts to languagePreferences languageFlow emission`(): Unit = runTest {
        every { languagePreferences.getLanguage() } returns LanguagePreferences.LANGUAGE_PL

        viewModel.uiState.test {
            // Skip initial emission
            awaitItem()

            languageFlow.value = LanguagePreferences.LANGUAGE_PL
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(LanguagePreferences.LANGUAGE_PL, state.currentLanguage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setProfile followed by setCustomLimit reflects both changes`(): Unit = runTest {
        every { caffeinePreferences.getProfile() } returns CaffeineLimitProfile.CUSTOM
        every { caffeinePreferences.getCustomLimit() } returns 150

        viewModel.uiState.test {
            // Skip initial emission
            awaitItem()

            viewModel.setProfile(CaffeineLimitProfile.CUSTOM)
            viewModel.setCustomLimit(150)
            caffeineLimitFlow.value = 150
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(CaffeineLimitProfile.CUSTOM, state.currentProfile)
            assertEquals(150, state.customLimit)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
