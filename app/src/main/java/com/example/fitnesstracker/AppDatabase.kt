package com.example.fitnesstracker


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Attività::class, OthersActivity::class, GeoFence::class, timeGeofence::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun attivitàDao(): ActivityDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `othersActivity` ( " +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`username` TEXT NOT NULL, " +
                        "`startTime` TEXT NOT NULL, " +
                        "`endTime` TEXT NOT NULL, " +
                        "`date` TEXT NOT NULL, " +
                        "`activityType` TEXT NOT NULL, " +
                        "`stepCount` INTEGER, " +
                        "`distance` REAL, " +
                        "`pace` REAL, " +
                        "`avgSpeed` REAL, " +
                        "`maxSpeed` REAL)")
            }
        }
    }
}


