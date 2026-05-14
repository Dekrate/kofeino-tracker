package com.example.kofeinotracker.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kofeinotracker.R
import com.example.kofeinotracker.domain.model.CaffeineIntake
import com.example.kofeinotracker.presentation.theme.KofeinoTrackerTheme
import com.example.kofeinotracker.presentation.viewmodel.CaffeineUiState
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun historyScreen_displaysTitleAndTotal() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(totalCaffeineMg = 200))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.history)).assertIsDisplayed()
        composeTestRule.onNodeWithText("200 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysIntakeItems() {
        val intakes = listOf(
            CaffeineIntake(1, "latte", 63, 250, 0L),
            CaffeineIntake(2, "green_tea", 28, 250, 0L)
        )
        val fakeViewModel = createFakeViewModel(CaffeineUiState(todayIntakes = intakes, totalCaffeineMg = 91))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText("latte", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("green_tea", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("28 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_showsEmptyMessage_whenNoIntakes() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(todayIntakes = emptyList()))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_today)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysMultipleItems() {
        val intakes = List(5) { index ->
            CaffeineIntake(index.toLong(), "drink_$index", 50, 200, 0L)
        }
        val fakeViewModel = createFakeViewModel(CaffeineUiState(todayIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onAllNodesWithText("mg", substring = true).assertCountEquals(5)
    }

    private fun createFakeViewModel(state: CaffeineUiState): CaffeineViewModel {
        val vm = mockk<CaffeineViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<CaffeineUiState>
        return vm
    }
}
