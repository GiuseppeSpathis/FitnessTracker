package com.example.fitnesstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
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

    private var stepLengthInMeters = 0.762f
    private var startTime = System.currentTimeMillis()
    private var distanceGoal = 5.0f // esempio di distanza in km
    private lateinit var distanceGoalTextView: TextView
    private lateinit var db: AppDatabase
    private lateinit var attivitàDao: ActivityDao
    private var pace = 0.0f
    private val Model : Model = Model()
    private var timerHandler: Handler = Handler()

    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = millis / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            timeCounterText.setText(String.format(Locale.getDefault(), getString(R.string.tempo), minutes, remainingSeconds))
            timerHandler.postDelayed(this, 1000)

            val distanceInKm = stepCount * stepLengthInMeters / 1000
            if (distanceInKm > 0) {
                pace = minutes / distanceInKm
                paceText.text = String.format(Locale.getDefault(), getString(R.string.passo), pace)
            } else {
                paceText.text = getString(R.string.no_pace_data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        timerHandler.postDelayed(timerRunnable, 0)
    }

    override fun onStop() {
        super.onStop()

        sensorManager.unregisterListener(this)
        timerHandler.removeCallbacks(timerRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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

        distanceGoal = intent.getFloatExtra(getString(R.string.distance_goal), 5.0f)
        Log.d("RunActivity", "Ricevuto distanza obiettivo: $distanceGoal")
        progressBar.max = (distanceGoal * 1000).toInt()

        distanceGoalTextView.text = getString(R.string.distance_goal_format, distanceGoal)

        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        stopButton.setOnClickListener {
            onStopButtonclicked()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("stepCount", stepCount)
        outState.putInt("initialStepCount", initialStepCount)
        outState.putLong("startTime", startTime)
        outState.putFloat("pace", pace)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stepCount = savedInstanceState.getInt("stepCount")
        initialStepCount = savedInstanceState.getInt("initialStepCount")
        startTime = savedInstanceState.getLong("startTime")
        pace = savedInstanceState.getFloat("pace")

        stepCounterText.text = getString(R.string.step_count_format, stepCount)
        val distanceInMeters = stepCount * stepLengthInMeters
        distanceCounterText.text = String.format(Locale.getDefault(), getString(R.string.distanza), distanceInMeters / 1000)
        progressBar.progress = distanceInMeters.toInt()
        paceText.text = String.format(Locale.getDefault(), getString(R.string.passo), pace)

        if (distanceInMeters >= distanceGoal * 1000) {
            distanceGoalTextView.text = getString(R.string.gol_raggiunto)
        }

        timerHandler.postDelayed(timerRunnable, 0)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == 0) {
                initialStepCount = totalSteps
            }
            stepCount = totalSteps - initialStepCount
            stepCounterText.setText(getString(R.string.step_count_format, stepCount))

            val distanceInMeters = stepCount * stepLengthInMeters
            distanceCounterText.setText(String.format(Locale.getDefault(), getString(R.string.distanza), distanceInMeters / 1000))
            progressBar.setProgress(distanceInMeters.toInt())

            if (distanceInMeters >= distanceGoal * 1000) {
                distanceGoalTextView.setText(getString(R.string.gol_raggiunto))
            }
        }
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

        val distanceInKm = stepCount * 0.762f / 1000

        lifecycleScope.launch {
            val success = Model.saveActivity(
                userId,
                startTime,
                endTime,
                stepCount,
                distanceInKm,
                date,
                pace,
                "Corsa",
                null,
                null,
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
                    stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) as Sensor
                }
            }
        }
    }
}
