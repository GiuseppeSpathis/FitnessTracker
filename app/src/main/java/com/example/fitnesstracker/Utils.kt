import android.widget.ImageView
import com.example.fitnesstracker.R

object Utils {

    // Funzione ausiliaria che serve ad aggiustare l'icona in base al genere
    fun checkGender(gender: String, iconPerson: ImageView) {
        when (gender) {
            "male" -> {
                iconPerson.setImageResource(R.drawable.male)
            }
            "female" -> {
                iconPerson.setImageResource(R.drawable.female)
            }
            else -> {
                iconPerson.setImageResource(R.drawable.other)
            }
        }
    }


}