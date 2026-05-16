package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.domain.model.OfficialDrink
import pl.dekrate.kofeino.presentation.viewmodel.OfficialDrinkViewModel

/**
 * Rendering tests for OfficialDrinksScreen (Wear OS safe).
 *
 * Nie używają [performClick] (nie działa na Wear emulatorze przez touch injection limit).
 * Renderują ekran bezpośrednio przez [createComposeRule] z ręcznie stworzonym ViewModel.
 *
 * Pokrycie:
 * - Title, drinks list, caffeine values, brands
 * - Search field visibility
 * - Loading state → drinks state transition
 * - Error state with retry
 * - Empty state
 */
class OfficialDrinksScreenRenderingTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ===== Helper: tworzy mock repository z 4 napojami =====
    private fun createMockViewModel(
        drinks: List<OfficialDrink> = mockDrinks(),
        searchResults: List<OfficialDrink>? = null,
        throwOnFetch: Boolean = false
    ): OfficialDrinkViewModel {
        val repo = mockk<OfficialDrinkRepository>()

        if (throwOnFetch) {
            coEvery { repo.getOfficialDrinks() } returns Result.failure(Exception("Network error"))
        } else {
            coEvery { repo.getOfficialDrinks() } returns Result.success(drinks)
        }

        coEvery { repo.searchOfficialDrinks(any()) } returns Result.success(
            searchResults ?: emptyList()
        )
        coEvery { repo.hasFreshCache() } returns true

        return OfficialDrinkViewModel(repo)
    }

    private fun mockDrinks() = listOf(
        OfficialDrink(
            barcode = "001",
            name = "Espresso",
            brand = "Illy",
            caffeineMgPer100ml = 63.0,
            energyKcalPer100ml = 9.0,
            quantity = "250 ml"
        ),
        OfficialDrink(
            barcode = "002",
            name = "Red Bull Energy Drink",
            brand = "Red Bull",
            caffeineMgPer100ml = 32.0,
            energyKcalPer100ml = 45.0,
            quantity = "250 ml"
        ),
        OfficialDrink(
            barcode = "003",
            name = "Coca-Cola Zero",
            brand = "Coca-Cola",
            caffeineMgPer100ml = 10.0,
            energyKcalPer100ml = 0.3,
            quantity = "330 ml"
        ),
        OfficialDrink(
            barcode = "004",
            name = "Monster Energy Ultra",
            brand = "Monster",
            caffeineMgPer100ml = 30.0,
            energyKcalPer100ml = 10.0,
            quantity = "500 ml"
        )
    )

    // ===== Rendering tests =====

    @Test
    fun screen_showsTitle() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Oficjalne napoje z kofeiną").assertIsDisplayed()
    }

    @Test
    fun screen_showsDrinksList() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Red Bull Energy Drink", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Coca-Cola Zero", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Monster Energy Ultra", substring = true).assertIsDisplayed()
    }

    @Test
    fun screen_showsCaffeineValues() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("63 mg/100ml").assertIsDisplayed()
        composeRule.onNodeWithText("32 mg/100ml").assertIsDisplayed()
        composeRule.onNodeWithText("10 mg/100ml").assertIsDisplayed()
        composeRule.onNodeWithText("30 mg/100ml").assertIsDisplayed()
    }

    @Test
    fun screen_showsBrands() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Illy", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Red Bull", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Coca-Cola", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Monster", substring = true).assertIsDisplayed()
    }

    @Test
    fun screen_showsSearchField() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithTag("search_field").assertIsDisplayed()
    }

    @Test
    fun screen_showsSearchPlaceholder() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Szukaj napoju…").assertIsDisplayed()
    }

    @Test
    fun screen_showsBackButton() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Wstecz").assertIsDisplayed()
    }

    @Test
    fun screen_displaysFourDrinks() {
        val viewModel = createMockViewModel()
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        // Każdy napój ma mg/100ml — liczymy ile razy występuje
        composeRule.onAllNodesWithText(text = "mg/100ml", substring = true).assertCountEquals(4)
    }

    @Test
    fun screen_showsEmptyState_whenNoDrinks() {
        val viewModel = createMockViewModel(drinks = emptyList())
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Brak oficjalnych napojów w bazie. Spróbuj później.")
            .assertIsDisplayed()
    }

    @Test
    fun screen_showsErrorState_whenFetchFails() {
        val viewModel = createMockViewModel(throwOnFetch = true)
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Nie udało się pobrać danych. Sprawdź połączenie."
        ).assertIsDisplayed()
    }

    @Test
    fun screen_showsNoResults_whenSearchEmpty() {
        val viewModel = createMockViewModel(searchResults = emptyList())
        composeRule.setContent {
            OfficialDrinksScreen(onBack = {}, onDrinkSelected = {}, viewModel = viewModel)
        }

        // Najpierw ustaw tryb wyszukiwania
        viewModel.search("nieistniejacy")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Brak wyników. Spróbuj innej nazwy.")
            .assertIsDisplayed()
    }
}
