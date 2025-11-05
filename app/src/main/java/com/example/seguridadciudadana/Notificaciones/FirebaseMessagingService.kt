    package com.example.seguridadciudadana.push

    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.app.PendingIntent
    import android.content.Context
    import android.content.Intent
    import android.os.Build
    import androidx.core.app.NotificationCompat
    import androidx.core.app.NotificationManagerCompat
    import com.example.seguridadciudadana.MainActivity
    import com.example.seguridadciudadana.R
    import com.google.firebase.messaging.FirebaseMessagingService
    import com.google.firebase.messaging.RemoteMessage
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.Timestamp

    class MyFirebaseMessagingService : FirebaseMessagingService() {

        private val db by lazy { FirebaseFirestore.getInstance() }

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

            // Datos adicionales esperados del payload (si los envías)
            val url = remoteMessage.data["url"] ?: ""
            val source = remoteMessage.data["source"] ?: "Firebase"
            val imageUrl = remoteMessage.data["imageUrl"] ?: "" // Asumiendo que el campo para la imagen se llama 'imageUrl'

            // 1) Guardar para que luego aparezca en el listado
            val alerta = hashMapOf(
                "titulo" to title,
                // Usamos 'snippet' para coincidir con la clase Noticia
                "snippet" to body,
                "fuente" to source,
                "url" to url,
                // Usamos 'imagen' para coincidir con la clase Noticia
                "imagen" to imageUrl,
                // Usamos 'fecha_creacion' como campo principal de ordenamiento
                "fecha_creacion" to Timestamp.now(),
                "createdAt" to Timestamp.now()
            )
            // Usamos la colección 'noticias' para ser más claros, pero si ya usabas 'alertas' puedes mantenerlo
            db.collection("noticias").add(alerta)

            // 2) Abrir SOLO la app (no deep-link).
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "seguridad_alerts")
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            with(NotificationManagerCompat.from(this)) {
                // Usa un ID único para que no se sobrescriban las notificaciones
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
                    description = "Notificaciones de seguridad ciudadana en Trujillo"
                    enableVibration(true)
                }
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
            }
        }
    }