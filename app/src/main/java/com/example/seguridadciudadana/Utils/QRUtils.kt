package com.example.seguridadciudadana.Utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRUtils {

    fun generarQR(contenido: String, tamaño: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, tamaño, tamaño)
        val bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.RGB_565)
        for (x in 0 until tamaño) {
            for (y in 0 until tamaño) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
