package com.example.fitnesstracker


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

class SocialModel {

    private var personList: MutableList<Person> = mutableListOf()
    private val firebase_db = FirebaseDatabase.getInstance("https://fitnesstracker-637f9-default-rtdb.europe-west1.firebasedatabase.app").reference
    fun getPersonsList(): List<Person> {
        return personList.distinctBy { it.name }
    }

    @SuppressLint("MissingPermission")
    suspend fun updateList(device: BluetoothDevice, context: Context): Boolean {
        val deviceName = device.name
        println("device name: $deviceName")
        val user = Utils.getUser(device.name, context)
        println("user retrieved: $user")
        if (user != null) {
            personList.add(Person(user.name, user.gender, device))
            return true
        }
        return false
    }

    fun filterList(filter: String): List<Person> {
        return personList.filter { person ->
            person.name.contains(filter, ignoreCase = true)
        }
    }

    /**
     * Retrieves the current user's location from Firebase.
     *
     * @param uid The user ID of the current user.
     * @return The user's location or null if not found.
     */
    suspend fun getCurrentUserLocation(uid: String): RegistrationActivity.User? {
        val currentUserSnapshot = firebase_db.child("users").child(uid).get().await()
        return currentUserSnapshot.getValue(RegistrationActivity.User::class.java)
    }

    /**
     * Retrieves the nearby users from Firebase who have updated their location within the last 10 minutes.
     *
     * @param timestamp The timestamp from which to start the search.
     * @return A list of nearby users or null if an error occurs.
     */
    suspend fun getNearbyUsers(timestamp: Double): List<RegistrationActivity.User>? {
        val nearbyUsersSnapshot = firebase_db.child("users")
            .orderByChild("lastUpdated")
            .startAt(timestamp)
            .get()
            .await()

        return nearbyUsersSnapshot.children.mapNotNull { snapshot ->
            snapshot.getValue(RegistrationActivity.User::class.java)
        }
    }

