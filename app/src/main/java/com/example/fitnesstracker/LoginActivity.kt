package com.example.fitnesstracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.updateTransition
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton : Button
    private lateinit var email : EditText
    private lateinit var password : EditText
    private lateinit var goToRegistration: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_registration2)

        auth = FirebaseAuth.getInstance()
        loginButton = findViewById(R.id.login)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        goToRegistration = findViewById(R.id.register)

        goToRegistration.setOnClickListener{
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
        loginButton.setOnClickListener{
            lifecycleScope.launch {
                val insertedEmail = email.text.toString()
                val insertedPassword = password.text.toString()
                signInAndFetchUserData(insertedEmail, insertedPassword)
            }
        }
    }

    private suspend fun signInAndFetchUserData(insertedEmail: String, insertedPassword: String) {
        try {
            val authResult = withContext(Dispatchers.IO) {
                auth.signInWithEmailAndPassword(insertedEmail, insertedPassword).await()
            }

            updateUI(authResult.user)

            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference

            val userData = withContext(Dispatchers.IO) {
                database.child("users").child(uid).get().await().getValue(LoggedUser::class.java)
            }

            if (userData != null) {
                LoggedUser.username = userData.username ?: ""
                LoggedUser.gender = userData.gender ?: ""
                LoggedUser.email = userData.email ?: ""
                LoggedUser.macAddress = userData.macAddress ?: ""
                LoggedUser.id = uid
            } else {
                Log.e("LoginActivity", "Error while saving user data")
            }

        } catch (e: Exception) {
            Log.e("LoginActivity", "Error during sign-in or data fetch", e)
            updateUI(null)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null){
            Toast.makeText(baseContext, "Login successfull", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                //startActivity(Intent(this, HomeActivity::class.java))
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 2000)

        } else {
            Toast.makeText(baseContext, "Errore durante il login", Toast.LENGTH_SHORT).show()
        }
    }


}