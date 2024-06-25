package com.example.fitnesstracker


import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle

import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.w3c.dom.Text


class HomeActivity : AppCompatActivity(), MapListener {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_PERMISSION_REQUEST_CODE = 1
    private lateinit var welcomeBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        val imageProfile = findViewById<ImageView>(R.id.image_profile)
        var gender = LoggedUser.gender
        println(gender)
        when (gender) {
            "Maschio" -> imageProfile.setImageResource(R.drawable.male)
            "Femmina" -> imageProfile.setImageResource(R.drawable.female)
            else -> imageProfile.setImageResource(R.drawable.other)
        }
        welcomeBack = findViewById(R.id.username_text)
        var username = LoggedUser.username
        welcomeBack.setText("Bentornato, $username")
        val activityArray = resources.getStringArray(R.array.activity_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinnerActivity = findViewById<Spinner>(R.id.spinner_activity)
        spinnerActivity.adapter = adapter

        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.nav_home


        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, StatsActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.nav_users -> {
                    val intent = Intent(this, Social::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.geofence -> {
                    val intent = Intent(this, GeoFenceActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                else -> false
            }
        }

        map = findViewById(R.id.osmmap)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_REQUEST_CODE)
        } else {
            getLastKnownLocation()
        }

        val trackButton = findViewById<Button>(R.id.button_start_tracking)
        trackButton.setOnClickListener{
            when (spinnerActivity.selectedItem.toString()) {
                "Passeggiata" -> showStepGoalDialog()
                "Corsa" -> showDistanceGoalDialog()
                "Guidare" -> showSpeedLimitDialog()
                "Stare fermo" -> {
                    val intent = Intent(this, SitActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        val logoutButton = findViewById<ImageView>(R.id.logout)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showStepGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gol, null)
        val stepsEditText = dialogView.findViewById<EditText>(R.id.stepsEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            val stepGoal = stepsEditText.text.toString().toIntOrNull()
            if (stepGoal != null) {
                val intent = Intent(this, WalkActivity::class.java)
                intent.putExtra("STEP_GOAL", stepGoal)
                startActivity(intent)
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun showDistanceGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gol_distance, null)
        val distanceEditText = dialogView.findViewById<EditText>(R.id.distanceEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            val distanceGoal = distanceEditText.text.toString().toFloatOrNull()
            if (distanceGoal != null) {
                val intent = Intent(this, RunActivity::class.java)
                intent.putExtra("DISTANCE_GOAL", distanceGoal)
                Log.d("HomeActivity", "Passando distanza obiettivo: $distanceGoal")
                startActivity(intent)
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }


    private fun showSpeedLimitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_speed_limit, null)
        val speedLimitEditText = dialogView.findViewById<EditText>(R.id.speedLimitEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            val speedLimit = speedLimitEditText.text.toString().toDoubleOrNull()
            if (speedLimit != null) {
                val intent = Intent(this, SpeedActivity::class.java)
                intent.putExtra("SPEED_LIMIT", speedLimit)
                startActivity(intent)
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val startPoint = GeoPoint(location.latitude, location.longitude)
                        val mapController: IMapController = map.controller
                        mapController.setZoom(18.0)
                        mapController.setCenter(startPoint)

                        val startMarker = Marker(map)
                        startMarker.position = startPoint
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        startMarker.title = "You are here"
                        map.overlays.add(startMarker)
                    }
                }
        } catch (unlikely: SecurityException) {
            println("Lost location permission.$unlikely")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastKnownLocation()
            }
        }
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return true
    }
}