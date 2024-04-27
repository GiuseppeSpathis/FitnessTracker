package com.example.fitnesstracker

import MyAdapter
import Utils.hasPermission
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentFilter
import android.os.Build
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Social : AppCompatActivity() {





    private lateinit var search: EditText

    private lateinit var myAdapter: MyAdapter
    private lateinit var myRecyclerView: RecyclerView


    lateinit var socialController: SocialController






    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        socialController.handleBluetoothPermissionResult(requestCode, grantResults)
    }





    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

        socialController = SocialController(this)

        search = findViewById<EditText>(R.id.search)


        myRecyclerView = findViewById(R.id.myRecyclerView)

        myAdapter = MyAdapter(socialController.getPersonlist())


        myRecyclerView.adapter = myAdapter
        myRecyclerView.layoutManager = LinearLayoutManager(this)



        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //niente
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //niente
            }
            override fun afterTextChanged(s: Editable) {
                myAdapter.updateList(socialController.filterList(s.toString()))
            }
        })


        socialController.startBluetooth()

    }



}


