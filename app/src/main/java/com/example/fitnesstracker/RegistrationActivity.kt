package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

            if(validateInput(email, username, password)){
                registerUser(email, username, password, selectedGender)
            }

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

                        Toast.makeText(this, "Errore durate la registrazione", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun saveUserData(uid: String, email: String, username: String, gender: String){
        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
        val user = User(email, username, gender)

        database.child("users").child(uid).setValue(user).addOnSuccessListener {
            Log.d("RegistrationActivity", "Dati utente salvati con successo")
        }
            .addOnFailureListener{error ->
                error.printStackTrace()
                Log.e("RegistrationActivity", "Errore nel salvataggio dei dati", error)
                Toast.makeText(this, "Errore nel salvataggio dei dati.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(email: String, username: String, password: String): Boolean {
        val emailPattern = Patterns.EMAIL_ADDRESS // For email validation
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

        if (!emailPattern.matcher(email).matches()) {
            Toast.makeText(this, "Formato email non valido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!passwordPattern.matches(password)) {
            Toast.makeText(this, "La password deve contenere: almeno una maiuscola, un numero e un carattere speciale", Toast.LENGTH_SHORT).show()
            return false
        }

        val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
        var exists : Boolean
        exists = false
        database.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Email already exists
                    Toast.makeText(this@RegistrationActivity, "Email già utilizzata", Toast.LENGTH_SHORT).show()
                    exists = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })

        if(exists){
            return false
        }


        return true
    }

    data class User(val email: String, val username: String,val gender: String)

}