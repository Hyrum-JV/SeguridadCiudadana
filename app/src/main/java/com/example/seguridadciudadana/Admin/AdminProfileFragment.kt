package com.example.seguridadciudadana.Admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.seguridadciudadana.Login.LoginActivity
import com.example.seguridadciudadana.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class AdminProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    // Vistas
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminRole: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var chipAdminRole: Chip
    private lateinit var tvStatAtendidos: TextView
    private lateinit var tvStatProceso: TextView
    private lateinit var tvStatFalsos: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var tvAppVersion: TextView
    private lateinit var btnCerrarSesion: MaterialButton

    companion object {
        fun newInstance(): AdminProfileFragment {
            return AdminProfileFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_profile, container, false)

        initViews(view)
        setupGoogleSignIn()
        cargarDatosAdmin()
        cargarEstadisticas()
        setupListeners(view)

        return view
    }

    private fun initViews(view: View) {
        tvAdminName = view.findViewById(R.id.tv_admin_name)
        tvAdminRole = view.findViewById(R.id.tv_admin_role)
        tvProfileName = view.findViewById(R.id.tv_profile_name)
        tvProfileEmail = view.findViewById(R.id.tv_profile_email)
        tvProfilePhone = view.findViewById(R.id.tv_profile_phone)
        chipAdminRole = view.findViewById(R.id.chip_admin_role)
        tvStatAtendidos = view.findViewById(R.id.tv_stat_atendidos)
        tvStatProceso = view.findViewById(R.id.tv_stat_proceso)
        tvStatFalsos = view.findViewById(R.id.tv_stat_falsos)
        switchNotifications = view.findViewById(R.id.switch_notifications)
        tvAppVersion = view.findViewById(R.id.tv_app_version)
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion)

        // Cargar versión de la app
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvAppVersion.text = pInfo.versionName
        } catch (e: Exception) {
            tvAppVersion.text = "1.0.0"
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun setupListeners(view: View) {
        // Botón de retroceso
        view.findViewById<FloatingActionButton>(R.id.fab_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Botón cerrar sesión
        btnCerrarSesion.setOnClickListener {
            mostrarDialogCerrarSesion()
        }

        // Switch de notificaciones
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AdminNotificationService.suscribirATopicReportes()
                Toast.makeText(requireContext(), "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            } else {
                AdminNotificationService.desuscribirDeTopicReportes()
                Toast.makeText(requireContext(), "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarDatosAdmin() {
        val user = auth.currentUser ?: return

        tvProfileEmail.text = user.email ?: "No disponible"

        db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener

                val nombre = doc.getString("nombre") ?: user.displayName ?: "Administrador"
                val telefono = doc.getString("telefono") ?: "No disponible"
                val rol = doc.getString("rol") ?: "admin"

                tvAdminName.text = nombre
                tvProfileName.text = nombre
                tvProfilePhone.text = telefono

                // Configurar rol
                when (rol.lowercase()) {
                    "admin" -> {
                        chipAdminRole.text = "Administrador"
                        tvAdminRole.text = "Policía Nacional del Perú"
                    }
                    "policia" -> {
                        chipAdminRole.text = "Oficial de Policía"
                        tvAdminRole.text = "Policía Nacional del Perú"
                    }
                    "supervisor" -> {
                        chipAdminRole.text = "Supervisor"
                        tvAdminRole.text = "Jefatura Policial"
                    }
                    else -> {
                        chipAdminRole.text = rol.replaceFirstChar { it.uppercase() }
                        tvAdminRole.text = "Sistema de Seguridad Ciudadana"
                    }
                }

                // Cargar foto de perfil si existe
                cargarFotoPerfil(user.uid)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                tvAdminName.text = "Administrador"
                tvProfileName.text = user.displayName ?: "Sin nombre"
                tvProfilePhone.text = "No disponible"
            }
    }

    private fun cargarFotoPerfil(userId: String) {
        val file = File(requireContext().filesDir, "${userId}_perfil.jpg")
        if (file.exists()) {
            Glide.with(this)
                .load(file)
                .circleCrop()
                .placeholder(R.drawable.ic_admin)
                .into(requireView().findViewById(R.id.iv_admin_avatar))
        }
    }

    private fun cargarEstadisticas() {
        val user = auth.currentUser ?: return

        // Contar reportes atendidos por este admin (resueltos)
        db.collection("reportes")
            .whereEqualTo("adminUid", user.uid)
            .whereEqualTo("estado", "Caso resuelto")
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    tvStatAtendidos.text = docs.size().toString()
                }
            }

        // Contar reportes en proceso por este admin
        db.collection("reportes")
            .whereEqualTo("adminUid", user.uid)
            .whereIn("estado", listOf("Policía verificando", "Pendiente de resolución"))
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    tvStatProceso.text = docs.size().toString()
                }
            }

        // Contar reportes marcados como falsos por este admin
        db.collection("reportes")
            .whereEqualTo("adminUid", user.uid)
            .whereEqualTo("estado", "Noticia falsa")
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    tvStatFalsos.text = docs.size().toString()
                }
            }
    }

    private fun mostrarDialogCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión? Dejarás de recibir notificaciones de nuevos reportes.")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        // Detener escucha de notificaciones
        AdminNotificationService.stopListening()
        AdminNotificationService.desuscribirDeTopicReportes()

        // Cerrar sesión de Firebase
        auth.signOut()

        // Cerrar sesión de Google
        googleSignInClient.signOut().addOnCompleteListener {
            // Ir al login
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // La visibilidad del container se maneja en el listener del Activity
    }
}
