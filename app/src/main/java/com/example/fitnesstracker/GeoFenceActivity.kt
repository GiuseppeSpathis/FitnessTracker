package com.example.fitnesstracker

import Utils.setupBottomNavigationView
import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon


class GeoFenceActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var db: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBar: EditText
    private lateinit var searchButton: Button
    private lateinit var addGeofenceButton: Button
    private var searchedLocation: GeoPoint? = null
    private val model = Model()

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
        setupBottomNavigationView(this, "geofence", bottomNavigationView)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
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
                showGeofenceNameDialog(geoPoint)
                return true
            }
        })



        findViewById<Button>(R.id.btnViewGeofences).setOnClickListener {
            viewGeofences()
        }

        searchButton.setOnClickListener {
            val query = searchBar.text.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            } else {
                Toast.makeText(this, R.string.inserisci_luogo_cercare, Toast.LENGTH_SHORT).show()
            }
        }

        addGeofenceButton.setOnClickListener {
            searchedLocation?.let {
                showGeofenceNameDialog(it)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_LOCATION_PERMISSION)
            if(!isServiceRunning(LocationUpdatesService::class.java)){
                Log.d("GeoFence", "Service not running, started")
                startLocationService()
            } else {
                Log.d("Geofence", "Service already running, not started")
            }
        }
        findViewById<ImageButton>(R.id.infoButton).setOnClickListener {
            showInfoDialog()
        }
    }


    private fun isServiceRunning(serviceClass: Class<out Service>): Boolean {
        val sharedPref = getSharedPreferences("ServiceState", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(serviceClass.simpleName + "Running", false)
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationUpdatesService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.show()
    }

    private fun showGeofenceNameDialog(location: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.nome_gf)

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            val geofenceName = input.text.toString()
            if (geofenceName.isNotEmpty()) {
                addGeofenceAtLocation(location, geofenceName)
            } else {
                Toast.makeText(this, R.string.nome_non_vuoto, Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }



    private fun searchLocation(query: String) {
        lifecycleScope.launch {
            val result = model.searchLocation(query)
            if (result != null) {
                val lat = result.latitude
                val lon = result.longitude
                moveToLocation(lat, lon)
                addGeofenceButton.isEnabled = true
            } else {
                Toast.makeText(this@GeoFenceActivity, R.string.nessun_risultato, Toast.LENGTH_SHORT).show()
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

    private fun addGeofenceAtLocation(location: GeoPoint, placeName: String) {
        val geofenceRadius = 100.0
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

        val geofence = GeoFence(
            latitude = location.latitude,
            longitude = location.longitude,
            radius = geofenceRadius.toFloat(),
            placeName = placeName
        )

        lifecycleScope.launch {
            model.insertGeofence(db, geofence)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GeoFenceActivity, R.string.gf_successo, Toast.LENGTH_SHORT).show()
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
            val geofences = model.getAllGeofences(db)

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
            .setTitle(R.string.rimuovere_gf)
            .setMessage(R.string.rim_domanda)
            .setPositiveButton(R.string.si) { _, _ ->
                lifecycleScope.launch {
                    model.deleteGeofence(db, geofence)
                    viewGeofences()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}






