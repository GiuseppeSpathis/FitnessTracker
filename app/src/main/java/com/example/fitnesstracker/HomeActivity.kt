package com.example.fitnesstracker


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker



class HomeActivity : AppCompatActivity(), MapListener {

    private lateinit var socialButton: Button
    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Carica le configurazioni di OSMDroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        val imageProfile = findViewById<ImageView>(R.id.image_profile)
        var gender = LoggedUser.gender
        gender = "Maschio"
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

        socialButton = findViewById(R.id.socialButton)
        socialButton.setOnClickListener {
            startActivity(Intent(this, Social::class.java))
        }

        // Imposta la mappa
        map = findViewById(R.id.osmmap)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Inizializza il client per ottenere la posizione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_REQUEST_CODE)
        } else {
            getLastKnownLocation()
        }
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
        // Implementa logica di scroll se necessario
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        // Implementa logica di zoom se necessario
        return true
    }
}