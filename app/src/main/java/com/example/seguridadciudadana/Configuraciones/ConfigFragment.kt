package com.example.seguridadciudadana.Configuraciones

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationManager
import android.util.Log

class ConfigFragment : Fragment() {

    // Keys para SharedPreferences
    private val PREFS_NAME = "AppPrefs"
    private val KEY_NOTIFICACIONES = "notificaciones_activas"
    private val KEY_UBICACION = "ubicacion_activa"

    private lateinit var switchNotificaciones: SwitchMaterial
    private lateinit var switchUbicacion: SwitchMaterial

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ConfigFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false) // Asegúrate que el nombre del layout es correcto
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchNotificaciones = view.findViewById(R.id.switchNotificaciones)
        switchUbicacion = view.findViewById(R.id.switchUbicacion)

        cargarPreferencias()
        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoSwitchesDesdeSistema()
    }

    private fun actualizarEstadoSwitchesDesdeSistema() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 1. Desactivar Listeners antes de actualizar el estado
        switchNotificaciones.setOnCheckedChangeListener(null)
        switchUbicacion.setOnCheckedChangeListener(null)

        // --- Lógica de Lectura del Sistema ---

        // 1. Comprobar Notificaciones
        val notificacionesHabilitadas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            // Fallback: usar el estado de SharedPreferences
            getSharedPreferences().getBoolean(KEY_NOTIFICACIONES, true)
        }
        switchNotificaciones.isChecked = notificacionesHabilitadas

        // 2. Comprobar Ubicación (GPS)
        val ubicacionHabilitada = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        switchUbicacion.isChecked = ubicacionHabilitada

        // --- Lógica de Re-activación ---

        // 3. Re-activar Listeners
        switchNotificaciones.setOnCheckedChangeListener(createNotificacionesListener())
        switchUbicacion.setOnCheckedChangeListener(createUbicacionListener())

        // 4. (Opcional) Guardar el estado real de la ubicación
        val prefsEditor = getSharedPreferences().edit()
        prefsEditor.putBoolean(KEY_UBICACION, ubicacionHabilitada).apply()
    }

    private fun getSharedPreferences() =
        requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- 1. Persistencia ---

    private fun cargarPreferencias() {
        val prefs = getSharedPreferences()

        // Cargar estado de Notificaciones (por defecto: true)
        val notificacionesActivas = prefs.getBoolean(KEY_NOTIFICACIONES, true)
        switchNotificaciones.isChecked = notificacionesActivas

        // Cargar estado de Ubicación (por defecto: true)
        val ubicacionActiva = prefs.getBoolean(KEY_UBICACION, true)
        switchUbicacion.isChecked = ubicacionActiva
    }

    // --- 2. Lógica de Switches ---

    private fun configurarListeners() {
        // Definimos los listeners fuera para poder removerlos/agregarlos
        val notificacionesListener = createNotificacionesListener()
        val ubicacionListener = createUbicacionListener()

        switchNotificaciones.setOnCheckedChangeListener(notificacionesListener)
        switchUbicacion.setOnCheckedChangeListener(ubicacionListener)
    }

    private fun abrirConfiguracionUbicacionSistema() {
        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun abrirConfiguracionNotificacionesApp() {
        val intent = Intent().apply {
            when {
                // Android 8.0 (Oreo) y superior: Abre la configuración de notificación específica de la app
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                // Versiones anteriores (hasta Android 5.0 Lollipop): Intenta abrir la info de la app
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP -> {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                }
                else -> {
                    action = android.provider.Settings.ACTION_SETTINGS
                }
            }
        }
        startActivity(intent)
    }

    private fun createNotificacionesListener(): (View, Boolean) -> Unit {
        return { _, isChecked ->
            abrirConfiguracionNotificacionesApp()
            // Forzar el switch a la posición opuesta para que el usuario
            // lo cambie manualmente en la configuración.
            switchNotificaciones.isChecked = !isChecked
        }
    }

    private fun createUbicacionListener(): (View, Boolean) -> Unit {
        return { _, isChecked ->
            abrirConfiguracionUbicacionSistema()

            // Aquí reintroducimos la lógica de eliminar datos si la ubicación se desactiva
            if (!isChecked) {
                eliminarDatosGeoGraficos()
            }

            // Forzar el switch a la posición opuesta
            switchUbicacion.isChecked = !isChecked
        }
    }
    private fun eliminarDatosGeoGraficos() {
        val userId = auth.currentUser?.uid ?: return

        val userRef = db.collection("usuarios").document(userId)

        // 1. Eliminar el token FCM (para que NO reciba alertas geográficas)
        userRef.update("fcmToken", null)
            .addOnSuccessListener {
                Log.d(TAG, "fcmToken eliminado de Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar fcmToken: ${e.message}")
            }

        // 2. Eliminar/anular la ubicación (para que NO aparezca en el mapa de otros)
        userRef.update("ubicacion", null)
            .addOnSuccessListener {
                Log.d(TAG, "Ubicación eliminada de Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar ubicación: ${e.message}")
            }

        // 3. (Opcional) Desactivar el servicio de seguimiento de ubicación si está corriendo
        // Necesitarías implementar una función para detener el servicio de ubicación aquí.
    }
}