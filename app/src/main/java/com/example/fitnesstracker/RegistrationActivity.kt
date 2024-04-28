package com.example.fitnesstracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.util.newStringBuilder
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import android.Manifest.permission.ACCESS_WIFI_STATE
import androidx.core.content.ContextCompat

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var genderSpinner: AppCompatSpinner
    private lateinit var registerButton: Button
    private lateinit var goBack : Button
    private val REQUEST_WIFI_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_fragment)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.email)
        username = findViewById(R.id.username)
        password= findViewById(R.id.password)
        genderSpinner = findViewById(R.id.gender_spinner)
        registerButton = findViewById(R.id.register)
        goBack = findViewById(R.id.go_back)
        // Set up gender spinner
        val genderArray = resources.getStringArray(R.array.gender_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter

        val firebaseAppCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )

        registerButton.setOnClickListener{
            val email = email.text.toString()
            val username = username.text.toString()
            val password = password.text.toString()
            val selectedGender = genderSpinner.selectedItem.toString()

            lifecycleScope.launch {
                if (validateInput(email, username, password)) {
                    try {
                        registerUser(email, username, password, selectedGender)
                    } catch (e: Exception) {
                        Toast.makeText(this@RegistrationActivity, "Errore durante la registrazione", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        goBack.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private suspend fun registerUser(email: String, username: String, password: String, gender: String){
        try {
            withContext(Dispatchers.IO) {
                auth.createUserWithEmailAndPassword(email, password).await()
                val uid = auth.currentUser!!.uid
                if (ContextCompat.checkSelfPermission(this@RegistrationActivity, ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                    requestWifiPermission()
                    return@withContext
                }
                val mac = getMacAddress(this@RegistrationActivity)
                Log.d("RegistrationActivity", "Retrieved macAddress: $mac") // Print macAddress for debugging
                saveUserData(uid, email, username, gender, mac)

                // Switch to main thread for Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistrationActivity, "Registrazione avvenuta con successo!", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                        finish()
                    }, 1000)
                }
            }
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Registration failed", e)

            // Switch to main thread for Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegistrationActivity, "Errore durante la registrazione", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveUserData(uid: String, email: String, username: String, gender: String, mac: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
                val user = User(email, username, gender, mac)

                database.child("users").child(uid).setValue(user).await()
            }
            return true
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Error saving user data", e)
            Toast.makeText(this@RegistrationActivity, "Errore nel salvataggio dei dati.", Toast.LENGTH_SHORT)
                .show()
            return false
        }
    }

    private suspend fun validateInput(email: String, username: String, password: String): Boolean {
        val emailPattern = Patterns.EMAIL_ADDRESS
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

        if (!emailPattern.matcher(email).matches()) {
            Toast.makeText(this, "Formato email non valido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!passwordPattern.matches(password)) {
            Toast.makeText(this, "La password deve contenere: almeno una maiuscola, un numero e un carattere speciale", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if email exists using coroutine helper
        if (emailExists(email)) {
            Toast.makeText(this, "Email già utilizzata", Toast.LENGTH_SHORT).show()
            return false
        }

        return true // Validation successful
    }

    // Helper function to check email existence (suspending)
    private suspend fun emailExists(email: String): Boolean {
        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference

        // Use a suspending function inside coroutines (e.g., withContext)
        return withContext(Dispatchers.IO) {
            try {
                val result = database.child("users").orderByChild("email").equalTo(email).get().await()
                result.exists()
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "Error checking email existence", e)
                false // Handle error appropriately
            }
        }
    }

    private fun requestWifiPermission() {
        // Implement logic to request permission from the user
        println("ciaoooo")
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_WIFI_STATE), REQUEST_WIFI_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WIFI_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMacAddress(this)
        } else {
           println("No access")
        }
    }

    private fun getMacAddress(context: Context): String {
       val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        val mac = wifiManager.connectionInfo.macAddress
        return mac
    }



    data class User(val email: String, val username: String,val gender: String, val macAddress: String)

}