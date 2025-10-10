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

class CreatePinActivity : AppCompatActivity() {

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

        // Cambiar el título para indicar que es creación
        tvTitulo.text = "Crear PIN de Seguridad"
        btnContinuar.text = "Guardar PIN"

        val user = auth.currentUser ?: run {
            finish()
            return
        }

        btnContinuar.setOnClickListener {
            val pin = editPin.text.toString().trim()

            if (pin.length != 4) {
                Toast.makeText(this, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!pin.all { it.isDigit() }) {
                Toast.makeText(this, "El PIN solo debe contener números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar PIN en Firestore
            db.collection("usuarios").document(user.uid)
                .update("pin", pin)
                .addOnSuccessListener {
                    Toast.makeText(this, "PIN creado correctamente", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar el PIN", Toast.LENGTH_SHORT).show()
                }
        }
    }
}