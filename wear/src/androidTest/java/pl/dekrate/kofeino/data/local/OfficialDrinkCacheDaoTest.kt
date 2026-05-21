package pl.dekrate.kofeino.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class OfficialDrinkCacheDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: OfficialDrinkCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java).build()
        dao = database.officialDrinkCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getAllCachedEmpty() = runTest {
        val all = dao.getAllCached()
        assertTrue("Expected empty cache", all.isEmpty())
    }

    @Test
    fun insertAndGetAllCached() = runTest {
        val drinks = listOf(
            OfficialDrinkCacheEntity(
                barcode = "5901234123457",
                name = "Red Bull",
                brand = "Red Bull GmbH",
                caffeineMgPer100ml = 32.0,
                energyKcalPer100ml = null,
                quantity = "250ml"
            ),
            OfficialDrinkCacheEntity(
                barcode = "5904567890123",
                name = "Monster Energy",
                brand = "Monster Beverage",
                caffeineMgPer100ml = 32.0,
                energyKcalPer100ml = 50.0,
                quantity = "500ml"
            )
        )
        dao.insertAll(drinks)

        val all = dao.getAllCached()
        assertEquals(2, all.size)
    }

    @Test
    fun getAllCachedOrderedByName() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("003", "Z", null, 10.0, null, null),
                OfficialDrinkCacheEntity("001", "A", null, 10.0, null, null),
                OfficialDrinkCacheEntity("002", "M", null, 10.0, null, null)
            )
        )

        val all = dao.getAllCached()
        assertEquals("A", all[0].name)
        assertEquals("M", all[1].name)
        assertEquals("Z", all[2].name)
    }

    @Test
    fun getByBarcode() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("abc123", "Cola", "Coca-Cola", 10.0, 42.0, "330ml"),
                OfficialDrinkCacheEntity("def456", "Fanta", "Coca-Cola", 0.0, 25.0, "330ml")
            )
        )

        val found = dao.getByBarcode("abc123")
        assertNotNull(found)
        assertEquals("Cola", found!!.name)
        assertEquals("Coca-Cola", found.brand)
        assertEquals(10.0, found.caffeineMgPer100ml, 0.01)
    }

    @Test
    fun getByBarcodeNonExistent() = runTest {
        assertNull(dao.getByBarcode("nonexistent"))
    }

    @Test
    fun getByBarcodeIsCaseSensitive() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("ABC123", "Test", null, 10.0, null, null)
            )
        )

        // Should be case-sensitive search
        assertNull(dao.getByBarcode("abc123"))
        assertNotNull(dao.getByBarcode("ABC123"))
    }

    @Test
    fun insertAllReplacesOnConflict() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Original", null, 10.0, null, null)
            )
        )

        // Insert same barcode with new data → REPLACE
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Updated", null, 20.0, null, null)
            )
        )

        val all = dao.getAllCached()
        assertEquals(1, all.size)
        assertEquals("Updated", all[0].name)
        assertEquals(20.0, all[0].caffeineMgPer100ml, 0.01)
    }

    @Test
    fun insertAllDoesNotDuplicateDifferentBarcodes() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "A", null, 10.0, null, null)
            )
        )
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("002", "B", null, 20.0, null, null)
            )
        )

        val all = dao.getAllCached()
        assertEquals(2, all.size)
    }

    @Test
    fun clearAll() = runTest {
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "A", null, 10.0, null, null),
                OfficialDrinkCacheEntity("002", "B", null, 20.0, null, null)
            )
        )
        assertEquals(2, dao.count())

        dao.clearAll()

        assertEquals(0, dao.count())
        assertTrue(dao.getAllCached().isEmpty())
    }

    @Test
    fun count() = runTest {
        assertEquals(0, dao.count())

        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "A", null, 10.0, null, null)
            )
        )
        assertEquals(1, dao.count())

        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("002", "B", null, 20.0, null, null),
                OfficialDrinkCacheEntity("003", "C", null, 30.0, null, null)
            )
        )
        assertEquals(3, dao.count())

        dao.clearAll()
        assertEquals(0, dao.count())
    }

    @Test
    fun insertLargeBatch() = runTest {
        val batch = (1..50).map { i ->
            OfficialDrinkCacheEntity(
                barcode = "batch_$i",
                name = "Drink $i",
                brand = "Brand $i",
                caffeineMgPer100ml = i.toDouble(),
                energyKcalPer100ml = null,
                quantity = null
            )
        }
        dao.insertAll(batch)

        assertEquals(50, dao.count())
        val all = dao.getAllCached()
        assertEquals(50, all.size)
    }

    @Test
    fun insertAllPreservesFetchedAtMillis() = runTest {
        val before = System.currentTimeMillis()
        dao.insertAll(
            listOf(
                OfficialDrinkCacheEntity("001", "Test", null, 10.0, null, null)
            )
        )
        val after = System.currentTimeMillis()

        val entity = dao.getByBarcode("001")!!
        assertTrue(
            "fetchedAtMillis should be set within time range",
            entity.fetchedAtMillis in before..after
        )
    }
}
