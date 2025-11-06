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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.seguridadciudadana.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.provider.Settings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isFollowingUser = true

    private lateinit var spinnerFiltroTiempo: Spinner

    private val opcionesFiltro = listOf(
        "Última hora",
        "Últimas 12 horas",
        "Últimas 24 horas",
        "Últimos 7 días",
        "Todos los reportes"
    )

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

        createLocationRequest()

        // 💡 1. Inicializar Spinner
        spinnerFiltroTiempo = view.findViewById(R.id.spinnerFiltroTiempo) // ASUME que tienes este ID en fragment_mapa.xml

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcionesFiltro)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltroTiempo.adapter = adapter

        // Seleccionar la opción de "Último Día" por defecto
        spinnerFiltroTiempo.setSelection(0)

        // 💡 2. Configurar Listener del Spinner
        spinnerFiltroTiempo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Cuando el usuario selecciona un filtro, recargar los reportes
                cargarReportesEnMapa(opcionesFiltro[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada si no hay selección
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Centrar el mapa en Trujillo
        val trujillo = LatLng(-8.11599, -79.02998)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 13f))

        // Intentar activar la ubicación
        enableMyLocation()

        // 💡 Cargar reportes al iniciar (usa el valor por defecto: "Todos los reportes")
        cargarReportesEnMapa(opcionesFiltro[0])

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow() // ✅ Muestra el cuadro con la categoría
            true // Evita que el mapa se mueva al presionar
        }

        // Detener el seguimiento si el usuario interactúa manualmente con el mapa
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingUser = false
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Intervalo deseado para las actualizaciones (5 segundos)
            fastestInterval = 2500 // Intervalo más rápido (2.5 segundos)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Alta precisión
        }

        // Definir el callback que maneja las nuevas ubicaciones
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    if (isFollowingUser) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        // Mover la cámara a la nueva ubicación del usuario
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

    // --- Manejo de la Ubicación ---

    private fun startLocationUpdates(shouldRecenter: Boolean = false) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Re-habilitar el seguimiento de la cámara cuando el fragmento se reanuda
            isFollowingUser = true
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            // Lógica para centrar inmediatamente si se llama con shouldRecenter
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
        // 💡 Recargar reportes usando el filtro actual (al volver al fragmento)
        cargarReportesEnMapa(spinnerFiltroTiempo.selectedItem?.toString() ?: opcionesFiltro[0])
    }

    override fun onPause() {
        super.onPause()
        // Detener las actualizaciones de ubicación al pausar
        stopLocationUpdates()
    }

    // Cargar reportes en el mapa
    private fun cargarReportesEnMapa(filtro: String) {

        if (!::map.isInitialized || !::spinnerFiltroTiempo.isInitialized) {
            // Si el mapa O el spinner no están listos, no se puede continuar.
            // Salimos de la función para evitar el crash.
            return
        }

        // 1. Limpiar el mapa antes de cargar nuevos reportes
        map.clear()

        // 2. Determinar el límite de tiempo
        val tiempoLimite: Timestamp? = when (filtro) {
            // Usamos las nuevas etiquetas
            "Última hora" -> getTimestampOffset(-1 * 60 * 60 * 1000L)         // 1 hora
            "Últimas 12 horas" -> getTimestampOffset(-12 * 60 * 60 * 1000L)   // 12 horas
            "Últimas 24 horas" -> getTimestampOffset(-24 * 60 * 60 * 1000L)  // 24 horas (1 día)
            "Últimos 7 días" -> getTimestampOffset(-7 * 24 * 60 * 60 * 1000L) // 7 días
            else -> null // "Todos los reportes" o valor por defecto
        }

        // 3. Crear la consulta
        val db = FirebaseFirestore.getInstance()
        var query = db.collection("reportes").orderBy("timestamp") // ⚠️ Necesario si usas whereGreaterThan

        if (tiempoLimite != null) {
            query = query.whereGreaterThan("timestamp", tiempoLimite)
        }

        // 4. Ejecutar la consulta
        query.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val geo = document.getGeoPoint("ubicacion")
                    val categoria = document.getString("categoria") ?: "Sin categoría"
                    val timestamp = document.getTimestamp("timestamp")

                    if (geo != null) {
                        val posicion = LatLng(geo.latitude, geo.longitude)

                        // 💡 Nuevo: Formatear la fecha/hora
                        val horaReporte = formatTimestamp(timestamp)

                        // Dibujar círculo y marcador
                        map.addCircle(
                            CircleOptions()
                                .center(posicion)
                                .radius(100.0)
                                .strokeColor(0xFF4CAF50.toInt())
                                .fillColor(0x554CAF50)
                                .strokeWidth(3f)
                        )
                        map.addMarker(
                            MarkerOptions()
                                .position(posicion)
                                .title(categoria)
                                .snippet("Reporte: $horaReporte")
                                .visible(true)
                                .icon(null)
                        )
                    }
                }
            }
            .addOnFailureListener {
                // Manejo de error de Firestore
            }
    }

    //Para formatear el Timestamp a un string legible
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            // Define el formato que quieres (ej: 06/11/2025 18:02)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Opcional: Si quieres mostrar la hora local del usuario
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
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no hay permisos, pedirlos
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        // Si hay permisos, activar la ubicación
        map.isMyLocationEnabled = true
        startLocationUpdates()
    }


    // Cuando el usuario responde al permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }
}
