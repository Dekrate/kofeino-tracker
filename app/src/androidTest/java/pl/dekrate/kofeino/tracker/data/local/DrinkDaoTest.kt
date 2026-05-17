package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

@RunWith(AndroidJUnit4::class)
class DrinkDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: DrinkDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.drinkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val drink = DrinkEntity(name = "Espresso", caffeineMg = 63, volumeMl = 30, isDefault = true)
        val id = dao.insert(drink)

        val retrieved = dao.getDrinkById(id)
        assert(retrieved != null) { "Expected drink, got null" }
        assert(retrieved!!.name == "Espresso")
        assert(retrieved.caffeineMg == 63)
        assert(retrieved.isDefault)
    }

    @Test
    fun getAllDrinks() = runTest {
        dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))

        val drinks = dao.getAllDrinks().first()
        assert(drinks.size == 2) { "Expected 2 drinks, got ${drinks.size}" }
    }

    @Test
    fun getAllDrinksOrderedByName() = runTest {
        dao.insert(DrinkEntity(name = "Zebra", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "Alpha", caffeineMg = 20, volumeMl = 200))
        dao.insert(DrinkEntity(name = "Bravo", caffeineMg = 30, volumeMl = 300))

        val drinks = dao.getAllDrinks().first()
        assert(drinks[0].name == "Alpha") { "Expected Alpha first, got ${drinks[0].name}" }
        assert(drinks[1].name == "Bravo")
        assert(drinks[2].name == "Zebra")
    }

    @Test
    fun updateDrink() = runTest {
        val id = dao.insert(DrinkEntity(name = "Old", caffeineMg = 50, volumeMl = 100))

        dao.update(DrinkEntity(id = id, name = "Updated", caffeineMg = 100, volumeMl = 200))

        val retrieved = dao.getDrinkById(id)
        assert(retrieved!!.name == "Updated")
        assert(retrieved.caffeineMg == 100)
    }

    @Test
    fun deleteDrink() = runTest {
        val id = dao.insert(DrinkEntity(name = "Test", caffeineMg = 10, volumeMl = 50))

        dao.delete(DrinkEntity(id = id, name = "Test", caffeineMg = 10, volumeMl = 50))

        val retrieved = dao.getDrinkById(id)
        assert(retrieved == null) { "Expected null after delete" }
    }

    @Test
    fun getDrinkCount() = runTest {
        dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))
        dao.insert(DrinkEntity(name = "C", caffeineMg = 30, volumeMl = 300))

        val count = dao.getDrinkCount()
        assert(count == 3) { "Expected 3, got $count" }
    }

    @Test
    fun searchDrinks() = runTest {
        dao.insert(DrinkEntity(name = "Espresso", caffeineMg = 63, volumeMl = 30))
        dao.insert(DrinkEntity(name = "Cappuccino", caffeineMg = 75, volumeMl = 200))
        dao.insert(DrinkEntity(name = "Espresso Double", caffeineMg = 126, volumeMl = 60))

        val found = dao.searchDrinks("Espresso").first()
        assert(found.size == 2) { "Expected 2 results for 'Espresso', got ${found.size}" }
    }

    @Test
    fun searchDrinksCaseInsensitive() = runTest {
        dao.insert(DrinkEntity(name = "Green Tea", caffeineMg = 28, volumeMl = 250))

        val found = dao.searchDrinks("green").first()
        assert(found.isNotEmpty()) { "Expected results for 'green'" }
    }

    @Test
    fun searchDrinksReturnsEmptyForNoMatch() = runTest {
        dao.insert(DrinkEntity(name = "Coffee", caffeineMg = 95, volumeMl = 250))

        val found = dao.searchDrinks("nonexistent").first()
        assert(found.isEmpty()) { "Expected empty, got ${found.size}" }
    }
}
