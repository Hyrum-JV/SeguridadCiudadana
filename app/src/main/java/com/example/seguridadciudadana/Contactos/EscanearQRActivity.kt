package com.example.seguridadciudadana.Contactos

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject

class EscanearQRActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "EscanearQR"

    private lateinit var btnEscanearCamara: Button
    private lateinit var btnSeleccionarImagen: Button

    // Launcher para seleccionar imagen de la galería
    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "Imagen seleccionada: $uri")
                escanearQRDesdeImagen(uri)
            }
        } else {
            Log.d(TAG, "Selección de imagen cancelada")
            Toast.makeText(this, "Selección cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escanear_qr)

        Log.d(TAG, "=== INICIANDO ACTIVIDAD DE ESCANEO ===")

        btnEscanearCamara = findViewById(R.id.btnEscanearCamara)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)

        btnEscanearCamara.setOnClickListener {
            Log.d(TAG, "Usuario seleccionó escanear con cámara")
            iniciarEscaneoConCamara()
        }

        btnSeleccionarImagen.setOnClickListener {
            Log.d(TAG, "Usuario seleccionó galería")
            abrirGaleria()
        }
    }

    private fun iniciarEscaneoConCamara() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea el código QR del contacto")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        seleccionarImagenLauncher.launch(intent)
    }

    private fun escanearQRDesdeImagen(uri: Uri) {
        try {
            Log.d(TAG, "=== PROCESANDO IMAGEN ===")

            // Convertir URI a Bitmap
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "❌ No se pudo decodificar la imagen")
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Imagen cargada: ${bitmap.width}x${bitmap.height}")

            // Convertir bitmap a array de píxeles
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Crear fuente de luminancia RGB
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            // Intentar decodificar el QR
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)

            Log.d(TAG, "✅ QR detectado en la imagen")
            Log.d(TAG, "Contenido: ${result.text}")

            procesarQR(result.text)

        } catch (e: com.google.zxing.NotFoundException) {
            Log.e(TAG, "❌ No se encontró código QR en la imagen")
            Toast.makeText(this, "No se detectó ningún código QR en la imagen", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al procesar imagen: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (resultCode == Activity.RESULT_OK && result.contents != null) {
                val contenidoQR = result.contents.trim()
                Log.d(TAG, "=== QR ESCANEADO CON CÁMARA ===")
                Log.d(TAG, "Contenido original: [$contenidoQR]")
                Log.d(TAG, "Longitud: ${contenidoQR.length}")

                if (contenidoQR.isNotEmpty()) {
                    procesarQR(contenidoQR)
                } else {
                    Log.e(TAG, "QR vacío después de trim")
                    Toast.makeText(this, "QR inválido", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Log.d(TAG, "Escaneo cancelado por el usuario")
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun procesarQR(contenidoQR: String) {
        try {
            Log.d(TAG, "Intentando parsear como JSON...")
            val json = JSONObject(contenidoQR)
            val uidEscaneado = json.getString("uid").trim()

            Log.d(TAG, "✅ JSON parseado exitosamente")
            Log.d(TAG, "UID extraído: [$uidEscaneado]")
            agregarContacto(uidEscaneado)

        } catch (e: Exception) {
            Log.d(TAG, "⚠️ No es JSON válido: ${e.message}")
            Log.d(TAG, "Usando contenido directo como UID")
            agregarContacto(contenidoQR.trim())
        }
    }

    private fun agregarContacto(uidEscaneado: String) {
        Log.d(TAG, "=== INICIANDO AGREGACIÓN DE CONTACTO ===")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado")
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Usuario actual autenticado: ${currentUser.uid}")
        Log.d(TAG, "UID a buscar: [$uidEscaneado]")

        // Validar que no sea el mismo usuario
        if (uidEscaneado == currentUser.uid) {
            Log.w(TAG, "⚠️ Intento de agregarse a sí mismo")
            Toast.makeText(this, "No puedes agregarte a ti mismo como contacto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verificar si el contacto ya existe
        db.collection("usuarios")
            .document(currentUser.uid)
            .collection("contactos")
            .document(uidEscaneado)
            .get()
            .addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    Log.w(TAG, "⚠️ El contacto ya existe")
                    Toast.makeText(this, "Este contacto ya está agregado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.d(TAG, "✅ Contacto no existe, procediendo a buscar usuario...")
                    buscarYAgregarUsuario(currentUser.uid, uidEscaneado)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error verificando contacto existente: ${e.message}")
                // Continuar de todos modos
                buscarYAgregarUsuario(currentUser.uid, uidEscaneado)
            }
    }

    private fun buscarYAgregarUsuario(currentUserId: String, uidEscaneado: String) {
        Log.d(TAG, "Buscando usuario en Firestore: usuarios/$uidEscaneado")

        db.collection("usuarios")
            .document(uidEscaneado)
            .get()
            .addOnSuccessListener { documento ->
                Log.d(TAG, "Respuesta recibida de Firestore")
                Log.d(TAG, "Documento existe: ${documento.exists()}")

                if (documento.exists()) {
                    Log.d(TAG, "✅ Documento encontrado")
                    Log.d(TAG, "Datos del documento: ${documento.data}")

                    val nombre = documento.getString("nombre") ?: "Sin nombre"
                    val correo = documento.getString("correo") ?: "Sin correo"
                    val telefono = documento.getString("telefono") ?: ""

                    Log.d(TAG, "Nombre: $nombre")
                    Log.d(TAG, "Correo: $correo")
                    Log.d(TAG, "Teléfono: $telefono")

                    val contactoData = hashMapOf(
                        "nombre" to nombre,
                        "correo" to correo,
                        "telefono" to telefono,
                        "fechaAgregado" to com.google.firebase.Timestamp.now()
                    )

                    Log.d(TAG, "Guardando en: usuarios/$currentUserId/contactos/$uidEscaneado")

                    db.collection("usuarios")
                        .document(currentUserId)
                        .collection("contactos")
                        .document(uidEscaneado)
                        .set(contactoData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅✅✅ CONTACTO AGREGADO EXITOSAMENTE ✅✅✅")
                            Toast.makeText(this, "Contacto agregado: $nombre", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Error al guardar contacto en Firestore")
                            Log.e(TAG, "Mensaje de error: ${e.message}")
                            Log.e(TAG, "Tipo de error: ${e.javaClass.simpleName}")
                            e.printStackTrace()
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            finish()
                        }

                } else {
                    Log.e(TAG, "❌ Documento NO existe en Firestore")
                    Toast.makeText(this, "Usuario no encontrado en la base de datos", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌❌❌ ERROR AL BUSCAR USUARIO ❌❌❌")
                Log.e(TAG, "Mensaje: ${e.message}")
                Log.e(TAG, "Tipo: ${e.javaClass.simpleName}")
                e.printStackTrace()
                Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
}