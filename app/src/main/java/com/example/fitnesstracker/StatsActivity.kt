package com.example.fitnesstracker

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationBarView

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)

        calendarView.setOnDateChangeListener { _, _, _, _ ->
            showDateDialog()
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.nav_users -> {
                    val intent = Intent(this, Social::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                else -> false
            }
        }

        val activityArray = arrayOf(resources.getString(R.string.nothing)) + resources.getStringArray(R.array.activity_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinnerActivity = findViewById<Spinner>(R.id.filtro)
        spinnerActivity.adapter = adapter
    }

    private fun showDateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Imposta i parametri del layout del dialogo dopo averlo mostrato
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            1500 // Altezza in pixel
        )

        val closeButton: ImageButton = dialogView.findViewById(R.id.close_button)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }
    }



}
