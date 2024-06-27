package com.example.fitnesstracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.http.NetworkException
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.views.MapView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationUpdatesService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var  db : AppDatabase
    private var inside : Boolean = false

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        startLocationUpdates()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }
    private fun startForegroundService() {
        val channelId = "location_updates"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Updates", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in the background")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
            setMaxUpdateDelayMillis(20000)
        }.build()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                handleLocationUpdate(location)
                updateLocationInFirebase(location)
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        Log.d("LocationService", "Updating user location")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geofences = db.attivitàDao().getAllGeofences()
                var foundGeofence = false

                for (geofence in geofences) {
                    val distance = FloatArray(2)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        geofence.latitude, geofence.longitude,
                        distance
                    )

                    if (distance[0] < geofence.radius) {
                        foundGeofence = true

                        if (!inside) {
                            println("Entered a geofence")
                            inside = true
                            val enterTime = System.currentTimeMillis()
                            withContext(Dispatchers.Main) {
                                sendNotification("Entered Geofence", "You have entered a geofence.")
                            }
                            val timeGeofence = timeGeofence(
                                latitude = geofence.latitude,
                                longitude = geofence.longitude,
                                radius = geofence.radius,
                                enterTime = enterTime,
                                exitTime = 0L,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                placeName = geofence.placeName
                            )
                            db.attivitàDao().insertTimeGeofence(timeGeofence)
                        } else {
                            println("alreadyInside value: $inside")
                        }
                        break
                    }
                }

                if (!foundGeofence && inside) {
                    inside = false
                    val exitTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        sendNotification("Exited Geofence", "You have exited a geofence.")
                    }

                    for (geofence in geofences) {
                        val timeGeofence = db.attivitàDao().getLastTimeGeofenceByCoordinates(
                            geofence.latitude, geofence.longitude, geofence.radius
                        )
                        timeGeofence?.let {
                            it.exitTime = exitTime
                            db.attivitàDao().updateTimeGeofence(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationUpdatesService", "Error updating location in Firebase", e)
            }
        }
    }

    private fun updateLocationInFirebase(location: Location) {
        val database = FirebaseDatabase.getInstance(getString(R.string.db_connection)).reference
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userUpdates = mapOf(
            "lastLatitude" to location.latitude,
            "lastLongitude" to location.longitude,
            "lastUpdated" to System.currentTimeMillis()
        )
        LoggedUser.lastLatitude = location.latitude
        LoggedUser.lastLongitude = location.longitude
        LoggedUser.lastUpdated = System.currentTimeMillis()

        database.child("users").child(uid).updateChildren(userUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("LocationUpdateService", "User location updated successfully")
            } else {
                Log.e("LocationUpdateService", "Error updating location", task.exception)
            }
        }.addOnFailureListener { exception ->
            Log.e("LocationUpdateService", "Failed to update location", exception)
        }
    }

    private fun sendNotification(title: String, text: String) {
        val channelId = "location_updates"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Updates", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun isUserRecentlyInGeofence(geofence: GeoFence): Boolean {
        val distance = FloatArray(2)
        Location.distanceBetween(
            LoggedUser.lastLatitude, LoggedUser.lastLongitude,
            geofence.latitude, geofence.longitude,
            distance
        )

        val fiveMinutesInMillis = 1 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - LoggedUser.lastUpdated

        return distance[0] < geofence.radius && timeDifference <= fiveMinutesInMillis
    }
}
