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
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.views.MapView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationUpdatesService : JobIntentService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var db: AppDatabase
    private var isInsideGeofence = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = AppDatabase.getDatabase(this)


        createLocationRequest()
        startForegroundService()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
            setMaxUpdateDelayMillis(20000)
        }.build()
    }

    private fun startForegroundService() {
        val notificationChannelId = "location_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Location Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel used by location service"
            }

            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId).apply {
            setContentTitle("Tracking Location")
            setContentText("Your location is being tracked")
            setSmallIcon(R.drawable.logo)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }.build()

        startForeground(1, notification)
     //   simulateGeofenceTransitions()
    }

    override fun onHandleWork(intent: Intent) {
        startLocationUpdates()
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
            }
        }
    }


    private fun handleLocationUpdate(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
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
                    if (!isInsideGeofence) {
                        println("Entered a geofence")
                        isInsideGeofence = true
                        val enterTime = System.currentTimeMillis()
                        withContext(Dispatchers.Main) {
                            sendNotification("Entered Geofence", "You have entered a geofence.")
                        }

                        // Create and save a new instance of timeGeofence for the entry
                        val timeGeofence = timeGeofence(
                            latitude = geofence.latitude,
                            longitude = geofence.longitude,
                            radius = geofence.radius,
                            enterTime = enterTime,
                            exitTime = 0L, // Placeholder, will be updated on exit
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            placeName = geofence.placeName
                        )
                        db.attivitàDao().insertTimeGeofence(timeGeofence)
                    }
                    break
                }
            }

            if (!foundGeofence && isInsideGeofence) {
                println("Exited from a geofence")
                isInsideGeofence = false
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
        }
    }
    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "location_service_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun simulateGeofenceTransitions() {
        val insideLocation = Location("fused").apply {
            latitude = 44.4998854 // Inside geofence coordinates
            longitude = 11.3710302
            accuracy = 3.0f
            time = System.currentTimeMillis()
        }

        val outsideLocation = Location("fused").apply {
            latitude = 54.4998854 // Outside geofence coordinates
            longitude = 19.37103
            accuracy = 3.0f
            time = System.currentTimeMillis()
        }

        CoroutineScope(Dispatchers.IO).launch {
            handleLocationUpdate(insideLocation) // Simulate entering geofence
            delay(5 * 60 * 1000)
            handleLocationUpdate(outsideLocation) // Simulate exiting geofence
        }
    }

    companion object {
        private const val JOB_ID = 1000

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, LocationUpdatesService::class.java, JOB_ID, intent)
        }
    }
}

