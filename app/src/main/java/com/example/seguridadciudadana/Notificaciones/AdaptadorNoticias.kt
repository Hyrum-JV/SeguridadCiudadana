package com.example.seguridadciudadana.Notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import java.text.SimpleDateFormat
import java.util.Locale

class AdaptadorNoticias (
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<AdaptadorNoticias.NoticiaViewHolder>() {

    private var articulos = listOf<Articulo>()

    interface OnItemClickListener {
        fun onItemClick(articulo: Articulo)
    }

    inner class NoticiaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val tituloNoticia: TextView = itemView.findViewById(R.id.titulo_noticia)
        val descripcionNoticia: TextView = itemView.findViewById(R.id.descripcion_noticia)
        val fuenteNoticia: TextView = itemView.findViewById(R.id.fuente_noticia)
        val fechaNoticia: TextView = itemView.findViewById(R.id.fecha_noticia)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(articulos[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticiaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_noticia, parent, false)
        return NoticiaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: NoticiaViewHolder, position: Int) {
        val a = articulos[position]

        // Título y descripción
        holder.tituloNoticia.text = a.titulo ?: "Título no disponible"
        holder.descripcionNoticia.text = a.descripcion ?: "Descripción no disponible"

        // Fuente
        holder.fuenteNoticia.text = a.fuente?.nombre?.let { "Fuente: $it" } ?: "Fuente desconocida"

        // Fecha (formateo)
        holder.fechaNoticia.text = a.fechaPublicacion?.let { formatearFecha(it) } ?: "Fecha no disponible"
    }

    override fun getItemCount() = articulos.size

    fun actualizarArticulos(nuevaLista: List<Articulo>) {
        articulos = nuevaLista
        notifyDataSetChanged()
    }

    private fun formatearFecha(fechaISO: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outFmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val date = inFmt.parse(fechaISO)
            date?.let { outFmt.format(it) } ?: fechaISO
        } catch (_: Exception) {
            fechaISO
        }
    }
}

