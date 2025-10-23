package com.example.seguridadciudadana.Inicio

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Reporte(
    val categoria: String,
    val descripcion: String?,
    val ubicacion: GeoPoint?, // cuando lo integremos
    val evidenciaUrl: String?, // cuando lo integremos
    val timestamp: Timestamp = Timestamp.now()
)
