package pl.dekrate.kofeino

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Agresywne E2E testy dla flow oficjalnych napojów (Open Food Facts API).
 *
 * Wykorzystuje TestNetworkModule z mockiem CaffeineApiService:
 * - 4 napoje z kofeiną (Espresso, Red Bull, Coca-Cola Zero, Monster)
 * - 2 napoje odfiltrowane (caffeine=0, null name)
 *
 * Search mock: "red" → tylko Red Bull, "zzzanything" → empty, inny → wszystkie
 */
@HiltAndroidTest
class OfficialDrinksFlowE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<pl.dekrate.kofeino.presentation.MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun init() {
        hiltRule.inject()
    }

    // ---------- Helper: nawigacja do OfficialDrinksScreen ----------

    private fun navigateToOfficialDrinks() {
        // Home → "Napoje"
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.drinks))
            .performClick()
        composeRule.waitForIdle()

        // ManageDrinks → "Oficjalne napoje"
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.official_drinks_button))
            .performClick()
        composeRule.waitForIdle()
    }

    // ---------- Tests ----------

    @Test
    fun browseOfficialDrinks_showsTitleAndDrinks() {
        navigateToOfficialDrinks()

        // Tytuł widoczny
        composeRule.onNodeWithText(
            context.getString(pl.dekrate.kofeino.R.string.official_drinks_title)
        ).assertIsDisplayed()

        // 4 napoje wyświetlone (mock)
        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Coca-Cola Zero", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Monster Energy Ultra", substring = true).assertIsDisplayed()
    }

    @Test
    fun importOfficialDrink_addsToLocalDrinkList() {
        navigateToOfficialDrinks()

        // Kliknij "Red Bull Energy Drink" (nie ma w defaultowych napojach)
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true)
            .performClick()
        composeRule.waitForIdle()

        // Powrót do ManageDrinksScreen — Red Bull powinien być widoczny z dawką 32 mg
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("32 mg", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun searchOfficialDrink_filtersResults() {
        navigateToOfficialDrinks()

        // Wpisz "red" w pole wyszukiwania
        composeRule.onNodeWithTag("search_field")
            .performTextInput("red")
        composeRule.waitForIdle()

        // Kliknij "Szukaj"
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.official_drinks_search_button))
            .performClick()
        composeRule.waitForIdle()

        // Tylko Red Bull widoczny
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true)
            .assertIsDisplayed()

        // Espresso NIE powinno być widoczne
        composeRule.onNodeWithText("Espresso", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun searchOfficialDrink_empty_showsNoResults() {
        navigateToOfficialDrinks()

        // Wpisz "zzznieistnieje"
        composeRule.onNodeWithTag("search_field")
            .performTextInput("zzznieistnieje")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.official_drinks_search_button))
            .performClick()
        composeRule.waitForIdle()

        // Komunikat "Brak wyników"
        composeRule.onNodeWithText(
            context.getString(pl.dekrate.kofeino.R.string.official_drinks_no_results)
        ).assertIsDisplayed()
    }

    @Test
    fun clearSearch_returnsToBrowseMode() {
        navigateToOfficialDrinks()

        // Najpierw wyszukaj
        composeRule.onNodeWithTag("search_field")
            .performTextInput("red")
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.official_drinks_search_button))
            .performClick()
        composeRule.waitForIdle()

        // Kliknij "Wyczyść"
        composeRule.onNodeWithText(context.getString(pl.dekrate.kofeino.R.string.official_drinks_clear_search))
            .performClick()
        composeRule.waitForIdle()

        // Wszystkie 4 napoje z powrotem
        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Coca-Cola Zero", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Monster Energy Ultra", substring = true).assertIsDisplayed()
    }

    @Test
    fun importDrink_showsCorrectCaffeineValue() {
        navigateToOfficialDrinks()

        // Mock: Espresso ma 0.063 g/100g → 63 mg/100ml
        // Wyświetlane jako "63 mg"
        composeRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()

        // Coca-Cola Zero ma 0.010 g/100g → 10 mg/100ml
        composeRule.onNodeWithText("10 mg", substring = true).assertIsDisplayed()
    }
}
