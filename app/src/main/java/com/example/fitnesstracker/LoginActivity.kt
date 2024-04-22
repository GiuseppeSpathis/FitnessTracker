package com.example.fitnesstracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
            val insertedEmail = email.text.toString()
            val insertedPassword = password.text.toString()

            auth.signInWithEmailAndPassword(insertedEmail, insertedPassword).addOnCompleteListener(this){ task ->
                if(task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                    val uid = FirebaseAuth.getInstance().currentUser!!.uid
                    val database = FirebaseDatabase.getInstance(resources.getString(R.string.db_connection)).reference
                    database.child("users").child(uid).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val user = dataSnapshot.getValue(LoggedUser::class.java)
                            LoggedUser.username = user?.username ?: ""
                            LoggedUser.gender = user?.gender ?: ""
                            LoggedUser.email = user?.email ?: ""
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                } else {
                    updateUI(null)
                }
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null){
            Toast.makeText(baseContext, "Login successfull", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 2000)

        } else {
            Toast.makeText(baseContext, "Errore durante il login", Toast.LENGTH_SHORT).show()
        }
    }


}