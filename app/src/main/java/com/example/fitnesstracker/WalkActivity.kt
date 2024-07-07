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


class WalkActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var stepCounterText: TextView
    private lateinit var distanceCounterText: TextView
    private lateinit var timeCounterText: TextView
    private lateinit var stopButton: Button

    private lateinit var sensorManager: SensorManager
    private lateinit var stepCounterSensor: Sensor

    private var initialStepCount = 0
    private var stepCount = 0

    private lateinit var progressBar: ProgressBar
    private var stepLenghtInMeters = 0.762f
    private var startTime = System.currentTimeMillis()
    private var stepCountTarget = 8000
    private lateinit var stepCounterTargetTextView : TextView
    private val Model: Model = Model()
    private var timerHandler : Handler = Handler()

    private var timerRunnable : Runnable = object : Runnable {
        override fun run() {
            val milis = System.currentTimeMillis() - startTime
            var seconds = milis / 1000
            val min = seconds / 60
            seconds %= 60
            timeCounterText.text = getString(R.string.tempo, min, seconds)
            timerHandler.postDelayed(this, 1000)
        }
    }

    private lateinit var db: AppDatabase
    private lateinit var attivitàDao: ActivityDao

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
        setContentView(R.layout.activity_walk)

        val permission = Manifest.permission.ACTIVITY_RECOGNITION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }

        stepCounterText = findViewById(R.id.stepCounterTextView)
        distanceCounterText = findViewById(R.id.distanceCounterTextView)
        timeCounterText = findViewById(R.id.timeCounterTextView)
        stopButton = findViewById(R.id.stopButton)
        stepCounterTargetTextView = findViewById(R.id.stepCountTargetTextView)
        progressBar = findViewById(R.id.progressBar)

        startTime = System.currentTimeMillis()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) as Sensor
        
        stepCountTarget = intent.getIntExtra(getString(R.string.step_goal), 8000)
        progressBar.max = stepCountTarget

        stepCounterTargetTextView.text = getString(R.string.gol_passi, stepCountTarget)

        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        stopButton.setOnClickListener{
            onStopButtonclicked()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("stepCount", stepCount)
        outState.putInt("initialStepCount", initialStepCount)
        outState.putLong("startTime", startTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stepCount = savedInstanceState.getInt("stepCount")
        initialStepCount = savedInstanceState.getInt("initialStepCount")
        startTime = savedInstanceState.getLong("startTime")

        stepCounterText.text = getString(R.string.step_count_format, stepCount)
        progressBar.progress = stepCount

        val distanceInKm = stepCount * stepLenghtInMeters / 1000
        distanceCounterText.text = getString(R.string.distanza, distanceInKm)

        timerHandler.postDelayed(timerRunnable, 0)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if(initialStepCount == 0) {
                initialStepCount = totalSteps
            }
            stepCount = totalSteps - initialStepCount
            stepCounterText.text = getString(R.string.step_count_format, stepCount)
            progressBar.progress = stepCount

            if(stepCount >= stepCountTarget) {
                stepCounterTargetTextView.text = getString(R.string.gol_raggiunto)
            }

            val distanceInKm = stepCount * stepLenghtInMeters / 1000
            distanceCounterText.text = getString(R.string.distanza, distanceInKm)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

     fun onStopButtonclicked() {
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

        val distanceInKm = stepCount * stepLenghtInMeters / 1000

        lifecycleScope.launch {
            val success = Model.saveActivity(
                userId,
                startTime,
                endTime,
                stepCount,
                distanceInKm,
                date,
                null,
                "Passeggiata",
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

