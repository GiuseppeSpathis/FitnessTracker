package com.example.fitnesstracker

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Handler




class GeoFenceActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var db: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBar: EditText
    private lateinit var searchButton: Button
    private lateinit var addGeofenceButton: Button
    private var searchedLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_geofencing)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        db = AppDatabase.getDatabase(this)
        searchBar = findViewById(R.id.searchBar)
        searchButton = findViewById(R.id.btnSearch)
        addGeofenceButton = findViewById(R.id.btnAddGeofence)
        addGeofenceButton.isEnabled = false

        // Initialize map with current user location
        getCurrentLocation { latitude, longitude ->
            val userLocation = GeoPoint(latitude, longitude)
            map.controller.setCenter(userLocation)

            val marker = Marker(map)
            marker.position = userLocation
            map.overlays.add(marker)
        }

        map.overlays.add(object : Overlay() {
            override fun onDoubleTap(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                addGeofenceAtLocation(geoPoint)
                return true
            }
        })

        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.geofence

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
                    // Already on GeoFenceActivity, do nothing
                    true
                }
                else -> false
            }
        }

        findViewById<Button>(R.id.btnViewGeofences).setOnClickListener {
            viewGeofences()
        }

        searchButton.setOnClickListener {
            val query = searchBar.text.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            } else {
                Toast.makeText(this, "Inserisci un luogo da cercare", Toast.LENGTH_SHORT).show()
            }
        }

        addGeofenceButton.setOnClickListener {
            searchedLocation?.let {
                addGeofenceAtLocation(it)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_LOCATION_PERMISSION)
        } else {

        //    startLocationUpdatesService()
        }
    }

    private fun startLocationUpdatesService() {
        val intent = Intent(this, LocationUpdatesService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun searchLocation(query: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$query")
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.inputStream.bufferedReader().readText()
                } finally {
                    connection.disconnect()
                }
            }
            val jsonArray = JSONArray(result)
            if (jsonArray.length() > 0) {
                val location = jsonArray.getJSONObject(0)
                val lat = location.getDouble("lat")
                val lon = location.getDouble("lon")
                moveToLocation(lat, lon)
                addGeofenceButton.isEnabled = true
            } else {
                Toast.makeText(this@GeoFenceActivity, "Nessun risultato trovato", Toast.LENGTH_SHORT).show()
                addGeofenceButton.isEnabled = false
            }
        }
    }

    private fun moveToLocation(latitude: Double, longitude: Double) {
        val newLocation = GeoPoint(latitude, longitude)
        map.controller.setCenter(newLocation)

        val marker = Marker(map)
        marker.position = newLocation
        map.overlays.add(marker)

        searchedLocation = newLocation
    }

    private fun addGeofenceAtLocation(location: GeoPoint) {
        val geofenceRadius = 10.0
        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(location, geofenceRadius)
            fillPaint?.apply {
                color = Color.argb(50, 0, 0, 255)
                style = Paint.Style.FILL
            }
            outlinePaint.apply {
                color = Color.BLUE
                style = Paint.Style.STROKE
            }
        }
        map.overlays.add(circle)
        map.invalidate()

        val place = findViewById<EditText>(R.id.searchBar).text.toString()
        val geofence = GeoFence(
            latitude = location.latitude,
            longitude = location.longitude,
            radius = geofenceRadius.toFloat(),
            placeName = place
        )

        lifecycleScope.launch {
            db.attivitàDao().insertGeofence(geofence)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GeoFenceActivity, "Geofencing aggiunta con successo", Toast.LENGTH_SHORT).show()
                findViewById<EditText>(R.id.searchBar).text.clear()
                addGeofenceButton.isEnabled = false
                viewGeofences()
            }
        }
    }

    private fun getCurrentLocation(callback: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    callback(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun viewGeofences() {
        lifecycleScope.launch {
            val geofences = withContext(Dispatchers.IO) {
                db.attivitàDao().getAllGeofences()
            }

            map.overlays.clear()

            geofences.forEach { geofence ->
                val location = GeoPoint(geofence.latitude, geofence.longitude)

                val marker = Marker(map).apply {
                    position = location
                    setOnMarkerClickListener { _, _ ->
                        showDeleteGeofenceDialog(geofence)
                        true
                    }
                }
                map.overlays.add(marker)

                val circle = Polygon().apply {
                    points = Polygon.pointsAsCircle(location, geofence.radius.toDouble())
                    fillPaint?.apply {
                        color = Color.argb(50, 0, 0, 255)
                        style = Paint.Style.FILL
                    }
                    outlinePaint.apply {
                        color = Color.BLUE
                        style = Paint.Style.STROKE
                    }
                }
                map.overlays.add(circle)
            }

            map.invalidate()
        }
    }

    private fun showDeleteGeofenceDialog(geofence: GeoFence) {
        AlertDialog.Builder(this)
            .setTitle("Rimuovere Geofence")
            .setMessage("Vuoi rimuovere questa geofence?")
            .setPositiveButton("Sì") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.attivitàDao().deleteGeofence(geofence)
                    }
                    viewGeofences() // Refresh the map to remove the deleted geofence
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}





