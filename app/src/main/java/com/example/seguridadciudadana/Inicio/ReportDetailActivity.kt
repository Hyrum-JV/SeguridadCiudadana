package com.example.seguridadciudadana.Admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.databinding.ActivityReportDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seguridadciudadana.Inicio.ReporteZona

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var reportId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportId = intent.getStringExtra("reportId") ?: ""

        if (reportId.isNotEmpty()) {
            loadReport()
        }
    }

    private fun loadReport() {
        firestore.collection("reportes").document(reportId)
            .get()
            .addOnSuccessListener { doc ->
                val report = doc.toObject(ReporteZona::class.java) ?: return@addOnSuccessListener

                // Fill UI
                binding.tvCategoria.text = report.categoria
                binding.tvDescripcion.text = report.descripcion ?: "No description"
                binding.tvEstado.text = report.estado
                binding.tvDireccion.text = report.direccion ?: "No address"
                binding.tvUserId.text = report.userId
            }
    }
}
