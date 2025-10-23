package com.example.seguridadciudadana.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.seguridadciudadana.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Alerta de seguridad"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Revisa la noticia."

        val url = remoteMessage.data["url"]

        // Ajusta esta Activity a la que quieras abrir
        val intent = Intent(Intent.ACTION_VIEW).apply {
            // Si tienes una actividad propia de detalle, crea un Intent hacia ella y pásale la URL
            // setClass(this@MyFirebaseMessagingService, NewsDetailActivity::class.java)
            // putExtra("url", url)
            data = url?.let { android.net.Uri.parse(it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "seguridad_alerts")
            .setSmallIcon(R.drawable.ic_stat_alert) // crea un vector en res/drawable
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    override fun onNewToken(token: String) {
        // Envía tu token al backend si haces envíos directos por token
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "seguridad_alerts",
                "Alertas de seguridad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de robos/asaltos en Trujillo"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}