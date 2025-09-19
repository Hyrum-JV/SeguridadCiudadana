package com.example.seguridadciudadana.Configuraciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.seguridadciudadana.R

class ConfigFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardMensajeSOS = view.findViewById<CardView>(R.id.cardMensajeSOS)
        val cardContactoSOS = view.findViewById<CardView>(R.id.cardContactoSOS)
        val contactoEditable = view.findViewById<TextView>(R.id.ContactoEditable)

        cardMensajeSOS.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor_fragmentos, MsjSosFragment())
                .addToBackStack(null)
                .commit()
        }
        cardContactoSOS.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor_fragmentos, Config_SeleccionarContacto())
                .addToBackStack(null)
                .commit()
        }

        // recibir contacto seleccionado desde ContactsFragment
        parentFragmentManager.setFragmentResultListener("requestKeyContacto", viewLifecycleOwner) { _, bundle ->
            val name = bundle.getString("name") ?: "No asignado"
            val phone = bundle.getString("phone") ?: ""
            contactoEditable.text = "$name · $phone"
        }
    }

}
