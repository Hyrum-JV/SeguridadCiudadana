package com.example.seguridadciudadana.Admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminReportDetailFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: ReporteZona? = null

    // Estados finales (caso cerrado)
    private val estadosFinales = listOf("Caso resuelto", "Noticia falsa")

    private val estadosMap = mapOf(
        "Pendiente" to "Pendiente",
        "Policía verificando" to "Policía verificando",
        "Pendiente de resolución" to "Pendiente de resolución",
        "Caso resuelto" to "Caso resuelto",
        "Noticia falsa" to "Noticia falsa"
    )

    companion object {
        private const val ARG_REPORT_ID = "report_id"

        fun newInstance(reportId: String): AdminReportDetailFragment {
            val fragment = AdminReportDetailFragment()
            val args = Bundle()
            args.putString(ARG_REPORT_ID, reportId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reportId = it.getString(ARG_REPORT_ID)
        }
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_admin_report_detail, container, false)

        if (reportId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: ID del reporte no recibido", Toast.LENGTH_LONG).show()
            view.post {
                parentFragmentManager.popBackStack()
            }
            return view
        }

        // Referencias a vistas
        val fabBack = view.findViewById<FloatingActionButton>(R.id.fab_back)
        val ivEvidencia = view.findViewById<ImageView>(R.id.iv_evidencia)
        val tvCategoria = view.findViewById<TextView>(R.id.tv_categoria_detail)
        val tvDescripcion = view.findViewById<TextView>(R.id.tv_descripcion_detail)
        val tvTimestamp = view.findViewById<TextView>(R.id.tv_timestamp_detail)
        val tvUbicacion = view.findViewById<TextView>(R.id.tv_ubicacion_detail)
        val tvNombreUsuario = view.findViewById<TextView>(R.id.tv_nombre_usuario)
        val chipEstadoHeader = view.findViewById<Chip>(R.id.chip_estado_header)
        val spinnerEstado = view.findViewById<AutoCompleteTextView>(R.id.spinner_estado)
        val etComentario = view.findViewById<TextInputEditText>(R.id.et_admin_comentario)
        val btnGuardar = view.findViewById<Button>(R.id.btn_guardar_cambios)
        val btnEliminar = view.findViewById<Button>(R.id.btn_eliminar_reporte)
        val bannerCasoCerrado = view.findViewById<LinearLayout>(R.id.banner_caso_cerrado)
        val tvBannerMensaje = view.findViewById<TextView>(R.id.tv_banner_mensaje)

        // Configurar FAB de retroceso
        fabBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Cargar datos del reporte
        db.collection("reportes").document(reportId!!).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                currentReport = doc.toObject(ReporteZona::class.java)?.copy(id = doc.id)

                // Cargar imagen de evidencia
                if (!currentReport?.evidenciaUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(currentReport?.evidenciaUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(ivEvidencia)
                } else {
                    ivEvidencia.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Información básica
                tvCategoria.text = currentReport?.categoria ?: "Sin categoría"
                tvDescripcion.text = currentReport?.descripcion ?: "Sin descripción"

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvTimestamp.text = currentReport?.timestamp?.toDate()?.let { sdf.format(it) } ?: "Sin fecha"

                currentReport?.ubicacion?.let { geo ->
                    tvUbicacion.text = "${geo.latitude}, ${geo.longitude}"
                }

                etComentario.setText(currentReport?.adminComentario)

                // Chip de estado
                val estadoActual = currentReport?.estado ?: "Pendiente"
                chipEstadoHeader.text = estadoActual
                when (estadoActual.lowercase()) {
                    "pendiente" -> chipEstadoHeader.setChipBackgroundColorResource(R.color.estado_pendiente)
                    "policía verificando", "pendiente de resolución" -> 
                        chipEstadoHeader.setChipBackgroundColorResource(R.color.estado_proceso)
                    "caso resuelto" -> chipEstadoHeader.setChipBackgroundColorResource(R.color.estado_completado)
                    "noticia falsa" -> chipEstadoHeader.setChipBackgroundColorResource(R.color.estado_falso)
                }

                // Cargar nombre del usuario
                db.collection("usuarios").document(currentReport!!.userId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val nombre = userDoc.getString("nombre") ?: "Usuario desconocido"
                        tvNombreUsuario.text = nombre
                    }

                // Configurar AutoCompleteTextView para estados
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    estadosMap.values.toList()
                )
                spinnerEstado.setAdapter(adapter)
                spinnerEstado.setText(estadoActual, false)

                // Verificar si el caso está cerrado (estado final)
                val casoCerrado = estadoActual in estadosFinales
                configurarUISegunEstado(
                    casoCerrado = casoCerrado,
                    estadoActual = estadoActual,
                    spinnerEstado = spinnerEstado,
                    etComentario = etComentario,
                    btnGuardar = btnGuardar,
                    btnEliminar = btnEliminar,
                    bannerCasoCerrado = bannerCasoCerrado,
                    tvBannerMensaje = tvBannerMensaje,
                    view = view
                )
            }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            if (currentReport == null) return@setOnClickListener

            val estadoActual = currentReport?.estado ?: "Pendiente"
            val nuevoEstado = spinnerEstado.text.toString()
            
            // Verificar si el caso está cerrado y se intenta modificar
            if (estadoActual in estadosFinales && nuevoEstado in estadosFinales) {
                Toast.makeText(
                    requireContext(), 
                    "⚠️ Este caso ya está cerrado. Use 'Reabrir caso' para modificarlo.", 
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val nuevoComentario = etComentario.text.toString()
            val adminUid = auth.currentUser?.uid ?: ""

            // Si se está cerrando el caso, agregar fecha de cierre
            val updates = mutableMapOf<String, Any>(
                "estado" to nuevoEstado,
                "adminComentario" to nuevoComentario,
                "adminUid" to adminUid
            )
            
            // Si es un estado final, agregar fecha de cierre
            if (nuevoEstado in estadosFinales) {
                updates["fechaCierre"] = com.google.firebase.Timestamp.now()
            }

            db.collection("reportes").document(reportId!!).update(updates)
                .addOnSuccessListener {
                    val mensaje = if (nuevoEstado in estadosFinales) {
                        "✓ Caso cerrado exitosamente"
                    } else {
                        "✓ Cambios guardados exitosamente"
                    }
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                    view.post {
                        parentFragmentManager.popBackStack()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar cambios", Toast.LENGTH_SHORT).show()
                }
        }

        // Botón Eliminar
        btnEliminar.setOnClickListener {
            mostrarDialogoEliminar(view)
        }

        return view
    }

    private fun configurarUISegunEstado(
        casoCerrado: Boolean,
        estadoActual: String,
        spinnerEstado: AutoCompleteTextView,
        etComentario: TextInputEditText,
        btnGuardar: Button,
        btnEliminar: Button,
        bannerCasoCerrado: LinearLayout,
        tvBannerMensaje: TextView,
        view: View
    ) {
        if (casoCerrado) {
            // Caso cerrado - Bloquear edición
            spinnerEstado.isEnabled = false
            etComentario.isEnabled = false
            
            // Mostrar banner de caso cerrado
            bannerCasoCerrado.visibility = View.VISIBLE
            tvBannerMensaje.text = when (estadoActual) {
                "Caso resuelto" -> "✅ Este caso fue resuelto y está cerrado"
                "Noticia falsa" -> "⚠️ Este reporte fue marcado como noticia falsa"
                else -> "Este caso está cerrado"
            }
            
            // Cambiar texto del botón a "Reabrir Caso"
            btnGuardar.text = "🔓 Reabrir Caso"
            btnGuardar.setOnClickListener {
                mostrarDialogoReabrirCaso(spinnerEstado, etComentario, btnGuardar, bannerCasoCerrado, view)
            }
        } else {
            bannerCasoCerrado.visibility = View.GONE
        }
    }

    private fun mostrarDialogoReabrirCaso(
        spinnerEstado: AutoCompleteTextView,
        etComentario: TextInputEditText,
        btnGuardar: Button,
        bannerCasoCerrado: LinearLayout,
        view: View
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("🔓 Reabrir Caso")
            .setMessage("¿Estás seguro de reabrir este caso? Podrás editar el estado y comentarios nuevamente.\n\nEsto solo debe hacerse si hubo un error en la clasificación anterior.")
            .setPositiveButton("Reabrir") { _, _ ->
                // Habilitar edición
                spinnerEstado.isEnabled = true
                etComentario.isEnabled = true
                
                // Ocultar banner
                bannerCasoCerrado.visibility = View.GONE
                
                // Cambiar estado a "Pendiente de resolución"
                spinnerEstado.setText("Pendiente de resolución", false)
                
                // Restaurar botón normal
                btnGuardar.text = "Guardar Cambios"
                btnGuardar.setOnClickListener {
                    guardarCambiosNormales(spinnerEstado, etComentario, view)
                }
                
                Toast.makeText(requireContext(), "✓ Caso reabierto. Ahora puedes editarlo.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarCambiosNormales(
        spinnerEstado: AutoCompleteTextView,
        etComentario: TextInputEditText,
        view: View
    ) {
        if (currentReport == null) return

        val nuevoEstado = spinnerEstado.text.toString()
        val nuevoComentario = etComentario.text.toString()
        val adminUid = auth.currentUser?.uid ?: ""

        val updates = mutableMapOf<String, Any>(
            "estado" to nuevoEstado,
            "adminComentario" to nuevoComentario,
            "adminUid" to adminUid
        )
        
        if (nuevoEstado in estadosFinales) {
            updates["fechaCierre"] = com.google.firebase.Timestamp.now()
        }

        db.collection("reportes").document(reportId!!).update(updates)
            .addOnSuccessListener {
                val mensaje = if (nuevoEstado in estadosFinales) {
                    "✓ Caso cerrado exitosamente"
                } else {
                    "✓ Cambios guardados exitosamente"
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                view.post {
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar cambios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoEliminar(view: View) {
        val estadoActual = currentReport?.estado ?: "Pendiente"
        
        // Diferentes mensajes según el estado
        val (titulo, mensaje) = when {
            estadoActual == "Noticia falsa" -> Pair(
                "🗑️ Eliminar Reporte Falso",
                "Este reporte fue marcado como NOTICIA FALSA.\n\n¿Deseas eliminarlo permanentemente del sistema?\n\nEsta acción no se puede deshacer."
            )
            estadoActual == "Pendiente" -> Pair(
                "🗑️ Eliminar Reporte Spam",
                "¿Este reporte es spam o contenido inapropiado?\n\nAl eliminarlo se quitará permanentemente del sistema.\n\nEsta acción no se puede deshacer."
            )
            else -> Pair(
                "⚠️ Eliminar Reporte",
                "Este reporte tiene estado: $estadoActual\n\n¿Estás seguro de eliminarlo? Solo deberías eliminar reportes falsos o spam.\n\nEsta acción no se puede deshacer."
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarReporte(view)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarReporte(view: View) {
        db.collection("reportes").document(reportId!!).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "🗑️ Reporte eliminado", Toast.LENGTH_SHORT).show()
                view.post {
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // La visibilidad del container se maneja en el listener del Activity
    }
}