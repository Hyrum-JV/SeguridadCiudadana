package com.example.seguridadciudadana.Mapa

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.seguridadciudadana.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.provider.Settings
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Centrar el mapa en Trujillo
        val trujillo = LatLng(-8.11599, -79.02998)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 13f))

        // Intentar activar la ubicación
        enableMyLocation()

        // Cargar los reportes
        cargarReportesEnMapa()

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow() // ✅ Muestra el cuadro con la categoría
            true // Evita que el mapa se mueva al presionar
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

    override fun onResume() {
        super.onResume()
        checkLocationEnabled()
    }

    // Cargar reportes en el mapa
    private fun cargarReportesEnMapa() {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val geo = document.getGeoPoint("ubicacion")
                    val categoria = document.getString("categoria") ?: "Sin categoría"

                    if (geo != null) {
                        val posicion = LatLng(geo.latitude, geo.longitude)

                        // Dibujar círculo verde
                        map.addCircle(
                            CircleOptions()
                                .center(posicion)
                                .radius(100.0) // Aumentado a 100 metros
                                .strokeColor(0xFF4CAF50.toInt()) // verde fuerte
                                .fillColor(0x554CAF50) // verde con transparencia
                                .strokeWidth(3f)
                        )
                        // Marker invisible con título
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(posicion)
                                .title(categoria)
                                .visible(true) // Muestra un icono encima del radio
                                .icon(null) // Sin ícono personalizado
                        )
                    }
                }
            }
            .addOnFailureListener {

            }
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
    }


    // Cuando el usuario responde al permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }
}
