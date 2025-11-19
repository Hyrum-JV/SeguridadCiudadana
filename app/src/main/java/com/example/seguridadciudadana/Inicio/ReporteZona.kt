package com.example.seguridadciudadana.Inicio

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

data class ReporteZona(
    val id: String = "",
    val categoria: String = "",
    val ubicacion: GeoPoint? = null,
    val descripcion: String? = null,
    val evidenciaUrl: String? = null,
    val timestamp: Timestamp? = null,
    val direccion: String? = null,
    val userId: String = "",  // Nuevo campo: ID del usuario que reportó
    val estado: String = "pending",  // Nuevo campo: estado del reporte
    val adminComentario: String = "",  // Nuevo campo: comentario del admin
    val adminUid: String = ""  // Nuevo campo: UID del admin que actualizó
) {
    // Propiedad para verificar si hay descripción
    val tieneDescripcion: Boolean
        get() = !descripcion.isNullOrEmpty()
}