package com.example.seguridadciudadana.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.android.material.chip.Chip
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
        val urgencyBar: View = itemView.findViewById(R.id.v_urgency_bar)
        val btnQuickAction: ImageButton = itemView.findViewById(R.id.btn_quick_action)
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

        // UBICACIÓN
        holder.tvLocation.text = report.direccion ?: "Sin dirección"

        // REPORTANTE
        holder.tvReporter.text = report.userId.ifEmpty { "Anónimo" }

        // FECHA
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvTimestamp.text = report.timestamp?.toDate()?.let { sdf.format(it) }
            ?: "Sin fecha"

        // ESTADO DEL REPORTE (Chip + Icono)
        val estado = report.estado.lowercase()

        when (estado) {
            "pending", "pendiente" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_pendiente)
                holder.chipEstado.text = "Pendiente"
                holder.imgStatus.setImageResource(R.drawable.ic_status_pending)
            }
            "in_progress", "en proceso" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_proceso)
                holder.chipEstado.text = "En Proceso"
                holder.imgStatus.setImageResource(R.drawable.ic_status_progress)
            }
            "completed", "completado" -> {
                holder.chipEstado.setChipBackgroundColorResource(R.color.estado_completado)
                holder.chipEstado.text = "Completado"
                holder.imgStatus.setImageResource(R.drawable.ic_status_complete)
            }
            else -> {
                holder.chipEstado.text = report.estado
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
