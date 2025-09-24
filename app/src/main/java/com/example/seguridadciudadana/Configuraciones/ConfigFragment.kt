package com.example.seguridadciudadana.Configuraciones

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class ConfigFragment : Fragment() {

    // Claves para SharedPreferences
    private val PREFS = "sos_prefs"
    private val K_MENSAJE = "mensaje_sos"
    private val K_DURACION = "video_duracion_seg"
    private val K_DISCRETO = "modo_discreto"
    private val K_ENLACE = "enlace_grupo_whatsapp"

    // Vistas
    private lateinit var tvDuracionValor: TextView
    private lateinit var switchModoDiscreto: SwitchMaterial

    private lateinit var btnEditarMensaje: View
    private lateinit var filaDuracionVideo: View
    private lateinit var btnEditarEnlaceGrupo: View
    private lateinit var btnProbarEnlaceGrupo: View
    private lateinit var filaAcerca: View

    private val opcionesDuracion = intArrayOf(5, 10, 15)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vincular vistas (asegúrate que IDs coincidan con tu XML)
        tvDuracionValor       = view.findViewById(R.id.tvDuracionValor)
        switchModoDiscreto    = view.findViewById(R.id.switchModoDiscreto)

        btnEditarMensaje      = view.findViewById(R.id.btnEditarMensaje)
        filaDuracionVideo     = view.findViewById(R.id.filaDuracionVideo)
        btnEditarEnlaceGrupo  = view.findViewById(R.id.btnEditarEnlaceGrupo)
        btnProbarEnlaceGrupo  = view.findViewById(R.id.btnProbarEnlaceGrupo)
        filaAcerca            = view.findViewById(R.id.filaAcerca)

        // Estado inicial
        val p = prefs()
        val mensaje = p.getString(K_MENSAJE, "Ayuda, estoy en peligro")!!
        val duracion = p.getInt(K_DURACION, 5)
        val discreto = p.getBoolean(K_DISCRETO, false)
        val enlace = p.getString(K_ENLACE, "") ?: ""

        tvDuracionValor.text = "$duracion s  >"
        switchModoDiscreto.isChecked = discreto
        btnProbarEnlaceGrupo.isEnabled = enlace.isNotBlank()

        // Listeners
        btnEditarMensaje.setOnClickListener { mostrarDialogoMensaje(mensaje) }
        filaDuracionVideo.setOnClickListener { mostrarDialogoDuracion(duracion) }

        switchModoDiscreto.setOnCheckedChangeListener { _, checked ->
            prefs().edit { putBoolean(K_DISCRETO, checked) }
            toast(if (checked) "Modo discreto activado" else "Modo discreto desactivado")
        }

        btnEditarEnlaceGrupo.setOnClickListener { mostrarDialogoEnlace() }
        btnProbarEnlaceGrupo.setOnClickListener { probarEnlaceGrupo() }
        filaAcerca.setOnClickListener { mostrarDialogoAcerca() }
    }

    // --- Diálogo para mensaje ---
    private fun mostrarDialogoMensaje(actual: String) {
        val et = EditText(requireContext()).apply {
            setText(actual)
            setSelection(text.length)
            maxLines = 4
            hint = "Mensaje SOS"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar mensaje de auxilio")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = et.text.toString().trim()
                if (nuevo.isEmpty()) toast("El mensaje no puede estar vacío")
                else {
                    prefs().edit { putString(K_MENSAJE, nuevo) }
                    toast("Mensaje guardado")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- Diálogo para duración ---
    private fun mostrarDialogoDuracion(actual: Int) {
        val labels = arrayOf("5 s", "10 s", "15 s")
        val checked = opcionesDuracion.indexOf(actual).let { if (it >= 0) it else 0 }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Duración del video")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val sel = opcionesDuracion[which]
                prefs().edit { putInt(K_DURACION, sel) }
                tvDuracionValor.text = "$sel s  >"
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- Diálogo para enlace de WhatsApp ---
    private fun mostrarDialogoEnlace() {
        val actual = prefs().getString(K_ENLACE, "") ?: ""
        val et = EditText(requireContext()).apply {
            hint = "https://chat.whatsapp.com/..."
            setText(actual)
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enlace del grupo de WhatsApp")
            .setMessage("Pega el enlace del grupo (opcional). Debes ser miembro para abrirlo.")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                val link = et.text.toString().trim()
                if (link.isNotEmpty() && !esEnlaceValido(link)) {
                    toast("Ingresa un enlace válido de WhatsApp")
                } else {
                    prefs().edit { putString(K_ENLACE, link) }
                    btnProbarEnlaceGrupo.isEnabled = link.isNotBlank()
                    toast(if (link.isBlank()) "Enlace eliminado" else "Enlace guardado")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- Diálogo "Acerca de la app" ---
    private fun mostrarDialogoAcerca() {
        val version = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
        } catch (_: Exception) { "1.0.0" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Acerca de la app")
            .setMessage(
                "Versión: $version\n\n" +
                        "App de seguridad ciudadana.\n" +
                        "• Envía mensaje y ubicación por WhatsApp.\n" +
                        "• Graba video corto configurable.\n\n" +
                        "Privacidad: los datos no se suben a servidores, se quedan en tu dispositivo."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // --- Probar enlace de WhatsApp ---
    private fun probarEnlaceGrupo() {
        val link = prefs().getString(K_ENLACE, "") ?: ""
        if (link.isBlank()) { toast("Primero guarda el enlace del grupo"); return }
        if (!estaWhatsAppInstalado()) { toast("Instala WhatsApp para abrir el grupo"); return }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                setPackage("com.whatsapp")
            })
        } catch (_: ActivityNotFoundException) {
            // Intentar WhatsApp Business o sin paquete
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                    setPackage("com.whatsapp.w4b")
                })
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (_: Exception) {
            toast("No se pudo abrir el enlace")
        }
    }

    // --- Utils ---
    private fun esEnlaceValido(link: String): Boolean {
        val re = Regex("^https://chat\\.whatsapp\\.com/[-_A-Za-z0-9]+$")
        return re.matches(link)
    }

    private fun estaWhatsAppInstalado(): Boolean {
        val pm: PackageManager = requireContext().packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", 0); true
        } catch (_: Exception) {
            try { pm.getPackageInfo("com.whatsapp.w4b", 0); true }
            catch (_: Exception) { false }
        }
    }

    private fun prefs() = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
