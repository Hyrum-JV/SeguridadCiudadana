package com.example.seguridadciudadana.Admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.seguridadciudadana.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class AdminSurveyDialog : BottomSheetDialogFragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Estrellas para calificación
    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView
    private lateinit var tvRatingText: TextView
    
    // Sliders
    private lateinit var sliderFacilidad: Slider
    private lateinit var sliderVelocidad: Slider
    
    // Chips
    private lateinit var chipGroupRecomendar: ChipGroup
    
    // Comentarios y botones
    private lateinit var etComentarios: TextInputEditText
    private lateinit var btnEnviar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    
    // Calificación actual
    private var calificacionActual = 0

    private var onSurveySubmitted: (() -> Unit)? = null

    companion object {
        const val TAG = "AdminSurveyDialog"

        fun newInstance(onSubmitted: (() -> Unit)? = null): AdminSurveyDialog {
            return AdminSurveyDialog().apply {
                onSurveySubmitted = onSubmitted
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_admin_survey, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        verificarEncuestaExistente()
    }

    private fun initViews(view: View) {
        // Estrellas
        star1 = view.findViewById(R.id.star_1)
        star2 = view.findViewById(R.id.star_2)
        star3 = view.findViewById(R.id.star_3)
        star4 = view.findViewById(R.id.star_4)
        star5 = view.findViewById(R.id.star_5)
        tvRatingText = view.findViewById(R.id.tv_rating_text)
        
        // Sliders
        sliderFacilidad = view.findViewById(R.id.slider_facilidad)
        sliderVelocidad = view.findViewById(R.id.slider_velocidad)
        
        // Chips
        chipGroupRecomendar = view.findViewById(R.id.chip_group_recomendar)
        
        // Comentarios y botones
        etComentarios = view.findViewById(R.id.et_comentarios)
        btnEnviar = view.findViewById(R.id.btn_enviar)
        btnCancelar = view.findViewById(R.id.btn_cancelar)
    }

    private fun setupListeners() {
        // Listeners para las estrellas
        val estrellas = listOf(star1, star2, star3, star4, star5)
        estrellas.forEachIndexed { index, star ->
            star.setOnClickListener {
                calificacionActual = index + 1
                actualizarEstrellas(calificacionActual)
            }
        }

        // Botón enviar
        btnEnviar.setOnClickListener {
            enviarEncuesta()
        }
        
        // Botón cancelar
        btnCancelar.setOnClickListener {
            dismiss()
        }
    }
    
    private fun actualizarEstrellas(rating: Int) {
        val estrellas = listOf(star1, star2, star3, star4, star5)
        estrellas.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star_filled)
            } else {
                star.setImageResource(R.drawable.ic_star_outline)
            }
        }
        
        // Actualizar texto de feedback
        tvRatingText.text = when (rating) {
            1 -> "😢 ¿Qué podemos mejorar?"
            2 -> "😐 Nos esforzaremos más"
            3 -> "🙂 Gracias por tu opinión"
            4 -> "😊 ¡Nos alegra que te guste!"
            5 -> "🌟 ¡Excelente! Gracias por tu apoyo"
            else -> "Toca las estrellas para calificar"
        }
    }



    private fun verificarEncuestaExistente() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("encuestas_admin")
            .whereEqualTo("adminUid", userId)
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener

                if (!docs.isEmpty) {
                    // Ya existe una encuesta, mostrar los datos anteriores
                    val encuestaAnterior = docs.documents.first()
                    val ratingAnterior = encuestaAnterior.getLong("calificacion")?.toInt() ?: 0
                    val comentarioAnterior = encuestaAnterior.getString("comentarios") ?: ""
                    val facilidadAnterior = encuestaAnterior.getLong("facilidad")?.toFloat() ?: 3f
                    val velocidadAnterior = encuestaAnterior.getLong("velocidad")?.toFloat() ?: 3f

                    calificacionActual = ratingAnterior
                    actualizarEstrellas(ratingAnterior)
                    etComentarios.setText(comentarioAnterior)
                    sliderFacilidad.value = facilidadAnterior
                    sliderVelocidad.value = velocidadAnterior

                    btnEnviar.text = "Actualizar Encuesta"
                    Toast.makeText(context, "Puedes actualizar tu encuesta anterior", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun enviarEncuesta() {
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        if (userId == null) {
            Toast.makeText(context, "Error: No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (calificacionActual == 0) {
            Toast.makeText(context, "Por favor, selecciona una calificación", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener opción de recomendación
        val chipId = chipGroupRecomendar.checkedChipId
        val recomendaria = when (chipId) {
            R.id.chip_si -> "Sí"
            R.id.chip_tal_vez -> "Tal vez"
            R.id.chip_no -> "No"
            else -> "Sin respuesta"
        }
        
        val facilidad = sliderFacilidad.value.toInt()
        val velocidad = sliderVelocidad.value.toInt()

        val comentarios = etComentarios.text?.toString()?.trim() ?: ""

        // Deshabilitar botón mientras se envía
        btnEnviar.isEnabled = false
        btnEnviar.text = "Enviando..."

        // Datos de la encuesta
        val encuestaData = hashMapOf(
            "adminUid" to userId,
            "adminEmail" to userEmail,
            "calificacion" to calificacionActual,
            "facilidad" to facilidad,
            "velocidad" to velocidad,
            "recomendaria" to recomendaria,
            "comentarios" to comentarios,
            "fecha" to Date(),
            "version" to obtenerVersionApp(),
            "dispositivo" to android.os.Build.MODEL,
            "androidVersion" to android.os.Build.VERSION.SDK_INT
        )

        // Verificar si ya existe una encuesta para actualizar
        db.collection("encuestas_admin")
            .whereEqualTo("adminUid", userId)
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener

                if (!docs.isEmpty) {
                    // Actualizar encuesta existente
                    val docId = docs.documents.first().id
                    db.collection("encuestas_admin").document(docId)
                        .update(encuestaData as Map<String, Any>)
                        .addOnSuccessListener {
                            onEncuestaEnviada(true)
                        }
                        .addOnFailureListener { e ->
                            onEncuestaError(e)
                        }
                } else {
                    // Crear nueva encuesta
                    db.collection("encuestas_admin")
                        .add(encuestaData)
                        .addOnSuccessListener {
                            onEncuestaEnviada(false)
                        }
                        .addOnFailureListener { e ->
                            onEncuestaError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onEncuestaError(e)
            }
    }

    private fun onEncuestaEnviada(actualizada: Boolean) {
        if (!isAdded) return

        val mensaje = if (actualizada) {
            "✅ ¡Encuesta actualizada correctamente!"
        } else {
            "✅ ¡Gracias por tu feedback! Tu opinión nos ayuda a mejorar."
        }

        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        onSurveySubmitted?.invoke()
        dismiss()
    }

    private fun onEncuestaError(e: Exception) {
        if (!isAdded) return

        Toast.makeText(context, "Error al enviar encuesta: ${e.message}", Toast.LENGTH_SHORT).show()
        btnEnviar.isEnabled = true
        btnEnviar.text = "Enviar Encuesta"
    }

    private fun obtenerVersionApp(): String {
        return try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_App_BottomSheetDialog
    }
}
