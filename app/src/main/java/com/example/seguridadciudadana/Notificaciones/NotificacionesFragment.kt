package com.example.seguridadciudadana.Notificaciones

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import android.widget.TextView
import com.google.firebase.Timestamp
import java.util.Date


class NotificacionesFragment : Fragment(), AdaptadorNoticias.OnItemClickListener{

    private lateinit var adapter: AdaptadorNoticias
    private lateinit var emptyTextView: TextView // Referencia al TextView de "Cargando noticias..."

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val COLLECTION_NAME = "noticias_trujillo"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Usa el layout activity_notificaciones.xml que contiene el RecyclerView y el emptyTextView
        return inflater.inflate(R.layout.activity_notificaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdaptadorNoticias(this)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewNoticias)
        // Obtenemos la referencia al TextView de lista vacía
        emptyTextView = view.findViewById<TextView>(R.id.emptyTextView)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Mostramos el mensaje de carga mientras se obtienen los datos
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = "Cargando noticias..."

        // Iniciamos la carga de datos desde Firestore
        cargarNoticiasEnTiempoReal()
    }

    private fun cargarNoticiasEnTiempoReal() {
        // Escuchador en tiempo real de la colección 'noticias'
        db.collection(COLLECTION_NAME)
            // Ordenar por la fecha de creación de forma descendente (más reciente primero)
            .orderBy("fecha_creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Error al escuchar noticias:", e)
                    emptyTextView.text = "Error al cargar noticias: ${e.message}"
                    emptyTextView.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val listaNoticias = snapshots.documents.mapNotNull { document ->
                        // Mapeamos los campos de Firestore a la Data Class Noticia
                        val fechaTimestamp = document.get("fecha_creacion") as? Timestamp

                        document.toObject(Noticia::class.java)?.copy(
                            // Aseguramos que la fecha sea un objeto Date compatible con el adaptador
                            fecha_creacion = fechaTimestamp?.toDate()
                        )
                    }
                    adapter.actualizarNoticias(listaNoticias)
                    // Ocultamos el mensaje de lista vacía si hay datos
                    emptyTextView.visibility = View.GONE
                } else {
                    // La colección está vacía o no hay documentos
                    adapter.actualizarNoticias(emptyList())
                    emptyTextView.text = "No hay alertas ni noticias recientes."
                    emptyTextView.visibility = View.VISIBLE
                }
            }
    }


    // El listener del clic ahora recibe un objeto Noticia
    override fun onItemClick(noticia: Noticia) {
        // Abre el URL de la noticia en un navegador externo
        noticia.url?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }

}