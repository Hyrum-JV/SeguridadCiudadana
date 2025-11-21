package com.example.seguridadciudadana.Login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.Admin.AdminDashboardActivity
import com.example.seguridadciudadana.Pin.CreatePinActivity
import com.example.seguridadciudadana.Pin.UnlockPinActivity
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.Registro.RolUsuarioActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        FirebaseApp.initializeApp(this) // Asegura que Firebase esté listo

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        if (user == null) {
            // Usuario no autenticado → pantalla de selección de rol
            startActivity(Intent(this, RolUsuarioActivity::class.java))
            finish()
        } else {
            // Usuario autenticado → verificar rol y PIN
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val rol = document.getString("rol") ?: "user"

                        when (rol) {
                            "admin" -> {
                                // Si es admin, va directo al dashboard
                                startActivity(Intent(this, AdminDashboardActivity::class.java))
                                finish()
                            }
                            else -> {
                                // Si es usuario normal, verificar PIN
                                if (document.contains("pin")) {
                                    startActivity(Intent(this, UnlockPinActivity::class.java))
                                } else {
                                    startActivity(Intent(this, CreatePinActivity::class.java))
                                }
                                finish()
                            }
                        }
                    } else {
                        // Documento no existe → crear PIN
                        startActivity(Intent(this, CreatePinActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    // Error al obtener datos → pantalla de selección de rol
                    startActivity(Intent(this, RolUsuarioActivity::class.java))
                    finish()
                }
        }
    }
}
