package com.example.seguridadciudadana.Registro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.R

class RolUsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rol_usuario)

        val btnAdministrador = findViewById<Button>(R.id.btnAdministrador)
        val btnCiudadano = findViewById<Button>(R.id.btnCiudadano)

        btnAdministrador.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("rol", "admin")
            startActivity(intent)
            finish()
        }

        btnCiudadano.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("rol", "user")
            startActivity(intent)
            finish()
        }
    }
}