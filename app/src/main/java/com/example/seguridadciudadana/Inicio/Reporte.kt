package com.example.seguridadciudadana.Inicio

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Reporte(
    val categoria: String,
    val descripcion: String?,
    val ubicacion: GeoPoint,
    val evidenciaLocalUri: String,
    val timestamp: Timestamp = Timestamp.now()
)
