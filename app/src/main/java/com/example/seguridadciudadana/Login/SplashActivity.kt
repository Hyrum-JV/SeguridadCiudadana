package com.example.seguridadciudadana.Login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.Admin.AdminDashboardActivity
import com.example.seguridadciudadana.Pin.CreatePinActivity
import com.example.seguridadciudadana.Pin.UnlockPinActivity
import com.example.seguridadciudadana.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                val rol = doc.getString("rol") ?: "user"

                if (rol == "admin") {
                    // ADMIN NUNCA PASA POR PIN
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                } else {
                    // SOLO USERS LLEGAN AQUÍ
                    if (doc.contains("pin")) {
                        startActivity(Intent(this, UnlockPinActivity::class.java))
                    } else {
                        startActivity(Intent(this, CreatePinActivity::class.java))
                    }
                    finish()
                }
            }
            .addOnFailureListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
    }
}
