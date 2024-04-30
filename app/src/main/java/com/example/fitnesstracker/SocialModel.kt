package com.example.fitnesstracker

import Utils.findUserByMacAddress
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.core.content.res.ResourcesCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException
import java.util.UUID

class SocialModel {






    private var personList: MutableList<Person> = mutableListOf(

    )

    fun getPersonsList(): List<Person>{
        return personList.toList()
    }

    suspend fun updateList(device: BluetoothDevice?): Boolean{ //da chiamare dal broadcast receiver
/*
        val user = device?.address?.let { findUserByMacAddress(it) }
        if (user != null) {
            if(user.username != null && user.gender != null && user.macAddress !=null) {
                personList.add(Person(user.username, user.gender, device))
                return true
            }
        }
        return false
        */
        personList.add(Person("pippo", "Maschio", device))
        return true
    }

    fun filterList(filter: String):  List<Person>{
        val filteredList = personList.filter { person ->
            person.name.contains(filter, ignoreCase = true)
        }
        return filteredList.toList()
    }




}