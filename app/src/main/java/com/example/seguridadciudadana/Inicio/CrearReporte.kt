package com.example.seguridadciudadana.Inicio

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.seguridadciudadana.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File

class CrearReporte : Fragment() {

    private lateinit var spinnerCategoria: Spinner
    private lateinit var layoutCategoriaOtro: TextInputLayout
    private lateinit var etCategoriaOtro: TextInputEditText
    private lateinit var btnAdjuntar: Button
    private lateinit var imgPreview: ImageView
    private lateinit var btnEnviar: Button
    private lateinit var etDescripcion: TextInputEditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvUbicacion: TextView

    private var archivoAdjuntoUri: Uri? = null
    private var imagenBytes: ByteArray? = null
    private var tipoEvidencia: String? = null
    private var ubicacionActual: Location? = null

    private val REQUEST_TOMAR_FOTO = 101
    private val REQUEST_GRABAR_VIDEO = 102
    private val REQUEST_GALERIA = 103

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crear_reporte, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerCategoria = view.findViewById(R.id.spinner_categoria)
        layoutCategoriaOtro = view.findViewById(R.id.layout_categoria_otro)
        etCategoriaOtro = view.findViewById(R.id.et_categoria_otro)
        btnAdjuntar = view.findViewById(R.id.btn_adjuntar)
        imgPreview = view.findViewById(R.id.img_preview)
        btnEnviar = view.findViewById(R.id.btn_enviar)
        etDescripcion = view.findViewById(R.id.et_descripcion)
        tvUbicacion = view.findViewById(R.id.tv_ubicacion)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        configurarSpinner()
        obtenerUbicacion()

        btnAdjuntar.setOnClickListener { mostrarDialogAdjuntar() }
        btnEnviar.setOnClickListener { enviarReporte() }
    }

    private fun configurarSpinner() {
        val categorias = arrayOf("Robo a mano armada", "Asalto", "Vandalismo", "Otro")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                layoutCategoriaOtro.visibility = if (categorias[position] == "Otro") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            ubicacionActual = location
            tvUbicacion.text = if (location != null) "Ubicación: ${location.latitude}, ${location.longitude}" else "Ubicación no disponible"
        }
    }

    private fun mostrarDialogAdjuntar() {
        val opciones = arrayOf("Tomar Foto", "Grabar Video", "Seleccionar de Galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Adjuntar Evidencia")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> tomarFoto()
                    1 -> grabarVideo()
                    2 -> seleccionarDeGaleria()
                }
            }
            .show()
    }

    private fun tomarFoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQUEST_TOMAR_FOTO)
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_TOMAR_FOTO)
    }

    private fun grabarVideo() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQUEST_GRABAR_VIDEO)
            return
        }
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, REQUEST_GRABAR_VIDEO)
    }

    private fun seleccionarDeGaleria() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_GALERIA)
            return
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(intent, REQUEST_GALERIA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_TOMAR_FOTO -> {
                    val bitmap = data.extras?.get("data") as Bitmap
                    imgPreview.setImageBitmap(bitmap)
                    imgPreview.visibility = View.VISIBLE
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    imagenBytes = baos.toByteArray()
                    archivoAdjuntoUri = null
                    tipoEvidencia = "image"
                }
                REQUEST_GRABAR_VIDEO -> {
                    archivoAdjuntoUri = data.data
                    val thumbnail = ThumbnailUtils.createVideoThumbnail(archivoAdjuntoUri.toString(), MediaStore.Video.Thumbnails.MINI_KIND)
                    imgPreview.setImageBitmap(thumbnail)
                    imgPreview.visibility = View.VISIBLE
                    imagenBytes = null
                    tipoEvidencia = "video"
                }
                REQUEST_GALERIA -> {
                    archivoAdjuntoUri = data.data
                    val mimeType = requireContext().contentResolver.getType(archivoAdjuntoUri!!)
                    tipoEvidencia = if (mimeType?.startsWith("video/") == true) "video" else "image"
                    imgPreview.setImageURI(archivoAdjuntoUri)
                    imgPreview.visibility = View.VISIBLE
                    imagenBytes = null
                }
            }
        }
    }

    private fun enviarReporte() {
        val categoria = if (spinnerCategoria.selectedItem.toString() == "Otro") etCategoriaOtro.text.toString() else spinnerCategoria.selectedItem.toString()
        val descripcion = etDescripcion.text.toString()

        if (categoria.isEmpty() || ubicacionActual == null) {
            Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        subirArchivoYGuardarReporte(categoria, descripcion, ubicacionActual!!)
    }

    private fun subirArchivoYGuardarReporte(categoria: String, descripcion: String, ubicacion: Location) {
        val currentUser = auth.currentUser ?: return

        Log.d("CrearReporte", "Iniciando subida de reporte: categoria=$categoria, descripcion=$descripcion, uri=$archivoAdjuntoUri, bytes=${imagenBytes != null}")

        if (archivoAdjuntoUri == null && imagenBytes == null) {
            // Sin evidencia
            enviarMensajeAChats(categoria, descripcion, ubicacion, "")
            return
        }

        val fileName = "${System.currentTimeMillis()}_${currentUser.uid}"
        val ref = storage.child("evidencias/$fileName")

        val uploadTask = if (imagenBytes != null) {
            ref.putBytes(imagenBytes!!)
        } else {
            ref.putFile(archivoAdjuntoUri!!)
        }

        uploadTask.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                enviarMensajeAChats(categoria, descripcion, ubicacion, uri.toString())
            }
        }.addOnFailureListener { e ->
            Log.e("CrearReporte", "Error al subir archivo: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al subir evidencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarMensajeAChats(categoria: String, descripcion: String?, ubicacion: Location, evidenciaUri: String) {
        val currentUser = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        Log.d("CrearReporte", "Enviando mensaje a chats: categoria=$categoria")

        // Obtener chats donde el usuario es miembro
        db.collection("chats")
            .whereArrayContains("miembrosUids", currentUser.uid)
            .get()
            .addOnSuccessListener { chats ->
                val mensajeTexto = "🚨 Reporte: $categoria\nDescripción: ${descripcion ?: "Sin descripción"}\nUbicación: ${ubicacion.latitude}, ${ubicacion.longitude}"

                // Enviar un solo mensaje con texto y evidencia (si existe)
                val mensajeData = hashMapOf(
                    "texto" to mensajeTexto,
                    "remitente" to currentUser.uid,
                    "timestamp" to System.currentTimeMillis(),
                    "tipo" to (tipoEvidencia ?: "text")  // Si hay evidencia, tipo="image" o "video", sino "text"
                )
                if (tipoEvidencia != null) {
                    mensajeData["mediaUrl"] = evidenciaUri  // Agregar mediaUrl solo si hay evidencia
                }

                chats.forEach { chatDoc ->
                    val chatId = chatDoc.id
                    db.collection("chats").document(chatId).collection("mensajes").add(mensajeData)
                }
                Toast.makeText(requireContext(), "Mensaje enviado a ${chats.size()} chats", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("CrearReporte", "Error al enviar mensaje a chats: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error al enviar mensaje a chats", Toast.LENGTH_SHORT).show()
            }
    }
}