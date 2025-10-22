package com.example.seguridadciudadana.Perfil

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

class MostrarQRActivity : AppCompatActivity() {

    private lateinit var ivCodigoQR: ImageView
    private lateinit var tvNombreUsuarioQR: TextView
    private lateinit var tvUidQR: TextView
    private lateinit var btnVolverQR: Button
    private lateinit var btnCompartirQR: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mostrar_qr)

        ivCodigoQR = findViewById(R.id.iv_codigo_qr)
        tvNombreUsuarioQR = findViewById(R.id.tv_nombre_usuario_qr)
        tvUidQR = findViewById(R.id.tv_uid_qr)
        btnVolverQR = findViewById(R.id.btn_volver_qr)
        btnCompartirQR = findViewById(R.id.btn_compartir_qr)

        // Recibir los datos
        val uid = intent.getStringExtra("uid")
        val nombre = intent.getStringExtra("nombre")
        val correo = intent.getStringExtra("correo")
        val telefono = intent.getStringExtra("telefono")

        if (uid == null) {
            Toast.makeText(this, "Error: no se encontró el UID del usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvNombreUsuarioQR.text = nombre ?: "Usuario"
        tvUidQR.text = "UID: $uid"

        // Crear el JSON con la información del usuario
        val userJson = JSONObject().apply {
            put("uid", uid)
            put("nombre", nombre)
            put("correo", correo)
            put("telefono", telefono)
        }

        // Generar el QR con el JSON
        generarCodigoQR(userJson.toString())

        // Botones
        btnVolverQR.setOnClickListener { finish() }

        btnCompartirQR.setOnClickListener { compartirQR() }
    }

    private fun generarCodigoQR(texto: String) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)
            val ancho = bitMatrix.width
            val alto = bitMatrix.height
            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565)
            for (x in 0 until ancho) {
                for (y in 0 until alto) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            ivCodigoQR.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar el código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirQR() {
        ivCodigoQR.isDrawingCacheEnabled = true
        val bitmap = ivCodigoQR.drawingCache

        val path = android.provider.MediaStore.Images.Media.insertImage(
            contentResolver, bitmap, "QR_${System.currentTimeMillis()}", null
        )

        val uri = android.net.Uri.parse(path)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Este es mi código QR con mis datos de Seguridad Ciudadana")
        }
        startActivity(Intent.createChooser(intent, "Compartir QR"))
    }
}
