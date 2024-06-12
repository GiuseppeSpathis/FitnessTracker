package com.example.fitnesstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class RunActivity : AppCompatActivity(), SensorEventListener {


    private lateinit var stepCounterText: TextView
    private lateinit var distanceCounterText: TextView
    private lateinit var timeCounterText: TextView
    private lateinit var paceText: TextView
    private lateinit var stopButton: Button

    private lateinit var sensorManager: SensorManager
    private lateinit var stepCounterSensor: Sensor

    private var initialStepCount = 0
    private var stepCount = 0

    private lateinit var progressBar: ProgressBar
    private var isStopped = false

    private var timeStopped = 0
    private var stepLengthInMeters = 0.762f
    private var startTime = System.currentTimeMillis()
    private var distanceGoal = 5.0f // esempio di distanza in km
    private lateinit var distanceGoalTextView: TextView
    private lateinit var db: AppDatabase
    private lateinit var attivitàDao: ActivityDao
    private var pace = 0.0f

    private var timerHandler: Handler = Handler()

    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = millis / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            timeCounterText.setText(String.format(Locale.getDefault(), "Time: %02d:%02d", minutes, remainingSeconds))
            timerHandler.postDelayed(this, 1000)

            // Calcolare la media minuti per km
            val distanceInKm = stepCount * stepLengthInMeters / 1000
            if (distanceInKm > 0) {
                pace = minutes / distanceInKm
                paceText.setText(String.format(Locale.getDefault(), "Pace: %.2f min/km", pace))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            timerHandler.postDelayed(timerRunnable, 0)
        }
    }

    override fun onStop() {
        super.onStop()
        if (stepCounterSensor != null) {
            sensorManager.unregisterListener(this)
            timerHandler.removeCallbacks(timerRunnable)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run)

        val permission = Manifest.permission.ACTIVITY_RECOGNITION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }

        stepCounterText = findViewById(R.id.stepCounterTextView)
        distanceCounterText = findViewById(R.id.distanceCounterTextView)
        timeCounterText = findViewById(R.id.timeCounterTextView)
        paceText = findViewById(R.id.paceTextView)
        stopButton = findViewById(R.id.stopButton)
        distanceGoalTextView = findViewById(R.id.distanceTargetTextView)
        progressBar = findViewById(R.id.progressBar)

        startTime = System.currentTimeMillis()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) as Sensor

        distanceGoal = intent.getFloatExtra("DISTANCE_GOAL", 5.0f)
        Log.d("RunActivity", "Ricevuto distanza obiettivo: $distanceGoal")
        progressBar.max = (distanceGoal * 1000).toInt() // Convertire in metri

        distanceGoalTextView.text = "Distance Goal: %.2f km".format(distanceGoal)

        if (stepCounterSensor == null) {
            stepCounterText.text = "Step counter not available"
        }

        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        stopButton.setOnClickListener {
            onStopButtonclicked(it)
        }
    }


    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == 0) {
                initialStepCount = totalSteps
            }
            stepCount = totalSteps - initialStepCount
            stepCounterText.setText("Step Count: " + stepCount)

            val distanceInMeters = stepCount * stepLengthInMeters
            distanceCounterText.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceInMeters / 1000))
            progressBar.setProgress(distanceInMeters.toInt())

            if (distanceInMeters >= distanceGoal * 1000) {
                distanceGoalTextView.setText("Distance Goal Achieved")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //dskf
    }

    public fun onStopButtonclicked(view: View) {
        val endTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(endTime))
        val userId = LoggedUser.id

        val distanceInKm = stepCount * 0.762f / 1000

        val attività = Attività(
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            stepCount = stepCount,
            distance = distanceInKm,
            date = date,
            pace = pace,
            activityType = "Corsa",
            avgSpeed = null,
            maxSpeed = null
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
                    stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) as Sensor
                }
            }
        }
    }
}