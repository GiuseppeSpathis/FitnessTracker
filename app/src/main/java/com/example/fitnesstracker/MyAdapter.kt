import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.Person
import com.example.fitnesstracker.PersonProfile
import com.example.fitnesstracker.R
import Utils.checkGender
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class MyAdapter(private var personList: List<Person>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var iconPerson: ImageView = view.findViewById(R.id.iconPerson)
        var name: TextView = view.findViewById(R.id.name)
        var message: ImageButton = view.findViewById(R.id.message)
        var share: ImageButton = view.findViewById(R.id.share)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val person = personList[position]
        holder.name.text = person.name
        checkGender(person.gender, holder.iconPerson)
        holder.name.setOnClickListener {
            val intent = Intent(it.context, PersonProfile::class.java)
            intent.putExtra("name", person.name)
            intent.putExtra("gender", person.gender)
            it.context.startActivity(intent)
        }
        holder.message.setOnClickListener{
            showDialog(it.context, holder.name.text.toString())
        }
        holder.share.setOnClickListener{
            Toast.makeText(it.context, "vuoi condividere", Toast.LENGTH_SHORT).show() //da modificare in futuro
        }
    }

    override fun getItemCount(): Int {
        return personList.size
    }

    fun updateList(newList: List<Person>) {
        personList = newList
        notifyDataSetChanged()
    }

    private fun showDialog(context: Context, name: String) {
        val builder = AlertDialog.Builder(context, R.style.DialogTheme)
        builder.setTitle("Invia un toast a $name")

        val input = EditText(context)
        val layout = LinearLayout(context)
        layout.setPadding(40, 40, 40, 40)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        input.layoutParams = params
        layout.addView(input)
        builder.setView(layout)

        // Creo un OnClickListener vuoto perche' il dialog di base si chiude appena clicchi uno dei due pulsanti
        //indipendentemente dal onClickListener del pulsante che in questo caso nel pulsante invia devi fare il check se
        //l'input text sia vuoto oppure no
        val dummyListener = DialogInterface.OnClickListener { _, _ -> }

        builder.setPositiveButton("Invia", dummyListener)

        builder.setNegativeButton("Annulla") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = false // all'inizio il pulsante e' disabilitato

            // Imposta il vero OnClickListener sul pulsante positivo
            positiveButton.setOnClickListener {
                val toastMessage = input.text.toString() //in futuro questo toastMessage devi inviare un toast a quell'utente

                dialog.dismiss()

                if(context is Activity){
                    MotionToast.createColorToast(context,
                        "Successo",
                        "il toast é stato inviato con successo",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(context, www.sanju.motiontoast.R.font.helvetica_regular))
                }
            }

            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    positiveButton.isEnabled = s.isNotEmpty() // il pulsante diventa abilitato se il testo non e' vuoto
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })
        }

        dialog.show()
    }


}
