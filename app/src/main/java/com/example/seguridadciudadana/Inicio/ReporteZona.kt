package com.example.seguridadciudadana.Inicio

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

data class ReporteZona(
    var id: String = "",
    val categoria: String = "",
    val ubicacion: GeoPoint? = null,
    val descripcion: String? = null,
    val evidenciaUrl: String? = null,
    val timestamp: Timestamp? = null,
    val direccion: String? = null,
    val userId: String = "",
    val estado: String = "pending",
    val adminComentario: String = "",
    val adminUid: String = "",

    // ---------- CAMPOS QUE NECESITA TU ADAPTER ----------
    val nombre: String? = null,        // Nombre del usuario
    val prioridad: String? = null,     // Prioridad
    val urgencia: String? = null       // Urgencia
) {
    val tieneDescripcion: Boolean
        get() = !descripcion.isNullOrEmpty()
}
