package com.example.seguridadciudadana.Contactos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator

class EscanearQRActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicia el escáner de inmediato
        iniciarEscaneo()
    }

    private fun iniciarEscaneo() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea el código QR del contacto")
        integrator.setCameraId(0) // 0 = cámara trasera
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (resultCode == Activity.RESULT_OK && result.contents != null) {
                val uidEscaneado = result.contents
                agregarContacto(uidEscaneado)
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun agregarContacto(uidEscaneado: String) {
        val user = auth.currentUser ?: return

        db.collection("usuarios").document(uidEscaneado)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val contactoData = mapOf(
                        "nombre" to (documento.getString("nombre") ?: "Sin nombre"),
                        "correo" to (documento.getString("correo") ?: ""),
                        "telefono" to (documento.getString("telefono") ?: "")
                    )

                    db.collection("usuarios")
                        .document(user.uid)
                        .collection("contactos")
                        .document(uidEscaneado)
                        .set(contactoData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Contacto agregado correctamente", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al agregar contacto", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }
}
