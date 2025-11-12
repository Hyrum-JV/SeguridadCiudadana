package com.example.seguridadciudadana

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ChatComunitario.Mensaje
import com.example.seguridadciudadana.ChatComunitario.MensajeAdapter  // Importa MensajeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageButton

    private lateinit var mensajesAdapter: MensajeAdapter  // Cambia a MensajeAdapter
    private val mensajesList = mutableListOf<Mensaje>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getStringExtra("chatId") ?: return
        title = intent.getStringExtra("chatNombre") ?: "Chat"

        rvMensajes = findViewById(R.id.rv_mensajes)
        etMensaje = findViewById(R.id.et_mensaje)
        btnEnviar = findViewById(R.id.btn_enviar)

        configurarRecyclerView()
        cargarMensajes()

        btnEnviar.setOnClickListener {
            enviarMensaje()
        }
    }

    private fun configurarRecyclerView() {
        mensajesAdapter = MensajeAdapter(mensajesList)  // Cambia a MensajeAdapter
        rvMensajes.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = mensajesAdapter
        }
    }

    private fun cargarMensajes() {
        db.collection("chats").document(chatId).collection("mensajes")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                mensajesList.clear()
                snapshots?.forEach { doc ->
                    val mensaje = doc.toObject(Mensaje::class.java).copy(id = doc.id)
                    mensajesList.add(mensaje)
                }

                mensajesAdapter.notifyDataSetChanged()
                rvMensajes.scrollToPosition(mensajesList.size - 1)
            }
    }

    private fun enviarMensaje() {
        val texto = etMensaje.text.toString().trim()
        if (texto.isEmpty()) return

        val currentUser = auth.currentUser ?: return

        val mensajeData = hashMapOf(
            "texto" to texto,
            "remitente" to currentUser.uid,
            "timestamp" to System.currentTimeMillis(),
            "tipo" to "text"
        )

        db.collection("chats").document(chatId).collection("mensajes")
            .add(mensajeData)
            .addOnSuccessListener {
                etMensaje.text.clear()
            }
    }
}