package com.example.fitnesstracker

import Utils.convertToActivities
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.fitnesstracker.databinding.ActivityStatsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.time.LocalDateTime
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Random


class StatsActivity : AppCompatActivity() {


    private lateinit var db: AppDatabase
    private lateinit var binding: ActivityStatsBinding
    private lateinit var pieChart: PieChart
    private lateinit var periodMessage: TextView
    private lateinit var geofencePieChart: PieChart
    private lateinit var periodMessageActivities: TextView
    private lateinit var attivitàDao: ActivityDao

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)
        attivitàDao = db.attivitàDao()

        periodMessage = binding.periodMessage
        periodMessageActivities = binding.periodMessageActivities

        val calendarView = binding.calendarView
        val bottomNavigationView = binding.bottomNavigation
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            showDateDialog(this, year, month + 1, dayOfMonth, db)  // month is zero-based in CalendarView
        }
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
                R.id.nav_users -> {
                    val intent = Intent(this, Social::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                R.id.geofence -> {
                    val intent = Intent(this, GeoFenceActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                    startActivity(intent, options.toBundle())
                    true
                }
                else -> false
            }
        }
        val activityArray = arrayOf(getString(R.string.nothing)) + resources.getStringArray(R.array.activity_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinnerActivity = binding.filtro
        spinnerActivity.adapter = adapter
        //resetDatabase()
        //insertFakeData()
        pieChart = binding.pieChart
        geofencePieChart = binding.geofencePieChart
        val buttons = listOf(binding.btnDay, binding.btnWeek, binding.btnMonth, binding.btnYear, binding.btnGeofenceDay, binding.btnGeofenceWeek, binding.btnGeofenceMonth, binding.btnGeofenceYear)
        val listener = View.OnClickListener { view ->
            buttons.forEach { button ->
                button.isSelected = button == view
                button.setTextColor(Color.WHITE)
            }
            when (view.id) {
                R.id.btn_day -> updatePieChartForPeriod("day", "activities")
                R.id.btn_week -> updatePieChartForPeriod("week", "activities")
                R.id.btn_month -> updatePieChartForPeriod("month", "activities")
                R.id.btn_year -> updatePieChartForPeriod("year", "activities")
                R.id.btn_geofence_day -> updatePieChartForPeriod("day", "geofences")
                R.id.btn_geofence_week -> updatePieChartForPeriod("week", "geofences")
                R.id.btn_geofence_month -> updatePieChartForPeriod("month", "geofences")
                R.id.btn_geofence_year -> updatePieChartForPeriod("year", "geofences")
            }
        }
        buttons.forEach { it.setOnClickListener(listener) }



    }

    suspend fun insertFakeGeofences() {
        withContext(Dispatchers.IO) {
            val random = Random()

            for (i in 1..10) {
                val latitude = 37.7749 + random.nextDouble() / 100
                val longitude = -122.4194 + random.nextDouble() / 100
                val radius = (100..500).random().toFloat()
                val enterTime = System.currentTimeMillis() - (random.nextInt(10000) * 1000).toLong()
                val exitTime = enterTime + (random.nextInt(10000) * 1000).toLong()
                val date = "2024-06-23"
                val placeName = "Fake Place $i"

                val geofence = timeGeofence(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    enterTime = enterTime,
                    exitTime = exitTime,
                    date = date,
                    placeName = placeName
                )
                db.attivitàDao().insertTimeGeofence(geofence)
                println("geofence inserita: $geofence")
            }
        }
    }

    companion object {

        @RequiresApi(Build.VERSION_CODES.O)
        fun showActivityPopup(activityType: String, activity: Attività, context: Context) {

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.popup_activity_details, null)

            val startTimeTextView = view.findViewById<TextView>(R.id.start_time)
            startTimeTextView.text = "Start: ${activity.startTime.toLocalTime()}"

            val endTimeTextView = view.findViewById<TextView>(R.id.end_time)
            endTimeTextView.text = "End: ${activity.endTime.toLocalTime()}"

            val stepCountLayout = view.findViewById<LinearLayout>(R.id.step_count_layout)
            val stepCountTextView = view.findViewById<TextView>(R.id.step_count)
            if (activity.stepCount != null) {
                stepCountTextView.text = "Step Count: ${activity.stepCount}"
            } else {
                stepCountLayout.visibility = View.GONE
            }

            val distanceLayout = view.findViewById<LinearLayout>(R.id.distance_layout)
            val distanceTextView = view.findViewById<TextView>(R.id.distance)
            if (activity.distance != null) {
                distanceTextView.text = "Distance: ${activity.distance}"
            } else {
                distanceLayout.visibility = View.GONE
            }

            val paceLayout = view.findViewById<LinearLayout>(R.id.pace_layout)
            val paceTextView = view.findViewById<TextView>(R.id.pace)
            if (activity.pace != null) {
                paceTextView.text = "Pace: ${activity.pace}"
            } else {
                paceLayout.visibility = View.GONE
            }

            val avgSpeedLayout = view.findViewById<LinearLayout>(R.id.avg_speed_layout)
            val avgSpeedTextView = view.findViewById<TextView>(R.id.avg_speed)
            if (activity.avgSpeed != null) {
                avgSpeedTextView.text = "Avg Speed: ${activity.avgSpeed}"
            } else {
                avgSpeedLayout.visibility = View.GONE
            }

            val maxSpeedLayout = view.findViewById<LinearLayout>(R.id.max_speed_layout)
            val maxSpeedTextView = view.findViewById<TextView>(R.id.max_speed)
            if (activity.maxSpeed != null) {
                maxSpeedTextView.text = "Max Speed: ${activity.maxSpeed}"
            } else {
                maxSpeedLayout.visibility = View.GONE
            }

            val dateTextView = view.findViewById<TextView>(R.id.date)
            dateTextView.text = "Date: ${activity.date}"

            AlertDialog.Builder(context)
                .setTitle("Activity Details: $activityType")
                .setView(view)
                .setPositiveButton("OK", null)
                .show()
        }

        fun getActivityTypeByColor(colors: List<Int>, activityColors: Map<String, Int>, stackIndex: Int?): String {
            stackIndex?.let {
                val colorIndex = it % colors.size
                return activityColors.entries.firstOrNull { it.value == colors[colorIndex] }?.key ?: "Not tracked"
            }
            return "Not tracked"
        }
        @RequiresApi(Build.VERSION_CODES.O)
         fun displayActivitiesForDate(context: Context, container: LinearLayout, activities: List<Attività>) {
            val activityColors = mapOf(
                "Passeggiata" to ContextCompat.getColor(context, R.color.passeggiata),
                "Corsa" to ContextCompat.getColor(context, R.color.corsa),
                "Stare fermo" to ContextCompat.getColor(context, R.color.stare_fermo),
                "Guidare" to ContextCompat.getColor(context, R.color.guidare),
                "Not tracked" to ContextCompat.getColor(context, R.color.not_tracked)
            )

            val totalMinutesPerHour = Array(24) { mutableListOf<Pair<String, Int>>() }

            for (activity in activities) {
                Log.d("ActivityDebug", "processing activity: $activity")
                var startHour = activity.startTime.hour
                val endHour = activity.endTime.hour
                var startMinute = activity.startTime.minute
                val endMinute = activity.endTime.minute

                while (startHour <= endHour) {
                    val activityType = activity.activityType
                    val currentHourList = totalMinutesPerHour[startHour]

                    val duration = when {
                        startHour == endHour -> endMinute - startMinute
                        startHour == activity.startTime.hour -> 60 - startMinute
                        startHour < endHour && startHour != activity.endTime.hour -> 60
                        startHour == activity.endTime.hour -> endMinute
                        else -> 0
                    }

                    if (duration > 0) {
                        currentHourList.add(Pair(activityType, duration))
                    }

                    startHour++
                    startMinute = 0  // Reset startMinute for the next hour
                }
            }

            container.removeAllViews()

            for (hour in 0..23) {
                val hourLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100
                ).apply {
                    if (hour == 23) {
                        setMargins(0, -10, 0, 15)
                    } else {
                        setMargins(0, -10, 0, -10)
                    }
                }

                val hourLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = hourLayoutParams
                }

                val hourLabel = TextView(context).apply {
                    text = String.format("%02d - %02d", hour, hour + 1)
                    layoutParams = LinearLayout.LayoutParams(
                        200,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        setPadding(0, 40, 0, 0)
                    }
                }

                val chart = HorizontalBarChart(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        150
                    ).apply {
                        weight = 1f
                    }
                    setDrawGridBackground(false)
                    axisLeft.isEnabled = false
                    axisRight.isEnabled = false
                    xAxis.isEnabled = false
                    legend.isEnabled = false
                    description.isEnabled = false
                    setExtraOffsets(0f, 0f, 0f, 0f)
                }

                val currentHourList = totalMinutesPerHour[hour]
                val durations = mutableListOf<Float>()
                val colors = mutableListOf<Int>()

                var totalDuration = 0

                if (currentHourList.isEmpty()) {
                    durations.add(60f)
                    colors.add(activityColors["Not tracked"] ?: Color.GRAY)
                } else {
                    for ((activityType, duration) in currentHourList) {
                        durations.add(duration.toFloat())
                        colors.add(activityColors[activityType] ?: Color.GRAY)
                        totalDuration += duration

                        // Aggiungi una barra nera di separazione
                        durations.add(1f) // Puoi regolare lo spessore della barra nera cambiando questo valore
                        colors.add(Color.BLACK)
                    }
                    if (totalDuration < 60) {
                        durations.add((60 - totalDuration).toFloat())
                        colors.add(activityColors["Not tracked"] ?: Color.GRAY)
                    }
                }

                val barEntries = listOf(BarEntry(0f, durations.toFloatArray()))
                val barDataSet = BarDataSet(barEntries, "")
                barDataSet.colors = colors
                barDataSet.setDrawValues(false)

                val barData = BarData(barDataSet)
                chart.data = barData
                chart.invalidate()

                chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e is BarEntry) {
                            val activityType = getActivityTypeByColor(colors, activityColors, h?.stackIndex)
                            Log.d("ActivityDebug:", "Clicked activity: $activityType")

                            val startMinute = (hour * 60) + ((h?.stackIndex ?: 0) * 60 / durations.size)
                            val endMinute = startMinute + (60 / durations.size)

                            val clickedActivities = activities.filter {
                                it.activityType == activityType &&
                                        it.startTime.hour * 60 + it.startTime.minute <= endMinute &&
                                        it.endTime.hour * 60 + it.endTime.minute >= startMinute
                            }

                            clickedActivities.forEach { activity ->
                                Log.d("ActivityDebug", "Activity Clicked: $activity")
                                showActivityPopup(activityType, activity, context)
                            }
                        }
                    }

                    override fun onNothingSelected() {}
                })

                hourLayout.addView(hourLabel)
                hourLayout.addView(chart)

                container.addView(hourLayout)
            }

            val legendLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            activityColors.forEach { (activity, color) ->
                val legendItemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                }

                val colorView = View(context).apply {
                    setBackgroundColor(color)
                    layoutParams = LinearLayout.LayoutParams(50, 50)
                }

                val activityLabel = TextView(context).apply {
                    text = activity
                    setPadding(16, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                legendItemLayout.addView(colorView)
                legendItemLayout.addView(activityLabel)
                legendLayout.addView(legendItemLayout)
            }

            container.addView(legendLayout)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun getOtherActivitiesForDate(year: Int, month: Int, day: Int, db: AppDatabase): List<OthersActivity> {
            val date = String.format("%02d/%02d/%04d", day, month, year)
            var attivitàDao : ActivityDao = db.attivitàDao()
            Log.d("StatsActivity", "Getting activities for date: $date")
            return withContext(Dispatchers.IO) {
                attivitàDao.getOtherActivitiesByDate(date)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getActivitiesForDate(year: Int, month: Int, day: Int, db: AppDatabase): List<Attività> {
            val date = String.format("%02d/%02d/%04d", day, month, year)
            var attivitàDao : ActivityDao = db.attivitàDao()
            Log.d("StatsActivity", "Getting activities for date: $date")
            return withContext(Dispatchers.IO) {
                attivitàDao.getAttivitàByDate(date)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun showDateDialog(context: Context, year: Int, month: Int, day: Int,  db: AppDatabase, isDialog: Boolean = false) {
            println("sono dentro showDateDialog")
            val dialogView = if(isDialog) {
                LayoutInflater.from(context).inflate(R.layout.custom_dialog2, null)
            }
            else {
                LayoutInflater.from(context).inflate(R.layout.custom_dialog, null)
            }
            val builder = AlertDialog.Builder(context)
                .setView(dialogView)

            val dialog = builder.create()
            dialog.show()

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1500 // Altezza in pixel
            )

            val closeButton: ImageButton = dialogView.findViewById(R.id.close_button)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            val activityChartContainer = dialogView.findViewById<LinearLayout>(R.id.chartContainer)
            val geofenceChartContainer = dialogView.findViewById<LinearLayout>(R.id.geofenceCchartContainer)

            // Assuming the context is an Activity
            val activity = context as? StatsActivity
            activity?.lifecycleScope?.launch {
                try {
                    val activities: List<Attività> = if (isDialog) {
                            val othersActivities = getOtherActivitiesForDate(year, month, day, db)
                            convertToActivities(othersActivities)
                        } else {
                            getActivitiesForDate(year, month, day, db)
                        }
                        if (!isDialog) {
                            val geofenceChartContainer =
                                dialogView.findViewById<LinearLayout>(R.id.geofenceCchartContainer)
                            val geofences = activity!!.getGeofencesForDate(year, month, day)
                            withContext(Dispatchers.Main) {
                                activity.displayGeofencesForDate(geofenceChartContainer, geofences)
                            }
                        }
                        withContext(Dispatchers.Main){
                            Log.d("StatsActivity", "Number of activities retrieved: ${activities.size}")
                            displayActivitiesForDate(context, activityChartContainer, activities)
                        }
                } catch (e: Exception) {
                    Snackbar.make(activity.binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }



    /*
    @RequiresApi(Build.VERSION_CODES.O)
    fun showDateDialog(year: Int, month: Int, day: Int, isDialog: Boolean=false) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            1500 // Altezza in pixel
        )

        val closeButton: ImageButton = dialogView.findViewById(R.id.close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        val activityChartContainer = dialogView.findViewById<LinearLayout>(R.id.chartContainer)
        val geofenceChartContainer = dialogView.findViewById<LinearLayout>(R.id.geofenceCchartContainer)

        lifecycleScope.launch {
            try {
                //insertFakeGeofences()

                val activities = if(isDialog) {

                    val othersActivities = getOtherActivitiesForDate(year, month, day)
                    convertToActivities(othersActivities)
                }
                 else {
                    getActivitiesForDate(year, month, day)
                }
                    val geofences = getGeofencesForDate(year, month, day)
                    displayGeofencesForDate(geofenceChartContainer, geofences)


                Log.d("StatsActivity", "Number of activities retrieved: ${activities.size}")
                displayActivitiesForDate(activityChartContainer, activities)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }*/



    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getActivitiesForDate(year: Int, month: Int, day: Int): List<Attività> {
        val date = String.format("%02d/%02d/%04d", day, month, year)
        Log.d("StatsActivity", "Getting activities for date: $date")
        return withContext(Dispatchers.IO) {
            Log.d("StatsActivity", "searching for date: $date")
            attivitàDao.getAttivitàByDate(date)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getOtherActivitiesForDate(year: Int, month: Int, day: Int): List<OthersActivity> {
        val date = String.format("%02d/%02d/%04d", day, month, year)
        Log.d("StatsActivity", "Getting activities for date: $date")
        return withContext(Dispatchers.IO) {
            attivitàDao.getOtherActivitiesByDate(date)
        }
    }







    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayActivitiesForDate(container: LinearLayout, activities: List<Attività>) {
        val activityColors = mapOf(
            "Passeggiata" to ContextCompat.getColor(this, R.color.passeggiata),
            "Corsa" to ContextCompat.getColor(this, R.color.corsa),
            "Stare fermo" to ContextCompat.getColor(this, R.color.stare_fermo),
            "Guidare" to ContextCompat.getColor(this, R.color.guidare),
            "Not tracked" to ContextCompat.getColor(this, R.color.not_tracked)
        )

        val totalMinutesPerHour = Array(24) { mutableListOf<Pair<String, Int>>() }

        for (activity in activities) {
            Log.d("ActivityDebug", "processing activity: $activity")
            var startHour = activity.startTime.hour
            val endHour = activity.endTime.hour
            var startMinute = activity.startTime.minute
            val endMinute = activity.endTime.minute

            while (startHour <= endHour) {
                val activityType = activity.activityType
                val currentHourList = totalMinutesPerHour[startHour]

                val duration = when {
                    startHour == endHour -> endMinute - startMinute
                    startHour == activity.startTime.hour -> 60 - startMinute
                    startHour < endHour && startHour != activity.endTime.hour -> 60
                    startHour == activity.endTime.hour -> endMinute
                    else -> 0
                }

                if (duration > 0) {
                    currentHourList.add(Pair(activityType, duration))
                }

                startHour++
                startMinute = 0  // Reset startMinute for the next hour
            }
        }

        container.removeAllViews()

        for (hour in 0..23) {
            val hourLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            ).apply {
                if (hour == 23) {
                    setMargins(0, -10, 0, 15)
                } else {
                    setMargins(0, -10, 0, -10)
                }
            }

            val hourLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = hourLayoutParams
            }

            val hourLabel = TextView(this).apply {
                text = String.format("%02d - %02d", hour, hour + 1)
                layoutParams = LinearLayout.LayoutParams(
                    200,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setPadding(0, 40, 0, 0)
                }
            }

            val chart = HorizontalBarChart(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    150
                ).apply {
                    weight = 1f
                }
                setDrawGridBackground(false)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                xAxis.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                setExtraOffsets(0f, 0f, 0f, 0f)
            }

            val currentHourList = totalMinutesPerHour[hour]
            val durations = mutableListOf<Float>()
            val colors = mutableListOf<Int>()

            var totalDuration = 0

            if (currentHourList.isEmpty()) {
                durations.add(60f)
                colors.add(activityColors["Not tracked"] ?: Color.GRAY)
            } else {
                for ((activityType, duration) in currentHourList) {
                    durations.add(duration.toFloat())
                    colors.add(activityColors[activityType] ?: Color.GRAY)
                    totalDuration += duration

                    // Aggiungi una barra nera di separazione
                    durations.add(1f) // Puoi regolare lo spessore della barra nera cambiando questo valore
                    colors.add(Color.BLACK)
                }
                if (totalDuration < 60) {
                    durations.add((60 - totalDuration).toFloat())
                    colors.add(activityColors["Not tracked"] ?: Color.GRAY)
                }
            }

            val barEntries = listOf(BarEntry(0f, durations.toFloatArray()))
            val barDataSet = BarDataSet(barEntries, "")
            barDataSet.colors = colors
            barDataSet.setDrawValues(false)

            val barData = BarData(barDataSet)
            chart.data = barData
            chart.invalidate()

            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is BarEntry) {
                        val activityType = getActivityTypeByColor(colors, activityColors, h?.stackIndex)
                        Log.d("ActivityDebug:", "Clicked activity: $activityType")

                        val startMinute = (hour * 60) + ((h?.stackIndex ?: 0) * 60 / durations.size)
                        val endMinute = startMinute + (60 / durations.size)

                        val clickedActivities = activities.filter {
                            it.activityType == activityType &&
                                    it.startTime.hour * 60 + it.startTime.minute <= endMinute &&
                                    it.endTime.hour * 60 + it.endTime.minute >= startMinute
                        }

                        clickedActivities.forEach { activity ->
                            Log.d("ActivityDebug", "Activity Clicked: $activity")
                            showActivityPopup(activityType, activity)
                        }
                    }
                }

                override fun onNothingSelected() {}
            })

            hourLayout.addView(hourLabel)
            hourLayout.addView(chart)

            container.addView(hourLayout)
        }

        val legendLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        activityColors.forEach { (activity, color) ->
            val legendItemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val colorView = View(this).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(50, 50)
            }

            val activityLabel = TextView(this).apply {
                text = activity
                setPadding(16, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            legendItemLayout.addView(colorView)
            legendItemLayout.addView(activityLabel)
            legendLayout.addView(legendItemLayout)
        }

        container.addView(legendLayout)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayGeofencesForDate(container: LinearLayout, geofences: List<timeGeofence>) {
        val geofenceColors = mutableMapOf<String, Int>()
        val random = Random()

        geofences.forEach { geofence ->
            if (!geofenceColors.containsKey(geofence.placeName)) {
                val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                geofenceColors[geofence.placeName] = color
            }
        }

        val totalMinutesPerHour = Array(24) { mutableListOf<Pair<String, Int>>() }

        for (geofence in geofences) {
            var startHour = Instant.ofEpochMilli(geofence.enterTime ?: 0).atZone(ZoneId.systemDefault()).hour
            val endHour = Instant.ofEpochMilli(geofence.exitTime ?: 0).atZone(ZoneId.systemDefault()).hour
            var startMinute = Instant.ofEpochMilli(geofence.enterTime ?: 0).atZone(ZoneId.systemDefault()).minute
            val endMinute = Instant.ofEpochMilli(geofence.exitTime ?: 0).atZone(ZoneId.systemDefault()).minute

            while (startHour <= endHour) {
                val placeName = geofence.placeName
                val currentHourList = totalMinutesPerHour[startHour]

                val duration = when {
                    startHour == endHour -> endMinute - startMinute
                    startHour == Instant.ofEpochMilli(geofence.enterTime ?: 0).atZone(ZoneId.systemDefault()).hour -> 60 - startMinute
                    startHour < endHour && startHour != Instant.ofEpochMilli(geofence.exitTime ?: 0).atZone(ZoneId.systemDefault()).hour -> 60
                    startHour == Instant.ofEpochMilli(geofence.exitTime ?: 0).atZone(ZoneId.systemDefault()).hour -> endMinute
                    else -> 0
                }

                if (duration > 0) {
                    currentHourList.add(Pair(placeName, duration))
                }

                startHour++
                startMinute = 0
            }
        }

        container.removeAllViews()

        for (hour in 0..23) {
            val hourLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            ).apply {
                if (hour == 23) {
                    setMargins(0, -10, 0, 15)
                } else {
                    setMargins(0, -10, 0, -10)
                }
            }

            val hourLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = hourLayoutParams
            }

            val hourLabel = TextView(this).apply {
                text = String.format("%02d - %02d", hour, hour + 1)
                layoutParams = LinearLayout.LayoutParams(
                    200,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setPadding(0, 40, 0, 0)
                }
            }

            val chart = HorizontalBarChart(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    150
                ).apply {
                    weight = 1f
                }
                setDrawGridBackground(false)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                xAxis.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                setExtraOffsets(0f, 0f, 0f, 0f)
            }

            val currentHourList = totalMinutesPerHour[hour]
            val durations = mutableListOf<Float>()
            val colors = mutableListOf<Int>()

            var totalDuration = 0

            if (currentHourList.isEmpty()) {
                durations.add(60f)
                colors.add(Color.GRAY)
            } else {
                for ((placeName, duration) in currentHourList) {
                    durations.add(duration.toFloat())
                    colors.add(geofenceColors[placeName] ?: Color.GRAY)
                    totalDuration += duration

                    durations.add(1f)
                    colors.add(Color.BLACK)
                }
                if (totalDuration < 60) {
                    durations.add((60 - totalDuration).toFloat())
                    colors.add(Color.GRAY)
                }
            }

            val barEntries = listOf(BarEntry(0f, durations.toFloatArray()))
            val barDataSet = BarDataSet(barEntries, "")
            barDataSet.colors = colors
            barDataSet.setDrawValues(false)

            val barData = BarData(barDataSet)
            chart.data = barData
            chart.invalidate()

            hourLayout.addView(hourLabel)
            hourLayout.addView(chart)

            container.addView(hourLayout)
        }

        // Crea e aggiungi la legenda
        val legendLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setPadding(16, 16, 16, 16)
            }
        }

        geofenceColors.forEach { (placeName, color) ->
            val legendItemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val colorView = View(this).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(50, 50)
            }

            val placeNameLabel = TextView(this).apply {
                text = placeName
                setPadding(16, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            legendItemLayout.addView(colorView)
            legendItemLayout.addView(placeNameLabel)
            legendLayout.addView(legendItemLayout)
        }

        container.addView(legendLayout)
    }



    private fun getActivityTypeByColor(colors: List<Int>, activityColors: Map<String, Int>, stackIndex: Int?): String {
        stackIndex?.let {
            val colorIndex = it % colors.size
            return activityColors.entries.firstOrNull { it.value == colors[colorIndex] }?.key ?: "Not tracked"
        }
        return "Not tracked"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showActivityPopup(activityType: String, activity: Attività) {

        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.popup_activity_details, null)

        val startTimeTextView = view.findViewById<TextView>(R.id.start_time)
        startTimeTextView.text = "Start: ${activity.startTime.toLocalTime()}"

        val endTimeTextView = view.findViewById<TextView>(R.id.end_time)
        endTimeTextView.text = "End: ${activity.endTime.toLocalTime()}"

        val stepCountLayout = view.findViewById<LinearLayout>(R.id.step_count_layout)
        val stepCountTextView = view.findViewById<TextView>(R.id.step_count)
        if (activity.stepCount != null) {
            stepCountTextView.text = "Step Count: ${activity.stepCount}"
        } else {
            stepCountLayout.visibility = View.GONE
        }

        val distanceLayout = view.findViewById<LinearLayout>(R.id.distance_layout)
        val distanceTextView = view.findViewById<TextView>(R.id.distance)
        if (activity.distance != null) {
            distanceTextView.text = "Distance: ${activity.distance}"
        } else {
            distanceLayout.visibility = View.GONE
        }

        val paceLayout = view.findViewById<LinearLayout>(R.id.pace_layout)
        val paceTextView = view.findViewById<TextView>(R.id.pace)
        if (activity.pace != null) {
            paceTextView.text = "Pace: ${activity.pace}"
        } else {
            paceLayout.visibility = View.GONE
        }

        val avgSpeedLayout = view.findViewById<LinearLayout>(R.id.avg_speed_layout)
        val avgSpeedTextView = view.findViewById<TextView>(R.id.avg_speed)
        if (activity.avgSpeed != null) {
            avgSpeedTextView.text = "Avg Speed: ${activity.avgSpeed}"
        } else {
            avgSpeedLayout.visibility = View.GONE
        }

        val maxSpeedLayout = view.findViewById<LinearLayout>(R.id.max_speed_layout)
        val maxSpeedTextView = view.findViewById<TextView>(R.id.max_speed)
        if (activity.maxSpeed != null) {
            maxSpeedTextView.text = "Max Speed: ${activity.maxSpeed}"
        } else {
            maxSpeedLayout.visibility = View.GONE
        }

        val dateTextView = view.findViewById<TextView>(R.id.date)
        dateTextView.text = "Date: ${activity.date}"

        AlertDialog.Builder(this)
            .setTitle("Activity Details: $activityType")
            .setView(view)
            .setPositiveButton("OK", null)
            .show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun resetDatabase() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                attivitàDao.deleteAll()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)

    private fun updatePieChartForPeriod(period: String, chartType: String) {
        lifecycleScope.launch {
            try {
                if(chartType == "activities"){
                    val activities: List<Attività> = when (period) {
                        "day" -> {
                            periodMessageActivities.text = "Dati relativi all'ultimo giorno"
                            getActivitiesForPeriod(period)
                        }
                        "week" -> {
                            periodMessageActivities.text = "Dati relativi all'ultima settimana"
                            getActivitiesForPeriod(period)
                        }
                        "month" -> {
                            periodMessageActivities.text = "Dati relativi all'ultimo mese"
                            getActivitiesForPeriod(period)
                        }
                        "year" -> {
                            periodMessageActivities.text = "Dati relativi all'ultimo anno"
                            getActivitiesForPeriod(period)
                        }
                        else -> emptyList()
                    }

                    if (activities.isEmpty()) {
                        pieChart.clear()
                        Snackbar.make(binding.root, "Nessun dato disponibile", Snackbar.LENGTH_SHORT).show()
                    } else {
                        displayPieChart(activities)
                    }
                } else {
                    val geofences : List<timeGeofence> = when (period) {
                        "day" -> {
                            periodMessage.text = "Dati relativi all'ultimo giorno"
                           getGeofencesForPeriod(period)
                        }
                        "week" -> {
                            periodMessage.text = "Dati relativi all'ultima settimana"
                            getGeofencesForPeriod(period)
                        }
                        "month" -> {
                            periodMessage.text = "Dati relativi all'ultimo mese"
                            getGeofencesForPeriod(period)
                        }
                        "year" -> {
                            periodMessage.text ="Dati relativi all'ultimo anno"
                            getGeofencesForPeriod(period)
                        }
                        else -> emptyList()

                    }
                    if (geofences.isEmpty()) {
                        geofencePieChart.clear()
                        Snackbar.make(binding.root, "Nessun dato disponibile", Snackbar.LENGTH_SHORT).show()
                    } else {
                        displayGeofencePieChart(geofences)
                    }
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getActivitiesForPeriod(period: String): List<Attività> {
        val now = LocalDateTime.now()
        val startDate = when (period) {
            "day" -> now.minusDays(1)
            "week" -> now.minusWeeks(1)
            "month" -> now.minusMonths(1)
            "year" -> now.minusYears(1)
            else -> now
        }
        return withContext(Dispatchers.IO) {
            attivitàDao.getAttivitàByDateRange(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getGeofencesForPeriod(period: String) : List<timeGeofence>{
        val now = LocalDateTime.now()
        val startDate = when (period) {
            "day" -> now.minusDays(1)
            "week" -> now.minusWeeks(1)
            "month" -> now.minusMonths(1)
            "year" -> now.minusYears(1)
            else -> now
        }
        println("For period: $period, startDate: $startDate")
        return withContext(Dispatchers.IO){
            attivitàDao.getGeofencesByDateRange(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayPieChart(activities: List<Attività>) {
        val activityDurations = mutableMapOf<String, Long>()
        for (activity in activities) {
            val duration = calculateDuration(activity.startTime, activity.endTime)
            activityDurations[activity.activityType] = activityDurations.getOrDefault(activity.activityType, 0L) + duration
        }

        val entries = activityDurations.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Activities")
        dataSet.colors = listOf(
            ColorTemplate.rgb("#1EE3CF"), // Passeggiata
            ColorTemplate.rgb("#F66942"), // Corsa
            ColorTemplate.rgb("#96D74C"), // Stare fermo
            ColorTemplate.rgb("#FFD500"), // Guidare
            ColorTemplate.rgb("#FFD6FF")
        )
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        pieChart.data = data

        // Imposta il tipo di carattere delle etichette a grassetto
        val tf = Typeface.DEFAULT_BOLD
        pieChart.setEntryLabelTypeface(tf)
        pieChart.data.setValueTypeface(tf)

        // Imposta il colore delle etichette a nero
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.data.setValueTextColor(Color.BLACK)

        pieChart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayGeofencePieChart(geofences: List<timeGeofence>) {
        val activityDurations = mutableMapOf<String, Long>()
        val geofenceColors = mutableMapOf<String, Int>()

        for (geofence in geofences) {
            val duration = calculateDuration(geofence.enterTime, geofence.exitTime)
            activityDurations[geofence.placeName] = activityDurations.getOrDefault(geofence.placeName, 0L) + duration
            if (!geofenceColors.containsKey(geofence.placeName)) {
                geofenceColors[geofence.placeName] = generateRandomColor()
            }
        }

        val entries = activityDurations.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Geofence Activities")
        dataSet.colors = geofenceColors.values.toList()
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        geofencePieChart.data = data


        val tf = Typeface.DEFAULT_BOLD
        geofencePieChart.setEntryLabelTypeface(tf)
        geofencePieChart.data.setValueTypeface(tf)


        geofencePieChart.setEntryLabelColor(Color.BLACK)
        geofencePieChart.data.setValueTextColor(Color.BLACK)

        geofencePieChart.invalidate()
    }

    private fun generateRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDuration(startTime: LocalDateTime, endTime: LocalDateTime): Long {
        return Duration.between(startTime, endTime).toMinutes()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDuration(startTimeMillis: Long?, endTimeMillis: Long?): Long {
        if (startTimeMillis == null || endTimeMillis == null) return 0L
        val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault())
        val endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault())
        return calculateDuration(startTime, endTime)
    }

    suspend fun getGeofencesForDate(year: Int, month: Int, day: Int): List<timeGeofence> {
        val date = String.format("%04d-%02d-%02d", year, month, day)
        return attivitàDao.getGeofencesForDate(date)
    }




}
