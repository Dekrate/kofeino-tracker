package com.example.kofeinotracker.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kofeinotracker.R
import com.example.kofeinotracker.presentation.theme.KofeinoTrackerTheme
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class AddDrinkScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addDrinkScreen_displaysTitle() {
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(onDrinkAdded = {})
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_displaysAllDrinks() {
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(onDrinkAdded = {})
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.espresso), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cappuccino), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.black_coffee), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.energy_drink), substring = true).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_clickDrink_callsViewModelAndNavigatesBack() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(com.example.kofeinotracker.presentation.viewmodel.CaffeineUiState()) as StateFlow<com.example.kofeinotracker.presentation.viewmodel.CaffeineUiState>

        var navigatedBack = false
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = { navigatedBack = true },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.espresso), substring = true).performClick()
        verify { fakeViewModel.addDrink(any()) }
        assert(navigatedBack)
    }

    @Test
    fun addDrinkScreen_displaysCorrectCaffeineValues() {
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(onDrinkAdded = {})
            }
        }
        composeTestRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("126 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("95 mg", substring = true).assertIsDisplayed()
    }
}
