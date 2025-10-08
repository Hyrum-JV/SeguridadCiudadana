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

        // --- Acción del botón: abrir cámara ---
        btnSOS.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            // Verifica que haya cámara en el dispositivo
            if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    // Recibir el resultado de la cámara (por ahora solo muestra si la foto vuelve bien)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") // Miniatura de la foto
            // ⚠️ Aquí más adelante guardaremos la foto o la enviaremos por WhatsApp
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
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

