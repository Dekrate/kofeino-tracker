package com.example.kofeinotracker.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kofeinotracker.R
import com.example.kofeinotracker.domain.model.CaffeineIntake
import com.example.kofeinotracker.presentation.theme.KofeinoTrackerTheme
import com.example.kofeinotracker.presentation.viewmodel.CaffeineUiState
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun homeScreen_displaysTitle() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today_caffeine)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysTotalCaffeine() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(totalCaffeineMg = 150))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("150").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysLimitExceededWarning() {
        val fakeViewModel = createFakeViewModel(
            CaffeineUiState(totalCaffeineMg = 450, isLimitExceeded = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.limit_exceeded)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysIntakeList() {
        val intakes = listOf(
            CaffeineIntake(1, "espresso", 63, 30, 0L),
            CaffeineIntake(2, "black_coffee", 95, 250, 0L)
        )
        val fakeViewModel = createFakeViewModel(CaffeineUiState(todayIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onAllNodesWithText("espresso", substring = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("black_coffee", substring = true).assertCountEquals(1)
    }

    @Test
    fun homeScreen_showsEmptyMessage_whenNoIntakes() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(todayIntakes = emptyList()))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_today)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_navigatesToAddDrink_whenPlusClicked() {
        var clicked = false
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = { clicked = true },
                    onNavigateToHistory = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("+").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun homeScreen_navigatesToHistory_whenHistoryClicked() {
        var clicked = false
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = { clicked = true },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.history)).assertIsDisplayed().performClick()
        assert(clicked)
    }

    private fun createFakeViewModel(state: CaffeineUiState): CaffeineViewModel {
        val vm = mockk<CaffeineViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<CaffeineUiState>
        return vm
    }
}
