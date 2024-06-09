package com.example.fitnesstracker

import com.example.fitnesstracker.Utils.checkGender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class PersonProfile : AppCompatActivity() {


    private lateinit var name : TextView
    private lateinit var image : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_profile)

        val gender = intent.getStringExtra("gender")

        name = findViewById(R.id.personName)
        name.text = intent.getStringExtra("name")
        image = findViewById(R.id.imagePerson)
        checkGender(gender!!, image, this)

    }
}