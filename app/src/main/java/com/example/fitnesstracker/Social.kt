package com.example.fitnesstracker

import MyAdapter
import Utils.setupBottomNavigationView
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

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


    private lateinit var socialController: SocialController


    private lateinit var gifDrawable: GifDrawable


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        socialController.handleBluetoothPermissionResult(requestCode, grantResults)
    }

    private lateinit var nDevices: TextView


    override fun listUpdated(personList: List<Person>) {
        nDevices.text = socialController.getfoundDevices(personList)
        myAdapter.updateList(personList)


        if(this::noPeople.isInitialized && search.visibility != View.VISIBLE && socialController.getPersonlist().isNotEmpty()) {
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

            if(socialController.getPersonlist().isNotEmpty() && search.visibility != View.VISIBLE){
                noPeople.visibility = View.GONE
                search.visibility = View.VISIBLE
            }

            myRecyclerView = findViewById(R.id.myRecyclerView)
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




        }, 1 * 8 * 1000L)  // meno di un minuto

    }


    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)
        setupBottomNavigationView(this, "nav_users", bottomNavigationView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth)




        val db = AppDatabase.getDatabase(this)

        socialController = SocialController(this, db)
        socialController.setupBluetooth()



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
                .setTitle("Info")
                .setMessage(Html.fromHtml(infoMessage, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        val gifImageView: GifImageView = findViewById(R.id.bluetoothGif)
        gifDrawable = gifImageView.drawable as GifDrawable

        nDevices = findViewById(R.id.n_devices)

        gifDrawable.stop()

        gifImageView.setOnClickListener {
            socialController.startBluetooth()
        }


        val buttonDiscoverable: Button = findViewById(R.id.discover)
        buttonDiscoverable.setOnClickListener{
            MotionToast.createColorToast(
                this,
                this.resources.getString(R.string.successo),
                "sei discoverabile da altri dispositivi",
                MotionToastStyle.SUCCESS,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this, www.sanju.motiontoast.R.font.helvetica_regular))


            socialController.beDiscoverable()
            gifImageView.isEnabled = false

            // Cambia il colore della GifImageView a grigio
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)  // 0 significa che è completamente desaturato
            val filter = ColorMatrixColorFilter(matrix)
            gifImageView.colorFilter = filter

            // Crea un Handler e un Runnable per riabilitare il bottone dopo 1 minuto
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                gifImageView.isEnabled = true
                gifImageView.clearColorFilter()
            }, 60000 )  // 60000  millisecondi = 1 minuto
        }



        myAdapter = MyAdapter(socialController.getPersonlist(), socialController )


    }



    override fun onDestroy() {
        super.onDestroy()
        socialController.closeConnections()
    }




}


