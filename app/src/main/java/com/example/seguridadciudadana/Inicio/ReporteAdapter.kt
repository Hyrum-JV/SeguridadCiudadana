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

class ReporteAdapter(
    private val reportes: List<ReporteZona>, 
    private val clickListener: OnReporteClickListener
) : RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tv_ubicacion)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tv_descripcion)
        val tvHoraReporte: TextView = itemView.findViewById(R.id.tv_hora_reporte)
        val imgEvidencia: ImageView = itemView.findViewById(R.id.img_evidencia)
        val layoutImagen: FrameLayout = itemView.findViewById(R.id.layout_imagen_contenedor)
        val btnVerDetalle: ImageView = itemView.findViewById(R.id.btn_ver_detalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_zona, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = reportes[position]

        // Categoría
        holder.tvCategoria.text = "💡 REPORTE: ${reporte.categoria}"

        // Ubicación
        val ubicacionDisplay = reporte.direccion ?: if (reporte.ubicacion != null) {
            "Lat ${String.format("%.4f", reporte.ubicacion.latitude)}, Lon ${String.format("%.4f", reporte.ubicacion.longitude)}"
        } else {
            "Ubicación no disponible"
        }
        holder.tvUbicacion.text = ubicacionDisplay

        // Descripción
        if (reporte.tieneDescripcion) {
            holder.tvDescripcion.text = reporte.descripcion
            holder.tvDescripcion.visibility = View.VISIBLE
        } else {
            holder.tvDescripcion.visibility = View.GONE
        }

        // Hora
        holder.tvHoraReporte.text = formatTimeAgo(reporte.timestamp)

        // Evidencia
        if (!reporte.evidenciaUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(reporte.evidenciaUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgEvidencia)
            holder.layoutImagen.visibility = View.VISIBLE
        } else {
            holder.layoutImagen.visibility = View.GONE
        }

        // ✅ Click en el card completo: centrar en mapa
        holder.itemView.setOnClickListener {
            val geoPoint = reporte.ubicacion
            if (geoPoint != null) {
                clickListener.onReporteClicked(geoPoint.latitude, geoPoint.longitude)
            }
        }

        // ✅ Click en la flecha: mostrar detalles
        holder.btnVerDetalle.setOnClickListener {
            clickListener.onVerDetalleClicked(reporte)
        }
    }

    interface OnReporteClickListener {
        fun onReporteClicked(lat: Double, lon: Double)
        fun onVerDetalleClicked(reporte: ReporteZona)
    }

    override fun getItemCount() = reportes.size

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