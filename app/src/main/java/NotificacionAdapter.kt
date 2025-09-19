import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.Notificacion
import com.example.seguridadciudadana.R
class NotificacionAdapter(private val lista: List<Notificacion>) :
    RecyclerView.Adapter<NotificacionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo = itemView.findViewById<TextView>(R.id.tituloNotificacion)
        val fecha = itemView.findViewById<TextView>(R.id.fechaNotificacion)
        val icono = itemView.findViewById<ImageView>(R.id.iconoNotificacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val noti = lista[position]
        holder.titulo.text = noti.titulo
        holder.fecha.text = noti.fecha
        holder.icono.setImageResource(noti.icono)
    }

    override fun getItemCount(): Int = lista.size
}
