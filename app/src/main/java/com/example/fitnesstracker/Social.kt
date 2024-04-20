package com.example.fitnesstracker

import MyAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Social : AppCompatActivity() {


    companion object{ //questo companion in futuro non ci dovra' essere
        val personList = listOf(
            Person("Mario", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),
            Person("Mariossadasdasdasd", "male"),Person("Mariossadasdasdasd", "male"),






            Person("Maria", "female")
        )
    }


    private lateinit var search: EditText

    private lateinit var myAdapter: MyAdapter
    private lateinit var myRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)
        search = findViewById<EditText>(R.id.search)


        myRecyclerView = findViewById(R.id.myRecyclerView)

        myAdapter = MyAdapter(personList) //in futuro non ci sara' questo


        myRecyclerView.adapter = myAdapter
        myRecyclerView.layoutManager = LinearLayoutManager(this)




    }
}