package com.example.fitnesstracker

import android.app.Activity
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.google.android.gms.location.Geofence
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Query("SELECT * FROM attività WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getStepCountDataForLastWeek(startDate: String, endDate: String): List<Attività>

    @Query("SELECT distance FROM attività WHERE activityType = 'Corsa' AND date BETWEEN :startDate AND :endDate")
    suspend fun getDistanceDataForLastWeek(startDate: String, endDate: String): List<Float>

    @Query("SELECT avgSpeed FROM attività WHERE activityType = 'Guidare' AND date BETWEEN :startDate AND :endDate")
    suspend fun getAvgSpeedDataForLastWeek(startDate: String, endDate: String): List<Double>

    @Query("SELECT (julianday(endTime) - julianday(startTime)) * 24 * 60 FROM attività WHERE activityType = 'Stare fermo' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTimeStationaryDataForLastWeek(startDate: String, endDate: String): List<Double>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOthersActivity(attivita: OthersActivity)

    @Query("SELECT * FROM OthersActivity WHERE date = :date")
    fun getOtherActivitiesByDate(date: String): List<OthersActivity>

    @Query("SELECT DISTINCT date FROM Attività WHERE activityType = :type")
    fun getDatesByActivityType(type: String): List<String>
    @Insert
    suspend fun insertGeofence(geofence: GeoFence)

    @Query("SELECT * FROM geofences_new")
    suspend fun getAllGeofences(): List<GeoFence>

    @Delete
    suspend fun deleteGeofence(geofence: GeoFence)

    @Update
    suspend fun updateGeofence(geoFence: GeoFence)

    @Insert
    suspend fun insertTimeGeofence(timegeofence: timeGeofence)

    @Query("SELECT * FROM timeGeofences WHERE latitude = :latitude AND longitude = :longitude AND radius = :radius ORDER BY id DESC LIMIT 1")
    suspend fun getLastTimeGeofenceByCoordinates(latitude: Double, longitude: Double, radius: Float): timeGeofence

    @Update
    suspend fun updateTimeGeofence(timegeofence: timeGeofence)

    @Query("SELECT * FROM timeGeofences")
    suspend fun getAllTimeGeofences(): List<timeGeofence>


    @Query("SELECT * FROM timeGeofences WHERE date = :date")
    suspend fun getGeofencesForDate(date: String): List<timeGeofence>
    @Query("SELECT * FROM timeGeofences WHERE date BETWEEN :startDate AND :endDate")
    fun getGeofencesByDateRange(startDate: String, endDate: String): List<timeGeofence>
}






