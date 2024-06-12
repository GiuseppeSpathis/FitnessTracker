import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
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
import com.google.firebase.database.FirebaseDatabase
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
    fun receiveMessage (context: Context, name: String, message: String, gender: String){

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = layoutInflater.inflate(R.layout.custom_toast_layout, null, false)


        val image = layout.findViewById<ImageView>(R.id.toastImage)

        checkGender(gender, image, context)

        val title = layout.findViewById<TextView>(R.id.toastTitle)
        title.text = name

        val text = layout.findViewById<TextView>(R.id.toastText)
        text.text = message


        layout.background = ContextCompat.getDrawable(context, R.drawable.custom_toast_borders)


        val toast = Toast(context)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()

    }
    fun socketError(e: IOException, activity: Activity){
        activity.runOnUiThread {
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


     suspend fun findUserByMacAddress(macAddress: String): RegistrationActivity.User? {
        val database = FirebaseDatabase.getInstance("https://fitnesstracker-637f9-default-rtdb.europe-west1.firebasedatabase.app").reference
        return try {
            val query = database.child("users").orderByChild("macAddress").equalTo(macAddress).get().await()
            val userMap = query.value as? Map<String, Any>
            val user = userMap?.values?.firstOrNull() as? Map<String, Any>
            // Assuming your User class has appropriate properties (e.g., id, name, email)
            val userUsername = user?.get("username") as? String
            val userEmail = user?.get("email") as? String
            val userMac = user?.get("macAddress") as? String
            val userGender = user?.get("gender") as? String
            val userId = user?.get("id") as String

            RegistrationActivity.User(
                userId,
                userEmail,
                userUsername,
                userGender,
                userMac
            )
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Errore durante la ricerca dell'utente tramite macAddress", e)
            null
        }
    }






}