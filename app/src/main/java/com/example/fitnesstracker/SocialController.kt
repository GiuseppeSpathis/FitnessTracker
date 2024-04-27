package com.example.fitnesstracker

import Utils.hasPermission
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.registerReceiver


class SocialController (private val SocialActivity: AppCompatActivity) {

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT  = 1
        private const val REQUEST_BLUETOOTH_SCAN  = 2

    }

    private val SocialModel = SocialModel()

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = SocialActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }


    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )

                    }
                    else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    }
                    //SocialModel.updateList(device?.address)
                    Toast.makeText(context, "Dispositivo trovato: ${device?.name}, MAC: ${device?.address}", Toast.LENGTH_LONG).show() //da togliere in futuro

                }
            }
        }}

    @SuppressLint("MissingPermission")
    private val enableBtResultLauncher = SocialActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if(hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialActivity)){
                bluetoothAdapter?.startDiscovery()
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(SocialActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
                }
                else {
                    bluetoothAdapter?.startDiscovery()
                }
            }
        }
    }


    private fun requestBluetoothConnectAndScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialActivity))
            {

                ActivityCompat.requestPermissions(SocialActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
            } else {
                // Il permesso è già stato concesso, puoi procedere con l'abilitazione del Bluetooth
                enableBluetooth()
            }
        } else {
            // Per versioni di Android inferiori alla 31, procedi direttamente con l'abilitazione del Bluetooth
            enableBluetooth()
        }
    }


    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtResultLauncher.launch(enableBtIntent)
    }


    @SuppressLint("MissingPermission")
    fun handleBluetoothPermissionResult(requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults.isNotEmpty()) { // Check if there are any results at all
                    val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        // Permission is granted, proceed with enabling Bluetooth
                        enableBluetooth()
                    } else {
                        // Permission denied, handle the case where the user denies permission
                        Toast.makeText(SocialActivity, "Permesso BLUETOOTH_CONNECT negato", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }
            REQUEST_BLUETOOTH_SCAN -> {
                if (grantResults.isNotEmpty()) { // Check if there are any results at all
                    val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        // Permission is granted, start Bluetooth discovery
                        bluetoothAdapter?.startDiscovery()
                    } else {
                        // Permission denied, handle the case where the user denies permission
                        Toast.makeText(SocialActivity, "Permesso BLUETOOTH_SCAN negato", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun startBluetooth (){
        // Registra il BroadcastReceiver per ricevere i dispositivi trovati
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        SocialActivity.registerReceiver(receiver, filter)
        // Controlla se il Bluetooth è abilitato, altrimenti richiedi all'utente di abilitarlo
        if (bluetoothAdapter?.isEnabled == false) {
            requestBluetoothConnectAndScanPermission()
        } else {
            if(hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialActivity)){
                bluetoothAdapter?.startDiscovery()
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(SocialActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
                }
                else {
                    bluetoothAdapter?.startDiscovery()
                }
            }
        }
    }

    fun getPersonlist(): List<Person>{
        return SocialModel.getPersonsList()
    }

    fun filterList(filter: String): List<Person> {
        return SocialModel.filterList(filter)
    }


}