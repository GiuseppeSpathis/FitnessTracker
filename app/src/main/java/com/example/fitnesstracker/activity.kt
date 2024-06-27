package com.example.fitnesstracker
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "attività")
data class Attività(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val date: String,
    val activityType: String,
    val stepCount: Int?,
    val distance: Float?,
    val pace: Float?,
    val avgSpeed: Double?,
    val maxSpeed: Double?,
)

@Entity(
    tableName = "othersActivity",
    indices = [Index(value = ["username", "startTime", "endTime", "date", "activityType"], unique = true)]
)
data class OthersActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val date: String,
    val activityType: String,
    val stepCount: Int?,
    val distance: Float?,
    val pace: Float?,
    val avgSpeed: Double?,
    val maxSpeed: Double?,
) {
    constructor(username: String, attività: Attività) : this(
        username = username,
        startTime = attività.startTime,
        endTime = attività.endTime,
        date = attività.date,
        activityType = attività.activityType,
        stepCount = attività.stepCount,
        distance = attività.distance,
        pace = attività.pace,
        avgSpeed = attività.avgSpeed,
        maxSpeed = attività.maxSpeed
    )
}

@Entity(tableName = "geofences_new")
data class GeoFence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val placeName: String
)

@Entity(tableName = "timeGeofences")
data class timeGeofence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val enterTime: Long? = null,
    var exitTime: Long? = null,
    val date: String,
    val placeName: String
)

