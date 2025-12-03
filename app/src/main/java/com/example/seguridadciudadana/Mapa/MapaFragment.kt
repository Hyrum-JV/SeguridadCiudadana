package com.example.seguridadciudadana.Mapa

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.seguridadciudadana.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.provider.Settings
import android.util.Log
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.seguridadciudadana.Inicio.ReporteAdapter
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler

class MapaFragment : Fragment(), OnMapReadyCallback, ReporteAdapter.OnReporteClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isFollowingUser = true
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var rvReportes: RecyclerView
    private lateinit var reporteAdapter: ReporteAdapter
    private val reportesList = mutableListOf<ReporteZona>()
    private val handler = Handler(Looper.getMainLooper())
    private val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L
    
    private val markerReporteMap = mutableMapOf<Marker, ReporteZona>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bottomSheet = view.findViewById<LinearLayout>(R.id.bottom_sheet_layout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val peakHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peak_height)
        bottomSheetBehavior.peekHeight = peakHeight
        bottomSheetBehavior.isHideable = true

        rvReportes = view.findViewById(R.id.rv_reportes_zona)
        reporteAdapter = ReporteAdapter(reportesList, this)
        rvReportes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reporteAdapter
            isNestedScrollingEnabled = true
        }

        createLocationRequest()
    }

    private fun iniciarLogicaMapa() {
        startLocationUpdates()
        cargarReportesEnMapa()
        iniciarActualizacionAutomatica()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val trujillo = LatLng(-8.11599, -79.02998)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 13f))

        enableMyLocation()
        iniciarLogicaMapa()

        map.setOnMarkerClickListener { marker ->
            val reporte = markerReporteMap[marker]
            if (reporte != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 16f))
            }
            true
        }

        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingUser = false
            }
        }
    }

    // ✅ Implementar la interfaz del adapter
    override fun onReporteClicked(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    // ✅ Nuevo método para mostrar detalles
    override fun onVerDetalleClicked(reporte: ReporteZona) {
        mostrarDialogoDetalle(reporte)
    }

    private fun mostrarDialogoDetalle(reporte: ReporteZona) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reporte_detalle, null)
        
        val tvCategoria = dialogView.findViewById<TextView>(R.id.tv_dialog_categoria)
        val chipEstado = dialogView.findViewById<Chip>(R.id.chip_dialog_estado)
        val ivEvidencia = dialogView.findViewById<ImageView>(R.id.iv_dialog_evidencia)
        val tvDescripcion = dialogView.findViewById<TextView>(R.id.tv_dialog_descripcion)
        val tvFecha = dialogView.findViewById<TextView>(R.id.tv_dialog_fecha)
        val tvUbicacion = dialogView.findViewById<TextView>(R.id.tv_dialog_ubicacion)

        tvCategoria.text = reporte.categoria
        tvDescripcion.text = reporte.descripcion ?: "Sin descripción adicional"
        tvUbicacion.text = reporte.direccion ?: "Ubicación desconocida"
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvFecha.text = reporte.timestamp?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

        val estado = reporte.estado?.lowercase() ?: "pendiente"
        chipEstado.text = reporte.estado ?: "Pendiente"
        
        when (estado) {
            "pendiente" -> {
                chipEstado.setChipBackgroundColorResource(R.color.estado_pendiente)
                chipEstado.setTextColor(resources.getColor(android.R.color.white, null))
            }
            "policía verificando", "pendiente de resolución" -> {
                chipEstado.setChipBackgroundColorResource(R.color.estado_proceso)
                chipEstado.setTextColor(resources.getColor(android.R.color.white, null))
            }
            "caso resuelto" -> {
                chipEstado.setChipBackgroundColorResource(R.color.estado_completado)
                chipEstado.setTextColor(resources.getColor(android.R.color.white, null))
            }
            "noticia falsa" -> {
                chipEstado.setChipBackgroundColorResource(R.color.estado_falso)
                chipEstado.setTextColor(resources.getColor(android.R.color.white, null))
            }
        }

        if (!reporte.evidenciaUrl.isNullOrEmpty()) {
            ivEvidencia.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(reporte.evidenciaUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(ivEvidencia)
        } else {
            ivEvidencia.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun obtenerColorEstado(estado: String?): Pair<Int, Int> {
        return when (estado?.lowercase()) {
            "pendiente" -> Pair(0xFFFF9800.toInt(), 0x55FF9800)
            "policía verificando", "pendiente de resolución" -> Pair(0xFF2196F3.toInt(), 0x552196F3)
            "caso resuelto" -> Pair(0xFF4CAF50.toInt(), 0x554CAF50)
            "noticia falsa" -> Pair(0xFFF44336.toInt(), 0x55F44336)
            else -> Pair(0xFF9E9E9E.toInt(), 0x559E9E9E)
        }
    }

    private val actualizacionRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d("MapaFragment", "Actualizando reportes automáticamente...")
            cargarReportesEnMapa()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    private fun iniciarActualizacionAutomatica() {
        handler.postDelayed(actualizacionRunnable, UPDATE_INTERVAL_MS)
    }

    private fun detenerActualizacionAutomatica() {
        handler.removeCallbacks(actualizacionRunnable)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    if (isFollowingUser) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }
                }
            }
        }
    }

    private fun checkLocationEnabled() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isEnabled) {
            AlertDialog.Builder(requireContext())
                .setTitle("Ubicación desactivada")
                .setMessage("Por favor activa tu GPS para mostrar tu ubicación en el mapa.")
                .setPositiveButton("Activar") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun startLocationUpdates(shouldRecenter: Boolean = false) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isFollowingUser = true
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            if (shouldRecenter) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        checkLocationEnabled()
        startLocationUpdates()
        iniciarActualizacionAutomatica()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        detenerActualizacionAutomatica()
    }

    private fun cargarReportesEnMapa() {
        if (!::map.isInitialized || !isAdded) return
        val safeContext = context ?: return

        if (ActivityCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded || location == null) return@addOnSuccessListener

            val radioMetros = 1000.0
            val latUsuario = location.latitude
            val lonUsuario = location.longitude

            map.clear()
            markerReporteMap.clear()
            reportesList.clear()
            reporteAdapter.notifyDataSetChanged()

            val tiempoLimite = getTimestampOffset(-6 * 60 * 60 * 1000L)
            val db = FirebaseFirestore.getInstance()

            db.collection("reportes")
                .whereGreaterThan("timestamp", tiempoLimite)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    if (!isAdded) return@addOnSuccessListener

                    for (document in result) {
                        val geo = document.getGeoPoint("ubicacion") ?: continue
                        val distancia = calcularDistancia(latUsuario, lonUsuario, geo.latitude, geo.longitude)

                        if (distancia <= radioMetros) {
                            val direccionReporte = obtenerDireccion(geo.latitude, geo.longitude, safeContext)
                            val categoria = document.getString("categoria") ?: "Sin categoría"
                            val timestamp = document.getTimestamp("timestamp")
                            val descripcion = document.getString("descripcion")
                            val evidenciaUrl = document.getString("evidenciaUrl")
                            val estado = document.getString("estado")
                            val posicion = LatLng(geo.latitude, geo.longitude)

                            val reporteZona = ReporteZona(
                                id = document.id,
                                categoria = categoria,
                                ubicacion = geo,
                                descripcion = descripcion,
                                evidenciaUrl = evidenciaUrl,
                                timestamp = timestamp,
                                direccion = direccionReporte,
                                estado = estado
                            )
                            reportesList.add(reporteZona)

                            val (strokeColor, fillColor) = obtenerColorEstado(estado)
                            map.addCircle(
                                CircleOptions()
                                    .center(posicion)
                                    .radius(100.0)
                                    .strokeColor(strokeColor)
                                    .fillColor(fillColor)
                                    .strokeWidth(3f)
                            )

                            val horaReporte = formatTimestamp(timestamp)
                            val marker = map.addMarker(
                                MarkerOptions()
                                    .position(posicion)
                                    .title(categoria)
                                    .snippet("${estado ?: "Pendiente"} • $horaReporte")
                            )
                            
                            if (marker != null) {
                                markerReporteMap[marker] = reporteZona
                            }
                        }
                    }
                    reporteAdapter.notifyDataSetChanged()
                    Log.d("MapaFragment", "Cargados ${reportesList.size} reportes")
                }
                .addOnFailureListener { error ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e("MapaFragment", "Error cargando reportes", error)
                }
        }
    }

    private fun calcularDistancia(latUsuario: Double, lonUsuario: Double, latReporte: Double, lonReporte: Double): Float {
        val locUsuario = android.location.Location("point A").apply {
            latitude = latUsuario
            longitude = lonUsuario
        }
        val locReporte = android.location.Location("point B").apply {
            latitude = latReporte
            longitude = lonReporte
        }
        return locUsuario.distanceTo(locReporte)
    }

    private fun obtenerDireccion(latitud: Double, longitud: Double, context: Context): String {
        val geocoder = android.location.Geocoder(context, Locale.getDefault())
        var addressText = "Dirección no disponible"

        try {
            val addresses = geocoder.getFromLocation(latitud, longitud, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare ?: address.featureName
                val number = address.subThoroughfare

                addressText = if (street != null) {
                    if (number != null) "$street #$number" else street
                } else {
                    address.getAddressLine(0) ?: "Dirección no disponible"
                }
            }
        } catch (e: Exception) {
            Log.e("MapaFragment", "Error en geocodificación: ${e.message}")
        }
        return addressText
    }

    private fun formatTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(timestamp.toDate())
        } else {
            "Fecha/Hora no disponible"
        }
    }

    private fun getTimestampOffset(offsetMs: Long): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis() + offsetMs
        return Timestamp(calendar.time)
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        map.isMyLocationEnabled = true
        startLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }
}