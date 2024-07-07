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
import kotlin.math.sqrt

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
    private val Model = Model()
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed)

        maxSpeed = intent.getDoubleExtra(getString(R.string.speed_limit), 50.0)

        speedTextView = findViewById(R.id.speedTextView)
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView)
        maxSpeedTextView = findViewById(R.id.maxSpeedTextView)

        maxSpeedTextView.text = getString(R.string.max_speed, maxSpeed)

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
            onStopButtonclicked()
        }

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, speedSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val linearAcceleration = sqrt(
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

            speedTextView.text = getString(R.string.speed, speed)
            avgSpeedTextView.text = getString(R.string.avg_speed, avgSpeed)
            maxSpeedTextView.text = getString(R.string.max_speed_recorded, maxSpeedRecorded)

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
            .setContentTitle(getString(R.string.speeding_notification_title))
            .setContentText(getString(R.string.speeding_notification_text))
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
        val name = getString(R.string.speed_channel_name)
        val descriptionText = getString(R.string.speed_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("speed_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun showShortActivityPopup() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attività_breve)
            .setMessage(R.string.attività_breve_testo)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .show()
    }
     private fun onStopButtonclicked() {
        val endTimeMillis = System.currentTimeMillis()
        val durationMillis = endTimeMillis - startTime

        if (durationMillis < 60000) {
            showShortActivityPopup()
            return
        }

        val endTime = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val startTime = Instant.ofEpochMilli(this.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(endTimeMillis))
        val userId = LoggedUser.id

        lifecycleScope.launch {
            val success = Model.saveActivity(
                userId,
                startTime,
                endTime,
                null,
                null,
                date,
                null,
                "Guidare",
                avgSpeed,
                maxSpeedRecorded,
                db
            )
            withContext(Dispatchers.Main) {
                if (success) {
                    showSuccessPopup()
                } else {
                    println("error while trying to save the activity")
                }
            }
        }
    }

    private fun showSuccessPopup() {
        AlertDialog.Builder(this)
            .setTitle(R.string.successo)
            .setMessage(R.string.dati_salvati)
            .setPositiveButton(R.string.ok) { dialog, _ ->
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
        speedTextView.text = getString(R.string.speed, speed)
        avgSpeedTextView.text = getString(R.string.avg_speed, avgSpeed)
        maxSpeedTextView.text = getString(R.string.max_speed_recorded, maxSpeedRecorded)
    }
}


