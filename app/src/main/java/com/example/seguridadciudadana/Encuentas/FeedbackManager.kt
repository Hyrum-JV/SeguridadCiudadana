package com.example.seguridadciudadana.Feedback

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.example.seguridadciudadana.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class FeedbackManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val PREFS_NAME = "FeedbackPrefs"
        private const val DAYS_BETWEEN_FEEDBACK = 30L
    }

    // ✅ CAMBIO: Obtener SharedPreferences específicas del usuario
    private fun getUserPrefs(): android.content.SharedPreferences {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return context.getSharedPreferences("${PREFS_NAME}_$userId", Context.MODE_PRIVATE)
    }

    fun shouldShowFeedbackDialog(): Boolean {
        val prefs = getUserPrefs()
        val hasGivenFeedback = prefs.getBoolean("has_given_feedback", false)
        
        if (hasGivenFeedback) {
            val lastFeedbackTime = prefs.getLong("last_feedback_time", 0)
            val daysSinceLastFeedback = (System.currentTimeMillis() - lastFeedbackTime) / (1000 * 60 * 60 * 24)
            return daysSinceLastFeedback >= DAYS_BETWEEN_FEEDBACK
        }
        
        val feedbackCount = prefs.getInt("feedback_count", 0)
        return feedbackCount >= 3  // Cambia a >= 3 para producción
    }

    fun incrementUsageCount() {
        val prefs = getUserPrefs()
        val currentCount = prefs.getInt("feedback_count", 0)
        prefs.edit().putInt("feedback_count", currentCount + 1).apply()
    }

    fun showFeedbackDialog(onDismiss: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null)
        
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val tvRatingText = dialogView.findViewById<TextView>(R.id.tv_rating_text)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_categoria)
        val etComentario = dialogView.findViewById<TextInputEditText>(R.id.et_comentario)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)
        val btnEnviar = dialogView.findViewById<MaterialButton>(R.id.btn_enviar)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val ratingTexts = mapOf(
            1f to "😞 Necesitamos mejorar",
            2f to "😐 Puede ser mejor",
            3f to "😊 Bien",
            4f to "😄 Muy bien!",
            5f to "🌟 Excelente!"
        )

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            tvRatingText.text = ratingTexts[rating] ?: "Toca las estrellas"
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        btnEnviar.setOnClickListener {
            val rating = ratingBar.rating
            if (rating == 0f) {
                Toast.makeText(context, "Por favor califica con estrellas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoriaChipId = chipGroup.checkedChipId
            val categoria = when (categoriaChipId) {
                R.id.chip_usabilidad -> "usabilidad"
                R.id.chip_funcionalidad -> "funcionalidad"
                R.id.chip_diseño -> "diseño"
                R.id.chip_seguridad -> "seguridad"
                else -> "general"
            }

            val comentario = etComentario.text?.toString()?.trim() ?: ""
            
            btnEnviar.isEnabled = false
            btnEnviar.text = "Enviando..."
            
            enviarFeedback(rating, categoria, comentario) { success ->
                btnEnviar.isEnabled = true
                btnEnviar.text = "Enviar"
                
                if (success) {
                    Toast.makeText(context, "¡Gracias por tu feedback! 🙏", Toast.LENGTH_LONG).show()
                    
                    // ✅ CAMBIO: Guardar en las preferencias del usuario
                    val prefs = getUserPrefs()
                    prefs.edit()
                        .putBoolean("has_given_feedback", true)
                        .putLong("last_feedback_time", System.currentTimeMillis())
                        .putInt("feedback_count", 0)
                        .apply()
                    
                    dialog.dismiss()
                    onDismiss?.invoke()
                } else {
                    Toast.makeText(context, "Error al enviar. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun enviarFeedback(
        calificacion: Float,
        categoria: String,
        comentario: String,
        callback: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("nombre") ?: "Usuario Anónimo"
                
                val version = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    "1.0.0"
                }

                val feedbackId = db.collection("feedback").document().id
                
                val feedback = FeedbackModel(
                    id = feedbackId,
                    userId = userId,
                    userName = userName,
                    calificacion = calificacion,
                    comentario = comentario,
                    categoria = categoria,
                    timestamp = Timestamp.now(),
                    version = version
                )

                db.collection("feedback")
                    .document(feedbackId)
                    .set(feedback)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            }
            .addOnFailureListener { callback(false) }
    }

    fun obtenerEstadisticas(callback: (Map<String, Any>) -> Unit) {
        db.collection("feedback")
            .get()
            .addOnSuccessListener { result ->
                var totalCalificacion = 0f
                var totalFeedbacks = 0
                val categorias = mutableMapOf<String, Int>()
                
                for (document in result) {
                    val calificacion = document.getDouble("calificacion")?.toFloat() ?: 0f
                    totalCalificacion += calificacion
                    totalFeedbacks++
                    
                    val categoria = document.getString("categoria") ?: "general"
                    categorias[categoria] = (categorias[categoria] ?: 0) + 1
                }
                
                val promedio = if (totalFeedbacks > 0) totalCalificacion / totalFeedbacks else 0f
                
                callback(mapOf(
                    "promedio" to promedio,
                    "total" to totalFeedbacks,
                    "categorias" to categorias
                ))
            }
            .addOnFailureListener {
                callback(emptyMap())
            }
    }

    // ✅ NUEVO: Método para resetear el contador manualmente (útil para pruebas)
    fun resetearContador() {
        val prefs = getUserPrefs()
        prefs.edit()
            .putInt("feedback_count", 0)
            .putBoolean("has_given_feedback", false)
            .putLong("last_feedback_time", 0)
            .apply()
    }
}