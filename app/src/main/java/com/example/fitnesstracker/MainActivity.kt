package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {


    private lateinit var social: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_main)
        startActivity(Intent(this, LoginActivity::class.java))
       // social = findViewById<Button>(R.id.socialButton)
        /*
        social.setOnClickListener {
            startActivity(Intent(this, Social::class.java))
        }*/
    }


}

