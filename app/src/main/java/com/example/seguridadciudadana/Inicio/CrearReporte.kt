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
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.content.Context
import android.os.Build
import java.util.Locale

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

    private val CHANNEL_ID = "report_upload_channel"
    private val NOTIFICATION_ID = 1

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
        // 1. Verificar permisos
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        // 2. Obtener la ubicación
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            ubicacionActual = location

            if (location != null) {
                // ✅ CLAVE: Usar la geocodificación para obtener la dirección
                val direccion = obtenerDireccion(location)
                tvUbicacion.text = direccion // Muestra la calle
            } else {
                // Ubicación no disponible
                tvUbicacion.text = "Ubicación no disponible"
            }
        }
    }

    private fun mostrarDialogAdjuntar() {
        val opciones = arrayOf("Tomar Foto", "Grabar Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Adjuntar Evidencia")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> tomarFoto()
                    1 -> grabarVideo()
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

    private fun mostrarNotificacionExito(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Crear el canal de notificación (Necesario para API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Reportes"
            val descriptionText = "Notificaciones sobre el estado de los reportes enviados."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Construir la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_app) // Reemplaza ic_notification por un ícono real
            .setContentTitle("✅ Reporte Enviado Correctamente")
            .setContentText("Tu reporte de seguridad ha sido procesado y distribuido.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Se cierra al tocarla

        // 3. Disparar la notificación
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }



    private fun subirArchivoYGuardarReporte(categoria: String, descripcion: String, ubicacion: Location) {
        val currentUser = auth.currentUser ?: return

        // Caso 1: SIN evidencia. Se llama al guardado principal con URI vacía.
        if (archivoAdjuntoUri == null && imagenBytes == null) {
            guardarReportePrincipal(categoria, descripcion, ubicacion, "")
            return
        }

        // Caso 2: CON evidencia. Subir a Storage.
        val fileName = "${System.currentTimeMillis()}_${currentUser.uid}"
        val ref = storage.child("evidencias/$fileName")

        val uploadTask = if (imagenBytes != null) {
            ref.putBytes(imagenBytes!!)
        } else {
            ref.putFile(archivoAdjuntoUri!!)
        }

        uploadTask.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                guardarReportePrincipal(categoria, descripcion, ubicacion, uri.toString())
            }
        }.addOnFailureListener { e ->
            Log.e("CrearReporte", "Error al subir archivo: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al subir evidencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarReportePrincipal(categoria: String, descripcion: String?, ubicacion: Location, evidenciaUri: String) {
        val currentUser = auth.currentUser ?: return

        val reporteData = hashMapOf(
            "userId" to currentUser.uid,
            "categoria" to categoria,
            "descripcion" to descripcion,
            "evidenciaUrl" to evidenciaUri,
            "tipoEvidencia" to (tipoEvidencia ?: "none"),
            "ubicacion" to GeoPoint(ubicacion.latitude, ubicacion.longitude),
            "timestamp" to Timestamp.now(),
            "estado" to "Pendiente", 
            "adminComentario" to "",
            "adminUid" to ""
        )


        // 1. Guardar en la colección 'reportes' para que aparezca en el mapa
        db.collection("reportes").add(reporteData)
            .addOnSuccessListener {
                Log.d("CrearReporte", "✅ Reporte principal guardado en Firestore: ${it.id}")

                // 2. Si el reporte principal se guarda, enviar el mensaje a los chats
                iniciarDistribucion(categoria, descripcion, ubicacion, evidenciaUri)
            }
            .addOnFailureListener { e ->
                Log.e("CrearReporte", "❌ Error al guardar reporte principal: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al guardar el reporte", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Convierte las coordenadas (Lat/Lon) en una dirección legible por humanos (nombre de calle).
     */
    private fun obtenerDireccion(location: Location): String {
        val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
        var addressText = "Ubicación: ${location.latitude}, ${location.longitude}" // Valor por defecto

        try {
            // Pedimos la dirección para la Lat/Lon
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // Intentamos obtener el nombre de la calle
                val street = address.thoroughfare ?: address.featureName
                val number = address.subThoroughfare

                addressText = if (street != null) {
                    if (number != null) "Ubicación: $street #$number" else "Ubicación: $street"
                } else {
                    // Si la calle no está disponible, usamos la línea de dirección completa
                    "Ubicación: ${address.getAddressLine(0) ?: "Dirección no disponible"}"
                }
            }
        } catch (e: Exception) {
            // Esto captura errores de red o I/O
            Log.e("CrearReporte", "Error en geocodificación: ${e.message}")
            // Mantenemos el valor por defecto con coordenadas
        }
        return addressText
    }

    private fun iniciarDistribucion(categoria: String, descripcion: String?, ubicacion: Location, evidenciaUri: String) {
        val currentUser = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. Obtener chats donde el usuario es miembro
        db.collection("chats")
            .whereArrayContains("miembrosUids", currentUser.uid)
            .get()
            .addOnSuccessListener { chats ->

                val totalChats = chats.size()
                if (totalChats == 0) {
                    finalizarEnvioExitoso()
                    return@addOnSuccessListener
                }

                // 2. 🚨 CLAVE: Crear el lote de escritura (WriteBatch)
                val batch = db.batch()

                val mensajeTexto = "🚨 Reporte: $categoria\nDescripción: ${descripcion ?: "Sin descripción"}\nUbicación: ${ubicacion.latitude}, ${ubicacion.longitude}"

                val mensajeData = hashMapOf(
                    "texto" to mensajeTexto,
                    "remitente" to currentUser.uid,
                    "timestamp" to System.currentTimeMillis(),
                    "tipo" to (tipoEvidencia ?: "text")
                )
                if (evidenciaUri.isNotEmpty()) {
                    mensajeData["mediaUrl"] = evidenciaUri
                }

                // 3. Agregar todas las escrituras (mensajes) al lote
                chats.forEach { chatDoc ->
                    val chatId = chatDoc.id
                    val mensajeRef = db.collection("chats").document(chatId).collection("mensajes").document() // Documento nuevo
                    batch.set(mensajeRef, mensajeData)
                }

                // 4. Ejecutar el lote de escritura (una única operación de red)
                batch.commit()
                    .addOnSuccessListener {
                        // ✅ El proceso se considera exitoso solo después de que TODOS los chats reciben el mensaje
                        finalizarEnvioExitoso()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CrearReporte", "Error al enviar mensajes por lote: ${e.message}", e)
                        Toast.makeText(requireContext(), "Error al enviar mensaje a chats", Toast.LENGTH_SHORT).show()
                        btnEnviar.isEnabled = true // Re-habilitar botón
                    }

            }
            .addOnFailureListener { exception ->
                Log.e("CrearReporte", "Error al obtener chats para distribución: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error al obtener chats para distribución", Toast.LENGTH_SHORT).show()
                btnEnviar.isEnabled = true
            }
    }

    private fun finalizarEnvioExitoso() {

        mostrarNotificacionExito(requireContext())

        Toast.makeText(requireContext(), "Reporte enviado exitosamente!", Toast.LENGTH_SHORT).show()
        btnEnviar.isEnabled = true // ⬅️ Re-habilitar botón

        // Cierre seguro
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Log.e("CrearReporte", "Error al cerrar fragmento de forma segura: ${e.message}")
            }
        }, 100)
    }

    /*private fun enviarMensajeAChats(categoria: String, descripcion: String?, ubicacion: Location, evidenciaUri: String) {
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
                    "tipo" to (tipoEvidencia ?: "text")
                )
                if (evidenciaUri.isNotEmpty()) { // Usamos evidenciaUri que viene de la subida
                    mensajeData["mediaUrl"] = evidenciaUri
                }

                chats.forEach { chatDoc ->
                    val chatId = chatDoc.id
                    db.collection("chats").document(chatId).collection("mensajes").add(mensajeData)
                }
                Toast.makeText(requireContext(), "Reporte enviado exitosamente!", Toast.LENGTH_SHORT).show()

                // Opcional: Cerrar el fragmento de CrearReporte
                parentFragmentManager.popBackStack()

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        parentFragmentManager.popBackStack()
                    } catch (e: Exception) {
                        Log.e("CrearReporte", "Error al cerrar fragmento de forma segura: ${e.message}")
                    }
                }, 100) // Esperar 100ms para asegurar la estabilidad

            }
            .addOnFailureListener { exception ->
                Log.e("CrearReporte", "Error al enviar mensaje a chats: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error al enviar mensaje a chats", Toast.LENGTH_SHORT).show()
            }
    }*/
}