package com.example.fitnesstracker

import MyAdapter
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent

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
import android.widget.Button
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
        println("sono in list update")
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


            val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)

            bottomNavigationView.selectedItemId = R.id.nav_users

            bottomNavigationView.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_stats -> {
                        val intent = Intent(this, StatsActivity::class.java)
                        val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                        startActivity(intent, options.toBundle())
                        true
                    }
                    R.id.nav_home -> {
                        val intent = Intent(this, HomeActivity::class.java)
                        val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                        startActivity(intent, options.toBundle())
                        true
                    }
                    else -> false
                }
            }

        }, 1 * 8 * 1000L)  // meno di un minuto

    }




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth)

        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.nav_users

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, StatsActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                else -> false
            }
        }


        socialController = SocialController(this)
        socialController.setupBluetooth()

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
        }

        val gifImageView: GifImageView = findViewById(R.id.bluetoothGif)
        gifDrawable = gifImageView.drawable as GifDrawable

        nDevices = findViewById(R.id.n_devices)

        gifDrawable.stop()

        gifImageView.setOnClickListener {
            socialController.startBluetooth()
        }


        myAdapter = MyAdapter(socialController.getPersonlist(), socialController )


    }



    override fun onDestroy() {
        super.onDestroy()
        socialController.closeConnections()
    }

    override fun onPause() {
        super.onPause()
        socialController.closeConnections()
    }


}


