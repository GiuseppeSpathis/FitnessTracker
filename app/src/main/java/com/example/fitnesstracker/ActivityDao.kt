package com.example.fitnesstracker

import android.app.Activity
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.android.gms.location.Geofence
import java.time.LocalDate

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(attività: Attività)

    @Query("SELECT * FROM Attività WHERE date = :date")
    fun getAttivitàByDate(date: String): List<Attività>

    @Query("DELETE FROM attività")
    suspend fun deleteAll()

    @Query("SELECT * FROM Attività WHERE date BETWEEN :startDate AND :endDate")
    fun getAttivitàByDateRange(startDate: String, endDate: String): List<Attività>

    @Query("SELECT * FROM Attività")
    fun getAllActivitites() : List<Attività>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOthersActivity(attivita: OthersActivity)

    @Insert
    suspend fun insertGeofence(geofence: GeoFence)

    @Query("SELECT * FROM geofences")
    suspend fun getAllGeofences(): List<GeoFence>

    @Delete
    suspend fun deleteGeofence(geofence: GeoFence)


}



