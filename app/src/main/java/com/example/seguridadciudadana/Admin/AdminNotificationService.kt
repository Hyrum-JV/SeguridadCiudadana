package com.example.seguridadciudadana.Admin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.seguridadciudadana.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Servicio para escuchar nuevos reportes en tiempo real y notificar a los administradores
 * Solo notifica reportes dentro del radio de cobertura configurado por el admin
 */
object AdminNotificationService {

    private const val TAG = "AdminNotificationService"
    private const val CHANNEL_ID = "admin_new_reports"
    private const val CHANNEL_NAME = "Nuevos Reportes"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var reportesListener: ListenerRegistration? = null
    private var isListening = false
    private var lastReportTimestamp: Long = 0
    private var ubicacionAdmin: Location? = null
    private var appContext: Context? = null

    /**
     * Obtiene el radio de cobertura desde las preferencias
     */
    private fun getRadioCobertura(): Double {
        return appContext?.let { AdminPreferences.getRadioCoberturaDouble(it) } 
            ?: AdminPreferences.DEFAULT_RADIO_KM.toDouble()
    }

    /**
     * Inicia la escucha de nuevos reportes en tiempo real
     */
    fun startListening(context: Context) {
        if (isListening) return

        appContext = context.applicationContext
        val currentUser = auth.currentUser ?: return

        // Verificar si es admin
        db.collection("usuarios").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val rol = doc.getString("rol") ?: ""
                if (rol == "admin" || rol == "policia") {
                    // Primero obtener ubicación, luego iniciar escucha
                    obtenerUbicacionYEscuchar(context)
                }
            }
    }

    private fun obtenerUbicacionYEscuchar(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Sin permisos, escuchar sin filtro de ubicación
            iniciarEscuchaReportes(context)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                ubicacionAdmin = location
                iniciarEscuchaReportes(context)
            }
            .addOnFailureListener {
                // Si falla, intentar con última ubicación conocida
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    ubicacionAdmin = lastLocation
                    iniciarEscuchaReportes(context)
                }
            }
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
     * Verifica si un reporte está dentro del radio de cobertura
     */
    private fun estaEnRadioCobertura(geoPoint: GeoPoint?): Boolean {
        val ubicacion = ubicacionAdmin ?: return true // Si no hay ubicación, permitir todos
        
        if (geoPoint == null) return false

        val distancia = calcularDistanciaKm(
            ubicacion.latitude,
            ubicacion.longitude,
            geoPoint.latitude,
            geoPoint.longitude
        )

        return distancia <= getRadioCobertura()
    }

    private fun iniciarEscuchaReportes(context: Context) {
        crearCanalNotificaciones(context)

        // Guardamos el timestamp actual para no notificar reportes antiguos
        lastReportTimestamp = System.currentTimeMillis()
        val radioActual = getRadioCobertura()

        reportesListener = db.collection("reportes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando reportes", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0

                        // Solo notificar si es un reporte nuevo (después de iniciar la escucha)
                        if (timestamp > lastReportTimestamp - 5000) {
                            // Verificar si el reporte está dentro del radio de cobertura
                            val geoPoint = doc.getGeoPoint("ubicacion")
                            
                            if (estaEnRadioCobertura(geoPoint)) {
                                val categoria = doc.getString("categoria") ?: "Reporte"
                                val descripcion = doc.getString("descripcion") ?: "Nuevo reporte recibido"
                                val reporteId = doc.id

                                // Calcular distancia para mostrar en la notificación
                                val distanciaTexto = if (ubicacionAdmin != null && geoPoint != null) {
                                    val dist = calcularDistanciaKm(
                                        ubicacionAdmin!!.latitude,
                                        ubicacionAdmin!!.longitude,
                                        geoPoint.latitude,
                                        geoPoint.longitude
                                    )
                                    String.format("(%.1f km)", dist)
                                } else ""

                                mostrarNotificacionNuevoReporte(context, categoria, descripcion, reporteId, distanciaTexto)
                            } else {
                                Log.d(TAG, "Reporte fuera del radio de cobertura (${getRadioCobertura()}km), no se notifica")
                            }
                            
                            lastReportTimestamp = timestamp
                        }
                    }
                }
            }

        isListening = true
        Log.d(TAG, "Escucha de reportes iniciada (radio: ${radioActual}km)")
    }

    /**
     * Detiene la escucha de reportes
     */
    fun stopListening() {
        reportesListener?.remove()
        reportesListener = null
        isListening = false
        Log.d(TAG, "Escucha de reportes detenida")
    }

    private fun mostrarNotificacionNuevoReporte(
        context: Context,
        categoria: String,
        descripcion: String,
        reporteId: String,
        distanciaTexto: String = ""
    ) {
        // Intent para abrir el dashboard y mostrar el reporte
        val intent = Intent(context, AdminDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reporteId", reporteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reporteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = if (distanciaTexto.isNotEmpty()) {
            "🚨 Nuevo Reporte $distanciaTexto: $categoria"
        } else {
            "🚨 Nuevo Reporte: $categoria"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(titulo)
            .setContentText(descripcion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(descripcion))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                reporteId.hashCode(),
                notification
            )
            Log.d(TAG, "Notificación mostrada para reporte: $reporteId $distanciaTexto")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para mostrar notificaciones", e)
        }
    }

    private fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos reportes ciudadanos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Suscribe al admin al topic de notificaciones de reportes
     */
    fun suscribirATopicReportes() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("admin_reportes")
            .addOnSuccessListener {
                Log.d(TAG, "Suscrito al topic admin_reportes")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al suscribirse al topic", e)
            }
    }

    /**
     * Desuscribe del topic de notificaciones
     */
    fun desuscribirDeTopicReportes() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .unsubscribeFromTopic("admin_reportes")
            .addOnSuccessListener {
                Log.d(TAG, "Desuscrito del topic admin_reportes")
            }
    }
}
