package com.example.seguridadciudadana

import android.content.Intent
import android.os.Bundle
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                        true
                    }

                    R.id.nav_mapa -> {
                        loadFragment(MapaFragment())
                        true
                    }

                    else -> false
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

        }

        private fun loadFragment(fragment: Fragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor_fragmentos, fragment)
                .commit()

            // Ocultar o mostrar el BottomNavigationView según el fragmento
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            when (fragment) {
                is com.example.seguridadciudadana.Configuraciones.ConfigFragment,
                is com.example.seguridadciudadana.Perfil.PerfilFragment -> {
                    bottomNavigation.visibility = View.GONE
                }
                else -> {
                    bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        private fun mostrarDialogoAcerca() {
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
}