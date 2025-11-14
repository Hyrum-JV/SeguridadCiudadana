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
import android.widget.Spinner
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
import com.example.seguridadciudadana.Inicio.ReporteAdapter
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.os.Handler
import java.util.concurrent.TimeUnit

class MapaFragment : Fragment(), OnMapReadyCallback {

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

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Puedes agregar lógica aquí si la hoja se expande (STATE_EXPANDED) o colapsa (STATE_COLLAPSED)
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> Log.d("MapaFragment", "Bottom Sheet Expandido")
                    BottomSheetBehavior.STATE_COLLAPSED -> Log.d("MapaFragment", "Bottom Sheet Colapsado (Estado Normal)")
                    BottomSheetBehavior.STATE_HIDDEN -> Log.d("MapaFragment", "Bottom Sheet Oculto")
                    // Otros estados: DRAGGING, SETTLING
                    else -> {}
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Reacción visual mientras el usuario arrastra la hoja
            }
        })

        rvReportes = view.findViewById(R.id.rv_reportes_zona)
        reporteAdapter = ReporteAdapter(reportesList)
        rvReportes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reporteAdapter
            isNestedScrollingEnabled = true // Clave para que el arrastre funcione con el RecyclerView
        }

        createLocationRequest()
    }
    private fun iniciarLogicaMapa() {
        // 1. Siempre iniciamos las actualizaciones de ubicación
        startLocationUpdates()

        // 2. Cargamos los reportes la primera vez que se ejecuta o al volver
        cargarReportesEnMapa()

        // 3. Reiniciamos el ciclo de actualización de 6 horas
        iniciarActualizacionAutomatica()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Centrar el mapa en Trujillo
        val trujillo = LatLng(-8.11599, -79.02998)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 13f))

        // Intentar activar la ubicación
        enableMyLocation()

        iniciarLogicaMapa()

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

    private val actualizacionRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d("MapaFragment", "Actualizando reportes automáticamente (Últimas 6 horas)...")
            cargarReportesEnMapa()
            // Repetir después del intervalo
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
        iniciarActualizacionAutomatica()
    }

    override fun onPause() {
        super.onPause()
        // Detener las actualizaciones de ubicación al pausar
        stopLocationUpdates()
        detenerActualizacionAutomatica()
    }

    // Cargar reportes en el mapa
    private fun cargarReportesEnMapa() {

        if (!::map.isInitialized) {
            Log.w("MapaFragment", "Mapa no inicializado. Retrasando carga de reportes.")
            return
        }

        // 1. OBTENER LA UBICACIÓN DEL USUARIO
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            // No hay permisos, no podemos filtrar por cercanía
            Log.e("MapaFragment", "Permisos de ubicación no concedidos para filtrar.")
            // Opcional: puedes cargar todos los reportes aquí si lo deseas, o salir.
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            // Si no se pudo obtener la ubicación (location == null), salimos o cargamos todos.
            if (location == null) {
                Log.w("MapaFragment", "Ubicación del usuario no disponible. No se puede filtrar por cercanía.")
                // Aquí puedes llamar a una función que cargue sin filtro de ubicación si es necesario.
                return@addOnSuccessListener
            }

            // 2. Definir el radio de búsqueda (en metros)
            val radioMetros = 1000.0 // 1 kilómetro
            val latUsuario = location.latitude
            val lonUsuario = location.longitude

            // Limpiar el mapa y la lista antes de cargar nuevos reportes
            map.clear()
            reportesList.clear()
            reporteAdapter.notifyDataSetChanged()

            // 3. Consulta de Firebase (Mantenemos el filtro de 6 horas)
            val tiempoLimite: Timestamp = getTimestampOffset(-6 * 60 * 60 * 1000L)
            val db = FirebaseFirestore.getInstance()

            db.collection("reportes")
                .whereGreaterThan("timestamp", tiempoLimite)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->

                    for (document in result) {
                        val geo = document.getGeoPoint("ubicacion")

                        if (geo != null) {
                            // 4. CALCULAR DISTANCIA Y FILTRAR
                            val distancia = calcularDistancia(latUsuario, lonUsuario, geo.latitude, geo.longitude)

                            if (distancia <= radioMetros) {
                                // ✅ El reporte está DENTRO del radio de 1km
                                val direccionReporte = obtenerDireccion(geo.latitude, geo.longitude, requireContext())

                                val categoria = document.getString("categoria") ?: "Sin categoría"
                                val timestamp = document.getTimestamp("timestamp")
                                val descripcion = document.getString("descripcion")
                                val evidenciaUrl = document.getString("evidenciaUrl")
                                val posicion = LatLng(geo.latitude, geo.longitude)

                                val reporteZona = ReporteZona(
                                    id = document.id,
                                    categoria = categoria,
                                    ubicacion = geo,
                                    descripcion = descripcion,
                                    evidenciaUrl = evidenciaUrl,
                                    timestamp = timestamp,
                                    direccion = direccionReporte
                                )
                                reportesList.add(reporteZona)

                                // 💡 Opcional: Dibujar un círculo en la ubicación del reporte
                                map.addCircle(
                                    CircleOptions()
                                        .center(posicion).radius(100.0)
                                        .strokeColor(0xFF4CAF50.toInt())
                                        .fillColor(0x554CAF50)
                                        .strokeWidth(3f)
                                )

                                val horaReporte = formatTimestamp(timestamp)
                                map.addMarker(
                                    MarkerOptions()
                                        .position(posicion)
                                        .title(categoria)
                                        .snippet("Reporte: $horaReporte")
                                )
                            }
                        }
                    }
                    // 5. Notificar al adaptador (Solo con los reportes filtrados)
                    reporteAdapter.notifyDataSetChanged()
                    Log.d("MapaFragment", "Cargados ${reportesList.size} reportes dentro de ${radioMetros}m.")
                }
                .addOnFailureListener {
                    Log.e("MapaFragment", "Error cargando reportes", it)
                }
        }
    }

    /**
     * Calcula la distancia en metros entre la ubicación del usuario y el reporte.
     * Utilizamos un objeto Location temporal para aprovechar la función distanceTo.
     */
    private fun calcularDistancia(
        latUsuario: Double, lonUsuario: Double,
        latReporte: Double, lonReporte: Double
    ): Float {
        val locUsuario = android.location.Location("point A").apply {
            latitude = latUsuario
            longitude = lonUsuario
        }
        val locReporte = android.location.Location("point B").apply {
            latitude = latReporte
            longitude = lonReporte
        }
        // distanceTo devuelve la distancia en metros
        return locUsuario.distanceTo(locReporte)
    }

    //Obtener la dirección exacta del reporte que fue subido
    private fun obtenerDireccion(latitud: Double, longitud: Double, context: Context): String {
        val geocoder = android.location.Geocoder(context, Locale.getDefault())
        var addressText = "Dirección no disponible"

        try {
            // Obtenemos una lista de direcciones para las coordenadas
            val addresses = geocoder.getFromLocation(latitud, longitud, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // Intentamos obtener el nombre de la calle y el número (si existe)
                val street = address.thoroughfare ?: address.featureName // featureName es una alternativa común
                val number = address.subThoroughfare

                addressText = if (street != null) {
                    if (number != null) "$street #$number" else street
                } else {
                    // Si la calle no está disponible, usamos la ubicación más general
                    address.getAddressLine(0) ?: "Dirección no disponible"
                }
            }
        } catch (e: Exception) {
            Log.e("MapaFragment", "Error en geocodificación: ${e.message}")
            // Si hay error de red o I/O, mantenemos el texto de error
        }
        return addressText
    }

    //Para formatear el Timestamp a un string legible
    private fun formatTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Mostrar hora local del usuario
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
