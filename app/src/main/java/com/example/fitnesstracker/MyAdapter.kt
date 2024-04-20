import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.Person
import com.example.fitnesstracker.R

class MyAdapter(private val personList: List<Person>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

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
        if (person.gender == "male") {
            holder.iconPerson.setImageResource(R.drawable.male)
        } else {
            holder.iconPerson.setImageResource(R.drawable.female)
        }
        holder.message.setOnClickListener{
            Toast.makeText(it.context, "vuoi messaggiare", Toast.LENGTH_SHORT).show() //da modificare in futuro

        }
        holder.share.setOnClickListener{
            Toast.makeText(it.context, "vuoi condividere", Toast.LENGTH_SHORT).show() //da modificare in futuro
        }
    }

    override fun getItemCount(): Int {
        return personList.size
    }
}
