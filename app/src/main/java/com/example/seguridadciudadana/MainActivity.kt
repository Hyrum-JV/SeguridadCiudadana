package com.example.seguridadciudadana

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        reemplazarFragmento(InicioFragment())

        // Listener de navegación inferior
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> reemplazarFragmento(InicioFragment())
                R.id.nav_mapa -> reemplazarFragmento(MapaFragment())
                R.id.nav_perfil -> reemplazarFragmento(PerfilFragment())
                R.id.nav_config -> reemplazarFragmento(ConfigFragment())
            }
            true
        }
    }
    private fun reemplazarFragmento(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor_fragmentos, fragmento)
            .commit()
    }
}
