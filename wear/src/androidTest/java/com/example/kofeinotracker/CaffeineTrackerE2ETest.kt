package com.example.kofeinotracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kofeinotracker.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CaffeineTrackerE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun fullFlow_addEspresso_andSeeTotalInHistory() {
        // Home screen visible
        composeRule.onNodeWithText(context.getString(R.string.today_caffeine)).assertIsDisplayed()

        // Click add drink (+ edge button)
        composeRule.onNodeWithText("+").performClick()

        // AddDrink screen visible
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsDisplayed()

        // Click espresso
        composeRule.onNodeWithText(context.getString(R.string.espresso), substring = true).performClick()

        // Back to home, total should show 63
        composeRule.waitForIdle()
        composeRule.onNodeWithText("63").assertIsDisplayed()

        // Navigate to history
        composeRule.onNodeWithText(context.getString(R.string.history)).performClick()

        // History screen visible with espresso entry
        composeRule.waitForIdle()
        composeRule.onNodeWithText("espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullFlow_addMultipleDrinks_exceedsLimit() {
        // Add 3 double espressos = 378 mg
        repeat(3) {
            composeRule.onNodeWithText("+").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText(context.getString(R.string.double_espresso), substring = true).performClick()
            composeRule.waitForIdle()
        }

        // Total should be 378
        composeRule.onNodeWithText("378").assertIsDisplayed()

        // Add another espresso = 441, limit exceeded
        composeRule.onNodeWithText("+").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.espresso), substring = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(context.getString(R.string.limit_exceeded)).assertIsDisplayed()
    }
}
