package com.example.seguridadciudadana.Notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.util.Date
import java.util.concurrent.TimeUnit
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.util.Log // Importamos Log para el debugging

/**
 * Función utilitaria para calcular la antigüedad de una fecha (hace X tiempo).
 * Se mueve al adaptador o un util si lo necesitas en más sitios.
 */
fun getRelativeTime(date: Date): String {
    val diff = Date().time - date.time
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)

    return when {
        seconds < 60 -> "Hace unos segundos"
        seconds < 3600 -> "Hace ${seconds / 60} minutos"
        seconds < 86400 -> "Hace ${seconds / 3600} horas"
        seconds < 604800 -> "Hace ${seconds / 86400} días"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }
}
class AdaptadorNoticias (
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<AdaptadorNoticias.NoticiaViewHolder>() {

    private var noticias = listOf<Noticia>()

    interface OnItemClickListener {
        fun onItemClick(noticia: Noticia)
    }

    inner class NoticiaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        // Mapeo de IDs existentes en item_notificacion_noticia.xml
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val tituloNoticia: TextView = itemView.findViewById(R.id.tituloNoticia)
        val descripcionNoticia: TextView = itemView.findViewById(R.id.descripcionNoticia)

        // ¡ÚNICO TextView para Fuente y Fecha, llamado 'fuente_noticia' en el XML!
        val fuenteFechaNoticia: TextView = itemView.findViewById(R.id.fuenteFechaNoticia)


        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(noticias[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticiaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_noticia, parent, false)
        return NoticiaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: NoticiaViewHolder, position: Int) {
        val n = noticias[position]
        val context = holder.itemView.context

        // 1. Título y Descripción
        holder.tituloNoticia.text = n.titulo ?: "Título no disponible"
        holder.descripcionNoticia.text = n.snippet ?: "Descripción no disponible"

        // 2. UNIFICACIÓN DE FUENTE Y FECHA en un solo TextView
        val fuente = n.fuente ?: "Fuente desconocida"
        val timeAgo = n.fecha_creacion?.let { getRelativeTime(it) } ?: "Fecha no disponible"

        // Concatenar y asignar al único TextView disponible (fuenteFechaNoticia)
        holder.fuenteFechaNoticia.text = "$fuente"


        // 3. Imagen con Glide
        if (!n.imagen.isNullOrEmpty()) {
            Glide.with(context)
                .load(n.imagen)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(holder.imageView)
            holder.imageView.visibility = View.VISIBLE
        } else {
            holder.imageView.visibility = View.GONE
        }
    }

    override fun getItemCount() = noticias.size

    fun actualizarNoticias(nuevaLista: List<Noticia>) {
        noticias = nuevaLista
        notifyDataSetChanged()
    }
}

