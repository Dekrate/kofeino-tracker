package pl.dekrate.kofeino.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrinkDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: DrinkDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java).build()
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
        assertNotNull(retrieved)
        assertEquals("Espresso", retrieved!!.name)
        assertEquals(63, retrieved.caffeineMg)
        assertEquals(30, retrieved.volumeMl)
        assertTrue(retrieved.isDefault)
    }

    @Test
    fun insertAndGetByIdWithoutDefault() = runTest {
        val drink = DrinkEntity(name = "Custom", caffeineMg = 100, volumeMl = 250)
        val id = dao.insert(drink)

        val retrieved = dao.getDrinkById(id)
        assertNotNull(retrieved)
        assertEquals("Custom", retrieved!!.name)
        assertEquals(100, retrieved.caffeineMg)
        assertEquals(250, retrieved.volumeMl)
    }

    @Test
    fun insertReturnsPositiveId() = runTest {
        val id = dao.insert(DrinkEntity(name = "Test", caffeineMg = 50, volumeMl = 200))
        assertTrue("Generated ID should be positive", id > 0)
    }

    @Test
    fun getAllDrinks() = runTest {
        dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))

        val drinks = dao.getAllDrinks().first()
        assertEquals(2, drinks.size)
    }

    @Test
    fun getAllDrinksOrderedByName() = runTest {
        dao.insert(DrinkEntity(name = "Zebra", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "Alpha", caffeineMg = 20, volumeMl = 200))
        dao.insert(DrinkEntity(name = "Bravo", caffeineMg = 30, volumeMl = 300))

        val drinks = dao.getAllDrinks().first()
        assertEquals("Alpha", drinks[0].name)
        assertEquals("Bravo", drinks[1].name)
        assertEquals("Zebra", drinks[2].name)
    }

    @Test
    fun getAllDrinksEmpty() = runTest {
        val drinks = dao.getAllDrinks().first()
        assertTrue("Expected empty list", drinks.isEmpty())
    }

    @Test
    fun updateDrink() = runTest {
        val id = dao.insert(DrinkEntity(name = "Old", caffeineMg = 50, volumeMl = 100))

        dao.update(DrinkEntity(id = id, name = "Updated", caffeineMg = 100, volumeMl = 200))

        val retrieved = dao.getDrinkById(id)
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved!!.name)
        assertEquals(100, retrieved.caffeineMg)
        assertEquals(200, retrieved.volumeMl)
    }

    @Test
    fun updateDrinkPartial() = runTest {
        val id = dao.insert(DrinkEntity(name = "Original", caffeineMg = 50, volumeMl = 200))

        // Only update name and caffeine, keep volume unchanged
        dao.update(DrinkEntity(id = id, name = "Changed", caffeineMg = 75, volumeMl = 200))

        val retrieved = dao.getDrinkById(id)
        assertNotNull(retrieved)
        assertEquals("Changed", retrieved!!.name)
        assertEquals(75, retrieved.caffeineMg)
        assertEquals(200, retrieved.volumeMl)
    }

    @Test
    fun deleteDrink() = runTest {
        val id = dao.insert(DrinkEntity(name = "Test", caffeineMg = 10, volumeMl = 50))
        assertNotNull(dao.getDrinkById(id))

        dao.delete(DrinkEntity(id = id, name = "Test", caffeineMg = 10, volumeMl = 50))

        assertNull(dao.getDrinkById(id))
    }

    @Test
    fun deleteDrinkDoesNotAffectOthers() = runTest {
        val id1 = dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        val id2 = dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))

        dao.delete(DrinkEntity(id = id1, name = "A", caffeineMg = 10, volumeMl = 100))

        assertNull(dao.getDrinkById(id1))
        assertNotNull(dao.getDrinkById(id2))
    }

    @Test
    fun getDrinkCount() = runTest {
        assertEquals(0, dao.getDrinkCount())

        dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        assertEquals(1, dao.getDrinkCount())

        dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))
        dao.insert(DrinkEntity(name = "C", caffeineMg = 30, volumeMl = 300))
        assertEquals(3, dao.getDrinkCount())
    }

    @Test
    fun getDrinkCountAfterDelete() = runTest {
        val id = dao.insert(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        dao.insert(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))

        dao.delete(DrinkEntity(id = id, name = "A", caffeineMg = 10, volumeMl = 100))

        assertEquals(1, dao.getDrinkCount())
    }

    @Test
    fun getDrinkByIdNonExistent() = runTest {
        assertNull(dao.getDrinkById(99999L))
    }
}
