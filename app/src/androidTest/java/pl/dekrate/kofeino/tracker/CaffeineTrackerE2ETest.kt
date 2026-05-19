package pl.dekrate.kofeino.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end tests for the phone (app) module.
 *
 * Mirrors the wear module's [pl.dekrate.kofeino.CaffeineTrackerE2ETest] pattern
 * but adapted for phone navigation (Scaffold + FAB + TopAppBar).
 *
 * **SOLID / Design patterns:**
 * - Arrange-Act-Assert (AAA) pattern
 * - Tests focus on user-visible behavior, not implementation details
 * - Each test covers one complete user flow
 */
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
        // Home screen visible — FAB has content description "Add drink"
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.add_drink)
        ).assertIsDisplayed()

        // Click FAB → AddDrink screen
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.add_drink)
        ).performClick()
        composeRule.waitForIdle()

        // AddDrinkScreen — click "Espresso" (default drink name from DB seed)
        composeRule.onNodeWithText("Espresso", substring = true).performClick()
        composeRule.waitForIdle()

        // Confirmation dialog — click "Log drink"
        composeRule.onNodeWithText(
            context.getString(R.string.log_drink)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.log_drink)
        ).performClick()
        composeRule.waitForIdle()

        // Navigate back to Home
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.back)
        ).performClick()
        composeRule.waitForIdle()

        // Home screen — total should show "63 mg" (Espresso = 63 mg)
        composeRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()

        // Navigate to History via TopAppBar icon
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.history_title)
        ).performClick()
        composeRule.waitForIdle()

        // History screen — shows "Espresso" entry with "63 mg"
        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullFlow_addMultipleDrinks_exceedsLimit() {
        // Add 5 espressos = 315 mg (below limit of 400)
        repeat(5) {
            composeRule.onNodeWithContentDescription(
                context.getString(R.string.add_drink)
            ).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Espresso", substring = true).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(
                context.getString(R.string.log_drink)
            ).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithContentDescription(
                context.getString(R.string.back)
            ).performClick()
            composeRule.waitForIdle()
        }

        // Total should be 315
        composeRule.onNodeWithText("315", substring = true).assertIsDisplayed()

        // Add 2 more espressos = 441, limit exceeded
        repeat(2) {
            composeRule.onNodeWithContentDescription(
                context.getString(R.string.add_drink)
            ).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Espresso", substring = true).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(
                context.getString(R.string.log_drink)
            ).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithContentDescription(
                context.getString(R.string.back)
            ).performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText(
            context.getString(R.string.limit_exceeded)
        ).assertIsDisplayed()
    }

    @Test
    fun fullFlow_fineAdjustCaffeineAndSeeCorrectTotal() {
        // Home screen visible
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.add_drink)
        ).assertIsDisplayed()

        // Click FAB -> AddDrink screen
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.add_drink)
        ).performClick()
        composeRule.waitForIdle()

        // Select "Espresso" (default 63 mg)
        composeRule.onNodeWithText("Espresso", substring = true).performClick()
        composeRule.waitForIdle()

        // Use fine adjustment: +1 three times -> 66
        repeat(3) {
            composeRule.onNodeWithText(
                context.getString(R.string.caffeine_adjustment_increase_fine)
            ).performClick()
            composeRule.waitForIdle()
        }

        // Verify the displayed value
        composeRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 66)
        ).assertIsDisplayed()

        // Log the drink
        composeRule.onNodeWithText(
            context.getString(R.string.log_drink)
        ).performClick()
        composeRule.waitForIdle()

        // Navigate back to Home
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.back)
        ).performClick()
        composeRule.waitForIdle()

        // Home screen — total should show "66 mg"
        composeRule.onNodeWithText("66 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullFlow_navigateDateInHistory() {
        // Add 1 espresso first
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.add_drink)
        ).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Espresso", substring = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            context.getString(R.string.log_drink)
        ).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.back)
        ).performClick()
        composeRule.waitForIdle()

        // Navigate to History
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.history_title)
        ).performClick()
        composeRule.waitForIdle()

        // "Today" label should be visible
        composeRule.onNodeWithText(
            context.getString(R.string.today)
        ).assertIsDisplayed()

        // Navigate to yesterday using "Previous day" button
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.previous_day)
        ).performClick()
        composeRule.waitForIdle()

        // "Yesterday" label + no drinks message
        composeRule.onNodeWithText(
            context.getString(R.string.yesterday)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.no_intakes_for_date)
        ).assertIsDisplayed()

        // Navigate back to today using "Go to today" button
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.go_to_today)
        ).performClick()
        composeRule.waitForIdle()

        // Back on today — espresso should be visible
        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
    }
}
