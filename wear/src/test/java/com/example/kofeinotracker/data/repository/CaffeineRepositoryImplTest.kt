package com.example.kofeinotracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.kofeinotracker.data.local.CaffeineDatabase
import com.example.kofeinotracker.data.local.CaffeineIntakeDao
import com.example.kofeinotracker.domain.model.CaffeineIntake
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class CaffeineRepositoryImplTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: CaffeineIntakeDao
    private lateinit var repository: CaffeineRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.caffeineIntakeDao()
        repository = CaffeineRepositoryImpl(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `addIntake should insert and return in today list`() = runTest {
        val intake = createIntake(caffeineMg = 63)
        repository.addIntake(intake)

        repository.getTodayIntakes().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(63, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTodayTotalCaffeine should sum only today entries`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 63))
        repository.addIntake(createIntake(caffeineMg = 95))

        repository.getTodayTotalCaffeine().test {
            assertEquals(158, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTodayIntakes should ignore yesterday entries`() = runTest {
        val yesterday = System.currentTimeMillis() - 86_400_000L
        repository.addIntake(createIntake(caffeineMg = 200, timestamp = yesterday))
        repository.addIntake(createIntake(caffeineMg = 50))

        repository.getTodayIntakes().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(50, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll should remove all entries`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 100))
        repository.clearAll()

        repository.getTodayIntakes().test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty repository should emit zero total and empty list`() = runTest {
        repository.getTodayIntakes().test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        repository.getTodayTotalCaffeine().test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple intakes should be ordered by timestamp descending`() = runTest {
        val now = System.currentTimeMillis()
        repository.addIntake(createIntake(caffeineMg = 10, timestamp = now - 10_000))
        repository.addIntake(createIntake(caffeineMg = 20, timestamp = now))
        repository.addIntake(createIntake(caffeineMg = 30, timestamp = now - 5_000))

        repository.getTodayIntakes().test {
            val list = awaitItem()
            assertEquals(listOf(20, 30, 10), list.map { it.caffeineMg })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createIntake(
        caffeineMg: Int = 63,
        timestamp: Long = System.currentTimeMillis()
    ): CaffeineIntake {
        return CaffeineIntake(
            drinkName = "test_drink",
            caffeineMg = caffeineMg,
            volumeMl = 250,
            timestamp = timestamp
        )
    }
}
