package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_main)
        //startActivity(Intent(this, HomeActivity::class.java))
        startActivity(Intent(this, LoginActivity::class.java))


    }


}

