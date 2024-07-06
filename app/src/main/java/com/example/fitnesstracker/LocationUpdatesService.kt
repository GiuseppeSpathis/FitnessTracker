package com.example.fitnesstracker

import android.Manifest
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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationUpdatesService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var db: AppDatabase
    private var inside: Boolean = false
    private lateinit var model: Model

    override fun onCreate() {
        super.onCreate()
        saveServiceRunningState(true)
        db = AppDatabase.getDatabase(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        model = Model()
        createLocationRequest()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "location_updates"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Updates", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.location_service))
            .setContentText(getString(R.string.track_background))
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
                handleLocationUpdate(location, db, inside) { updatedInside ->
                    inside = updatedInside
                }
                model.updateLocationInFirebase(location)
            }
        }
    }

    private fun handleLocationUpdate(location: Location, db: AppDatabase, inside: Boolean, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geofences = db.attivitàDao().getAllGeofences()
                var foundGeofence = false
                var isInside = inside

                for (geofence in geofences) {
                    val distance = FloatArray(2)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        geofence.latitude, geofence.longitude,
                        distance
                    )

                    if (distance[0] < geofence.radius) {
                        foundGeofence = true

                        if (!isInside) {
                            isInside = true
                            val enterTime = System.currentTimeMillis()
                            withContext(Dispatchers.Main) {
                                sendNotification(getString(R.string.entered_geofence_title), getString(R.string.geofence_entered_message, geofence.placeName))
                            }
                            val timeGeofence = timeGeofence(
                                latitude = geofence.latitude,
                                longitude = geofence.longitude,
                                radius = geofence.radius,
                                enterTime = enterTime,
                                exitTime = 0L,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                placeName = geofence.placeName,
                                userId = LoggedUser.id
                            )
                            db.attivitàDao().insertTimeGeofence(timeGeofence)
                        }
                        break
                    }
                }

                if (!foundGeofence && isInside) {
                    isInside = false
                    val exitTime = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        sendNotification(getString(R.string.exited_geofence_title), getString(R.string.exited_geofence))
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
                callback(isInside)
            } catch (e: Exception) {
                Log.e("LocationModel", "Error handling location update", e)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        println("Destroyed")
        saveServiceRunningState(false)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveServiceRunningState(isRunning: Boolean) {
        val sharedPref = getSharedPreferences("ServiceState", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("LocationUpdatesServiceRunning", isRunning)
            apply()
        }
    }


    companion object {
        fun stopLocationService(context: Context) {
            val stopIntent = Intent(context, LocationUpdatesService::class.java)
            context.stopService(stopIntent)
        }
    }
}



