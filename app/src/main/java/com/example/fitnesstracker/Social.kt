package com.example.fitnesstracker

import MyAdapter
import Utils.setupBottomNavigationView

import android.app.AlertDialog

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



    private var isSocialLayout : Boolean = false
    private lateinit var search: EditText

    private lateinit var noPeople: TextView

    lateinit var myAdapter: MyAdapter
    private lateinit var myRecyclerView: RecyclerView


    private lateinit var socialHandler: SocialHandler
    private val uuid =  UUID.fromString("f8a51872-8d35-4e07-b352-508e7681d33a")


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

    private fun initializeSocialLayout() {
        setContentView(R.layout.activity_social)

        noPeople = findViewById(R.id.noPeople)
        search = findViewById(R.id.search)

        if (socialHandler.getPersonlist().isNotEmpty() && search.visibility != View.VISIBLE) {
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
                // Nothing to do here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nothing to do here
            }

            override fun afterTextChanged(s: Editable) {
                myAdapter.updateList(socialHandler.filterList(s.toString()))
            }
        })
    }

    override fun startAnimation(){
        gifDrawable.start()
        val handler = Handler(Looper.getMainLooper())

        // Invia un Runnable che verrà eseguito dopo meno di un minuto, gira sul mainThread
        handler.postDelayed({
            isSocialLayout = true
            initializeSocialLayout()
        }, 1 * 8 * 1000L)  // meno di un minuto

    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        val db = AppDatabase.getDatabase(this)


        socialHandler = SocialHandler(this, db, uuid)


        myAdapter = MyAdapter(socialHandler.getPersonlist(), socialHandler )
        if(isSocialLayout){
            initializeSocialLayout()
        } else {
            setContentView(R.layout.bluetooth)
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
        }

    }





    override fun onDestroy() {
        super.onDestroy()
        socialHandler.closeConnections()
    }


}


