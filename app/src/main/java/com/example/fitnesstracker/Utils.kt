
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.fitnesstracker.HomeActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.RegistrationActivity
import com.google.firebase.database.DataSnapshot
import androidx.core.content.res.ResourcesCompat
import com.example.fitnesstracker.LoggedUser
import com.example.fitnesstracker.Person
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException

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

    @SuppressLint("InflateParams")
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
            // Show the OK button and create a Dialog
            val dialog = Dialog(context)
            okButton.visibility = View.VISIBLE

            okButton.setOnClickListener {
                dialog.dismiss()
            }

            // Imposta il layout personalizzato nel dialogo
            dialog.setContentView(layout)

            // Mostra il dialogo
            dialog.show()
        } else {
            // Hide the OK button and show a Toast
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

    fun isJSONComplete(jsonString: String): Boolean {
        // Implementa la logica per verificare se hai ricevuto l'intero JSON
        // Ad esempio, potresti verificare se il JSON inizia con "{" e termina con "}"
        return jsonString.startsWith("{") && jsonString.endsWith("}")
    }



    suspend fun getUser(username: String, context: Context): Person? {
        val database = FirebaseDatabase.getInstance("https://fitnesstracker-637f9-default-rtdb.europe-west1.firebasedatabase.app").reference
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = database.child("users").orderByChild("username").equalTo(username).get().await()
                println("Snapshot: ${snapshot.value}")

                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.firstOrNull()
                    if (userSnapshot != null) {
                        val email = userSnapshot.child("email").getValue<String>()
                        val gender = userSnapshot.child("gender").getValue<String>()
                        val id = userSnapshot.child("id").getValue<String>()
                        val macAddress = userSnapshot.child("macAddress").getValue<String>()
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










}