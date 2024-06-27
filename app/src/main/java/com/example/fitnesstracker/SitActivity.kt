package com.example.fitnesstracker
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class SitActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var totalTimeTextView: TextView
    private lateinit var lastStandTimeTextView: TextView
    private lateinit var resetButton: Button
    private lateinit var stopButton: Button
    private var startTime = System.currentTimeMillis()
    private var totalTime = 0L
    private var lastStandTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManagerCompat
    private var maxSitTime = 10
    private lateinit var db: AppDatabase
    private lateinit var attivitàDao: ActivityDao

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            totalTime += 1
            lastStandTime += 1
            updateTextViews()

            if (lastStandTime >= maxSitTime) {
                showNotification()
                lastStandTime = 0L
            }

            handler.postDelayed(this, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sit)

        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        lastStandTimeTextView = findViewById(R.id.lastStandTimeTextView)
        resetButton = findViewById(R.id.resetButton)
        stopButton = findViewById(R.id.stopButton)

        notificationManager = NotificationManagerCompat.from(this)

        createNotificationChannel()

        resetButton.setOnClickListener {
            lastStandTime = 0L
            updateTextViews()
        }

        stopButton.setOnClickListener {
            handler.removeCallbacks(updateTimeRunnable)
            onStopButtonclicked(it)
        }

        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        checkAndRequestNotificationPermission()
        handler.post(updateTimeRunnable)
    }

    private fun updateTextViews() {
        totalTimeTextView.text = "Total Time: ${formatTime(totalTime)}"
        lastStandTimeTextView.text = "Time since last stand: ${formatTime(lastStandTime)}"
    }

    private fun formatTime(timeInSeconds: Long): String {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("totalTime", totalTime)
        outState.putLong("lastStandTime", lastStandTime)
        outState.putLong("startTime", startTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        totalTime = savedInstanceState.getLong("totalTime")
        lastStandTime = savedInstanceState.getLong("lastStandTime")
        startTime = savedInstanceState.getLong("startTime")

        updateTextViews()
        handler.post(updateTimeRunnable)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                showNotification()
            } else {
                Toast.makeText(this, "Permesso per le notifiche negato", Toast.LENGTH_SHORT).show()
            }
        }
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
            startTime = startTime,
            endTime = endTime,
            stepCount = null,
            distance = null,
            date = date,
            pace = null,
            activityType = "Stare fermo",
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

    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, "your_channel_id")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Ricorda di alzarti")
            .setContentText("È passato mezz'ora dall'ultima volta che ti sei alzato. Ricorda di fare una pausa e alzarti.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(notificationManager) {
            if (ActivityCompat.checkSelfPermission(
                    this@SitActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, builder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Channel"
            val descriptionText = "Channel for reminder notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("your_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
