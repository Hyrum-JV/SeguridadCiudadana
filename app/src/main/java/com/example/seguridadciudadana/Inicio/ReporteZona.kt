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
    val direccion: String? = null
) {
    // Propiedad para verificar si hay descripción
    val tieneDescripcion: Boolean
        get() = !descripcion.isNullOrEmpty()
}