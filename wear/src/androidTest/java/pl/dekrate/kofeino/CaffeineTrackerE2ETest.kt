package pl.dekrate.kofeino

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import pl.dekrate.kofeino.presentation.MainActivity
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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun fullFlow_addEspresso_andSeeTotalInHistory() {
        // Home screen visible
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.today_caffeine))
            .assertIsDisplayed()

        // Click add drink (+ edge button)
        composeRule.onNodeWithText("+").performClick()

        // AddDrink screen visible
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.select_drink))
            .assertIsDisplayed()

        // Click "Espresso" (display name from DB, not resource)
        composeRule.onNodeWithText("Espresso", substring = true).performClick()

        // Back to home, total should show 63
        composeRule.waitForIdle()
        composeRule.onNodeWithText("63").assertIsDisplayed()

        // Navigate to history
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.history)).performClick()

        // History screen visible with espresso entry
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullFlow_addMultipleDrinks_exceedsLimit() {
        // Add 3 double espressos = 378 mg
        repeat(3) {
            composeRule.onNodeWithText("+").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Podwójne espresso", substring = true).performClick()
            composeRule.waitForIdle()
        }

        // Wait for UI updates
        composeRule.waitForIdle()

        // Total should be 378
        composeRule.onNodeWithText("378").assertIsDisplayed()

        // Add another espresso = 441, limit exceeded
        composeRule.onNodeWithText("+").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Espresso", substring = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            context.getString(pl.dekrate.kofeino.R.string.limit_exceeded)
        ).assertIsDisplayed()
    }

    @Test
    fun fullFlow_navigateDateInHistory() {
        // Add a drink
        composeRule.onNodeWithText("+").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Espresso", substring = true).performClick()
        composeRule.waitForIdle()

        // Go to history
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.history)).performClick()
        composeRule.waitForIdle()

        // Should show "Dzisiaj"
        composeRule.onNodeWithText("Dzisiaj").assertIsDisplayed()

        // Navigate to yesterday
        composeRule.onNodeWithText("◀").performClick()
        composeRule.waitForIdle()

        // Should show "Wczoraj" and no drinks
        composeRule.onNodeWithText("Wczoraj").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(pl.dekrate.kofeino.R.string.no_drinks_today)
        ).assertIsDisplayed()
    }
}
