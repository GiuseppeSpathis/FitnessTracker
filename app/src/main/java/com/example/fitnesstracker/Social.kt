package com.example.fitnesstracker

import MyAdapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import okhttp3.internal.wait
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager


interface SocialInterface {
    fun listUpdated(personList: List<Person>)
    fun getActivity(): AppCompatActivity

    fun startAnimation()
}



class Social : AppCompatActivity(), SocialInterface {





    private lateinit var search: EditText

    private lateinit var myAdapter: MyAdapter
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
            search = findViewById(R.id.search)
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

        }, 1 * 10 * 1000L)  // meno di un minuto

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth)


        val gifImageView: GifImageView = findViewById(R.id.bluetoothGif)
        gifDrawable = gifImageView.drawable as GifDrawable

        nDevices = findViewById(R.id.n_devices)

        gifDrawable.stop()

        gifImageView.setOnClickListener {
            socialController.startBluetooth()
        }


        socialController = SocialController(this)


        myAdapter = MyAdapter(socialController.getPersonlist())







    }



}


