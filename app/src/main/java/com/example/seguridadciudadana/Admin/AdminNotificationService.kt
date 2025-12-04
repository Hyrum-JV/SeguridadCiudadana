package com.example.seguridadciudadana.Admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Servicio para escuchar nuevos reportes en tiempo real y notificar a los administradores
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

    /**
     * Inicia la escucha de nuevos reportes en tiempo real
     */
    fun startListening(context: Context) {
        if (isListening) return

        val currentUser = auth.currentUser ?: return

        // Verificar si es admin
        db.collection("usuarios").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val rol = doc.getString("rol") ?: ""
                if (rol == "admin" || rol == "policia") {
                    iniciarEscuchaReportes(context)
                }
            }
    }

    private fun iniciarEscuchaReportes(context: Context) {
        crearCanalNotificaciones(context)

        // Guardamos el timestamp actual para no notificar reportes antiguos
        lastReportTimestamp = System.currentTimeMillis()

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
                            val categoria = doc.getString("categoria") ?: "Reporte"
                            val descripcion = doc.getString("descripcion") ?: "Nuevo reporte recibido"
                            val reporteId = doc.id

                            mostrarNotificacionNuevoReporte(context, categoria, descripcion, reporteId)
                            lastReportTimestamp = timestamp
                        }
                    }
                }
            }

        isListening = true
        Log.d(TAG, "Escucha de reportes iniciada")
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
        reporteId: String
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("🚨 Nuevo Reporte: $categoria")
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
            Log.d(TAG, "Notificación mostrada para reporte: $reporteId")
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
