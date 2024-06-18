package com.example.fitnesstracker


import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context

class SocialModel {

    private var personList: MutableList<Person> = mutableListOf(

    )

    fun getPersonsList(): List<Person>{
        return personList.distinctBy { it.name }
    }

    @SuppressLint("MissingPermission")
    suspend fun updateList(device: BluetoothDevice, context: Context): Boolean{ //da chiamare dal broadcast receiver
        val deviceName = device.name
        println("device name: $deviceName")
        val user = Utils.getUser(device.name, context)
        println("user retrivied: $user")
        if (user != null) {
            personList.add(Person(user.name, user.gender, device))
            return true
        }
        return false
    }

    fun filterList(filter: String):  List<Person>{
        val filteredList = personList.filter { person ->
            person.name.contains(filter, ignoreCase = true)
        }
        return filteredList.toList()
    }




}