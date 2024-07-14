package com.example.fitnesstracker

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable


data class Person(
    val name: String = "",
    val gender: String = "",
    val device: BluetoothDevice? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(BluetoothDevice::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(gender)
        parcel.writeParcelable(device, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Person> {
        override fun createFromParcel(parcel: Parcel): Person {
            return Person(parcel)
        }

        override fun newArray(size: Int): Array<Person?> {
            return arrayOfNulls(size)
        }
    }
}