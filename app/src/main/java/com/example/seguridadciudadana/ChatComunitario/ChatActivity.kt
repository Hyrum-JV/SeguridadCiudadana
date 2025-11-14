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
import android.widget.Toast
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

            configurarRecyclerView() // Configurar el RecyclerView temprano

            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("chats").document(chatId).get().addOnSuccessListener { doc ->

                    // 1. Inicializar el objeto chat
                    chat = doc.toObject(Chat::class.java) ?: return@addOnSuccessListener

                    // 2. Verificar el rol *después* de que 'chat' se carga
                    val esAdmin = chat.esAdmin(currentUser.uid)

                    // 3. Configurar visibilidad del panel de administración
                    if (esAdmin) {
                        layoutAdmin.visibility = View.VISIBLE // MUESTRA los botones
                    } else {
                        layoutAdmin.visibility = View.GONE    // OCULTA los botones
                    }

                    // 4. Configurar listeners que dependen del chat/rol
                    cargarMensajes()
                    toolbar.setOnClickListener { mostrarDialogDetallesChat() }

                    // 5. Configurar botones de administración
                    btnAgregarMiembro.setOnClickListener { agregarMiembroDialog() } // Usar Dialog
                    btnEliminarMiembro.setOnClickListener { eliminarMiembroDialog() } // Usar Dialog

                }.addOnFailureListener { e ->
                    Log.e("ChatActivity", "Error al cargar datos del chat: ${e.message}", e)
                    // Opcional: Mostrar un Toast de error
                }
            } else {
                // Lógica para usuario no autenticado (solo cargar mensajes)
                cargarMensajes()
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

    private fun ejecutarEliminacionMiembro(uidEliminar: String) {
        if (!::chat.isInitialized || !chat.miembrosUids.contains(uidEliminar)) return

        // Prevenir eliminar al creador
        if (uidEliminar == chat.creador) {
            Toast.makeText(this, "No puedes eliminar al creador del chat.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Crear los nuevos mapas sin el miembro
        val nuevosMiembros = chat.miembros.toMutableMap()
        nuevosMiembros.remove(uidEliminar) // Elimina del mapa de roles

        val nuevosUids = chat.miembrosUids.toMutableList()
        nuevosUids.remove(uidEliminar) // Elimina de la lista de UIDs

        // 2. Actualizar Firestore
        db.collection("chats").document(chatId)
            .update(
                "miembros", nuevosMiembros,
                "miembrosUids", nuevosUids
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Miembro eliminado exitosamente.", Toast.LENGTH_SHORT).show()

                // Opcional: Refrescar el objeto 'chat' local para mantener el estado
                chat = chat.copy(miembros = nuevosMiembros, miembrosUids = nuevosUids)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar miembro: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                val contactos = docs.map { it.id } // Lista de UIDs
                val contactosConNombre = mutableMapOf<String, String>() // Mapa: UID -> Nombre

                // Si no hay contactos, mostrar mensaje y salir
                if (contactos.isEmpty()) {
                    Toast.makeText(this, "No tienes contactos para agregar.", Toast.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
                }

                var cargados = 0
                val totalContactos = contactos.size

                // Función para verificar si todos han cargado y mostrar el diálogo
                val verificarYMostrarDialog = {
                    if (cargados == totalContactos) {
                        mostrarDialogoSeleccion(contactos, contactosConNombre)
                    }
                }

                contactos.forEach { uid ->
                    // 1. Obtener el nombre de cada contacto
                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val nombre = doc.getString("nombre") ?: "Usuario desconocido ($uid)"
                            contactosConNombre[uid] = nombre
                            cargados++
                            verificarYMostrarDialog() // Verificar después de cada éxito
                        }
                        .addOnFailureListener {
                            // Si falla, usar el UID como nombre de respaldo
                            contactosConNombre[uid] = "Error al cargar ($uid)"
                            cargados++
                            verificarYMostrarDialog() // Verificar también si falla
                        }
                }
            }
    }

    private fun mostrarDialogoSeleccion(contactosUids: List<String>, contactosConNombre: Map<String, String>) {
        // 1. Obtener los nombres para el AlertDialog (manteniendo el orden de contactosUids)
        val nombresParaMostrar = contactosUids.map { uid ->
            contactosConNombre[uid] ?: uid // Usar nombre cargado o fallback a UID
        }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar Usuario")

        builder.setItems(nombresParaMostrar) { _, which ->
            // 2. Usar la lista original de UIDs para obtener el UID correcto
            val uidSeleccionado = contactosUids[which]
            agregarMiembro(uidSeleccionado)
        }
        builder.show()
    }

    private fun eliminarMiembroDialog() {
        val currentUser = auth.currentUser ?: return

        // 1. Verificar si el chat está inicializado y si el usuario es admin
        if (!::chat.isInitialized || !chat.esAdmin(currentUser.uid)) {
            Toast.makeText(this, "Solo los administradores pueden eliminar miembros.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Filtrar miembros: excluimos al creador (admin) y al propio usuario por seguridad
        val miembrosParaEliminar = chat.miembrosUids
            .filter { uid -> uid != chat.creador }
            .filter { uid -> uid != currentUser.uid } // No permitimos que se auto-elimine

        if (miembrosParaEliminar.isEmpty()) {
            Toast.makeText(this, "No hay miembros elegibles para eliminar.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Crear listas para el diálogo
        val nombresMiembros = mutableListOf<String>()
        val uidsMiembros = mutableListOf<String>()

        var cargados = 0
        val totalMiembros = miembrosParaEliminar.size

        val mostrarDialogo = {
            if (cargados == totalMiembros) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Eliminar Miembro")

                builder.setItems(nombresMiembros.toTypedArray()) { _, which ->
                    val uidSeleccionado = uidsMiembros[which]
                    confirmarEliminarMiembro(uidSeleccionado, nombresMiembros[which])
                }
                builder.setNegativeButton("Cancelar", null)
                builder.show()
            }
        }

        // 4. Cargar nombres de los miembros (lógica asíncrona con contador)
        miembrosParaEliminar.forEach { uid ->
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: "Usuario desconocido ($uid)"
                    nombresMiembros.add(nombre)
                    uidsMiembros.add(uid)
                    cargados++
                    mostrarDialogo()
                }
                .addOnFailureListener {
                    // En caso de fallo, usamos el UID
                    nombresMiembros.add("Error al cargar ($uid)")
                    uidsMiembros.add(uid)
                    cargados++
                    mostrarDialogo()
                }
        }
    }

    private fun confirmarEliminarMiembro(uid: String, nombre: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a $nombre del chat?")
            .setPositiveButton("Eliminar") { _, _ ->
                ejecutarEliminacionMiembro(uid)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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