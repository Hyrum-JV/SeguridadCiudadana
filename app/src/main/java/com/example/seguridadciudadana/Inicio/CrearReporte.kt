package com.example.seguridadciudadana.Inicio

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.seguridadciudadana.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class CrearReporte : Fragment() {

    private lateinit var spinnerCategoria: Spinner
    private lateinit var layoutCategoriaOtro: TextInputLayout
    private lateinit var etCategoriaOtro: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerCategoria = view.findViewById(R.id.spinnerCategoria)
        layoutCategoriaOtro = view.findViewById(R.id.layoutCategoriaOtro)
        etCategoriaOtro = view.findViewById(R.id.etCategoriaOtro)

        val categorias = listOf(
            "Robo a mano armada",
            "Asalto a vivienda o local",
            "Balacera",
            "Violencia doméstica",
            "Otro"
        )

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val seleccion = categorias[position]
                layoutCategoriaOtro.visibility = if (seleccion == "Otro") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                layoutCategoriaOtro.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crear_reporte, container, false)
    }
}