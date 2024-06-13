package com.example.fitnesstracker

import Utils.hasPermission
import Utils.receiveMessage
import Utils.socketError
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

//import pl.droidsonroids.gif.GifDrawable


class SocialController (private val SocialInterface: SocialInterface) {

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT  = 1
        private const val REQUEST_BLUETOOTH_SCAN  = 2

    }



    private val socialModel = SocialModel()

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = SocialInterface.getActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }


    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private var serverSocket: BluetoothServerSocket? = null


    @SuppressLint("MissingPermission")
    private fun startDiscovery(){
        println("incomincio la discovery")
        SocialInterface.startAnimation()
        println("durante l'animazione")
        bluetoothAdapter?.startDiscovery()
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
                    if(device?.name != null){
                        CoroutineScope(Dispatchers.IO).launch {
                            val found = socialModel.updateList(device)
                            if(found){
                                withContext(Dispatchers.Main) {
                                    SocialInterface.listUpdated(getPersonlist())
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    private val enableBtResultLauncher = SocialInterface.getActivity().registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /*
        if (result.resultCode == Activity.RESULT_OK) {
            if(hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialInterface.getActivity())){
                startDiscovery()
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
                }
                else {
                    startDiscovery()
                }
            }
        }*/
    }


    private fun requestBluetoothConnectAndScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity()))
            {
                ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
            } else {
                // Il permesso è già stato concesso, puoi procedere con l'abilitazione del Bluetooth
                enableBluetooth()
                startDiscovery()
            }
        } else {
            // Per versioni di Android inferiori alla 31, procedi direttamente con l'abilitazione del Bluetooth
            enableBluetooth()
            startDiscovery()
        }
    }


    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtResultLauncher.launch(enableBtIntent)
    }



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
                        Toast.makeText(SocialInterface.getActivity(), "Permesso BLUETOOTH_CONNECT negato", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }
            REQUEST_BLUETOOTH_SCAN -> {
                if (grantResults.isNotEmpty()) { // Check if there are any results at all
                    val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        // Permission is granted, start Bluetooth discovery
                        startDiscovery()
                    } else {
                        // Permission denied, handle the case where the user denies permission
                        Toast.makeText(SocialInterface.getActivity(), "Permesso BLUETOOTH_SCAN negato", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }
        }
    }


    fun setupBluetooth() {

        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity()))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
            }
        }

        if (bluetoothAdapter?.isEnabled == false && hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity())) {
            enableBluetooth()

        }
    }

    @SuppressLint("MissingPermission")
    fun beDiscoverable(){
        bluetoothAdapter?.setName("pippo");
        startServer()
    }

    fun startBluetooth (){
        // Registra il BroadcastReceiver per ricevere i dispositivi trovati
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        SocialInterface.getActivity().registerReceiver(receiver, filter)
        // Controlla se il Bluetooth è abilitato, altrimenti richiedi all'utente di abilitarlo
        if (bluetoothAdapter?.isEnabled == false) {
            requestBluetoothConnectAndScanPermission()
        } else {

            if(hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialInterface.getActivity())){
                startDiscovery()
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
                }
                else {
                    startDiscovery()
                }
            }
        }
    }



    @SuppressLint("MissingPermission")
    fun startServer() {
        val thread = Thread {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("FitnessTrackerService", uuid)
                val tmpSocket = serverSocket?.accept()
                if(tmpSocket!= null){
                    socket = tmpSocket
                    receiveFromSocket(SocialInterface.getActivity())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

     fun disconnectDevice(activity: Activity, holder: MyAdapter.MyViewHolder, you_are_connected: MyAdapter.Ref<Boolean>, device: BluetoothDevice?){
        Thread {
            try {

                try {
                    socket.close()
                    outputStream.close()
                    inputStream.close()
                } catch (e: UninitializedPropertyAccessException) {
                    //non fare niente se le variaibli non sono state inizializzate
                }

                unBondBluetoothDevice(device)
                activity.runOnUiThread {

                    MotionToast.createColorToast(
                        activity,
                        activity.resources.getString(R.string.successo),
                        activity.resources.getString(R.string.disconnected),
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(activity, www.sanju.motiontoast.R.font.helvetica_regular))

                    holder.connect.visibility = View.VISIBLE
                    holder.message.visibility = View.GONE
                    holder.share.visibility = View.GONE
                    holder.disconnect.visibility = View.GONE

                    you_are_connected.value = false

                }
            }
            catch (e: IOException) {

                activity.runOnUiThread {
                    socketError(e, activity)
                }

            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun connect2device(device: BluetoothDevice?, activity: Activity, holder: MyAdapter.MyViewHolder, person: Person, you_are_connected: MyAdapter.Ref<Boolean>){
        if (device == null) return

        // Verifica se il dispositivo è già accoppiato
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            // Se non è accoppiato, inizia il processo di accoppiamento
            device.createBond()

            // Registra un BroadcastReceiver per gestire l'evento di accoppiamento
            val bondReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                        val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                        val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                        if (state == BluetoothDevice.BOND_BONDED) {
                            // Il dispositivo è ora accoppiato, procedi con la connessione
                            context.unregisterReceiver(this)
                            connectSocket(device, activity, holder, person, you_are_connected)
                        } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING) {
                            // L'accoppiamento è fallito
                            context.unregisterReceiver(this)
                            activity.runOnUiThread {
                                Toast.makeText(context, "Accoppiamento fallito", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // Registra il BroadcastReceiver
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            activity.registerReceiver(bondReceiver, filter)
        } else {
            // Se già accoppiato, procedi con la connessione
            connectSocket(device, activity, holder, person, you_are_connected)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectSocket(device: BluetoothDevice, activity: Activity, holder: MyAdapter.MyViewHolder, person: Person, you_are_connected: MyAdapter.Ref<Boolean>){
        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                activity.runOnUiThread {
                    MotionToast.createColorToast(
                        activity,
                        activity.resources.getString(R.string.successo),
                        activity.resources.getString(R.string.connected),
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(activity, www.sanju.motiontoast.R.font.helvetica_regular))

                    holder.connect.visibility = View.GONE
                    holder.message.visibility = View.VISIBLE
                    holder.share.visibility = View.VISIBLE
                    holder.disconnect.visibility = View.VISIBLE

                    you_are_connected.value = true

                    outputStream = socket.outputStream
                    inputStream = socket.inputStream
                    receiveFromSocket(activity) //questo in realta' si puo' togliere tanto l'altra persona non ti puo' inviare messaggi
                }
            } catch (e: IOException) {
                activity.runOnUiThread {
                    socketError(e, activity)
                }
            }
        }.start()
    }

    fun sendMessage(message: String, activity: Activity) {
        Thread {
            try {
                outputStream.write(message.toByteArray())
            } catch (e: IOException) {
                socketError(e, activity)
            }
        }.start()

    }

     private fun receiveFromSocket(activity: Activity) {
        Thread {
            try {
                inputStream = socket.inputStream
                val buffer = ByteArray(1024)  // buffer store for the stream
                var bytes: Int // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    receiveMessage(activity, "pippo", incomingMessage, "Maschio") //da modificare username e gender
                }
            } catch (e: IOException) {
                if(e.message != "bt socket closed, read return: -1")
                    socketError(e, activity)
            }


        }.start()

    }

    @SuppressLint("MissingPermission")
    fun alreadyConnected(device: BluetoothDevice?): Boolean {
        return device?.bondState == BluetoothDevice.BOND_BONDED
    }

    fun unBondBluetoothDevice(device: BluetoothDevice?) {
        val pair = device?.javaClass?.getMethod("removeBond")
        if (pair != null) {
            pair.invoke(device)
        }
    }

    fun closeConnections() {
        println("chiudo la connessione")
        try {
            serverSocket?.close()
            socket.close()
            inputStream.close()
            outputStream.close()
        } catch (e: UninitializedPropertyAccessException) {
           //non fare niente se le variaibli non sono state inizializzate
        }
    }

    fun getPersonlist(): List<Person>{
        return socialModel.getPersonsList()
    }

    fun filterList(filter: String): List<Person> {
        return socialModel.filterList(filter)
    }

    fun getfoundDevices(personlist: List<Person>): String{
        return when(val number = personlist.size){
            0 -> {
                ""
            }

            1 -> {
                "$number persona trovata"
            }

            else -> {
                "$number persone trovate"
            }
        }
    }

}