    /**
     * Searches for a location using the given query.
     *
     * @param query The search query.
     * @return A GeoPoint object containing the latitude and longitude, or null if not found.
     */
    suspend fun searchLocation(query: String): GeoPoint? {
        return withContext(Dispatchers.IO) {
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$query")
            val connection = url.openConnection() as HttpURLConnection
            try {
                val result = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(result)
                if (jsonArray.length() > 0) {
                    val location = jsonArray.getJSONObject(0)
                    val lat = location.getDouble("lat")
                    val lon = location.getDouble("lon")
                    GeoPoint(lat, lon)
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Inserts a geofence into the database.
     *
     * @param db The AppDatabase instance.
     * @param geofence The GeoFence object to insert.
     */
    suspend fun insertGeofence(db: AppDatabase, geofence: GeoFence) {
        withContext(Dispatchers.IO) {
            db.attivitàDao().insertGeofence(geofence)
        }
    }

    /**
     * Retrieves all geofences from the database.
     *
     * @param db The AppDatabase instance.
     * @return A list of GeoFence objects.
     */
    suspend fun getAllGeofences(db: AppDatabase): List<GeoFence> {
        return withContext(Dispatchers.IO) {
            db.attivitàDao().getAllGeofences()
        }
    }

    /**
     * Deletes a geofence from the database.
     *
     * @param db The AppDatabase instance.
     * @param geofence The GeoFence object to delete.
     */
    suspend fun deleteGeofence(db: AppDatabase, geofence: GeoFence) {
        withContext(Dispatchers.IO) {
            db.attivitàDao().deleteGeofence(geofence)
        }
    }

    /**
     * Gets the last known location using the fused location client.
     *
     * @param fusedLocationClient The fused location client.
     * @param callback A callback function that takes a Location object.
     */
    fun getLastKnownLocation(fusedLocationClient: FusedLocationProviderClient, callback: (Location?) -> Unit) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    callback(location)
                }
        } catch (unlikely: SecurityException) {
            println("Lost location permission.$unlikely")
            callback(null)
        }
    }


    /**
     * Updates the user's location in Firebase.
     *
     * @param location The new location data.
     */
    fun updateLocationInFirebase(location: Location) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userUpdates = mapOf(
            "lastLatitude" to location.latitude,
            "lastLongitude" to location.longitude,
            "lastUpdated" to System.currentTimeMillis()
        )

        firebase_db.child("users").child(uid).updateChildren(userUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("LocationModel", "User location updated successfully")
            } else {
                Log.e("LocationModel", "Error updating location", task.exception)
            }
        }.addOnFailureListener { exception ->
            Log.e("LocationModel", "Failed to update location", exception)
        }
    }

    suspend fun fetchUserData(uid: String): LoggedUser? {
        return try {
            val userDataSnapshot = withContext(Dispatchers.IO) {
                firebase_db.child("users").child(uid).get().await()
            }
            userDataSnapshot.getValue(LoggedUser::class.java)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error fetching user data", e)
            null
        }
    }

    suspend fun usernameExists(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = firebase_db.child("users").orderByChild("username").equalTo(username).get().await()
                result.exists()
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "Error checking username existence", e)
                false
            }
        }
    }

    suspend fun emailExists(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = firebase_db.child("users").orderByChild("email").equalTo(email).get().await()
                result.exists()
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "Error checking email existence", e)
                false
            }
        }
    }

    suspend fun saveUserData(uid: String, email: String, username: String, gender: String, context: Context): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val uniqueId = UUID.randomUUID().toString()
                val lastLongitude = 0.0
                val lastLatitude = 0.0
                val lastUpdated = System.currentTimeMillis()
                val user = RegistrationActivity.User(
                    uniqueId,
                    email,
                    username,
                    gender,
                    lastLatitude,
                    lastLongitude,
                    lastUpdated
                )
                firebase_db.child("users").child(uid).setValue(user).await()
            }
            return true
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Error saving user data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errore nel salvataggio dei dati.", Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }
    suspend fun getGeofencesForDate(db: AppDatabase, year: Int, month: Int, day: Int): List<timeGeofence> {
        val date = String.format("%04d-%02d-%02d", year, month, day)
        return db.attivitàDao().getGeofencesForDate(date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getActivitiesForPeriod(db: AppDatabase, period: String): List<Attività> {
        val now = LocalDateTime.now()
        val startDate = when (period) {
            "day" -> now.minusDays(1)
            "week" -> now.minusWeeks(1)
            "month" -> now.minusMonths(1)
            "year" -> now.minusYears(1)
            else -> now
        }
        return withContext(Dispatchers.IO) {
            db.attivitàDao().getAttivitàByDateRange(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), now.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getGeofencesForPeriod(db: AppDatabase, period: String) : List<timeGeofence>{
        val now = LocalDateTime.now()
        val startDate = when (period) {
            "day" -> now.minusDays(1)
            "week" -> now.minusWeeks(1)
            "month" -> now.minusMonths(1)
            "year" -> now.minusYears(1)
            else -> now
        }
        println("For period: $period, startDate: $startDate")
        return withContext(Dispatchers.IO){
            db.attivitàDao().getGeofencesByDateRange(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
     suspend fun getOtherActivitiesForDate(year: Int, month: Int, day: Int, db: AppDatabase): List<OthersActivity> {
        val date = String.format("%02d/%02d/%04d", day, month, year)
        var attivitàDao : ActivityDao = db.attivitàDao()
        Log.d("StatsActivity", "Getting activities for date: $date")
        return withContext(Dispatchers.IO) {
            attivitàDao.getOtherActivitiesByDate(date)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getActivitiesForDate(year: Int, month: Int, day: Int, db: AppDatabase): List<Attività> {
        val date = String.format("%02d/%02d/%04d", day, month, year)
        var attivitàDao : ActivityDao = db.attivitàDao()
        Log.d("StatsActivity", "Getting activities for date: $date")
        return withContext(Dispatchers.IO) {
            attivitàDao.getAttivitàByDate(date)
        }
    }

}
