package com.example.seguridadciudadana.Admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val db = FirebaseFirestore.getInstance()

    // Vistas
    private lateinit var loadingOverlay: View
    private lateinit var fabBack: FloatingActionButton
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var chipGroupEstado: ChipGroup
    private lateinit var filtersContainer: LinearLayout
    private lateinit var btnToggleFilters: ImageButton

    // Estadísticas
    private lateinit var tvTotalCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvProcessCount: TextView

    // Datos
    private val todosLosReportes = mutableListOf<ReporteZona>()
    private val reportesFiltrados = mutableListOf<ReporteZona>()
    private val markerReporteMap = mutableMapOf<Marker, ReporteZona>()
    private val userNamesCache = mutableMapOf<String, String>()

    // Filtro actual
    private var filtroEstadoActual: String = "Todos"
    private var filtersExpanded = true

    companion object {
        private const val TAG = "AdminMapFragment"

        fun newInstance(): AdminMapFragment {
            return AdminMapFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()

        val mapFragment = childFragmentManager.findFragmentById(R.id.admin_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initViews(view: View) {
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        fabBack = view.findViewById(R.id.fab_back)
        fabRefresh = view.findViewById(R.id.fab_refresh)
        chipGroupEstado = view.findViewById(R.id.chip_group_estado)
        filtersContainer = view.findViewById(R.id.filters_container)
        btnToggleFilters = view.findViewById(R.id.btn_toggle_filters)

        tvTotalCount = view.findViewById(R.id.tv_total_count)
        tvPendingCount = view.findViewById(R.id.tv_pending_count)
        tvProcessCount = view.findViewById(R.id.tv_process_count)
    }

    private fun setupListeners() {
        fabBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        fabRefresh.setOnClickListener {
            cargarReportes()
        }

        btnToggleFilters.setOnClickListener {
            toggleFilters()
        }

        // Listeners para los chips de filtro
        chipGroupEstado.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                // Si no hay ninguno seleccionado, seleccionar "Todos"
                view?.findViewById<Chip>(R.id.chip_todos)?.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            val chipId = checkedIds.first()
            filtroEstadoActual = when (chipId) {
                R.id.chip_todos -> "Todos"
                R.id.chip_pendiente -> "Pendiente"
                R.id.chip_verificando -> "Policía verificando"
                R.id.chip_resolucion -> "Pendiente de resolución"
                R.id.chip_resuelto -> "Caso resuelto"
                R.id.chip_falso -> "Noticia falsa"
                else -> "Todos"
            }
            aplicarFiltros()
        }
    }

    private fun toggleFilters() {
        filtersExpanded = !filtersExpanded
        if (filtersExpanded) {
            filtersContainer.visibility = View.VISIBLE
            btnToggleFilters.setImageResource(R.drawable.ic_expand_less)
        } else {
            filtersContainer.visibility = View.GONE
            btnToggleFilters.setImageResource(R.drawable.ic_expand_more)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Centrar en Trujillo, Perú
        val trujillo = LatLng(-8.11599, -79.02998)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 12f))

        // Configurar el mapa
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = false
        }

        // Click en marcador
        map.setOnMarkerClickListener { marker ->
            markerReporteMap[marker]?.let { reporte ->
                mostrarInfoReporte(reporte)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 16f))
            }
            true
        }

        // Click en info window (para ir al detalle)
        map.setOnInfoWindowClickListener { marker ->
            markerReporteMap[marker]?.let { reporte ->
                abrirDetalleReporte(reporte)
            }
        }

        // Cargar reportes
        cargarReportes()
    }

    private fun cargarReportes() {
        if (!::map.isInitialized) return

        loadingOverlay.visibility = View.VISIBLE

        db.collection("reportes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                todosLosReportes.clear()

                for (document in result) {
                    val geo = document.getGeoPoint("ubicacion") ?: continue

                    val reporte = ReporteZona(
                        id = document.id,
                        categoria = document.getString("categoria") ?: "Sin categoría",
                        ubicacion = geo,
                        descripcion = document.getString("descripcion"),
                        evidenciaUrl = document.getString("evidenciaUrl"),
                        timestamp = document.getTimestamp("timestamp"),
                        direccion = obtenerDireccion(geo.latitude, geo.longitude),
                        userId = document.getString("userId") ?: "",
                        estado = document.getString("estado") ?: "Pendiente",
                        adminComentario = document.getString("adminComentario") ?: "",
                        adminUid = document.getString("adminUid") ?: "",
                        tipoEvidencia = document.getString("tipoEvidencia")
                    )
                    todosLosReportes.add(reporte)
                }

                // Precargar nombres de usuarios
                precargarNombresUsuarios()

                aplicarFiltros()
                loadingOverlay.visibility = View.GONE

                Log.d(TAG, "Cargados ${todosLosReportes.size} reportes")
            }
            .addOnFailureListener { e ->
                loadingOverlay.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al cargar reportes", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error cargando reportes", e)
            }
    }

    private fun precargarNombresUsuarios() {
        val userIds = todosLosReportes.map { it.userId }.distinct().filter { it.isNotEmpty() }

        for (userId in userIds) {
            if (userNamesCache.containsKey(userId)) continue

            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: "Usuario"
                    userNamesCache[userId] = nombre
                }
        }
    }

    private fun aplicarFiltros() {
        reportesFiltrados.clear()

        reportesFiltrados.addAll(
            if (filtroEstadoActual == "Todos") {
                todosLosReportes
            } else {
                todosLosReportes.filter { it.estado == filtroEstadoActual }
            }
        )

        actualizarEstadisticas()
        mostrarMarcadoresEnMapa()
    }

    private fun actualizarEstadisticas() {
        val total = reportesFiltrados.size
        val pendientes = reportesFiltrados.count { it.estado == "Pendiente" }
        val enProceso = reportesFiltrados.count {
            it.estado == "Policía verificando" || it.estado == "Pendiente de resolución"
        }

        tvTotalCount.text = total.toString()
        tvPendingCount.text = pendientes.toString()
        tvProcessCount.text = enProceso.toString()
    }

    private fun mostrarMarcadoresEnMapa() {
        map.clear()
        markerReporteMap.clear()

        for (reporte in reportesFiltrados) {
            reporte.ubicacion?.let { geo ->
                val posicion = LatLng(geo.latitude, geo.longitude)

                // Color del marcador según estado
                val markerColor = obtenerColorMarcador(reporte.estado)

                // Crear círculo de zona
                val (strokeColor, fillColor) = obtenerColoresCirculo(reporte.estado)
                map.addCircle(
                    CircleOptions()
                        .center(posicion)
                        .radius(80.0)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .strokeWidth(2f)
                )

                // Crear marcador
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(reporte.categoria)
                        .snippet(reporte.estado ?: "Pendiente")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )

                marker?.let { markerReporteMap[it] = reporte }
            }
        }

        // Ajustar cámara para mostrar todos los marcadores
        if (reportesFiltrados.isNotEmpty()) {
            ajustarCamaraATodosLosMarcadores()
        }
    }

    private fun ajustarCamaraATodosLosMarcadores() {
        if (reportesFiltrados.isEmpty()) return

        val builder = LatLngBounds.Builder()
        for (reporte in reportesFiltrados) {
            reporte.ubicacion?.let { geo ->
                builder.include(LatLng(geo.latitude, geo.longitude))
            }
        }

        try {
            val bounds = builder.build()
            val padding = 100 // pixels
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            Log.e(TAG, "Error ajustando cámara", e)
        }
    }

    private fun obtenerColorMarcador(estado: String?): Float {
        return when (estado?.lowercase()) {
            "pendiente" -> BitmapDescriptorFactory.HUE_ORANGE
            "policía verificando", "pendiente de resolución" -> BitmapDescriptorFactory.HUE_AZURE
            "caso resuelto" -> BitmapDescriptorFactory.HUE_GREEN
            "noticia falsa" -> BitmapDescriptorFactory.HUE_RED
            else -> BitmapDescriptorFactory.HUE_ORANGE
        }
    }

    private fun obtenerColoresCirculo(estado: String?): Pair<Int, Int> {
        return when (estado?.lowercase()) {
            "pendiente" -> Pair(0xFFFF9800.toInt(), 0x33FF9800)
            "policía verificando", "pendiente de resolución" -> Pair(0xFF2196F3.toInt(), 0x332196F3)
            "caso resuelto" -> Pair(0xFF4CAF50.toInt(), 0x334CAF50)
            "noticia falsa" -> Pair(0xFFF44336.toInt(), 0x33F44336)
            else -> Pair(0xFF9E9E9E.toInt(), 0x339E9E9E)
        }
    }

    private fun mostrarInfoReporte(reporte: ReporteZona) {
        // Crear un dialog con información del reporte
        val dialogView = layoutInflater.inflate(R.layout.layout_admin_marker_info, null)

        val ivEvidencia = dialogView.findViewById<ImageView>(R.id.iv_info_evidencia)
        val tvCategoria = dialogView.findViewById<TextView>(R.id.tv_info_categoria)
        val chipEstado = dialogView.findViewById<Chip>(R.id.chip_info_estado)
        val tvDescripcion = dialogView.findViewById<TextView>(R.id.tv_info_descripcion)
        val tvFecha = dialogView.findViewById<TextView>(R.id.tv_info_fecha)
        val tvReporter = dialogView.findViewById<TextView>(R.id.tv_info_reporter)
        val btnVerDetalle = dialogView.findViewById<View>(R.id.btn_info_ver_detalle)

        // Configurar datos
        tvCategoria.text = reporte.categoria
        tvDescripcion.text = reporte.descripcion ?: "Sin descripción"

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvFecha.text = reporte.timestamp?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

        // Estado con color
        chipEstado.text = reporte.estado ?: "Pendiente"
        val estadoColor = when (reporte.estado?.lowercase()) {
            "pendiente" -> R.color.estado_pendiente
            "policía verificando", "pendiente de resolución" -> R.color.estado_proceso
            "caso resuelto" -> R.color.estado_completado
            "noticia falsa" -> R.color.estado_falso
            else -> R.color.estado_pendiente
        }
        chipEstado.setChipBackgroundColorResource(estadoColor)

        // Usuario
        val userName = userNamesCache[reporte.userId] ?: "Cargando..."
        tvReporter.text = userName

        // Evidencia
        if (!reporte.evidenciaUrl.isNullOrEmpty()) {
            ivEvidencia.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(reporte.evidenciaUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(ivEvidencia)
        } else {
            ivEvidencia.visibility = View.GONE
        }

        // Dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Click en ver detalle
        btnVerDetalle.setOnClickListener {
            dialog.dismiss()
            abrirDetalleReporte(reporte)
        }

        dialog.show()
    }

    private fun abrirDetalleReporte(reporte: ReporteZona) {
        val fragment = AdminReportDetailFragment.newInstance(reporte.id)

        // Usar add en lugar de replace para mantener el mapa en el backstack
        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.admin_container, fragment)
            .addToBackStack("detail_from_map")
            .commit()
    }

    private fun obtenerDireccion(latitud: Double, longitud: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitud, longitud, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.getAddressLine(0) ?: "Ubicación desconocida"
            } else {
                "Ubicación desconocida"
            }
        } catch (e: Exception) {
            "$latitud, $longitud"
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // Cuando el fragment vuelve a ser visible después de cerrar el detalle
        if (!hidden) {
            activity?.findViewById<View>(R.id.admin_container)?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar recursos del mapa
        markerReporteMap.clear()
    }
}
