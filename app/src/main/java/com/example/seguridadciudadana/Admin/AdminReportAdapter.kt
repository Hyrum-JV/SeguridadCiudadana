package com.example.seguridadciudadana.Admin

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminReportAdapter(
    private val onReportClick: (ReporteZona) -> Unit
) : RecyclerView.Adapter<AdminReportAdapter.ViewHolder>() {

    private var reports = listOf<ReporteZona>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tv_descripcion_corta)

        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvReporter: TextView = itemView.findViewById(R.id.tv_reporter)

        val chipEstado: Chip = itemView.findViewById(R.id.chip_estado)
        val chipPriority: Chip = itemView.findViewById(R.id.chip_priority)

        val imgStatus: ImageView = itemView.findViewById(R.id.iv_status_icon)
        val imgPreview: ImageView = itemView.findViewById(R.id.iv_preview_thumbnail)
        val urgencyBar: View = itemView.findViewById(R.id.v_urgency_bar)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]

        // CATEGORÍA
        holder.tvCategoria.text = report.categoria.ifEmpty { "Sin categoría" }

        // DESCRIPCIÓN CORTA
        holder.tvDescripcion.text = report.descripcion ?: "Sin descripción"

        // UBICACIÓN - Convertir GeoPoint a dirección
        report.ubicacion?.let { geoPoint ->
            val geocoder = Geocoder(holder.itemView.context, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val direccion = address.getAddressLine(0) ?: "Ubicación desconocida"
                    holder.tvLocation.text = direccion
                } else {
                    holder.tvLocation.text = "Sin dirección"
                }
            } catch (e: Exception) {
                holder.tvLocation.text = "${geoPoint.latitude}, ${geoPoint.longitude}"
            }
        } ?: run {
            holder.tvLocation.text = "Sin ubicación"
        }

        // REPORTANTE - Cargar nombre desde Firestore
        if (report.userId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(report.userId)
                .get()
                .addOnSuccessListener { document ->
                    val nombre = document.getString("nombre") ?: "Usuario"
                    holder.tvReporter.text = nombre
                }
                .addOnFailureListener {
                    holder.tvReporter.text = "Usuario desconocido"
                }
        } else {
            holder.tvReporter.text = "Anónimo"
        }

        // FECHA
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvTimestamp.text = report.timestamp?.toDate()?.let { sdf.format(it) }
            ?: "Sin fecha"

        // VISTA PREVIA DE EVIDENCIA
        if (!report.evidenciaUrl.isNullOrEmpty()) {
            holder.imgPreview.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(report.evidenciaUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(holder.imgPreview)
        } else {
            holder.imgPreview.visibility = View.GONE
        }

        // ESTADO DEL REPORTE (Chip + Icono)
        val estado = report.estado?.lowercase() ?: "pendiente"

        when (estado) {
            "pendiente" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_pendiente)
                holder.chipEstado.text = "Pendiente"
                holder.imgStatus.setImageResource(R.drawable.ic_status_pending)
            }
            "policía verificando" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_proceso)
                holder.chipEstado.text = "Policía verificando"
                holder.imgStatus.setImageResource(R.drawable.ic_status_progress)
            }
            "pendiente de resolución" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_proceso)
                holder.chipEstado.text = "Pendiente de resolución"
                holder.imgStatus.setImageResource(R.drawable.ic_status_progress)
            }
            "caso resuelto" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_completado)
                holder.chipEstado.text = "Caso resuelto"
                holder.imgStatus.setImageResource(R.drawable.ic_status_complete)
            }
            "noticia falsa" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_falso)
                holder.chipEstado.text = "Noticia falsa"
                holder.imgStatus.setImageResource(R.drawable.ic_status_false)
            }
            else -> {
                holder.chipEstado.text = report.estado ?: "Desconocido"
            }
        }

        // OCULTAR urgent-bar (no implementado aún)
        holder.urgencyBar.visibility = View.GONE

        // PRIORIDAD (oculta si no se usa)
        holder.chipPriority.visibility = View.GONE

        // CLICK EN TODO EL ITEM → abre fragment de detalle
        holder.itemView.setOnClickListener {
            onReportClick(report)
        }
    }

    override fun getItemCount() = reports.size

    // Actualizar lista del recycler
    fun updateReports(newReports: List<ReporteZona>) {
        reports = newReports
        notifyDataSetChanged()
    }
}