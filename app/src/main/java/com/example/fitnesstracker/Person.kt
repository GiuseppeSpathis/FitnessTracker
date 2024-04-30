package com.example.fitnesstracker

import android.bluetooth.BluetoothDevice

data class Person(
    val name: String,
    val gender: String,
    val device: BluetoothDevice?
)

