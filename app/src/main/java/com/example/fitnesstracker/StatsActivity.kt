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
import android.widget.AdapterView
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.format.DayFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Random



/*

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    lateinit var day: CalendarDay

    init {
        view.setOnClickListener {
            if (day.owner == DayOwner.THIS_MONTH && !day.isDisabled) {
                // Fai qualcosa quando viene cliccato un giorno abilitato
            }
        }
    }
}
 */
class StatsActivity : AppCompatActivity() {


    private lateinit var db: AppDatabase
    private lateinit var binding: ActivityStatsBinding
    private lateinit var pieChart: PieChart
    private lateinit var periodMessage: TextView
    private lateinit var geofencePieChart: PieChart
    private lateinit var periodMessageActivities: TextView
    private lateinit var attivitàDao: ActivityDao
    private val socialModel = SocialModel()

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
        calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                val year = date.year
                val month = date.month + 1 // month è zero-based, quindi aggiungiamo 1
                val dayOfMonth = date.day

                showDateDialog(this, year, month, dayOfMonth, db)
            }
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

        spinnerActivity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Azioni da intraprendere quando viene selezionato un elemento
                val selectedItem = parent.getItemAtPosition(position).toString()
                // Nella tua Coroutine o funzione di setup
                if (selectedItem != "niente") {
                    CoroutineScope(Dispatchers.IO).launch {
                        val giorni = attivitàDao.getDatesByActivityType(selectedItem)
                        println("stampo i giorni : $giorni")

                        // Applicazione del decorator al calendario
                        withContext(Dispatchers.Main) {
                            calendarView.addDecorator(object : DayViewDecorator {
                                override fun shouldDecorate(day: CalendarDay): Boolean {
                                    val formattedDay = "${day.year}-${(day.month + 1).toString().padStart(2, '0')}-${day.day.toString().padStart(2, '0')}"
                                    return !giorni.contains(formattedDay)
                                }

                                override fun decorate(view: DayViewFacade) {
                                    view.setDaysDisabled(true)
                                    println("questo tasto e' disabilitato")
                                }
                            })
                        }
                    }
                }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //niente
            }
        }


        val activityTypes = arrayOf("Passeggiata", "Corsa", "Guidare", "Stare fermo")
        val activityTypeSpinner = binding.activityTypeSpinner
        val adapterLineChart = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityTypes)
        adapterLineChart.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activityTypeSpinner.adapter = adapterLineChart

        activityTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedActivity = parent.getItemAtPosition(position).toString()
                updateLineChart(selectedActivity)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

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

        binding.btnMonth.performClick()
        binding.btnGeofenceMonth.performClick()
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
                val date = "2024-06-29"
                val placeName = "Fake Place $i"

                val geofence = timeGeofence(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    enterTime = enterTime,
                    exitTime = exitTime,
                    date = date,
                    placeName = placeName,
                    userId = LoggedUser.id
                )
                db.attivitàDao().insertTimeGeofence(geofence)
                println("geofence inserita: $geofence")
            }
        }
    }

    companion object {
        private val socialModel = SocialModel()
        @RequiresApi(Build.VERSION_CODES.O)
        fun showActivityPopup(activityType: String, activity: Attività, context: Context) {

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.popup_activity_details, null)

            val startTimeTextView = view.findViewById<TextView>(R.id.start_time)
            startTimeTextView.text = context.getString(R.string.start_time, activity.startTime.toLocalTime())

            val endTimeTextView = view.findViewById<TextView>(R.id.end_time)
            endTimeTextView.text = context.getString(R.string.end_time, activity.endTime.toLocalTime())

            val stepCountLayout = view.findViewById<LinearLayout>(R.id.step_count_layout)
            val stepCountTextView = view.findViewById<TextView>(R.id.step_count)
            if (activity.stepCount != null) {
                stepCountTextView.text = context.getString(R.string.step_count_format, activity.stepCount)
            } else {
                stepCountLayout.visibility = View.GONE
            }

            val distanceLayout = view.findViewById<LinearLayout>(R.id.distance_layout)
            val distanceTextView = view.findViewById<TextView>(R.id.distance)
            if (activity.distance != null) {
                distanceTextView.text = context.getString(R.string.distanza, activity.distance)
            } else {
                distanceLayout.visibility = View.GONE
            }

            val paceLayout = view.findViewById<LinearLayout>(R.id.pace_layout)
            val paceTextView = view.findViewById<TextView>(R.id.pace)
            if (activity.pace != null) {
                paceTextView.text = context.getString(R.string.passo, activity.pace)
            } else {
                paceLayout.visibility = View.GONE
            }

            val avgSpeedLayout = view.findViewById<LinearLayout>(R.id.avg_speed_layout)
            val avgSpeedTextView = view.findViewById<TextView>(R.id.avg_speed)
            if (activity.avgSpeed != null) {
                avgSpeedTextView.text = context.getString(R.string.avg_speed, activity.avgSpeed)
            } else {
                avgSpeedLayout.visibility = View.GONE
            }

            val maxSpeedLayout = view.findViewById<LinearLayout>(R.id.max_speed_layout)
            val maxSpeedTextView = view.findViewById<TextView>(R.id.max_speed)
            if (activity.maxSpeed != null) {
                maxSpeedTextView.text = context.getString(R.string.max_speed, activity.maxSpeed)
            } else {
                maxSpeedLayout.visibility = View.GONE
            }

            val dateTextView = view.findViewById<TextView>(R.id.date)
            dateTextView.text = context.getString(R.string.date, activity.date)

            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.activity_details, activityType))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
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
            val filteredActivities = activities.filter { it.userId == LoggedUser.id }
            val activityColors = mapOf(
                "Passeggiata" to ContextCompat.getColor(context, R.color.passeggiata),
                "Corsa" to ContextCompat.getColor(context, R.color.corsa),
                "Stare fermo" to ContextCompat.getColor(context, R.color.stare_fermo),
                "Guidare" to ContextCompat.getColor(context, R.color.guidare),
                "Not tracked" to ContextCompat.getColor(context, R.color.not_tracked)
            )

            val totalMinutesPerHour = Array(24) { mutableListOf<Pair<String, Int>>() }

            for (activity in filteredActivities) {
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

                            val clickedActivities = filteredActivities.filter {
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
        fun showDateDialog(context: Context, year: Int, month: Int, day: Int,  db: AppDatabase, isDialog: Boolean = false) {
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
                1200 // Altezza in pixel
            )

            val closeButton: ImageButton = dialogView.findViewById(R.id.close_button)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }
            val activityChartContainer = dialogView.findViewById<LinearLayout>(R.id.chartContainer)

            val activity = context as? StatsActivity
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activities: List<Attività> = if (isDialog) {
                            val othersActivities = socialModel.getOtherActivitiesForDate(year, month, day, db)
                            convertToActivities(othersActivities)
                        } else {
                            socialModel.getActivitiesForDate(year, month, day, db)
                        }
                        println("stampo activity")
                        println(activities)
                        if (!isDialog) {
                            val geofenceChartContainer = dialogView.findViewById<LinearLayout>(R.id.geofenceCchartContainer)
                            val geofences = socialModel.getGeofencesForDate(db, year, month, day)
                            withContext(Dispatchers.Main) {
                                activity!!.displayGeofencesForDate(geofenceChartContainer, geofences)
                            }
                        }
                        withContext(Dispatchers.Main){
                            Log.d("StatsActivity", "Number of activities retrieved: ${activities.size}")
                            displayActivitiesForDate(context, activityChartContainer, activities)
                        }
                } catch (e: Exception) {
                    Snackbar.make(activity!!.binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayGeofencesForDate(container: LinearLayout, geofences: List<timeGeofence>) {
        val geofenceColors = mutableMapOf<String, Int>()
        val random = Random()
        val geofencesFiltered = geofences.filter { it.userId == LoggedUser.id }

        geofencesFiltered.forEach { geofence ->
            if (!geofenceColors.containsKey(geofence.placeName)) {
                val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                geofenceColors[geofence.placeName] = color
            }
        }

        val totalMinutesPerHour = Array(24) { mutableListOf<Pair<timeGeofence, Int>>() }

        for (geofence in geofencesFiltered) {
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
                    currentHourList.add(Pair(geofence, duration))
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
                for ((geofence, duration) in currentHourList) {
                    durations.add(duration.toFloat())
                    colors.add(geofenceColors[geofence.placeName] ?: Color.GRAY)
                    totalDuration += duration

                    durations.add(1f) // Separation line
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

            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is BarEntry) {
                        val index = h?.stackIndex ?: return
                        val selectedGeofence = currentHourList.getOrNull(index / 2)?.first ?: return

                        showGeofenceInfoDialog(selectedGeofence)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showGeofenceInfoDialog(geofence: timeGeofence) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_geofences_info, null)
        val placeNameTextView = dialogView.findViewById<TextView>(R.id.textViewPlaceName)
        val dateTextView = dialogView.findViewById<TextView>(R.id.date)
        val startTimeTextView = dialogView.findViewById<TextView>(R.id.start_time)
        val endTimeTextView = dialogView.findViewById<TextView>(R.id.end_time)

        placeNameTextView.text = geofence.placeName
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val enterTime = Instant.ofEpochMilli(geofence.enterTime ?: 0).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val exitTime = Instant.ofEpochMilli(geofence.exitTime ?: 0).atZone(ZoneId.systemDefault()).toLocalDateTime()

        dateTextView.text = enterTime.toLocalDate().format(dateFormatter)
        startTimeTextView.text = getString(R.string.start_time, enterTime.toLocalTime().format(timeFormatter))
        endTimeTextView.text = getString(R.string.end_time, exitTime.toLocalTime().format(timeFormatter))

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .show()
    }



    private fun getActivityTypeByColor(colors: List<Int>, activityColors: Map<String, Int>, stackIndex: Int?): String {
        stackIndex?.let {
            val colorIndex = it % colors.size
            return activityColors.entries.firstOrNull { it.value == colors[colorIndex] }?.key ?: "Not tracked"
        }
        return "Not tracked"
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
                            periodMessageActivities.text = getString(R.string.dati_ultimo_giorno)
                            socialModel.getActivitiesForPeriod(db, period)
                        }
                        "week" -> {
                            periodMessageActivities.text = getString(R.string.dati_ultima_settimana)
                            socialModel.getActivitiesForPeriod(db, period)
                        }
                        "month" -> {
                            periodMessageActivities.text = getString(R.string.dati_ultimo_mese)
                            socialModel.getActivitiesForPeriod(db, period)
                        }
                        "year" -> {
                            periodMessageActivities.text = getString(R.string.dati_ultimo_anno)
                            socialModel.getActivitiesForPeriod(db, period)
                        }
                        else -> emptyList()
                    }

                    if (activities.isEmpty()) {
                        pieChart.clear()
                        Snackbar.make(binding.root, R.string.no_data, Snackbar.LENGTH_SHORT).show()
                    } else {
                        displayPieChart(activities)
                    }
                } else {
                    val geofences : List<timeGeofence> = when (period) {
                        "day" -> {
                            periodMessage.text = getString(R.string.dati_ultimo_giorno)
                            socialModel.getGeofencesForPeriod(db, period)
                        }
                        "week" -> {
                            periodMessage.text = getString(R.string.dati_ultima_settimana)
                            socialModel.getGeofencesForPeriod(db, period)
                        }
                        "month" -> {
                            periodMessage.text = getString(R.string.dati_ultimo_mese)
                            socialModel.getGeofencesForPeriod(db, period)
                        }
                        "year" -> {
                            periodMessage.text =getString(R.string.dati_ultimo_anno)
                            socialModel.getGeofencesForPeriod(db, period)
                        }
                        else -> emptyList()

                    }
                    if (geofences.isEmpty()) {
                        geofencePieChart.clear()
                        Snackbar.make(binding.root, R.string.no_data, Snackbar.LENGTH_SHORT).show()
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
    private fun displayPieChart(activities: List<Attività>) {

        val activityDurations = mutableMapOf<String, Long>()
        val filtered_activities = activities.filter { it.userId == LoggedUser.id }
        for (activity in filtered_activities) {
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
        val tf = Typeface.DEFAULT_BOLD
        pieChart.setEntryLabelTypeface(tf)
        pieChart.data.setValueTypeface(tf)

        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.data.setValueTextColor(Color.BLACK)

        pieChart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayGeofencePieChart(geofences: List<timeGeofence>) {
        val activityDurations = mutableMapOf<String, Long>()
        val geofenceColors = mutableMapOf<String, Int>()
        val geofences_filter = geofences.filter { it.userId == LoggedUser.id }
        for (geofence in geofences_filter) {
            val duration = calculateDuration(geofence.enterTime, geofence.exitTime)
            activityDurations[geofence.placeName] = activityDurations.getOrDefault(geofence.placeName, 0L) + duration
            if (!geofenceColors.containsKey(geofence.placeName)) {
                geofenceColors[geofence.placeName] = generateRandomColor()
            }
        }

        println("geofence colors: $geofenceColors")
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



    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateLineChart(activityType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = socialModel.getActivitiesForPeriod(db, "week")

            val filteredData = data.filter { it.activityType == activityType && it.userId == LoggedUser.id }

            val chartData = when (activityType) {
                "Passeggiata" -> prepareStepCountData(filteredData)
                "Corsa" -> prepareDistanceData(filteredData)
                "Guidare" -> prepareAvgSpeedData(filteredData)
                "Stare fermo" -> prepareStationaryTimeData(filteredData)
                else -> emptyList()
            }

            withContext(Dispatchers.Main) {
                val entries = chartData.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.second.toFloat())
                }
                val lineDataSet = LineDataSet(entries, activityType)
                val lineData = LineData(lineDataSet)

                val xAxis = binding.lineChart.xAxis
                xAxis.valueFormatter = IndexAxisValueFormatter(chartData.map { it.first.substring(0, 5) }) // Mostra solo giorno e mese
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawLabels(true)

                val yAxis = binding.lineChart.axisLeft
                yAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
                yAxis.axisMinimum = 0f
                yAxis.granularity = 1f

                binding.lineChart.axisRight.isEnabled = false

                binding.lineChart.data = lineData
                binding.lineChart.getAxisLeft().setDrawGridLines(false);
                binding.lineChart.getXAxis().setDrawGridLines(false);
                binding.lineChart.invalidate()

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareStepCountData(data: List<Attività>): List<Pair<String, Int>> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dailySteps = mutableMapOf<String, Int>()


        data.forEach { attività ->
            val date = LocalDate.parse(attività.date, formatter).format(formatter)
            dailySteps[date] = dailySteps.getOrDefault(date, 0) + (attività.stepCount ?: 0)
        }


        val today = LocalDate.now()
        val lastWeekDates = (0..6).map { today.minusDays(it.toLong()).format(formatter) }


        lastWeekDates.forEach { date ->
            dailySteps.putIfAbsent(date, 0)
        }

        println("dailySteps: $dailySteps")

        return dailySteps.toList().sortedBy { (key, _) -> LocalDate.parse(key, formatter) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareDistanceData(data: List<Attività>): List<Pair<String, Float>> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dailyDistances = mutableMapOf<String, Float>()
        data.forEach { attività ->
            val date = LocalDate.parse(attività.date, formatter).format(formatter)
            dailyDistances[date] = dailyDistances.getOrDefault(date, 0f) + (attività.distance ?: 0f)
        }

        val today = LocalDate.now()
        val lastWeekDates = (0..6).map { today.minusDays(it.toLong()).format(formatter) }

        lastWeekDates.forEach { date ->
            dailyDistances.putIfAbsent(date, 0f)
        }

        return dailyDistances.toList().sortedBy { (key, _) -> LocalDate.parse(key, formatter) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareAvgSpeedData(data: List<Attività>): List<Pair<String, Double>> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dailySpeeds = mutableMapOf<String, Double>()
        val speedCount = mutableMapOf<String, Int>()

        data.forEach { attività ->
            val date = LocalDate.parse(attività.date, formatter).format(formatter)
            dailySpeeds[date] = dailySpeeds.getOrDefault(date, 0.0) + (attività.avgSpeed ?: 0.0)
            speedCount[date] = speedCount.getOrDefault(date, 0) + 1
        }

        val today = LocalDate.now()
        val lastWeekDates = (0..6).map { today.minusDays(it.toLong()).format(formatter) }

        lastWeekDates.forEach { date ->
            if (!dailySpeeds.containsKey(date)) {
                dailySpeeds[date] = 0.0
                speedCount[date] = 1
            }
        }

        return dailySpeeds.map { (date, speed) ->
            date to if (speedCount[date]!! > 0) speed / speedCount[date]!! else 0.0
        }.sortedBy { (key, _) -> LocalDate.parse(key, formatter) }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareStationaryTimeData(data: List<Attività>): List<Pair<String, Long>> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dailyStationaryTime = mutableMapOf<String, Long>()

        data.forEach { attività ->
            val date = LocalDate.parse(attività.date, formatter).format(formatter)
            val duration = Duration.between(attività.startTime, attività.endTime).toMinutes()
            dailyStationaryTime[date] = dailyStationaryTime.getOrDefault(date, 0L) + duration
        }

        val today = LocalDate.now()
        val lastWeekDates = (0..6).map { today.minusDays(it.toLong()).format(formatter) }

        lastWeekDates.forEach { date ->
            dailyStationaryTime.putIfAbsent(date, 0L)
        }

        return dailyStationaryTime.toList().sortedBy { (key, _) -> LocalDate.parse(key, formatter) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertFakeWalkActivities() {
        val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()
        val userId = LoggedUser.id

        val random = Random()


        for (i in 0..6) {
            val date = today.minusDays(i.toLong())
            val formattedDate = date.format(dateFormat)
            val startTime = date.atStartOfDay()
            val endTime = startTime.plusHours(random.nextInt( 5).toLong())

            val stepCount = random.nextInt(10000)
            val stepLengthInMeters = 0.8f
            val distanceInKm = stepCount * stepLengthInMeters / 1000

            val attività = Attività(
                userId = LoggedUser.id,
                startTime = startTime,
                endTime = endTime,
                stepCount = stepCount,
                distance = distanceInKm,
                date = formattedDate,
                pace = null,
                activityType = "Passeggiata",
                avgSpeed = null,
                maxSpeed = null
            )

            // Inserisci l'attività nel database
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    attivitàDao.insertActivity(attività)
                    Log.d("FakeData", "Attività fittizia salvata: $attività")
                }
                withContext(Dispatchers.Main) {
                }
            }
        }
    }











}
