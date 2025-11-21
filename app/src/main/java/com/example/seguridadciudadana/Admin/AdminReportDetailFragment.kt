package com.example.seguridadciudadana.Admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminReportDetailFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: ReporteZona? = null

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

        // ⚠️ PREVENIR CRASH DEL FragmentManager
        if (reportId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: ID del reporte no recibido", Toast.LENGTH_LONG).show()

            // Ejecutar navegación después que termine la transacción actual
            view.post {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            return view
        }

        val tvCategoria = view.findViewById<TextView>(R.id.tv_categoria_detail)
        val tvDescripcion = view.findViewById<TextView>(R.id.tv_descripcion_detail)
        val tvTimestamp = view.findViewById<TextView>(R.id.tv_timestamp_detail)
        val tvUbicacion = view.findViewById<TextView>(R.id.tv_ubicacion_detail)
        val spinnerEstado = view.findViewById<Spinner>(R.id.spinner_estado)
        val etComentario = view.findViewById<EditText>(R.id.et_admin_comentario)
        val tvNombreUsuario = view.findViewById<TextView>(R.id.tv_nombre_usuario)
        val btnGuardar = view.findViewById<Button>(R.id.btn_guardar_cambios)
        val btnEliminar = view.findViewById<Button>(R.id.btn_eliminar_reporte)

        // ---------- CARGAR DATOS DEL REPORTE ----------
        db.collection("reportes").document(reportId!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {

                    currentReport = doc.toObject(ReporteZona::class.java)?.copy(id = doc.id)

                    tvCategoria.text = "Categoría: ${currentReport?.categoria}"
                    tvDescripcion.text = "Descripción: ${currentReport?.descripcion ?: "Sin descripción"}"
                    tvTimestamp.text = "Fecha: ${currentReport?.timestamp?.toDate()?.toString() ?: "Sin fecha"}"
                    etComentario.setText(currentReport?.adminComentario)

                    currentReport?.ubicacion?.let { geo ->
                        tvUbicacion.text = "Ubicación: ${geo.latitude}, ${geo.longitude}"
                    }

                    // ---------- CARGAR NOMBRE DEL USUARIO ----------
                    db.collection("usuarios").document(currentReport!!.userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val nombre = userDoc.getString("nombre") ?: "Usuario desconocido"
                            tvNombreUsuario.text = "Reportado por: $nombre"
                        }

                    // ---------- CONFIGURAR SPINNER ----------
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        estadosMap.values.toList()
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerEstado.adapter = adapter

                    val estadoActual = estadosMap[currentReport?.estado]
                    val index = estadosMap.values.indexOf(estadoActual)
                    if (index >= 0) spinnerEstado.setSelection(index)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar reporte", Toast.LENGTH_SHORT).show()
            }


        // ---------- BOTÓN GUARDAR ----------
        btnGuardar.setOnClickListener {
            if (currentReport == null) return@setOnClickListener

            val textoSeleccionado = spinnerEstado.selectedItem.toString()
            val nuevoEstado = estadosMap.filterValues { it == textoSeleccionado }.keys.first()
            val nuevoComentario = etComentario.text.toString()
            val adminUid = auth.currentUser?.uid ?: ""

            val updates = mapOf(
                "estado" to nuevoEstado,
                "adminComentario" to nuevoComentario,
                "adminUid" to adminUid
            )

            db.collection("reportes").document(reportId!!).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }


        // ---------- BOTÓN ELIMINAR ----------
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Reporte")
                .setMessage("¿Estás seguro de eliminar este reporte?")
                .setPositiveButton("Sí") { _, _ ->
                    db.collection("reportes").document(reportId!!).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Reporte eliminado", Toast.LENGTH_SHORT).show()

                            // Evitar crash de transacciones
                            view.post {
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }

        return view
    }
}
