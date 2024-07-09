package com.example.fitnesstracker

import MyAdapter
import Utils.setupBottomNavigationView
import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationBarView
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.util.UUID


interface SocialInterface {
    fun listUpdated(personList: List<Person>)
    fun getActivity(): AppCompatActivity

    fun startAnimation()
}



class Social : AppCompatActivity(), SocialInterface {





    private lateinit var search: EditText

    private lateinit var noPeople: TextView

    lateinit var myAdapter: MyAdapter
    private lateinit var myRecyclerView: RecyclerView


    private lateinit var socialHandler: SocialHandler
    private val uuid =  UUID.fromString("79c16f25-a50b-450e-9d10-fc267964b3aa")


    private lateinit var gifDrawable: GifDrawable




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            socialHandler.handleBluetoothPermissionResult(requestCode, grantResults)
        }
    }

    private lateinit var nDevices: TextView


    override fun listUpdated(personList: List<Person>) {
        nDevices.text = socialHandler.getfoundDevices(personList)
        myAdapter.updateList(personList)


        if(this::noPeople.isInitialized && search.visibility != View.VISIBLE && socialHandler.getPersonlist().isNotEmpty()) {
            noPeople.visibility = View.GONE
            search.visibility = View.VISIBLE
        }

    }

    override fun getActivity(): AppCompatActivity {
        return this
    }

    override fun startAnimation(){
        gifDrawable.start()
        val handler = Handler(Looper.getMainLooper())

        // Invia un Runnable che verrà eseguito dopo meno di un minuto, gira sul mainThread
        handler.postDelayed({

            setContentView(R.layout.activity_social)
            noPeople = findViewById(R.id.noPeople)
            search = findViewById(R.id.search)

            if(socialHandler.getPersonlist().isNotEmpty() && search.visibility != View.VISIBLE){
                noPeople.visibility = View.GONE
                search.visibility = View.VISIBLE
            }

            myRecyclerView = findViewById(R.id.myRecyclerView)
            myRecyclerView.adapter = myAdapter
            myRecyclerView.layoutManager = LinearLayoutManager(this)

            val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
            setupBottomNavigationView(this, "nav_users", bottomNavigationView)

            search.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    //niente
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    //niente
                }
                override fun afterTextChanged(s: Editable) {
                    myAdapter.updateList(socialHandler.filterList(s.toString()))
                }
            })


        }, 1 * 8 * 1000L)  // meno di un minuto

    }



    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Salva lo stato delle variabili nel SocialHandler
        socialHandler.socket?.let { outState.putBluetoothSocket("socket", it) }
        socialHandler.pairedDevice?.let { outState.putParcelable("pairedDevice", it) }

    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Ripristina lo stato delle variabili nel SocialHandler
        socialHandler.socket = savedInstanceState.getBluetoothSocket("socketInfo", uuid, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            socialHandler.pairedDevice = savedInstanceState.getParcelable("pairedDevice", BluetoothDevice::class.java)
        }
        else {
            socialHandler.pairedDevice = savedInstanceState.getParcelable("pairedDevice")
        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth)

        val db = AppDatabase.getDatabase(this)


        socialHandler = if (savedInstanceState != null) {
            // Ripristina lo stato
            val socket = savedInstanceState.getBluetoothSocket("socket", uuid, this)

                val pairedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    savedInstanceState.getParcelable("pairedDevice", BluetoothDevice::class.java)
                }
            else {
                    savedInstanceState.getParcelable("pairedDevice") as BluetoothDevice?
                }

                SocialHandler(this, db, uuid, socket, pairedDevice)

        } else {
            SocialHandler(this, db, uuid)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            socialHandler.setupBluetooth()
        }

        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
        setupBottomNavigationView(this, "nav_users", bottomNavigationView)

        val infoButton: ImageButton = findViewById(R.id.infoButton)
        infoButton.setOnClickListener {
            val beDiscoverable = getString(R.string.beDiscoverable)
            val infoMessageParts = resources.getStringArray(R.array.info_message_parts)

            // Combina tutte le parti del messaggio HTML
            val builder = StringBuilder()
            for (part in infoMessageParts) {
                // Interpola la stringa beDiscoverable nella prima parte
                if (part.contains("%1\$s")) {
                    builder.append(String.format(part, beDiscoverable))
                } else {
                    builder.append(part)
                }
            }
            val infoMessage = builder.toString()

            // Mostra il dialogo con il messaggio HTML
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.info))
                .setMessage(Html.fromHtml(infoMessage, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        val gifImageView: GifImageView = findViewById(R.id.bluetoothGif)
        gifDrawable = gifImageView.drawable as GifDrawable

        nDevices = findViewById(R.id.n_devices)

        gifDrawable.stop()

        gifImageView.setOnClickListener {
            socialHandler.startBluetooth()
        }


        val buttonDiscoverable: Button = findViewById(R.id.discover)
        buttonDiscoverable.setOnClickListener{
            MotionToast.createColorToast(
                this,
                this.resources.getString(R.string.successo),
                getString(R.string.successDiscoverable),
                MotionToastStyle.SUCCESS,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this, www.sanju.motiontoast.R.font.helvetica_regular))


            socialHandler.beDiscoverable()
            gifImageView.isEnabled = false

            // Cambia il colore della GifImageView a grigio
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)  // 0 significa che è completamente desaturato
            val filter = ColorMatrixColorFilter(matrix)
            gifImageView.colorFilter = filter

            // Crea un Handler e un Runnable per riabilitare il bottone dopo 1 minuto
            //un Runnable e diciamo un codice gestito da un Handler che viene lanciato un thread specifico, in questo caso il Main Thread
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                gifImageView.isEnabled = true
                gifImageView.clearColorFilter()
            }, 60000 )  // 60000  millisecondi = 1 minuto
        }



        myAdapter = MyAdapter(socialHandler.getPersonlist(), socialHandler )


    }





    override fun onDestroy() {
        super.onDestroy()
        socialHandler.closeConnections()
    }

    private fun Bundle.putBluetoothSocket(key: String, socket: BluetoothSocket) {
        val deviceAddress = socket.remoteDevice.address
        putString("$key-deviceAddress", deviceAddress)
    }



    private fun Bundle.getBluetoothSocket(key: String, uuid: UUID, context: Context): BluetoothSocket? {
        val deviceAddress = getString("$key-deviceAddress") ?: return null
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return null
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.createRfcommSocketToServiceRecord(uuid)
        } else {
            null
        }
    }


}


