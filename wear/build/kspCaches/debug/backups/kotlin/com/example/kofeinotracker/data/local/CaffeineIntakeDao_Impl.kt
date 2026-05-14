package com.example.kofeinotracker.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.kofeinotracker.domain.model.CaffeineIntake
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CaffeineIntakeDao_Impl(
  __db: RoomDatabase,
) : CaffeineIntakeDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCaffeineIntake: EntityInsertAdapter<CaffeineIntake>
  init {
    this.__db = __db
    this.__insertAdapterOfCaffeineIntake = object : EntityInsertAdapter<CaffeineIntake>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `caffeine_intakes` (`id`,`drinkName`,`caffeineMg`,`volumeMl`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CaffeineIntake) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.drinkName)
        statement.bindLong(3, entity.caffeineMg.toLong())
        statement.bindLong(4, entity.volumeMl.toLong())
        statement.bindLong(5, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(intake: CaffeineIntake): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfCaffeineIntake.insertAndReturnId(_connection, intake)
    _result
  }

  public override fun getTodayIntakes(startOfDay: Long, endOfDay: Long):
      Flow<List<CaffeineIntake>> {
    val _sql: String =
        "SELECT * FROM caffeine_intakes WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("caffeine_intakes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfDay)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDrinkName: Int = getColumnIndexOrThrow(_stmt, "drinkName")
        val _columnIndexOfCaffeineMg: Int = getColumnIndexOrThrow(_stmt, "caffeineMg")
        val _columnIndexOfVolumeMl: Int = getColumnIndexOrThrow(_stmt, "volumeMl")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<CaffeineIntake> = mutableListOf()
        while (_stmt.step()) {
          val _item: CaffeineIntake
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDrinkName: String
          _tmpDrinkName = _stmt.getText(_columnIndexOfDrinkName)
          val _tmpCaffeineMg: Int
          _tmpCaffeineMg = _stmt.getLong(_columnIndexOfCaffeineMg).toInt()
          val _tmpVolumeMl: Int
          _tmpVolumeMl = _stmt.getLong(_columnIndexOfVolumeMl).toInt()
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = CaffeineIntake(_tmpId,_tmpDrinkName,_tmpCaffeineMg,_tmpVolumeMl,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTodayTotalCaffeine(startOfDay: Long, endOfDay: Long): Flow<Int> {
    val _sql: String =
        "SELECT COALESCE(SUM(caffeineMg), 0) FROM caffeine_intakes WHERE timestamp >= ? AND timestamp < ?"
    return createFlow(__db, false, arrayOf("caffeine_intakes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfDay)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM caffeine_intakes"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
