package com.example.seguridadciudadana

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ChatComunitario.Chat
import com.example.seguridadciudadana.ChatComunitario.Mensaje
import com.example.seguridadciudadana.ChatComunitario.MensajeAdapter
import com.example.seguridadciudadana.ChatComunitario.Miembro
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var btnEnviarImagen: ImageButton
    private lateinit var layoutAdmin: View
    private lateinit var btnAgregarMiembro: ImageButton
    private lateinit var btnEliminarMiembro: ImageButton
    private lateinit var toolbar: Toolbar

    private lateinit var mensajesAdapter: MensajeAdapter
    private val mensajesList = mutableListOf<Mensaje>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private lateinit var chatId: String
    private lateinit var chat: Chat

    private val REQUEST_MEDIA_PICK = 1001
    private val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_chat)

            chatId = intent.getStringExtra("chatId") ?: return
            title = intent.getStringExtra("chatNombre") ?: "Chat"

            rvMensajes = findViewById(R.id.rv_mensajes)
            etMensaje = findViewById(R.id.et_mensaje)
            btnEnviar = findViewById(R.id.btn_enviar)
            btnEnviarImagen = findViewById(R.id.btn_enviar_imagen)
            layoutAdmin = findViewById(R.id.layout_admin)
            btnAgregarMiembro = findViewById(R.id.btn_agregar_miembro)
            btnEliminarMiembro = findViewById(R.id.btn_eliminar_miembro)
            toolbar = findViewById(R.id.toolbar_chat)

            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }

            btnEnviar.setOnClickListener { enviarMensaje() }
            btnEnviarImagen.setOnClickListener { seleccionarMedia() }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("chats").document(chatId).get().addOnSuccessListener { doc ->
                    chat = doc.toObject(Chat::class.java) ?: return@addOnSuccessListener
                    if (!chat.esAdmin(currentUser.uid)) {
                        layoutAdmin.visibility = View.GONE
                    }
                    configurarRecyclerView()
                    cargarMensajes()
                    toolbar.setOnClickListener { mostrarDialogDetallesChat() }
                    btnAgregarMiembro.setOnClickListener { agregarMiembro() }
                    btnEliminarMiembro.setOnClickListener { eliminarMiembro() }
                }
            } else {
                configurarRecyclerView()
                cargarMensajes()
                toolbar.setOnClickListener { /* Nada */ }
                btnAgregarMiembro.setOnClickListener { /* Nada */ }
                btnEliminarMiembro.setOnClickListener { /* Nada */ }
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Crash en onCreate: ${e.message}", e)
            throw e
        }
    }

    private fun configurarRecyclerView() {
        mensajesAdapter = MensajeAdapter(mensajesList) { mensaje ->
            if (::chat.isInitialized && chat.esAdmin(auth.currentUser?.uid ?: "")) {
                db.collection("chats").document(chatId).collection("mensajes").document(mensaje.id).delete()
            }
        }
        rvMensajes.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = mensajesAdapter
        }
    }

    private fun cargarMensajes() {
        Log.d("ChatActivity", "Cargando mensajes para chatId: $chatId")
        db.collection("chats").document(chatId).collection("mensajes")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatActivity", "Error al cargar mensajes: ${e.message}", e)
                    return@addSnapshotListener
                }
                mensajesList.clear()
                snapshots?.forEach { doc ->
                    val mensaje = doc.toObject(Mensaje::class.java)?.copy(id = doc.id, tipo = doc.getString("tipo") ?: "text")
                    Log.d("ChatActivity", "Mensaje cargado: tipo=${mensaje?.tipo}, mediaUrl=${mensaje?.mediaUrl}")
                    mensaje?.let { mensajesList.add(it) }
                }
                mensajesAdapter.notifyDataSetChanged()
                if (rvMensajes.adapter != null) {
                    rvMensajes.scrollToPosition(mensajesList.size - 1)
                }
            }
    }

    private fun enviarMensaje(tipo: String = "text", mediaUrl: String? = null) {
        val texto = if (tipo == "text") etMensaje.text.toString().trim() else ""
        if (tipo == "text" && texto.isEmpty()) return
        val currentUser = auth.currentUser ?: return
        val mensajeData = hashMapOf(
            "texto" to texto,
            "remitente" to currentUser.uid,
            "timestamp" to System.currentTimeMillis(),
            "tipo" to tipo
        )
        if (mediaUrl != null) {
            mensajeData["mediaUrl"] = mediaUrl
        }
        db.collection("chats").document(chatId).collection("mensajes").add(mensajeData)
            .addOnSuccessListener { if (tipo == "text") etMensaje.text.clear() }
    }

    private fun seleccionarMedia() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION_READ_EXTERNAL_STORAGE)
        } else {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/* video/*"
            }
            startActivityForResult(intent, REQUEST_MEDIA_PICK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val mediaUri = data.data
            mediaUri?.let { subirMedia(it) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            seleccionarMedia()
        }
    }

    private fun subirMedia(mediaUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val mimeType = contentResolver.getType(mediaUri) ?: ""
        val tipo = if (mimeType.startsWith("image/")) "image" else if (mimeType.startsWith("video/")) "video" else return
        val fileName = "${System.currentTimeMillis()}_${currentUser.uid}"
        val ref = storage.child("evidencias/$fileName")
        ref.putFile(mediaUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    enviarMensaje(tipo = tipo, mediaUrl = uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error al subir media: ${e.message}", e)
            }
    }

    private fun agregarMiembro() {
        agregarMiembroDialog()
    }

    private fun eliminarMiembro() {
        val uidEliminar = "UID_A_ELIMINAR"
        if (!chat.miembrosUids.contains(uidEliminar) || uidEliminar == chat.creador) return
        val nuevosMiembros = chat.miembros.toMutableMap()
        nuevosMiembros.remove(uidEliminar)
        val nuevosUids = chat.miembrosUids.toMutableList()
        nuevosUids.remove(uidEliminar)
        db.collection("chats").document(chatId).update("miembros", nuevosMiembros, "miembrosUids", nuevosUids)
    }

    private fun mostrarDialogDetallesChat() {
        val miembrosNombres = mutableListOf<String>()
        val totalMiembros = chat.miembros.size
        var cargados = 0

        if (totalMiembros == 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(chat.nombre)
            builder.setMessage("Integrantes: Ninguno")
            if (chat.esAdmin(auth.currentUser?.uid ?: "")) {
                builder.setPositiveButton("Agregar Usuario") { _, _ -> agregarMiembroDialog() }
            }
            builder.setNegativeButton("Cerrar", null)
            builder.show()
            return
        }

        chat.miembros.forEach { (uid, miembro) ->
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: uid
                    miembrosNombres.add("$nombre (${miembro.rol})")
                    cargados++
                    if (cargados == totalMiembros) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(chat.nombre)
                        builder.setMessage("Integrantes:\n${miembrosNombres.joinToString("\n")}")
                        if (chat.esAdmin(auth.currentUser?.uid ?: "")) {
                            builder.setPositiveButton("Agregar Usuario") { _, _ -> agregarMiembroDialog() }
                        }
                        builder.setNegativeButton("Cerrar", null)
                        builder.show()
                    }
                }
                .addOnFailureListener {
                    miembrosNombres.add("$uid (${miembro.rol})")
                    cargados++
                    if (cargados == totalMiembros) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(chat.nombre)
                        builder.setMessage("Integrantes:\n${miembrosNombres.joinToString("\n")}")
                        if (chat.esAdmin(auth.currentUser?.uid ?: "")) {
                            builder.setPositiveButton("Agregar Usuario") { _, _ -> agregarMiembroDialog() }
                        }
                        builder.setNegativeButton("Cerrar", null)
                        builder.show()
                    }
                }
        }
    }

    private fun agregarMiembroDialog() {
        val currentUser = auth.currentUser ?: return
        db.collection("usuarios").document(currentUser.uid).collection("contactos").get()
            .addOnSuccessListener { docs ->
                val contactos = docs.map { it.id }
                val nombres = mutableListOf<String>()
                contactos.forEach { uid ->
                    db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
                        nombres.add(doc.getString("nombre") ?: uid)
                    }
                }
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Seleccionar Usuario")
                builder.setItems(nombres.toTypedArray()) { _, which ->
                    val uidSeleccionado = contactos[which]
                    agregarMiembro(uidSeleccionado)
                }
                builder.show()
            }
    }

    private fun agregarMiembro(uid: String) {
        if (chat.miembrosUids.contains(uid)) return
        val nuevosMiembros = chat.miembros.toMutableMap()
        nuevosMiembros[uid] = Miembro(uid, "member")
        val nuevosUids = chat.miembrosUids.toMutableList()
        nuevosUids.add(uid)
        db.collection("chats").document(chatId).update("miembros", nuevosMiembros, "miembrosUids", nuevosUids)
    }
}