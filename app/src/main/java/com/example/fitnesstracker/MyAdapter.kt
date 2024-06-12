import Utils.checkGender
import Utils.receiveMessage
import Utils.socketError
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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MyAdapter(private var personList: List<Person>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    private var you_are_connected = false
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var iconPerson: ImageView = view.findViewById(R.id.iconPerson)
        var name: TextView = view.findViewById(R.id.name)
        var message: ImageButton = view.findViewById(R.id.message)
        var share: ImageButton = view.findViewById(R.id.share)
        var connect: ImageButton = view.findViewById(R.id.connect)
        var disconnect: ImageButton = view.findViewById(R.id.disconnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val person = personList[position]
        holder.name.text = person.name
        checkGender(person.gender, holder.iconPerson, holder.itemView.context)
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
        holder.connect.setOnClickListener{
            if(you_are_connected){
                MotionToast.createColorToast(
                    it.context as Activity,
                    it.context.resources.getString(R.string.connect_failed),
                    it.context.resources.getString(R.string.still_connected),
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(it.context, www.sanju.motiontoast.R.font.helvetica_regular))


            }
            else {
                connect2device(person.device, it.context as Activity, holder, person)
            }
        }
        holder.disconnect.setOnClickListener {
            disconnectDevice(it.context as Activity, holder)
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
        builder.setTitle("Invia un messaggio a $name")

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
                sendMessage(toastMessage, context as Activity)


                dialog.dismiss()
                    MotionToast.createColorToast(context,
                        context.resources.getString(R.string.successo),
                        context.resources.getString(R.string.messaggio_inviato),
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(context, www.sanju.motiontoast.R.font.helvetica_regular))
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


    private fun disconnectDevice(activity: Activity, holder: MyViewHolder){
        Thread {
            try {
                socket.close()
                outputStream.close()
                inputStream.close()

                activity.runOnUiThread {

                    MotionToast.createColorToast(
                        activity,
                        activity.resources.getString(R.string.successo),
                        activity.resources.getString(R.string.disconnected),
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(activity, www.sanju.motiontoast.R.font.helvetica_regular))

                    holder.connect.visibility = View.VISIBLE
                    holder.message.visibility = View.GONE
                    holder.share.visibility = View.GONE
                    holder.disconnect.visibility = View.GONE

                    you_are_connected = false

                }
            }
            catch (e: IOException) {

                activity.runOnUiThread {
                    socketError(e, activity)
                }

            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun connect2device(device: BluetoothDevice?, activity: Activity, holder: MyViewHolder, person: Person){
        Thread {
            try {
                if (device != null) {
                    socket = device.createRfcommSocketToServiceRecord(uuid)

                    socket.connect()

                    activity.runOnUiThread {

                        MotionToast.createColorToast(
                            activity,
                            activity.resources.getString(R.string.successo),
                            activity.resources.getString(R.string.connected),
                            MotionToastStyle.SUCCESS,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.LONG_DURATION,
                            ResourcesCompat.getFont(activity, www.sanju.motiontoast.R.font.helvetica_regular))

                        holder.connect.visibility = View.GONE
                        holder.message.visibility = View.VISIBLE
                        holder.share.visibility = View.VISIBLE
                        holder.disconnect.visibility = View.VISIBLE

                        you_are_connected = true

                        outputStream = socket.outputStream

                        receiveFromSocket(activity, person.name, person.gender)

                    }
                }

            }
            catch (e: IOException){
                activity.runOnUiThread {
                    socketError(e, activity)
                }


            }

        }.start()

    }

    private fun sendMessage(message: String, activity: Activity) {
        Thread {
            try {
                outputStream.write(message.toByteArray())
            } catch (e: IOException) {
                socketError(e, activity)
            }
        }.start()

    }

    private fun receiveFromSocket(activity: Activity, username: String, gender: String) {
        Thread {
            try {
                inputStream = socket.inputStream
                val buffer = ByteArray(1024)  // buffer store for the stream
                var bytes: Int // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    receiveMessage(activity, username, incomingMessage, gender)
                }
            } catch (e: IOException) {
                println(e.message)
                if(e.message != "bt socket closed, read return: -1")
                    socketError(e, activity)
            }


        }.start()

    }



}
