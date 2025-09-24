package com.example.seguridadciudadana.Perfil

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.seguridadciudadana.R

class PerfilFragment : Fragment() {

    // Views
    private lateinit var ivAvatarPerfil: ImageView
    private lateinit var tvNombrePerfil: TextView
    private lateinit var tvCorreoPerfil: TextView
    private lateinit var tvTelefonoPerfil: TextView


    // Datos del usuario (estos vendrían de tu base de datos, SharedPreferences, etc.)
    private var usuarioActual: Usuario? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        // Inicializar views
        initViews(view)

        // Cargar datos del perfil
        cargarDatosPerfil()

        return view
    }

    private fun initViews(view: View) {
        ivAvatarPerfil = view.findViewById(R.id.iv_avatar_perfil)
        tvNombrePerfil = view.findViewById(R.id.tv_nombre_perfil)
        tvCorreoPerfil = view.findViewById(R.id.tv_correo_perfil)
        tvTelefonoPerfil = view.findViewById(R.id.tv_telefono_perfil)
    }

    private fun cargarDatosPerfil() {
        // Aquí cargarías los datos desde tu fuente de datos
        // Por ejemplo, desde SharedPreferences, Room Database, etc.
        usuarioActual = obtenerDatosUsuario()

        usuarioActual?.let { usuario ->
            mostrarDatosEnPantalla(usuario)
        } ?: run {
            // Mostrar datos de ejemplo si no hay usuario
            mostrarDatosEjemplo()
        }
    }

    private fun obtenerDatosUsuario(): Usuario? {
        // Implementa aquí la lógica para obtener los datos del usuario
        // Por ejemplo:
        /*
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return Usuario(
            nombre = sharedPref.getString("nombre", "") ?: "",
            correo = sharedPref.getString("correo", "") ?: "",
            telefono = sharedPref.getString("telefono", "") ?: "",
            pin = sharedPref.getString("pin", "") ?: ""
        )
        */

        // Por ahora retornamos null para mostrar datos de ejemplo
        return null
    }

    private fun mostrarDatosEnPantalla(usuario: Usuario) {
        tvNombrePerfil.text = usuario.nombre.ifEmpty { "No disponible" }
        tvCorreoPerfil.text = usuario.correo.ifEmpty { "No disponible" }
        tvTelefonoPerfil.text = usuario.telefono.ifEmpty { "No disponible" }
    }

    private fun mostrarDatosEjemplo() {
        tvNombrePerfil.text = "Juan Pérez García"
        tvCorreoPerfil.text = "juan.perez@email.com"
        tvTelefonoPerfil.text = "+51 987 654 321"
    }

    private fun onEditarPerfilClick() {
        // Implementa aquí la lógica para editar el perfil
        // Por ejemplo, navegar a otro fragment o mostrar un dialog

        /*
        // Ejemplo para navegar a fragment de edición:
        findNavController().navigate(R.id.action_perfilFragment_to_editarPerfilFragment)

        // O mostrar un dialog:
        val dialog = EditarPerfilDialog()
        dialog.show(childFragmentManager, "editar_perfil")
        */

        // Por ahora solo mostramos un mensaje
        Toast.makeText(
            requireContext(),
            "Función de edición en desarrollo",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Método para actualizar los datos desde otra parte de la app
    fun actualizarDatos(nuevoUsuario: Usuario) {
        usuarioActual = nuevoUsuario
        mostrarDatosEnPantalla(nuevoUsuario)
    }
}

// Data class para el modelo de Usuario
data class Usuario(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
)