package com.example.fitnesstracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getString
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CheckNearbyUsersWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val socialModel = SocialModel()

    override suspend fun doWork(): Result {
        Log.d("CheckNearbyUsersWorker", "doWork called")

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid == null) {
            Log.e("CheckNearbyUsersWorker", "Current user UID is null")
            return Result.failure()
        }
        Log.d("CheckNearbyUsersWorker", "Current user UID: $currentUserUid")

        val currentUserLocation = socialModel.getCurrentUserLocation(currentUserUid)
        if (currentUserLocation == null) {
            Log.e("CheckNearbyUsersWorker", "Current user location is null")
            return Result.failure()
        }
        Log.d("CheckNearbyUsersWorker", "Current user location: $currentUserLocation")

        val tenMinutesAgo = (System.currentTimeMillis() - 10 * 60 * 1000).toDouble()
        Log.d("CheckNearbyUsersWorker", "Timestamp for 10 minutes ago: $tenMinutesAgo")

        val nearbyUsers = socialModel.getNearbyUsers(tenMinutesAgo)
        if (nearbyUsers == null) {
            Log.e("CheckNearbyUsersWorker", "Nearby users snapshot is null")
            return Result.failure()
        }

        nearbyUsers.forEach { user ->
            Log.d("CheckNearbyUsersWorker", "Nearby user: $user")
            if (user.username != LoggedUser.username) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    currentUserLocation.lastLatitude, currentUserLocation.lastLongitude,
                    user.lastLatitude, user.lastLongitude, distance
                )
                Log.d("CheckNearbyUsersWorker", "Distance to user ${user.id}: ${distance[0]} meters")
                if (distance[0] <= 10) {
                    sendNotification(user.username ?: "Unknown")
                }
            } else {
                Log.d("CheckNearbyUsersWorker", "Skipping current user in nearby users")
            }
        }

        return Result.success()
    }

    private fun sendNotification(username: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "nearby_users_channel"
            val channelName = "Nearby Users Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)

            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, importance)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, "nearby_users_channel")
            .setContentTitle(applicationContext.getString(R.string.utente_vicino))
            .setContentText(applicationContext.getString(R.string.notifica_testo, username))
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (notificationManager != null) {
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notificationBuilder.build())
            Log.d("CheckNearbyUsersWorker", "Notification sent for user: $username")
        } else {
            Log.e("CheckNearbyUsersWorker", "Failed to send notification: notificationManager is null")
        }
    }
}



