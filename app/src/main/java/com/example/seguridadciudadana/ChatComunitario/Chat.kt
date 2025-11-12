package com.example.seguridadciudadana.ChatComunitario

data class Chat(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val creador: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val miembros: Map<String, Miembro> = emptyMap(),
    val miembrosUids: List<String> = emptyList()
) {
    // Método para verificar si un UID es admin
    fun esAdmin(uid: String): Boolean = creador == uid
}

data class Miembro(
    val uid: String = "",
    val rol: String = "member",
    val fechaUnion: Long = System.currentTimeMillis()
)

data class Mensaje(
    val id: String = "",
    val texto: String? = null,
    val remitente: String = "",
    val timestamp: Long = 0L,
    val mediaUrl: String? = null,
    val tipo: String? = "text"  // Cambia a nullable con default "text"
)