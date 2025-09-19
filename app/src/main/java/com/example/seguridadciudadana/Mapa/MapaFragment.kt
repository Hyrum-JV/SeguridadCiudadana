package com.example.seguridadciudadana.Mapa

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource

class MapaFragment : Fragment(R.layout.fragment_mapa), OnMapReadyCallback {

    private var map: GoogleMap? = null

    private val resolverSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Usuario encendió ubicación → volvemos a pedirla
            obtenerUbicacionActual(conReintento = true)
        } else {
            Toast.makeText(requireContext(), "Activa la ubicación para centrar el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para pedir permisos en tiempo de ejecución
    private val pedirPermisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            val ok = res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (ok) habilitarMiUbicacion()
            else Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtén (o crea) el SupportMapFragment dentro del FragmentContainerView del XML
        val smf = (childFragmentManager.findFragmentById(R.id.mapFragmentContainer)
                as? SupportMapFragment) ?: SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction()
                .replace(R.id.mapFragmentContainer, it)
                .commitNow()
        }
        smf.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // UI básica del mapa
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        // Si la bottom bar tapa el botón de ubicación, da algo de padding inferior
        googleMap.setPadding(0, 0, 0, 80)

        verificarPermisos()
    }
    override fun onResume() {
        super.onResume()
        if (map != null) verificarPermisos()
    }
    private fun verificarPermisos() {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            verificarYObtenerUbicacion()
        } else {
            pedirPermisos.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }


    private fun habilitarMiUbicacion() {
        try { map?.isMyLocationEnabled = true } catch (_: SecurityException) {}
        obtenerUbicacionActual(conReintento = true)
    }

    private fun obtenerUbicacionActual(conReintento: Boolean) {
        val ctx = requireContext()
        val fused = LocationServices.getFusedLocationProviderClient(ctx)

        val tieneFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val priority = if (tieneFine)
            Priority.PRIORITY_HIGH_ACCURACY
        else
            Priority.PRIORITY_BALANCED_POWER_ACCURACY

        // 1) lastLocation primero
        try {
            fused.lastLocation.addOnSuccessListener { last ->
                if (last != null) {
                    moverCamara(last.latitude, last.longitude, if (tieneFine) 16f else 14f)
                } else {
                    // 2) Lectura fresca con timeout
                    val req = CurrentLocationRequest.Builder()
                        .setPriority(priority)
                        .setMaxUpdateAgeMillis(0)
                        .setGranularity(
                            if (tieneFine)
                                Granularity.GRANULARITY_FINE
                            else
                                Granularity.GRANULARITY_COARSE
                        )
                        .build()

                    val cts = CancellationTokenSource()
                    Handler(Looper.getMainLooper()).postDelayed({
                        cts.cancel()
                    }, 6000L)

                    fused.getCurrentLocation(req, cts.token).addOnSuccessListener { cur ->
                        if (cur != null) {
                            moverCamara(cur.latitude, cur.longitude, if (tieneFine) 16f else 14f)
                        } else {
                            if (conReintento) {
                                // un reintento
                                obtenerUbicacionActual(conReintento = false)
                            } else {
                                Toast.makeText(ctx, "No se pudo obtener tu ubicación actual", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        } catch (_: SecurityException) {}
    }
    private fun moverCamara(lat: Double, lon: Double, zoom: Float) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom)
        )
    }

    private fun verificarYObtenerUbicacion() {
        try { map?.isMyLocationEnabled = true } catch (_: SecurityException) {}

        val ctx = requireContext()
        val lr = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).build()

        val settingsReq = LocationSettingsRequest.Builder()
            .addLocationRequest(lr)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(ctx)
        settingsClient.checkLocationSettings(settingsReq)
            .addOnSuccessListener {
                obtenerUbicacionActual(conReintento = true)
            }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    // Abre el diálogo del sistema para encender GPS
                    try {
                        resolverSettings.launch(
                            IntentSenderRequest.Builder(e.resolution).build()
                        )
                    } catch (_: Exception) {}
                } else {
                    Toast.makeText(ctx, "Activa la ubicación del dispositivo", Toast.LENGTH_SHORT).show()
                }
            }
    }

}