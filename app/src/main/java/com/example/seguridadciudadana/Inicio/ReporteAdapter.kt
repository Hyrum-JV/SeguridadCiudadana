package com.example.seguridadciudadana.Inicio

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

class ReporteAdapter(private val reportes: List<ReporteZona>, private val clickListener: OnReporteClickListener) :
    RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tv_ubicacion)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tv_descripcion)
        val tvHoraReporte: TextView = itemView.findViewById(R.id.tv_hora_reporte)
        val imgEvidencia: ImageView = itemView.findViewById(R.id.img_evidencia)
        val layoutImagen: FrameLayout = itemView.findViewById(R.id.layout_imagen_contenedor) // Necesitas FrameLayout o ConstraintLayout en item_reporte_zona.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_zona, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = reportes[position]

        // 1. Categoría
        holder.tvCategoria.text = "💡 REPORTE: ${reporte.categoria}"

        // 2. Ubicación
        // ✅ Reemplazar la lógica anterior por esta línea:
        val ubicacionDisplay = reporte.direccion ?: if (reporte.ubicacion != null) {
            "Ubicación: Lat ${String.format("%.4f", reporte.ubicacion.latitude)}, Lon ${String.format("%.4f", reporte.ubicacion.longitude)}"
        } else {
            "Ubicación: No disponible"
        }

        // Asignar el texto de ubicación/dirección
        holder.tvUbicacion.text = ubicacionDisplay
        // FIN de la sección de ubicación

        // 3. Descripción (Condicional)
        if (reporte.tieneDescripcion) {
            holder.tvDescripcion.text = "DESCRIPCIÓN: ${reporte.descripcion}"
            holder.tvDescripcion.visibility = View.VISIBLE
        } else {
            // Ocultar la descripción si no existe (implementa el caso "Sin Descripción")
            holder.tvDescripcion.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val geoPoint = reporte.ubicacion
            if (geoPoint != null) {
                clickListener.onReporteClicked(geoPoint.latitude, geoPoint.longitude)
            } else {
                Log.w("ReporteAdapter", "Intento de click en reporte sin coordenadas.")
            }
        }

        // 4. Hora
        holder.tvHoraReporte.text = formatTimeAgo(reporte.timestamp)

        // 5. Evidencia (Usando Glide para cargar imágenes desde URL)
        if (!reporte.evidenciaUrl.isNullOrEmpty()) {
            // Cargar miniatura de la evidencia (requiere librería Glide)
            Glide.with(holder.itemView.context)
                .load(reporte.evidenciaUrl)
                .placeholder(R.drawable.ic_camara) // Reemplaza con un placeholder real
                .into(holder.imgEvidencia)
            holder.layoutImagen.visibility = View.VISIBLE
        } else {
            holder.layoutImagen.visibility = View.GONE
        }
    }

    interface OnReporteClickListener {
        fun onReporteClicked(lat: Double, lon: Double)
    }

    override fun getItemCount() = reportes.size

    // Función de ayuda para mostrar "Hace X minutos/horas"
    private fun formatTimeAgo(timestamp: Timestamp?): String {
        timestamp ?: return "Hace un momento"
        val now = System.currentTimeMillis()
        val diff = now - timestamp.toDate().time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Hace un momento"
            minutes < 60 -> "Hace $minutes minutos"
            hours < 24 -> "Hace $hours horas"
            days < 7 -> "Hace $days días"
            else -> android.text.format.DateFormat.format("dd/MM/yyyy", timestamp.toDate()).toString()
        }
    }
}