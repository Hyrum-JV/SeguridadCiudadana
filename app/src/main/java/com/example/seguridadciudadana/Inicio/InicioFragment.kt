package com.example.seguridadciudadana.Inicio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.seguridadciudadana.R

class InicioFragment : Fragment() {

    private lateinit var btnSOS: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        btnSOS = view.findViewById(R.id.btnSOS)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startPulseAnimation()

        // --- Acción del botón: abrir fragment Crear Reporte ---
        btnSOS.setOnClickListener {
            val fragment = CrearReporte()
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor_fragmentos, fragment)
                .addToBackStack(null)
                .commit()
        }
    }



    private fun startPulseAnimation() {
        val pulseAnimation = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        btnSOS.startAnimation(pulseAnimation)
    }

    override fun onResume() {
        super.onResume()
        // Reiniciar animación cuando el fragmento vuelva a ser visible
        startPulseAnimation()
    }

    override fun onPause() {
        super.onPause()
        // Limpiar animación cuando el fragmento no esté visible
        btnSOS.clearAnimation()
    }
}

