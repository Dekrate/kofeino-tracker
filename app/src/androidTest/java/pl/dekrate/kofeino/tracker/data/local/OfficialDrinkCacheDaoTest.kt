package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfficialDrinkCacheDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: OfficialDrinkCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.officialDrinkCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getAllCachedReturnsEmptyInitially() = runTest {
        val all = dao.getAllCached()
        assert(all.isEmpty()) { "Expected empty initially" }
    }

    @Test
    fun insertAndGetAll() = runTest {
        val drinks = listOf(
            OfficialDrinkCacheEntity(
                barcode = "1",
                name = "Energy Drink",
                brand = "Brand",
                caffeineMgPer100ml = 32.0,
                energyKcalPer100ml = null,
                quantity = "250ml"
            )
        )
        dao.insertAll(drinks)

        val all = dao.getAllCached()
        assert(all.size == 1) { "Expected 1, got ${all.size}" }
        assert(all[0].barcode == "1")
        assert(all[0].name == "Energy Drink")
    }

    @Test
    fun insertMultipleAndGetAll() = runTest {
        val drinks = listOf(
            OfficialDrinkCacheEntity(barcode = "1", name = "A", brand = null, caffeineMgPer100ml = 10.0, energyKcalPer100ml = null, quantity = null),
            OfficialDrinkCacheEntity(barcode = "2", name = "B", brand = null, caffeineMgPer100ml = 20.0, energyKcalPer100ml = null, quantity = null)
        )
        dao.insertAll(drinks)

        val all = dao.getAllCached()
        assert(all.size == 2) { "Expected 2, got ${all.size}" }
    }

    @Test
    fun getByBarcode() = runTest {
        dao.insertAll(listOf(
            OfficialDrinkCacheEntity(barcode = "123", name = "Test", brand = null, caffeineMgPer100ml = 5.0, energyKcalPer100ml = null, quantity = null)
        ))

        val found = dao.getByBarcode("123")
        assert(found != null) { "Expected to find by barcode" }
        assert(found!!.name == "Test")
    }

    @Test
    fun getByBarcodeReturnsNullForUnknown() = runTest {
        val found = dao.getByBarcode("nonexistent")
        assert(found == null) { "Expected null for unknown barcode" }
    }

    @Test
    fun insertReplacesOnConflict() = runTest {
        val original = OfficialDrinkCacheEntity(barcode = "1", name = "Original", brand = null, caffeineMgPer100ml = 10.0, energyKcalPer100ml = null, quantity = null)
        val updated = OfficialDrinkCacheEntity(barcode = "1", name = "Updated", brand = null, caffeineMgPer100ml = 20.0, energyKcalPer100ml = null, quantity = null)

        dao.insertAll(listOf(original))
        dao.insertAll(listOf(updated))

        val found = dao.getByBarcode("1")
        assert(found!!.name == "Updated") { "Expected 'Updated', got '${found.name}'" }
    }

    @Test
    fun clearAll() = runTest {
        dao.insertAll(listOf(
            OfficialDrinkCacheEntity(barcode = "1", name = "A", brand = null, caffeineMgPer100ml = 1.0, energyKcalPer100ml = null, quantity = null)
        ))

        dao.clearAll()

        val all = dao.getAllCached()
        assert(all.isEmpty()) { "Expected empty after clear" }
    }

    @Test
    fun count() = runTest {
        dao.insertAll(listOf(
            OfficialDrinkCacheEntity(barcode = "1", name = "A", brand = null, caffeineMgPer100ml = 1.0, energyKcalPer100ml = null, quantity = null),
            OfficialDrinkCacheEntity(barcode = "2", name = "B", brand = null, caffeineMgPer100ml = 2.0, energyKcalPer100ml = null, quantity = null)
        ))

        val count = dao.count()
        assert(count == 2) { "Expected 2, got $count" }
    }

    @Test
    fun getAllCachedOrderedByName() = runTest {
        val drinks = listOf(
            OfficialDrinkCacheEntity(barcode = "3", name = "Zebra", brand = null, caffeineMgPer100ml = 3.0, energyKcalPer100ml = null, quantity = null),
            OfficialDrinkCacheEntity(barcode = "1", name = "Alpha", brand = null, caffeineMgPer100ml = 1.0, energyKcalPer100ml = null, quantity = null),
            OfficialDrinkCacheEntity(barcode = "2", name = "Bravo", brand = null, caffeineMgPer100ml = 2.0, energyKcalPer100ml = null, quantity = null)
        )
        dao.insertAll(drinks)

        val all = dao.getAllCached()
        assert(all[0].name == "Alpha") { "Expected Alpha first, got ${all[0].name}" }
        assert(all[1].name == "Bravo")
        assert(all[2].name == "Zebra")
    }
}
