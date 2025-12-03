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
            }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            if (currentReport == null) return@setOnClickListener

            val nuevoEstado = spinnerEstado.text.toString()
            val nuevoComentario = etComentario.text.toString()
            val adminUid = auth.currentUser?.uid ?: ""

            val updates = mapOf(
                "estado" to nuevoEstado,
                "adminComentario" to nuevoComentario,
                "adminUid" to adminUid
            )

            db.collection("reportes").document(reportId!!).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "✓ Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
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
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Reporte")
                .setMessage("¿Estás seguro de eliminar este reporte? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    db.collection("reportes").document(reportId!!).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Reporte eliminado", Toast.LENGTH_SHORT).show()
                            view.post {
                                parentFragmentManager.popBackStack()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ocultar el container cuando se cierra el fragment
        requireActivity().findViewById<View>(R.id.admin_container)?.visibility = View.GONE
    }
}