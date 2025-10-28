package com.example.seguridadciudadana.Notificaciones
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Noticia(

    val titulo: String? = null,
    val url: String? = null,
    val snippet: String? = null, // Mapea a la descripción
    val fuente: String? = null,
    val imagen: String? = null, // URL de la imagen (urlImagen en modelos anteriores)

    // Campo usado para ordenar y calcular la antigüedad.
    @ServerTimestamp
    val fecha_creacion: Date? = null

)
