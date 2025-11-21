package com.example.seguridadciudadana.Admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.databinding.ActivityAdminDashboardBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val db = Firebase.firestore

    private lateinit var adapter: AdminReportAdapter
    private var listaReportes: MutableList<ReporteZona> = mutableListOf()

    // Mapa: clave = valor real en Firestore, valor = lo mostrado en el spinner
    private val estadosMap = linkedMapOf(
        "Todos" to "Todos",
        "pending" to "Pendiente",
        "police_in_progress" to "Policía verificando",
        "pending_resolution" to "Pendiente de resolución",
        "case_resolved" to "Caso resuelto",
        "false_news" to "Noticia falsa"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iniciarRecycler()
        iniciarSpinner()
        cargarReportes()

        binding.btnRefresh.setOnClickListener { cargarReportes() }

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            aplicarFiltros()
            true
        }

        binding.spinnerFilterStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    aplicarFiltros()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun iniciarRecycler() {
        // Aqui pasamos el callback que ABRE el fragment de detalle
        adapter = AdminReportAdapter { reporte ->
            openReportDetail(reporte)
        }
        binding.rvAdminReports.layoutManager = LinearLayoutManager(this)
        binding.rvAdminReports.adapter = adapter
    }

    private fun iniciarSpinner() {
        val listaEstados = estadosMap.values.toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaEstados)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilterStatus.adapter = spinnerAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun cargarReportes() {
        db.collection("reportes")
            .get()
            .addOnSuccessListener { snap ->
                listaReportes.clear()
                listaReportes.addAll(
                    snap.documents.mapNotNull { d ->
                        d.toObject(ReporteZona::class.java)?.copy(id = d.id)
                    }
                )
                aplicarFiltros()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun aplicarFiltros() {
        var filtrados = listaReportes.toList()

        // FILTRO POR ESTADO
        val estadoSeleccionado = binding.spinnerFilterStatus.selectedItem.toString()
        if (estadoSeleccionado != "Todos") {
            val estadoFirestore = estadosMap.entries.firstOrNull { it.value == estadoSeleccionado }?.key
            filtrados = filtrados.filter { it.estado == estadoFirestore }
        }

        // FILTRO POR BÚSQUEDA
        val searchText = binding.etSearch.text.toString().trim().lowercase()
        if (searchText.isNotEmpty()) {
            filtrados = filtrados.filter {
                (it.descripcion?.lowercase()?.contains(searchText) == true) ||
                        (it.categoria?.lowercase()?.contains(searchText) == true)
            }
        }

        adapter.updateReports(filtrados)
    }

    private fun openReportDetail(report: ReporteZona) {
        // Crea el fragment con el id del reporte
        val fragment = AdminReportDetailFragment.newInstance(report.id)

        // Mostrar el fragment en el contenedor (admin_container) y mantener en backstack
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment)
            .addToBackStack(null)
            .commit()

        // Asegurarnos que el contenedor sea visible (tu layout ya tiene admin_container)
        binding.adminContainer.visibility = View.VISIBLE
    }
}
