package com.example.seguridadciudadana.Admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog

class AdminReportDetailFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: ReporteZona? = null

    // Mapeo estadoBD -> texto visible
    private val estadosMap = mapOf(
        "pending" to "Pendiente",
        "police_in_progress" to "Policía verificando",
        "pending_resolution" to "Pendiente de resolución",
        "case_resolved" to "Caso resuelto",
        "false_news" to "Noticia falsa"
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

        val tvCategoria = view.findViewById<TextView>(R.id.tv_categoria_detail)
        val tvDescripcion = view.findViewById<TextView>(R.id.tv_descripcion_detail)
        val tvTimestamp = view.findViewById<TextView>(R.id.tv_timestamp_detail)
        val spinnerEstado = view.findViewById<Spinner>(R.id.spinner_estado)
        val etComentario = view.findViewById<EditText>(R.id.et_admin_comentario)
        val btnGuardar = view.findViewById<Button>(R.id.btn_guardar_cambios)
        val btnEliminar = view.findViewById<Button>(R.id.btn_eliminar_reporte)

        reportId?.let { id ->
            db.collection("reportes").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentReport = ReporteZona(
                            id = doc.id,
                            categoria = doc.getString("categoria") ?: "",
                            ubicacion = doc.getGeoPoint("ubicacion"),
                            descripcion = doc.getString("descripcion"),
                            evidenciaUrl = doc.getString("evidenciaUrl"),
                            timestamp = doc.getTimestamp("timestamp"),
                            userId = doc.getString("userId") ?: "",
                            estado = doc.getString("estado") ?: "pending",
                            adminComentario = doc.getString("adminComentario") ?: "",
                            adminUid = doc.getString("adminUid") ?: ""
                        )

                        tvCategoria.text = "Categoría: ${currentReport?.categoria}"
                        tvDescripcion.text = "Descripción: ${currentReport?.descripcion ?: "Sin descripción"}"
                        tvTimestamp.text = "Fecha: ${currentReport?.timestamp?.toDate()?.toString() ?: "Sin fecha"}"
                        etComentario.setText(currentReport?.adminComentario)

                        // Configurar Spinner con textos en español
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            estadosMap.values.toList()
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerEstado.adapter = adapter

                        val index = estadosMap.keys.indexOf(currentReport?.estado)
                        if (index >= 0) spinnerEstado.setSelection(index)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al cargar reporte", Toast.LENGTH_SHORT).show()
                }
        }

        btnGuardar.setOnClickListener {
            val textoSeleccionado = spinnerEstado.selectedItem.toString()

            // Convertir texto en español -> estadoBD
            val nuevoEstado = estadosMap.filterValues { it == textoSeleccionado }.keys.first()

            val nuevoComentario = etComentario.text.toString()
            val adminUid = auth.currentUser?.uid ?: ""

            val updates = hashMapOf<String, Any>(
                "estado" to nuevoEstado,
                "adminComentario" to nuevoComentario,
                "adminUid" to adminUid
            )

            reportId?.let { id ->
                db.collection("reportes").document(id).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnEliminar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Reporte")
                .setMessage("¿Estás seguro de eliminar este reporte?")
                .setPositiveButton("Sí") { _, _ ->
                    reportId?.let { id ->
                        db.collection("reportes").document(id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Reporte eliminado", Toast.LENGTH_SHORT).show()
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        return view
    }
}
