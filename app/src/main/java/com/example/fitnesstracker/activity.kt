package com.example.fitnesstracker
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attività")
data class Attività(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val date: String,
    val activityType: String,
    val stepCount: Int?,
    val distance: Float?,
    val pace: Float?,
    val avgSpeed: Double?,
    val maxSpeed: Double?,
)
