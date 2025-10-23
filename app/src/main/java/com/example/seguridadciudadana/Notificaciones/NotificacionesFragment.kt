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


class NotificacionesFragment : Fragment(), AdaptadorNoticias.OnItemClickListener{

    private lateinit var adapter: AdaptadorNoticias

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflamos el mismo layout que tenías en la Activity
        return inflater.inflate(R.layout.activity_notificaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdaptadorNoticias(this)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewNoticias)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Carga temporal (luego vendrá del backend/Firestore)
        adapter.actualizarArticulos(
            listOf(
                Articulo(
                    fuente = Fuente(id = null, nombre = "La República"),
                    autor = null,
                    titulo = "Asalto cerca a Av. Pizarro",
                    descripcion = "Sujetos armados interceptaron a transeúnte...",
                    url = "https://example.com/noticia1",
                    urlImagen = null,
                    fechaPublicacion = "2025-10-18T14:30:00Z",
                    contenido = null
                )
            )
        )
    }

    override fun onItemClick(articulo: Articulo) {
        articulo.url?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }


}