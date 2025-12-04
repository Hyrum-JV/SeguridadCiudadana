package com.example.seguridadciudadana.Admin

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.databinding.ActivityAdminDashboardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val db = Firebase.firestore

    private lateinit var adapter: AdminReportAdapter
    private lateinit var statsAdapter: StatAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var listaReportes: MutableList<ReporteZona> = mutableListOf()
    private var listaReportesCercanos: MutableList<ReporteZona> = mutableListOf()
    private var ubicacionAdmin: Location? = null

    private val estadosMap = linkedMapOf(
        "Todos" to "Todos",
        "Pendiente" to "Pendiente",
        "Policía verificando" to "Policía verificando",
        "Pendiente de resolución" to "Pendiente de resolución",
        "Caso resuelto" to "Caso resuelto",
        "Noticia falsa" to "Noticia falsa"
    )

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    /**
     * Obtiene el radio de cobertura desde las preferencias
     */
    private fun getRadioCobertura(): Double {
        return AdminPreferences.getRadioCoberturaDouble(this)
    }

    /**
     * Callback cuando el radio de cobertura cambia desde el perfil
     */
    fun onRadioCoberturaChanged() {
        actualizarTituloConRadio()
        // Re-filtrar los reportes con el nuevo radio
        if (listaReportes.isNotEmpty()) {
            listaReportesCercanos.clear()
            listaReportesCercanos.addAll(filtrarReportesPorRadio(listaReportes))
            actualizarEstadisticas()
            aplicarFiltros()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        configurarRvStats()
        iniciarRecycler()
        iniciarSpinner()
        
        // Obtener ubicación y luego cargar reportes cercanos
        obtenerUbicacionYCargarReportes()

        // Iniciar escucha de notificaciones en tiempo real
        AdminNotificationService.startListening(this)
        AdminNotificationService.suscribirATopicReportes()

        binding.btnRefresh.setOnClickListener { obtenerUbicacionYCargarReportes() }

        // Botón para abrir el mapa de incidentes
        binding.fabOpenMap.setOnClickListener { abrirMapaIncidentes() }

        // Botón para abrir el perfil del administrador
        binding.fabProfile.setOnClickListener { abrirPerfilAdmin() }

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            aplicarFiltros()
            true
        }

        binding.spinnerFilterStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    aplicarFiltros()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // Listener para controlar la visibilidad del container cuando cambia el backstack
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.adminContainer.visibility = View.GONE
            } else {
                binding.adminContainer.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionYCargarReportes() {
        if (!tienePermisosUbicacion()) {
            solicitarPermisosUbicacion()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    ubicacionAdmin = location
                    actualizarTituloConRadio()
                    cargarReportes()
                } else {
                    // Si no hay ubicación actual, intentar con la última conocida
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            ubicacionAdmin = lastLocation
                            actualizarTituloConRadio()
                            cargarReportes()
                        } else {
                            Toast.makeText(this, "No se pudo obtener tu ubicación. Mostrando todos los reportes.", Toast.LENGTH_LONG).show()
                            cargarReportes()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                cargarReportes()
            }
    }

    private fun tienePermisosUbicacion(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisosUbicacion() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYCargarReportes()
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación para filtrar por zona", Toast.LENGTH_LONG).show()
                cargarReportes() // Cargar sin filtro de ubicación
            }
        }
    }

    private fun actualizarTituloConRadio() {
        val radio = getRadioCobertura().toInt()
        binding.toolbarAdmin.subtitle = "Radio de cobertura: $radio km"
    }

    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * @return Distancia en kilómetros
     */
    private fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371.0 // Radio de la Tierra en km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return radioTierra * c
    }

    /**
     * Filtra los reportes que están dentro del radio de cobertura
     */
    private fun filtrarReportesPorRadio(reportes: List<ReporteZona>): List<ReporteZona> {
        val ubicacion = ubicacionAdmin ?: return reportes // Si no hay ubicación, devolver todos
        val radioCobertura = getRadioCobertura()

        return reportes.filter { reporte ->
            val geoPoint = reporte.ubicacion
            if (geoPoint != null) {
                val distancia = calcularDistanciaKm(
                    ubicacion.latitude,
                    ubicacion.longitude,
                    geoPoint.latitude,
                    geoPoint.longitude
                )
                distancia <= radioCobertura
            } else {
                false // Si el reporte no tiene ubicación, no incluirlo
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener escucha de notificaciones al cerrar la actividad
        AdminNotificationService.stopListening()
    }

    private fun abrirPerfilAdmin() {
        val fragment = AdminProfileFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment)
            .addToBackStack(null)
            .commit()

        binding.adminContainer.visibility = View.VISIBLE
    }

    private fun abrirMapaIncidentes() {
        val fragment = AdminMapFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment)
            .addToBackStack(null)
            .commit()

        binding.adminContainer.visibility = View.VISIBLE
    }

    private fun configurarRvStats() {
        statsAdapter = StatAdapter()
        binding.rvStats.apply {
            layoutManager = LinearLayoutManager(
                this@AdminDashboardActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = statsAdapter
            setHasFixedSize(true)
        }

        // Estadísticas iniciales (se actualizarán al cargar reportes)
        statsAdapter.updateStats(
            listOf(
                StatItem("Pendientes", 0, R.drawable.ic_pending),
                StatItem("En Proceso", 0, R.drawable.ic_in_progress),
                StatItem("Resueltos", 0, R.drawable.ic_resolved)
            )
        )
    }

    private fun iniciarRecycler() {
        adapter = AdminReportAdapter { reporte ->
            openReportDetail(reporte)
        }
        binding.rvAdminReports.layoutManager = LinearLayoutManager(this)
        binding.rvAdminReports.adapter = adapter
    }

    private fun iniciarSpinner() {
        val listaEstados = estadosMap.values.toList()
        val spinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, listaEstados)
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
                
                // Filtrar por radio de cobertura
                listaReportesCercanos.clear()
                listaReportesCercanos.addAll(filtrarReportesPorRadio(listaReportes))
                
                // Mostrar cuántos reportes hay en la zona
                val totalReportes = listaReportes.size
                val reportesCercanos = listaReportesCercanos.size
                
                if (ubicacionAdmin != null) {
                    Toast.makeText(
                        this, 
                        "📍 $reportesCercanos reportes en tu zona (de $totalReportes totales)", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                actualizarEstadisticas()
                aplicarFiltros()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarEstadisticas() {
        // Estadísticas solo de reportes cercanos
        val pendientes = listaReportesCercanos.count { it.estado == "Pendiente" }
        val enProceso = listaReportesCercanos.count { 
            it.estado == "Policía verificando" || it.estado == "Pendiente de resolución" 
        }
        val resueltos = listaReportesCercanos.count { it.estado == "Caso resuelto" }

        statsAdapter.updateStats(
            listOf(
                StatItem("Pendientes", pendientes, R.drawable.ic_pending),
                StatItem("En Proceso", enProceso, R.drawable.ic_in_progress),
                StatItem("Resueltos", resueltos, R.drawable.ic_resolved)
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun aplicarFiltros() {
        // Usar la lista de reportes cercanos (filtrados por radio)
        var filtrados = listaReportesCercanos.toList()

        val estadoSeleccionado = binding.spinnerFilterStatus.selectedItem.toString()
        if (estadoSeleccionado != "Todos") {
            filtrados = filtrados.filter { it.estado == estadoSeleccionado }
        }

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
        val fragment = AdminReportDetailFragment.newInstance(report.id)

        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment)
            .addToBackStack(null)
            .commit()

        binding.adminContainer.visibility = View.VISIBLE
    }
}