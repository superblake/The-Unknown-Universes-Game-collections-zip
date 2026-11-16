package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset

@Database(
  entities = [
    OperatorProfile::class,
    ArmoryItem::class,
    RigAsset::class,
    MatchRecord::class,
    LinkedAccount::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(UniverseTypeConverters::class)
abstract class UniverseDatabase : RoomDatabase() {
  abstract fun universeDao(): UniverseDao

  companion object {
    @Volatile
    private var INSTANCE: UniverseDatabase? = null

    fun getInstance(context: Context): UniverseDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          UniverseDatabase::class.java,
          "unknown_universes_os.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
