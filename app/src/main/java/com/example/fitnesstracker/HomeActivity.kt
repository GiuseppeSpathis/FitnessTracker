package com.example.fitnesstracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class HomeActivity : AppCompatActivity() {

    private lateinit var social: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        social = findViewById<Button>(R.id.socialButton)

        social.setOnClickListener {
            startActivity(Intent(this, Social::class.java))
        }
    }
}