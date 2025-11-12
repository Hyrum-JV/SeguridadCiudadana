package com.example.seguridadciudadana.ChatComunitario

data class Chat(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val creador: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val miembros: Map<String, Miembro> = emptyMap(), // uid -> Miembro
    // NUEVO: Agrega este campo para la query
    val miembrosUids: List<String> = emptyList()
)

data class Miembro(
    val uid: String = "",
    val rol: String = "member", // "admin" o "member"
    val fechaUnion: Long = System.currentTimeMillis()
)

data class Mensaje(
    val id: String = "",
    val texto: String = "",
    val remitente: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tipo: String = "text" // "text" o "image"
)