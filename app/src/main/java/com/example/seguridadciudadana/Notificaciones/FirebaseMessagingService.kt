    package com.example.seguridadciudadana.push

    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.app.PendingIntent
    import android.content.Context
    import android.content.Intent
    import android.os.Build
    import android.util.Log
    import androidx.core.app.NotificationCompat
    import androidx.core.app.NotificationManagerCompat
    import com.example.seguridadciudadana.MainActivity
    import com.example.seguridadciudadana.R
    import com.google.firebase.messaging.FirebaseMessagingService
    import com.google.firebase.messaging.RemoteMessage
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.Timestamp
    import com.google.firebase.auth.FirebaseAuth

    class MyFirebaseMessagingService : FirebaseMessagingService() {

        private val db by lazy { FirebaseFirestore.getInstance() }
        private val auth by lazy { FirebaseAuth.getInstance() }

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
            super.onNewToken(token)
            Log.d("FCMToken", "Nuevo Token: $token")

            // ✅ Llamar a la función para guardar el token en Firestore
            sendRegistrationToServer(token)
        }

        /**
         * Envía el nuevo token FCM a Firestore para el usuario actual.
         */
        private fun sendRegistrationToServer(token: String) {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                Log.w("FCMService", "Usuario no autenticado. No se puede guardar el token.")
                return
            }

            val tokenData = hashMapOf(
                "fcmToken" to token
            )

            // Usamos .set con merge para asegurarnos de que el documento exista o se actualice,
            // sin sobrescribir otros datos del usuario.
            db.collection("usuarios").document(currentUser.uid)
                .set(tokenData as Map<String, Any>, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.i("FCMService", "✅ Token FCM actualizado en Firestore para ${currentUser.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("FCMService", "❌ Error al actualizar token FCM en Firestore", e)
                }
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