package com.example.seguridadciudadana.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import java.text.SimpleDateFormat
import java.util.*

class AdminReportAdapter(private val onReportClick: (ReporteZona) -> Unit) : RecyclerView.Adapter<AdminReportAdapter.ViewHolder>() {

    private var reports = listOf<ReporteZona>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.tvCategoria.text = report.categoria
        holder.tvEstado.text = report.estado
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvTimestamp.text = report.timestamp?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

        holder.itemView.setOnClickListener { onReportClick(report) }
    }

    override fun getItemCount() = reports.size

    fun updateReports(newReports: List<ReporteZona>) {
        reports = newReports
        notifyDataSetChanged()
    }
}