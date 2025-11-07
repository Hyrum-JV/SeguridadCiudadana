package com.example.seguridadciudadana

import android.Manifest
import android.content.Context
import com.example.seguridadciudadana.Notificaciones.NotificacionesFragment
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import android.widget.ImageView
import com.example.seguridadciudadana.Configuraciones.ConfigFragment
import com.example.seguridadciudadana.Inicio.InicioFragment
import com.example.seguridadciudadana.Login.LoginActivity
import com.example.seguridadciudadana.Mapa.MapaFragment
import com.example.seguridadciudadana.Contactos.ContactosFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth

    // Navigation Drawer
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        verificarPermisosUbicacion()

        auth = FirebaseAuth.getInstance()


        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Configuración BottomNavigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor_fragmentos, InicioFragment())
                .commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    loadFragment(InicioFragment())
                    true // Return true to indicate the item was handled
                }

                R.id.nav_mapa -> {
                    loadFragment(MapaFragment())
                    true // Return true
                }

                R.id.nav_contactos -> {
                    loadFragment(ContactosFragment()) // Use your loadFragment function for consistency
                    true // Return true
                }

                R.id.nav_chats -> {
                    loadFragment(ChatsFragment())
                    true
                }
                else -> false // For any other case, return false
            }
        }

        // Configuración Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar: androidx.appcompat.widget.Toolbar? = findViewById(R.id.toolbar)
        val navigationView: NavigationView = findViewById(R.id.navigation_view)

        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> {
                    loadFragment(InicioFragment())
                    bottomNavigation.selectedItemId = R.id.nav_inicio
                }
                R.id.nav_configuraciones -> {
                    loadFragment(ConfigFragment())
                }
                R.id.nav_notificaciones -> {
                    loadFragment(NotificacionesFragment())
                }

                R.id.nav_acerca -> {
                    mostrarDialogoAcerca()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }


        // Perfil de usuario en el header del Navigation Drawer
        val user = FirebaseAuth.getInstance().currentUser
        val headerView = navigationView.getHeaderView(0)

        val tvNombre = headerView.findViewById<TextView>(R.id.tv_user_name)
        val tvCorreo = headerView.findViewById<TextView>(R.id.tv_user_email)
        val imgUser = headerView.findViewById<ImageView>(R.id.img_user)

        if (user == null) {
            tvNombre.text = "Invitado"
            tvCorreo.text = "No autenticado"
            imgUser.setImageResource(R.drawable.ic_person_placeholder)
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                val nombre = document.getString("nombre") ?: "Usuario"
                val correo = document.getString("correo") ?: user.email ?: "Sin correo"
                val fotoUrl = document.getString("fotoPerfil")

                tvNombre.text = nombre
                tvCorreo.text = correo

                val userId = user.uid
                val localFile = File(filesDir, "${userId}_perfil.jpg")

                if (localFile.exists()) {
                    // 🖼️ Cargar la imagen localmente guardada
                    Glide.with(this)
                        .load(localFile)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .into(imgUser)
                } else if (!fotoUrl.isNullOrEmpty()) {
                    // 🔗 Cargar imagen desde Firestore (si existiera)
                    Glide.with(this)
                        .load(fotoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .into(imgUser)
                } else {
                    imgUser.setImageResource(R.drawable.ic_person_placeholder)
                }
            }
            .addOnFailureListener {
                tvNombre.text = "Error al cargar"
                tvCorreo.text = user.email ?: "Sin correo"
            }

        val cardPerfil = headerView.findViewById<androidx.cardview.widget.CardView>(R.id.card_user)

        cardPerfil?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            loadFragment(com.example.seguridadciudadana.Perfil.PerfilFragment())
        }
        // --- Permiso de notificaciones (Android 13+) ---
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        //Verificar suscripción
        FirebaseMessaging.getInstance().subscribeToTopic("trujillo-seguridad")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful)
                    "✅ Suscrito correctamente al tema trujillo-seguridad"
                else
                    "❌ Error al suscribirse al tema"
                Log.d("FirebaseTopic", msg)
            }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token: ${task.result}")
            } else {
                Log.e("FCM_TOKEN", "No se pudo obtener token", task.exception)
            }
        }

        handleNotificationExtras(intent)
    }

    private fun verificarGPSActivo() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!gpsActivo) {
            AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Activa tu GPS para usar las funciones de ubicación.")
                .setPositiveButton("Activar") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
        } else {
            verificarGPSActivo()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor_fragmentos, fragment)
            .commit()

        // Ocultar o mostrar el BottomNavigationView según el fragmento
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        when (fragment) {
            is com.example.seguridadciudadana.Configuraciones.ConfigFragment,
            is com.example.seguridadciudadana.Perfil.PerfilFragment,
            is NotificacionesFragment,
                // NUEVO: Agrega ChatsFragment aquí para ocultar bottom nav
            is ChatsFragment -> {
                bottomNavigation.visibility = View.GONE
            }
            else -> {
                bottomNavigation.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarDialogoAcerca() {
        // CORRECCIÓN: Usar 'this.packageManager' para resolver la referencia.
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "1.0.0"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Acerca de la app")
            .setMessage(
                "Versión: $version\n\n" +
                        "App de seguridad ciudadana.\n" +
                        "• Envía mensaje y ubicación por WhatsApp.\n" +
                        "• Graba video corto configurable.\n\n" +
                        "Privacidad: los datos no se suben a servidores, se quedan en tu dispositivo."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                verificarGPSActivo()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleNotificationExtras(intent)
    }

    // 🎯 MODIFICADA: Usa los mismos campos que MyFirebaseMessagingService (snippet, imagen, noticias)
    private fun handleNotificationExtras(intent: Intent) {
        val extras = intent.extras ?: return

        // El título se puede tomar directamente del payload o de la notificación
        val title = extras.getString("title") ?: extras.getString("gcm.notification.title") ?: return

        // El cuerpo se usa como 'snippet'
        val body = extras.getString("body") ?: extras.getString("gcm.notification.body") ?: ""

        // Extraemos URL y Source como antes
        val url = extras.getString("url") ?: ""
        val source = extras.getString("source") ?: "Firebase"

        // Nuevo: Extraer URL de imagen si existe en el payload
        val imageUrl = extras.getString("imageUrl") ?: ""

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Mapeo a los campos esperados por la clase Noticia y NotificacionesFragment
        val alerta = hashMapOf(
            "titulo" to title,
            "snippet" to body, // ⬅️ CAMBIO: Usar 'snippet' en lugar de 'descripcion'
            "fuente" to source,
            "url" to url,
            "imagen" to imageUrl, // ⬅️ CAMBIO: Usar 'imagen' en lugar de 'urlImagen'
            "fecha_creacion" to com.google.firebase.Timestamp.now(), // ⬅️ CAMBIO: Usar 'fecha_creacion'
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Usar la colección 'noticias'
        db.collection("noticias_trujillo").add(alerta) // ⬅️ CAMBIO: Usar colección 'noticias'
            .addOnSuccessListener { android.util.Log.d("FCM_SAVE", "✅ Alerta persistente guardada: ${it.id}") }
            .addOnFailureListener { e -> android.util.Log.e("FCM_SAVE", "❌ Error guardando alerta persistente", e) }

        // ⚠️ Opcional: Si el usuario hace clic en una notificación que abre la app y esta ya guardó el dato,
        // podrías querer navegar a NotificacionesFragment automáticamente aquí.
        // loadFragment(NotificacionesFragment())
    }
}