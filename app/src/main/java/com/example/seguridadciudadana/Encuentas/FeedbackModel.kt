package com.example.seguridadciudadana.Feedback

import com.google.firebase.Timestamp

data class FeedbackModel(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val calificacion: Float = 0f,
    val comentario: String = "",
    val categoria: String = "", // "usabilidad", "funcionalidad", "diseño", "seguridad"
    val timestamp: Timestamp = Timestamp.now(),
    val version: String? = "", // Versión de la app
    val respondido: Boolean = false,
    val respuestaAdmin: String = ""
)