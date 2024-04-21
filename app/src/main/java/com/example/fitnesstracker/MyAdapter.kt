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
            Toast.makeText(it.context, "vuoi messaggiare", Toast.LENGTH_SHORT).show() //da modificare in futuro

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
}
