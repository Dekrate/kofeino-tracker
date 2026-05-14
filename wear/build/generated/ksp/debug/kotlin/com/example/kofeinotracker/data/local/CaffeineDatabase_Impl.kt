package com.example.kofeinotracker.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CaffeineDatabase_Impl : CaffeineDatabase() {
  private val _caffeineIntakeDao: Lazy<CaffeineIntakeDao> = lazy {
    CaffeineIntakeDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "e7b5eaec576c1d06eeb312c5aa378854", "8ef10c16231ab5a59b5c962d152fa276") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `caffeine_intakes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `drinkName` TEXT NOT NULL, `caffeineMg` INTEGER NOT NULL, `volumeMl` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e7b5eaec576c1d06eeb312c5aa378854')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `caffeine_intakes`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsCaffeineIntakes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCaffeineIntakes.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCaffeineIntakes.put("drinkName", TableInfo.Column("drinkName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCaffeineIntakes.put("caffeineMg", TableInfo.Column("caffeineMg", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCaffeineIntakes.put("volumeMl", TableInfo.Column("volumeMl", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCaffeineIntakes.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCaffeineIntakes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCaffeineIntakes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCaffeineIntakes: TableInfo = TableInfo("caffeine_intakes", _columnsCaffeineIntakes,
            _foreignKeysCaffeineIntakes, _indicesCaffeineIntakes)
        val _existingCaffeineIntakes: TableInfo = read(connection, "caffeine_intakes")
        if (!_infoCaffeineIntakes.equals(_existingCaffeineIntakes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |caffeine_intakes(com.example.kofeinotracker.domain.model.CaffeineIntake).
              | Expected:
              |""".trimMargin() + _infoCaffeineIntakes + """
              |
              | Found:
              |""".trimMargin() + _existingCaffeineIntakes)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "caffeine_intakes")
  }

  public override fun clearAllTables() {
    super.performClear(false, "caffeine_intakes")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CaffeineIntakeDao::class, CaffeineIntakeDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun caffeineIntakeDao(): CaffeineIntakeDao = _caffeineIntakeDao.value
}
