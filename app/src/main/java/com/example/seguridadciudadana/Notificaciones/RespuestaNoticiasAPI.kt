package com.example.app.api

import com.google.gson.annotations.SerializedName
import com.example.seguridadciudadana.Notificaciones.Articulo

data class RespuestaNoticiasAPI (

    @SerializedName("status")
    val estado: String,
    @SerializedName("totalResults")
    val totalResultados: Int,
    @SerializedName("articles")
    val articulos: List<Articulo> // Lista de los artículos de noticias

)

