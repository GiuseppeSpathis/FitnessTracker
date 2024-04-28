import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.fitnesstracker.R

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
    fun sendMessage (context: Context, name: String, message: String, gender: String){

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

    fun hasPermission(permission: String, context: Context) : Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }


}