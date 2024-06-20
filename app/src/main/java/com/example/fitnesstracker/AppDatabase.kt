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
                    .addMigrations(MIGRATION_1_2) // Aggiungi la migrazione da 1 a 2
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Esegui eventuali modifiche allo schema qui
                // Per esempio, aggiungi la tabella OthersActivity
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
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with the updated schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `geofences_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `radius` REAL NOT NULL
                    )
                """)

                // Copy the data from the old table to the new table
                database.execSQL("""
                    INSERT INTO `geofences_new` (id, latitude, longitude, radius)
                    SELECT id, latitude, longitude, radius
                    FROM geofences
                """)

                // Remove the old table
                database.execSQL("DROP TABLE geofences")

                // Rename the new table to the old table name
                database.execSQL("ALTER TABLE geofences_new RENAME TO geofences")
            }
        }
    }
}


