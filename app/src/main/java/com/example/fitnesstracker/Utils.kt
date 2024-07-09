
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.HomeActivity
import com.example.fitnesstracker.R
import androidx.core.content.res.ResourcesCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitnesstracker.AppDatabase
import com.example.fitnesstracker.Attività
import com.example.fitnesstracker.GeoFenceActivity
import com.example.fitnesstracker.NotificationWorker
import com.example.fitnesstracker.OthersActivity
import com.example.fitnesstracker.Person
import com.example.fitnesstracker.ReminderWorker
import com.example.fitnesstracker.Social
import com.example.fitnesstracker.StatsActivity
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Utils {

    // Funzione ausiliaria che serve ad aggiustare l'icona in base al genere
    fun checkGender(gender: String, iconPerson: ImageView, context: Context) {

        val genderTypes = context.resources.getStringArray(R.array.gender_array)

        when (gender) {
            genderTypes[0] -> {
                iconPerson.setImageResource(R.drawable.male)
            }
            genderTypes[1] -> {
                iconPerson.setImageResource(R.drawable.female)
            }
            else -> {
                iconPerson.setImageResource(R.drawable.other)
            }
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    fun receiveMessage(context: Context, name: String, message: String, gender: String, fileShared: Boolean = false) {

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = layoutInflater.inflate(R.layout.custom_toast_layout, null, false)


        val image = layout.findViewById<ImageView>(R.id.toastImage)
        checkGender(gender, image, context)

        val title = layout.findViewById<TextView>(R.id.toastTitle)
        title.text = name

        val text = layout.findViewById<TextView>(R.id.toastText)
        text.text = message

        val okButton = layout.findViewById<Button>(R.id.okButton)
        if (fileShared) {

            // mostra L'OK button e crea ill dialog
            val dialog = Dialog(context)
            okButton.visibility = View.VISIBLE
            okButton.setOnClickListener {
                dialog.setContentView(R.layout.dialog_stats)
                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val calendarView = dialog.findViewById<MaterialCalendarView>(R.id.calendarView)
                val textView = dialog.findViewById<TextView>(R.id.title)
                textView.text = "Attività di $name"



                calendarView?.setOnDateChangedListener { _, date, selected ->
                    if (selected) {
                        dialog.dismiss()
                        val db = AppDatabase.getDatabase(context)
                        val year = date.year
                        val month = date.month
                        val dayOfMonth = date.day

                        StatsActivity.showDateDialog(context, year, month, dayOfMonth, db, true)
                    }
                }

            }

            // Imposta il layout personalizzato nel dialogo
            dialog.setContentView(layout)
            // Mostra il dialogo
            dialog.show()
        } else {
            // nascondi l'OK button e mostra un toast
            okButton.visibility = View.GONE
            layout.background = ContextCompat.getDrawable(context, R.drawable.custom_toast_borders)
            val toast = Toast(context)
            toast.duration = Toast.LENGTH_LONG
            toast.view = layout
            toast.show()
        }
    }



    fun socketError(e: IOException, activity: Activity){
        activity.runOnUiThread {
            println("sono in socket error")
            e.message?.let {
                MotionToast.createColorToast(
                    activity,
                    activity.resources.getString(R.string.error),
                    it,
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(activity, www.sanju.motiontoast.R.font.helvetica_regular))
            }
        }
    }

    fun hasPermission(permission: String, context: Context) : Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }



    suspend fun getUser(username: String): Person? {
        val database = FirebaseDatabase.getInstance("https://fitnesstracker-637f9-default-rtdb.europe-west1.firebasedatabase.app").reference
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = database.child("users").orderByChild("username").equalTo(username).get().await()
                println("Snapshot: ${snapshot.value}")

                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.firstOrNull()
                    if (userSnapshot != null) {
                        val gender = userSnapshot.child("gender").getValue<String>()
                        val name = userSnapshot.child("username").getValue<String>()  // Qui mappiamo username a name

                        if (name != null && gender != null) {
                            return@withContext Person(name, gender, null)
                        }
                    }
                    null
                } else {
                    println("User not found")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching user: ${e.message}")
                null
            }
        }
    }




     fun convertToActivities(othersActivities: List<OthersActivity>): List<Attività> {
        return othersActivities.map {
            Attività(
                id = it.id,
                userId = it.username,
                startTime = it.startTime,
                endTime = it.endTime,
                date = it.date,
                activityType = it.activityType,
                stepCount = it.stepCount,
                distance = it.distance,
                pace = it.pace,
                avgSpeed = it.avgSpeed,
                maxSpeed = it.maxSpeed
            )
        }
    }


    fun navigateTo(context: Context, activityClass: Class<out AppCompatActivity>): Boolean {
        val intent = Intent(context, activityClass as Class<*>)
        val options = ActivityOptions.makeCustomAnimation(context, 0, 0)
        context.startActivity(intent, options.toBundle())
        return true
    }


    fun setupBottomNavigationView(context: Context, nameActivity: String, bottomNavigationView: NavigationBarView) {
        // Ottieni l'array delle stringhe dal file di risorse
        val navNames = context.resources.getStringArray(R.array.navNames)

        // Mappa per associare i nomi delle attività agli ID delle voci di navigazione
        val navIds = mapOf(
            "nav_stats" to R.id.nav_stats,
            "nav_home" to R.id.nav_home,
            "nav_users" to R.id.nav_users,
            "geofence" to R.id.geofence
        )

        // Imposta il selectedItemId basato sul nameActivity
        when (nameActivity) {
            navNames[0] -> bottomNavigationView.selectedItemId = navIds["nav_stats"] ?: R.id.nav_home
            navNames[1] -> bottomNavigationView.selectedItemId = navIds["nav_home"] ?: R.id.nav_home
            navNames[2] -> bottomNavigationView.selectedItemId = navIds["nav_users"] ?: R.id.nav_home
            navNames[3] -> bottomNavigationView.selectedItemId = navIds["geofence"] ?: R.id.nav_home
            else -> {
                bottomNavigationView.selectedItemId = R.id.nav_home
            }
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_stats -> navigateTo(context, StatsActivity::class.java)
                R.id.nav_home -> navigateTo(context,HomeActivity::class.java)
                R.id.nav_users -> navigateTo(context, Social::class.java)
                R.id.geofence -> navigateTo(context, GeoFenceActivity::class.java)
                else -> false
            }
        }
    }

    //creo un worker che si attiva ogni 24 ore
    fun scheduleDailyNotification(context: Context) {
        val currentDate = Calendar.getInstance()

        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 22)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        // Uso il PeriodicWorkRequest
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("daily_notification")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_notification",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    //creo un worker che si attiva ogni 4 ore
    fun scheduleReminder(context: Context) {
        val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
            .addTag("reminder_notification")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reminder_notification",
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderWorkRequest
        )
    }



}