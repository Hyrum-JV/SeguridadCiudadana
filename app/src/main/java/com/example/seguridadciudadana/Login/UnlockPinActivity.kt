package com.example.seguridadciudadana.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.MainActivity
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UnlockPinActivity : AppCompatActivity() {

    private lateinit var tvTitulo: TextView
    private lateinit var editPin: EditText
    private lateinit var btnContinuar: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        tvTitulo = findViewById(R.id.tvTitulo)
        editPin = findViewById(R.id.editPin)
        btnContinuar = findViewById(R.id.btnContinuar)

        tvTitulo.text = "Ingresar PIN"
        btnContinuar.text = "Desbloquear"

        val user = auth.currentUser ?: run {
            finish()
            return
        }

        btnContinuar.setOnClickListener {
            val pinIngresado = editPin.text.toString().trim()

            if (pinIngresado.length != 4) {
                Toast.makeText(this, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar PIN en Firestore
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val pinGuardado = document.getString("pin")

                    if (pinGuardado == null) {
                        Toast.makeText(this, "No se encontró un PIN registrado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (pinGuardado == pinIngresado) {
                        Toast.makeText(this, "PIN correcto", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                        editPin.text.clear()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al verificar el PIN", Toast.LENGTH_SHORT).show()
                }
        }
    }
}