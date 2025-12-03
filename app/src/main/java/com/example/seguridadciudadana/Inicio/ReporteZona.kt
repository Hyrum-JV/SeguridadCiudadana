package com.example.seguridadciudadana.Inicio

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class ReporteZona(
    var id: String = "",
    val categoria: String = "",
    val ubicacion: GeoPoint? = null,
    val descripcion: String? = null,
    val evidenciaUrl: String? = null,
    val timestamp: Timestamp? = null,
    val direccion: String? = null,
    val userId: String = "",
    val estado: String? = "Pendiente",
    val adminComentario: String = "",
    val adminUid: String = "",
    val nombre: String? = null,
    val prioridad: String? = null,
    val urgencia: String? = null,
    val tipoEvidencia: String? = null
) {
    val tieneDescripcion: Boolean
        get() = !descripcion.isNullOrEmpty()
}