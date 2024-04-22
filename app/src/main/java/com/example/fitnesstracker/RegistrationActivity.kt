package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var genderSpinner: AppCompatSpinner
    private lateinit var registerButton: Button
    private lateinit var goBack : Button
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

            registerUser(email, username, password, selectedGender)
        }
        goBack.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun registerUser(email: String, username: String, password: String, gender: String){

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        val uid = auth.currentUser!!.uid
                        saveUserData(uid, email, username, gender)
                        Toast.makeText(this, "Registrazione avvenuta con successo!", Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, 1000)
                    } else {
                        println("ciao123")
                        Toast.makeText(this, "Errore durate la registrazione", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun saveUserData(uid: String, email: String, username: String, gender: String){
        println("coao")
        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
        val user = User(email, username, gender)

        database.child("users").child(uid).setValue(user).addOnSuccessListener {
            println("ciaooooo")
            Log.d("RegistrationActivity", "Dati utente salvati con successo")
        }
            .addOnFailureListener{error ->
                error.printStackTrace()
                Log.e("RegistrationActivity", "Errore nel salvataggio dei dati", error)
                Toast.makeText(this, "Errore nel salvataggio dei dati.", Toast.LENGTH_SHORT).show()
            }
    }

    data class User(val email: String, val username: String,val gender: String)

}