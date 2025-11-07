package com.example.seguridadciudadana

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ChatComunitario.Miembro
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CrearChatActivity : AppCompatActivity() {

    private lateinit var etNombreChat: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var rvSeleccionarMiembros: RecyclerView
    private lateinit var btnCrearChat: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val miembrosSeleccionados = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_chat)

        etNombreChat = findViewById(R.id.et_nombre_chat)
        etDescripcion = findViewById(R.id.et_descripcion)
        rvSeleccionarMiembros = findViewById(R.id.rv_seleccionar_miembros)
        btnCrearChat = findViewById(R.id.btn_crear_chat)

        cargarContactosParaSeleccion()

        btnCrearChat.setOnClickListener {
            crearChat()
        }
    }

    private fun cargarContactosParaSeleccion() {
        val currentUser = auth.currentUser ?: return

        db.collection("usuarios")
            .document(currentUser.uid)
            .collection("contactos")
            .get()
            .addOnSuccessListener { docs ->
                val contactos = docs.map { it.id } // UIDs de contactos
                rvSeleccionarMiembros.apply {
                    layoutManager = LinearLayoutManager(this@CrearChatActivity)
                    adapter = SeleccionarMiembrosAdapter(contactos, miembrosSeleccionados)
                }
            }
    }

    private fun crearChat() {
        val nombre = etNombreChat.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa un nombre para el chat", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser ?: return

        // Crear mapa de miembros (creador + seleccionados)
        val miembrosMap = mutableMapOf<String, Miembro>()
        miembrosMap[currentUser.uid] = Miembro(currentUser.uid, "admin")
        miembrosSeleccionados.forEach { uid ->
            miembrosMap[uid] = Miembro(uid, "member")
        }

        val chatData = hashMapOf(
            "nombre" to nombre,
            "descripcion" to descripcion,
            "creador" to currentUser.uid,
            "fechaCreacion" to System.currentTimeMillis(),
            "miembros" to miembrosMap, // Mapa completo
            "miembrosUids" to (miembrosSeleccionados + currentUser.uid) // Para queries
        )

        db.collection("chats").add(chatData)
            .addOnSuccessListener { docRef ->
                Toast.makeText(this, "Chat creado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}