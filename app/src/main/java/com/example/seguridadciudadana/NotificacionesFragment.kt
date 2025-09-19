package com.example.seguridadciudadana

import NotificacionAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NotificacionesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificacionesFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val vista = inflater.inflate(R.layout.fragment_notificaciones, container, false)

        val recyclerView = vista.findViewById<RecyclerView>(R.id.recyclerNotificaciones)

        val listaNotificaciones = listOf(
            Notificacion("Precaución: Accidente de tránsito", "19 de septiembre de 2025", android.R.drawable.ic_dialog_info),
            Notificacion("Alerta: Robo reportado en Av. Central", "18 de septiembre de 2025", android.R.drawable.ic_dialog_alert),
            Notificacion("Bienvenido a la app", "17 de septiembre de 2025", android.R.drawable.ic_menu_info_details)
        )

        val adapter = NotificacionAdapter(listaNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        return vista
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificacionesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotificacionesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}