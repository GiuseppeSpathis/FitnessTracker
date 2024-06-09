package com.example.fitnesstracker

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationBarView


class HomeActivity :  AppCompatActivity(){
   // private lateinit var mMap: GoogleMap
   // private lateinit var fusedLocationClient: FusedLocationProviderClient
   // private val REQUEST_CODE_PERMISSIONS = 101

    //private lateinit var socialButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val imageProfile = findViewById<ImageView>(R.id.image_profile)
        val gender = LoggedUser.gender
        println(gender)
        when (gender) {
            "Maschio" -> imageProfile.setImageResource(R.drawable.male)
            "Femmina" -> imageProfile.setImageResource(R.drawable.female)
            else -> imageProfile.setImageResource(R.drawable.other)
        }

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
                R.id.nav_users -> {
                    val intent = Intent(this, Social::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                else -> false
            }
        }

        /*
        socialButton = findViewById(R.id.socialButton)
        socialButton.setOnClickListener{
            startActivity(Intent(this, Social::class.java))
        }

         */
     /*   fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapView = findViewById<MapView>(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync{googleMap ->
            mMap = googleMap
            checkLocationPermissions()

        }*/



    }
/*
    private fun checkLocationPermissions() {
        if(ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            requestLocationPermission()
        } else {
            getLastKnownLocation()
        }
    }

    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)){
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_PERMISSIONS)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_PERMISSIONS)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                }
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation()
            } else {
                // Permission denied: Show a message
                Toast.makeText(this, "Accept permissions to view your location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        findViewById<MapView>(R.id.map).onStart()
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.map).onResume()
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.map).onPause()
    }

    override fun onStop() {
        super.onStop()
        findViewById<MapView>(R.id.map).onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        findViewById<MapView>(R.id.map).onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        findViewById<MapView>(R.id.map).onDestroy()
    } */



}