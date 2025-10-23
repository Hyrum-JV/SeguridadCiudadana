package com.example.seguridadciudadana.Notificaciones
import com.google.gson.annotations.SerializedName

data class Articulo(

    @SerializedName("source")
    val fuente: Fuente?, // Objeto que contiene el nombre de la fuente de la noticia
    @SerializedName("author")
    val autor: String?,
    @SerializedName("title")
    val titulo: String?,
    @SerializedName("description")
    val descripcion: String?,
    @SerializedName("url")
    val url: String?, // URL de la noticia completa
    @SerializedName("urlToImage")
    val urlImagen: String?, // URL de la imagen del artículo
    @SerializedName("publishedAt")
    val fechaPublicacion: String?, // Fecha de publicación
    @SerializedName("content")
    val contenido: String?

)
data class Fuente(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val nombre: String?
)