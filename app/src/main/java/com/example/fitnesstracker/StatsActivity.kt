package com.example.fitnesstracker

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationBarView

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val bottomNavigationView = findViewById<NavigationBarView>(R.id.bottom_navigation)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            showDateDialog(year, month, dayOfMonth)
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

    private fun showDateDialog(year: Int, month: Int, day: Int) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        val closeButton: ImageButton = dialogView.findViewById(R.id.close_button)
        val positiveButton: Button = dialogView.findViewById(R.id.positive_button)
        val negativeButton: Button = dialogView.findViewById(R.id.negative_button)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        positiveButton.setOnClickListener {
            showActivityDialog("Attività")
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            showActivityDialog("Attività in background")
            dialog.dismiss()
        }


    }


    private fun showActivityDialog(activityType: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(activityType)
            .setMessage("Questo è un dialog per $activityType")
            .setPositiveButton("CHIUDI") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }
}
