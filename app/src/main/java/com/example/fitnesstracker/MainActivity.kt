package com.example.fitnesstracker

import Utils.scheduleDailyNotification
import Utils.scheduleReminder
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkPermissions()) {
            LocationUpdatesService.startServiceIfNotRunning(this)
            startCheckNearbyUsersWorker()
        }
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("last_opened", System.currentTimeMillis())
        editor.apply()

        scheduleDailyNotification(this)
        scheduleReminder(this)

        startActivity(Intent(this, LoginActivity::class.java))
        FirebaseApp.initializeApp(this)
    }

    private fun checkPermissions(): Boolean {
        val neededPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun startCheckNearbyUsersWorker() {
        val checkNearbyUsersRequest = PeriodicWorkRequestBuilder<CheckNearbyUsersWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CheckNearbyUsers",
            ExistingPeriodicWorkPolicy.REPLACE,
            checkNearbyUsersRequest
        )
    }

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}


