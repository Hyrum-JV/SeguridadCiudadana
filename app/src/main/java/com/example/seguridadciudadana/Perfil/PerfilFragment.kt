package com.example.seguridadciudadana.Perfil

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.seguridadciudadana.Login.LoginActivity
import com.example.seguridadciudadana.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var ivAvatarPerfil: ImageView
    private lateinit var tvNombrePerfil: TextView
    private lateinit var tvCorreoPerfil: TextView
    private lateinit var tvTelefonoPerfil: TextView
    private lateinit var btnCerrarSesion: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private var usuarioActual: Usuario? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        initViews(view)
        setupGoogleSignIn()
        cargarDatosPerfil()
        configurarClicksEdicion()

        btnCerrarSesion.setOnClickListener { mostrarDialogCerrarSesion() }

        return view
    }

    private fun initViews(view: View) {
        ivAvatarPerfil = view.findViewById(R.id.iv_avatar_perfil)
        tvNombrePerfil = view.findViewById(R.id.tv_nombre_perfil)
        tvCorreoPerfil = view.findViewById(R.id.tv_correo_perfil)
        tvTelefonoPerfil = view.findViewById(R.id.tv_telefono_perfil)
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion)
    }

    private fun setupGoogleSignIn() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun configurarClicksEdicion() {
        tvNombrePerfil.setOnClickListener { mostrarDialogEditarNombre() }
        tvTelefonoPerfil.setOnClickListener { mostrarDialogEditarTelefono() }

        tvCorreoPerfil.setOnClickListener {
            Toast.makeText(requireContext(), "El correo no puede ser editado", Toast.LENGTH_SHORT).show()
        }

        // Cerrar sesión con long click en el avatar (extra opcional)
        ivAvatarPerfil.setOnLongClickListener {
            mostrarDialogCerrarSesion()
            true
        }
    }

    private fun cargarDatosPerfil() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        cargarFotoPerfil(user.photoUrl?.toString())
        tvCorreoPerfil.text = user.email ?: "No disponible"

        db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: "Sin nombre"
                    val correo = document.getString("correo") ?: user.email ?: ""
                    val telefono = document.getString("telefono") ?: "Sin teléfono"

                    usuarioActual = Usuario(nombre, correo, telefono)
                    mostrarDatosEnPantalla(usuarioActual!!)
                } else {
                    mostrarDatosBasicos(user.displayName ?: "Usuario", user.email ?: "", "Sin teléfono")
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                mostrarDatosBasicos(user.displayName ?: "Usuario", user.email ?: "", "Sin teléfono")
            }
    }

    private fun cargarFotoPerfil(photoUrl: String?) {
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(ivAvatarPerfil)
        } else {
            ivAvatarPerfil.setImageResource(R.drawable.ic_person_placeholder)
        }
    }

    private fun mostrarDatosEnPantalla(usuario: Usuario) {
        tvNombrePerfil.text = usuario.nombre.ifEmpty { "Sin nombre" }
        tvCorreoPerfil.text = usuario.correo.ifEmpty { "No disponible" }
        tvTelefonoPerfil.text = usuario.telefono.ifEmpty { "Sin teléfono" }
    }

    private fun mostrarDatosBasicos(nombre: String, correo: String, telefono: String) {
        tvNombrePerfil.text = nombre
        tvCorreoPerfil.text = correo
        tvTelefonoPerfil.text = telefono
    }

    private fun mostrarDialogEditarNombre() {
        val user = auth.currentUser ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_campo, null)

        val etCampo = dialogView.findViewById<EditText>(R.id.et_editar_campo)
        val tvTitulo = dialogView.findViewById<TextView>(R.id.tv_dialog_titulo)

        tvTitulo.text = "Editar Nombre"
        etCampo.hint = "Nombre completo"
        etCampo.setText(usuarioActual?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = etCampo.text.toString().trim()
                if (nuevoNombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                actualizarCampo(user.uid, "nombre", nuevoNombre) {
                    tvNombrePerfil.text = nuevoNombre
                    usuarioActual = usuarioActual?.copy(nombre = nuevoNombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarTelefono() {
        val user = auth.currentUser ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_editar_campo, null)

        val etCampo = dialogView.findViewById<EditText>(R.id.et_editar_campo)
        val tvTitulo = dialogView.findViewById<TextView>(R.id.tv_dialog_titulo)

        tvTitulo.text = "Editar Teléfono"
        etCampo.hint = "Número de teléfono"
        etCampo.inputType = android.text.InputType.TYPE_CLASS_PHONE
        etCampo.setText(usuarioActual?.telefono ?: "")

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoTelefono = etCampo.text.toString().trim()
                if (nuevoTelefono.isEmpty()) {
                    Toast.makeText(requireContext(), "El teléfono no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                actualizarCampo(user.uid, "telefono", nuevoTelefono) {
                    tvTelefonoPerfil.text = nuevoTelefono
                    usuarioActual = usuarioActual?.copy(telefono = nuevoTelefono)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarCampo(userId: String, campo: String, valor: String, onSuccess: () -> Unit) {
        val updates = hashMapOf<String, Any>(campo to valor)
        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}

data class Usuario(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = ""
)
