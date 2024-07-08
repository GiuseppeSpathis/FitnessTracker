package com.example.fitnesstracker

import Utils
import Utils.hasPermission
import Utils.navigateTo
import Utils.receiveMessage
import Utils.socketError
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException
import java.util.UUID
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime




class SocialHandler (private val SocialInterface: SocialInterface, private val database: AppDatabase,
                     private var uuid: UUID,
                     var socket: BluetoothSocket? = null,
                     var pairedDevice: BluetoothDevice? = null,
) {

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT  = 1
        private const val REQUEST_BLUETOOTH_SCAN  = 2
        private const val REQUEST_BLUETOOTH_ADMIN = 3

    }



    private val model = Model()

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = SocialInterface.getActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }





    @SuppressLint("MissingPermission")
    private fun startDiscovery(){
        SocialInterface.startAnimation()
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
                            val found = model.updateList(device)
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
        // L'utente ha rifiutato l'abilitazione del Bluetooth
        // Esegui la navigazione alla HomeActivity
        if (result.resultCode != Activity.RESULT_OK) {
            navigateTo(SocialInterface.getActivity(), HomeActivity::class.java)

        }
    }

    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtResultLauncher.launch(enableBtIntent)
    }



    @RequiresApi(Build.VERSION_CODES.S)
    fun handleBluetoothPermissionResult(requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults.isNotEmpty()) { // Check if there are any results at all
                    val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialInterface.getActivity()))
                        {
                            ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
                        }
                        enableBluetooth()
                    } else {
                        // Permission denied, handle the case where the user denies permission
                        Toast.makeText(SocialInterface.getActivity(), "Permesso BLUETOOTH_CONNECT negato", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun setupBluetooth() {

        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity()))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)

            }

        }
        if(!hasPermission(Manifest.permission.BLUETOOTH_ADMIN, SocialInterface.getActivity()))
        {
                ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_ADMIN)

        }
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN, SocialInterface.getActivity()))
        {
            ActivityCompat.requestPermissions(SocialInterface.getActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
        }


        if (bluetoothAdapter?.isEnabled == false && hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity())) {
            enableBluetooth()

        }
    }

    @SuppressLint("MissingPermission")
    fun beDiscoverable(){
       if (bluetoothAdapter?.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000000)
            SocialInterface.getActivity().startActivity(discoverableIntent)
        }
        bluetoothAdapter?.setName(LoggedUser.username)
        startServer()
    }

    fun startBluetooth (){
        // Registra il BroadcastReceiver per ricevere i dispositivi trovati
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        SocialInterface.getActivity().registerReceiver(receiver, filter)
        startDiscovery()
    }



    @SuppressLint("MissingPermission")
    fun startServer() {
        val thread = Thread {
            try {
                val serverSocket = bluetoothAdapter!!.listenUsingRfcommWithServiceRecord("FitnessTrackerService", uuid)
                val tmpSocket = serverSocket?.accept()

                if(tmpSocket!= null){

                    socket = tmpSocket
                    val remoteDeviceName = socket?.remoteDevice!!.name

                    receiveFromSocket(SocialInterface.getActivity(), remoteDeviceName)
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
                    closeConnections()

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
                    socketError(e, activity)
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun connect2device(device: BluetoothDevice?, activity: Activity, holder: MyAdapter.MyViewHolder, you_are_connected: MyAdapter.Ref<Boolean>){
        if (device == null) return

        // Verifica se il dispositivo è già accoppiato
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            // Se non è accoppiato, inizia il processo di accoppiamento
            device.createBond()
            pairedDevice = device
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
                            connectSocket(device, activity, holder, you_are_connected)
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
            connectSocket(device, activity, holder, you_are_connected)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectSocket(device: BluetoothDevice, activity: Activity, holder: MyAdapter.MyViewHolder, you_are_connected: MyAdapter.Ref<Boolean>){
        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()

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


                }
            } catch (e: IOException) {

                    socketError(e, activity)

            }
        }.start()
    }

    fun sendMessage(message: String) {
        Thread {
            try {
                val outputStream = socket?.outputStream
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                e.printStackTrace()

            }
        }.start()

    }

    private fun receiveFromSocket(activity: Activity, username: String) {
        Thread {
            try {
                val inputStream = socket?.inputStream
                val buffer = ByteArray(1024)  // buffer store for the stream
                var bytes: Int // bytes returned from read()
                var jsonString = ""

                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    // Read from the InputStream
                    bytes = inputStream!!.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)

                    // Concatena i dati ricevuti per formare il JSON completo
                    jsonString += incomingMessage


                    // Verifica se hai ricevuto l'intero JSON
                    if (jsonString.startsWith("#[{\"activityType\"")) {
                        if(jsonString.endsWith(("}]"))){
                            handleReceivedJSON(jsonString.removePrefix("#"))
                            jsonString = ""
                        }
                    }
                    else { //mi sta inviando una semplice stringa
                        val message = jsonString
                        jsonString= ""
                        CoroutineScope(Dispatchers.IO).launch {
                            val user = Utils.getUser(username)
                            withContext(Dispatchers.Main) {
                                receiveMessage(activity, user?.name ?: " ", message, user?.gender ?: "Maschio")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                if (e.message != "bt socket closed, read return: -1")
                    socketError(e, activity)
            }
        }.start()
    }

    private fun handleReceivedJSON(jsonString: String) {
        try {
            // Esegui il parsing del JSON
            val gson = GsonBuilder()
                .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
                .create()

            val activities: List<OthersActivity> = gson.fromJson(jsonString, object : TypeToken<List<OthersActivity>>() {}.type)
            CoroutineScope(Dispatchers.Main).launch {
                // Inserisci prima tutte le attività nel database

                activities.forEach { attivita ->
                    model.insertOthActivities(database, attivita)
                }
                // Poi ottieni l'utente e chiama receiveMessage
                val user = Utils.getUser(activities[0].username)
                SocialInterface.getActivity().runOnUiThread {
                    receiveMessage(SocialInterface.getActivity(), user!!.name, user.name + " " + SocialInterface.getActivity().resources.getString(R.string.messageShared), user.gender, true)
                }
            }
        } catch (e: Exception) {
            // Gestisci l'eccezione
        }
    }




    @SuppressLint("MissingPermission")
    fun alreadyConnected(device: BluetoothDevice?): Boolean {
        return device?.bondState == BluetoothDevice.BOND_BONDED
    }

    private fun unBondBluetoothDevice(device: BluetoothDevice?) {
        val pair = device?.javaClass?.getMethod("removeBond")
        if (pair != null) {
            pair.invoke(device)
        }
    }

    @SuppressLint("MissingPermission")
    fun closeConnections() {
        try {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT, SocialInterface.getActivity())
            ) {
                if(pairedDevice!= null) {
                    if (pairedDevice!!.bondState == BluetoothDevice.BOND_BONDED) {
                        unBondBluetoothDevice(pairedDevice)
                        pairedDevice = null
                    }
                }
            }

            val inputStream = socket?.inputStream
            val outputStream = socket?.outputStream
            inputStream?.close()
            outputStream?.close()
            val serverSocket = bluetoothAdapter!!.listenUsingRfcommWithServiceRecord("FitnessTrackerService", uuid)
            serverSocket?.close()
            socket?.close()
        } catch (e: IOException) {
            println("Errore durante la chiusura delle connessioni: ${e.message}")
        }
    }

    fun shareData() {
        Thread {
            val attivitaDao = database.attivitàDao()
            val activities = attivitaDao.getAllActivitites()
            // Converti le Attività in OthersActivity con username di LoggedUser
            val username = LoggedUser.username
            val otherActivities = activities.map { attività ->
                OthersActivity(username, attività)
            }

            // Converti la lista di attività in JSON usando Gson
            val gson = GsonBuilder()
                .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
                .create()
            val activitiesJson = gson.toJson(otherActivities)
            // Passa all'UI thread per mostrare il toast se non ci sono attività
            if (activitiesJson == "[]") {
                SocialInterface.getActivity().runOnUiThread {
                    MotionToast.createColorToast(
                        SocialInterface.getActivity(),
                        SocialInterface.getActivity().resources.getString(R.string.error),
                        SocialInterface.getActivity().resources.getString(R.string.noActivities),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(
                            SocialInterface.getActivity(),
                            www.sanju.motiontoast.R.font.helvetica_regular
                        )
                    )
                }
            } else {
                // Invia il JSON delle attività tramite Bluetooth
                SocialInterface.getActivity().runOnUiThread {
                    sendMessage("#$activitiesJson")
                }
            }
        }.start()
    }






    fun getPersonlist(): List<Person>{
        return model.getPersonsList()
    }

    fun filterList(filter: String): List<Person> {
        return model.filterList(filter)
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