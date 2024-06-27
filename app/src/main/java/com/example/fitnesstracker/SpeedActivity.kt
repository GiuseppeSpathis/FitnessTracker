package com.example.fitnesstracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

class SpeedActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var speedTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var maxSpeedTextView: TextView

    private lateinit var sensorManager: SensorManager
    private lateinit var speedSensor: Sensor
    private var startTime = System.currentTimeMillis()

    private var speed = 0.0
    private var avgSpeed = 0.0
    private var maxSpeed = 50.0 // Default max speed in km/h
    private var totalSpeed = 0.0
    private var speedCount = 0

    private var maxSpeedRecorded = 0.0
    private lateinit var stopButton: Button
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var db: AppDatabase
    private lateinit var attivitàDao: ActivityDao

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed)

        maxSpeed = intent.getDoubleExtra("SPEED_LIMIT", 50.0)

        speedTextView = findViewById(R.id.speedTextView)
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView)
        maxSpeedTextView = findViewById(R.id.maxSpeedTextView)

        maxSpeedTextView.text = String.format(Locale.getDefault(), "Max Speed: %.2f km/h", maxSpeed)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        speedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!

        stopButton = findViewById(R.id.stopButton)
        notificationManager = NotificationManagerCompat.from(this)

        val permission = Manifest.permission.ACTIVITY_RECOGNITION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }

        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        stopButton.setOnClickListener {
            onStopButtonclicked(it)
        }

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        if (speedSensor != null) {
            sensorManager.registerListener(this, speedSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        if (speedSensor != null) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val linearAcceleration = Math.sqrt(
                (event.values[0] * event.values[0]
                        + event.values[1] * event.values[1]
                        + event.values[2] * event.values[2]).toDouble()
            )
            speed = linearAcceleration * 3.6 // Converti m/s^2 in km/h

            totalSpeed += speed
            speedCount++
            avgSpeed = totalSpeed / speedCount

            if (speed > maxSpeedRecorded) {
                maxSpeedRecorded = speed
            }

            speedTextView.text = String.format(Locale.getDefault(), "Speed: %.2f km/h", speed)
            avgSpeedTextView.text = String.format(Locale.getDefault(), "Avg Speed: %.2f km/h", avgSpeed)
            maxSpeedTextView.text = String.format(Locale.getDefault(), "Max Speed: %.2f km/h", maxSpeedRecorded)

            if (speed > maxSpeed) {
                avgSpeedTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                // Notifica all'utente di rallentare
                sendNotification()
            } else {
                avgSpeedTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        }
    }

    private fun sendNotification() {
        val notificationBuilder = NotificationCompat.Builder(this, "speed_channel")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Velocità eccessiva")
            .setContentText("Stai superando la velocità impostata! Rallenta.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(notificationManager) {
            if (ActivityCompat.checkSelfPermission(
                    this@SpeedActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, notificationBuilder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Speed Channel"
            val descriptionText = "Channel for speed limit notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("speed_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public fun onStopButtonclicked(view: View) {
        val endTimeMillis = System.currentTimeMillis()
        val endTime = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val startTime = Instant.ofEpochMilli(this.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(endTimeMillis))
        val userId = LoggedUser.id

        val attività = Attività(
            userId = userId,
            startTime = startTime,  // Assicurati che startTime sia già stato inizializzato in precedenza
            endTime = endTime,
            stepCount = null,
            distance = null,
            date = date,
            pace = null,
            activityType = "Guidare",
            avgSpeed = avgSpeed,
            maxSpeed = maxSpeedRecorded
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                attivitàDao.insertActivity(attività)
                Log.d("RunActivity", "Attività salvata: $attività")
            }
            withContext(Dispatchers.Main) {
                showSuccessPopup()
            }
        }
    }

    private fun showSuccessPopup() {
        AlertDialog.Builder(this)
            .setTitle("Successo")
            .setMessage("Dati salvati con successo!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    speedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("startTime", startTime)
        outState.putDouble("speed", speed)
        outState.putDouble("avgSpeed", avgSpeed)
        outState.putDouble("maxSpeedRecorded", maxSpeedRecorded)
        outState.putDouble("totalSpeed", totalSpeed)
        outState.putInt("speedCount", speedCount)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        startTime = savedInstanceState.getLong("startTime")
        speed = savedInstanceState.getDouble("speed")
        avgSpeed = savedInstanceState.getDouble("avgSpeed")
        maxSpeedRecorded = savedInstanceState.getDouble("maxSpeedRecorded")
        totalSpeed = savedInstanceState.getDouble("totalSpeed")
        speedCount = savedInstanceState.getInt("speedCount")

        updateTextViews()
        sensorManager.registerListener(this, speedSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun updateTextViews() {
        speedTextView.text = String.format(Locale.getDefault(), "Speed: %.2f km/h", speed)
        avgSpeedTextView.text = String.format(Locale.getDefault(), "Avg Speed: %.2f km/h", avgSpeed)
        maxSpeedTextView.text = String.format(Locale.getDefault(), "Max Speed: %.2f km/h", maxSpeedRecorded)
    }
}

