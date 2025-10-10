package com.example.seguridadciudadana.Login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val user = auth.currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.contains("pin")) {
                        startActivity(Intent(this, UnlockPinActivity::class.java))
                    } else {
                        startActivity(Intent(this, CreatePinActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        }
    }
}