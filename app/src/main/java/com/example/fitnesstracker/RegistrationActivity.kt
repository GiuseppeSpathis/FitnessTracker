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
import android.text.InputType
import android.widget.ImageButton
import android.widget.Spinner
import androidx.core.content.ContextCompat
import java.util.UUID


class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var goBack: Button
    private val REQUEST_WIFI_PERMISSION_CODE = 101
    private lateinit var togglePasswordVisibilityButton : ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_fragment)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.email)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        togglePasswordVisibilityButton = findViewById(R.id.togglePasswordVisibilityButton)
        genderSpinner = findViewById(R.id.gender_spinner)
        registerButton = findViewById(R.id.register)
        goBack = findViewById(R.id.go_back)


        val genderArray = resources.getStringArray(R.array.gender_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter


        val firebaseAppCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )

        togglePasswordVisibilityButton.setOnClickListener {
            if (password.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibilityButton.setImageResource(R.drawable.visible)
            } else {
                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibilityButton.setImageResource(R.drawable.not_visible)
            }
            password.setSelection(password.text.length)
        }

        registerButton.setOnClickListener {
            val email = email.text.toString()
            val username = username.text.toString()
            val password = password.text.toString()
            val selectedGender = genderSpinner.selectedItem.toString()

            lifecycleScope.launch {
                if (validateInput(email, username, password)) {
                    try {
                        registerUser(email, username, password, selectedGender)
                    } catch (e: Exception) {
                        Toast.makeText(this@RegistrationActivity, R.string.errore_registrazione, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        goBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private suspend fun registerUser(email: String, username: String, password: String, gender: String) {
        try {
            withContext(Dispatchers.IO) {
                auth.createUserWithEmailAndPassword(email, password).await()
                val uid = auth.currentUser!!.uid
                if (ContextCompat.checkSelfPermission(this@RegistrationActivity, android.Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@RegistrationActivity, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                    requestWifiPermission()
                    return@withContext
                }
                val mac = getMacAddress()
                Log.d("RegistrationActivity", "Retrieved macAddress: $mac") // Print macAddress for debugging
                saveUserData(uid, email, username, gender, mac)

                // Switch to main thread for Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistrationActivity, R.string.registrazione_successo, Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                        finish()
                    }, 1000)
                }
            }
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Registration failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegistrationActivity, R.string.errore_registrazione, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveUserData(uid: String, email: String, username: String, gender: String, mac: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
                val uniqueId = UUID.randomUUID().toString()
                val user = User(uniqueId, email, username, gender, mac)

                database.child("users").child(uid).setValue(user).await()
            }
            return true
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Error saving user data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegistrationActivity, "Errore nel salvataggio dei dati.", Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }

    private suspend fun validateInput(email: String, username: String, password: String): Boolean {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

        if (!emailPattern.matcher(email).matches()) {
            Toast.makeText(this, R.string.email_non_valida, Toast.LENGTH_SHORT).show()
            return false
        }

        if (!passwordPattern.matches(password)) {
            Toast.makeText(this, R.string.password_non_valida, Toast.LENGTH_LONG).show()
            return false
        }

        if (emailExists(email)) {
            Toast.makeText(this, R.string.email_utilizzata, Toast.LENGTH_SHORT).show()
            return false
        }

        if(usernameExists(username)){
            Toast.makeText(this, R.string.username_utilizzato, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private suspend fun emailExists(email: String): Boolean {
        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference

        return withContext(Dispatchers.IO) {
            try {
                val result = database.child("users").orderByChild("email").equalTo(email).get().await()
                result.exists()
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "Error checking email existence", e)
                false
            }
        }
    }

    private suspend fun usernameExists(username: String): Boolean {
        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference


        return withContext(Dispatchers.IO) {
            try {
                val result = database.child("users").orderByChild("username").equalTo(username).get().await()
                result.exists()
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "Error checking username existence", e)
                false
            }
        }
    }

    private fun requestWifiPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_WIFI_STATE, android.Manifest.permission.ACCESS_NETWORK_STATE), REQUEST_WIFI_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WIFI_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMacAddress()
        } else {
            println("No access")
        }
    }

    private fun getMacAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo.macAddress ?: "02:00:00:00:00:00"
    }

    data class User(val id: String, val email: String?, val username: String?, val gender: String?, val macAddress: String?)
}

