package com.example.seguridadciudadana.ChatComunitario

import android.app.AlertDialog
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide  // Asegúrate de tener Glide en build.gradle
import com.bumptech.glide.request.RequestOptions
import com.example.seguridadciudadana.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MensajeAdapter(
    private val mensajes: List<Mensaje>,
    private val onMensajeLongClick: (Mensaje) -> Unit  // Callback para long click (eliminar mensaje)
) : RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje, parent, false)
        return MensajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.bind(mensaje)
    }

    override fun getItemCount(): Int = mensajes.size

    inner class MensajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutMensajeOtro: View = itemView.findViewById(R.id.layout_mensaje_otro)
        private val tvNombreOtro: TextView = itemView.findViewById(R.id.tv_nombre_otro)
        private val tvMensajeOtro: TextView = itemView.findViewById(R.id.tv_mensaje_otro)
        private val layoutMensajePropio: View = itemView.findViewById(R.id.layout_mensaje_propio)
        private val tvMensajePropio: TextView = itemView.findViewById(R.id.tv_mensaje_propio)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val ivMensajeOtro: ImageView = itemView.findViewById(R.id.iv_mensaje_otro)
        private val ivMensajePropio: ImageView = itemView.findViewById(R.id.iv_mensaje_propio)
        private val ivVideoOtro: ImageView = itemView.findViewById(R.id.iv_video_otro)  // Nuevo
        private val ivVideoPropio: ImageView = itemView.findViewById(R.id.iv_video_propio)  // Nuevo

        fun bind(mensaje: Mensaje) {
            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timestamp = sdf.format(Date(mensaje.timestamp))

                val isPropio = mensaje.remitente == currentUserUid

                // Ocultar todos los layouts primero
                layoutMensajeOtro.visibility = View.GONE
                layoutMensajePropio.visibility = View.GONE
                tvTimestamp.visibility = View.GONE

                // Ajustar constraints dinámicamente para tvTimestamp
                val layoutParams = tvTimestamp.layoutParams as ConstraintLayout.LayoutParams
                if (isPropio) {
                    // Posicionar a la derecha, debajo de layout_mensaje_propio
                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                    layoutParams.topToBottom = R.id.layout_mensaje_propio
                    tvTimestamp.layoutParams = layoutParams
                    tvTimestamp.visibility = View.VISIBLE
                    tvTimestamp.text = timestamp
                } else {
                    // Posicionar a la izquierda, debajo de layout_mensaje_otro
                    layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                    layoutParams.topToBottom = R.id.layout_mensaje_otro
                    tvTimestamp.layoutParams = layoutParams
                    tvTimestamp.visibility = View.VISIBLE
                    tvTimestamp.text = timestamp
                }

                when (mensaje.tipo) {
                    "text" -> {
                        if (isPropio) {
                            layoutMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.text = mensaje.texto
                        } else {
                            layoutMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.text = mensaje.texto
                            // Obtener nombre
                            db.collection("usuarios").document(mensaje.remitente).get()
                                .addOnSuccessListener { doc ->
                                    val nombre = doc.getString("nombre") ?: "Usuario"
                                    tvNombreOtro.text = nombre
                                }
                                .addOnFailureListener {
                                    tvNombreOtro.text = "Usuario desconocido"
                                }
                        }
                    }
                    "image" -> {
                        if (isPropio) {
                            layoutMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.visibility = if (mensaje.texto.isNullOrEmpty()) View.GONE else View.VISIBLE
                            tvMensajePropio.text = mensaje.texto ?: ""
                            ivMensajePropio.visibility = View.VISIBLE
                            Glide.with(itemView.context).load(mensaje.mediaUrl).into(ivMensajePropio)
                            ivMensajePropio.setOnClickListener { mostrarImagenGrande(mensaje.mediaUrl) }
                        } else {
                            layoutMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.visibility = if (mensaje.texto.isNullOrEmpty()) View.GONE else View.VISIBLE
                            tvMensajeOtro.text = mensaje.texto ?: ""
                            ivMensajeOtro.visibility = View.VISIBLE
                            Glide.with(itemView.context).load(mensaje.mediaUrl).into(ivMensajeOtro)
                            ivMensajeOtro.setOnClickListener { mostrarImagenGrande(mensaje.mediaUrl) }
                            // Obtener nombre
                            db.collection("usuarios").document(mensaje.remitente).get()
                                .addOnSuccessListener { doc ->
                                    val nombre = doc.getString("nombre") ?: "Usuario"
                                    tvNombreOtro.text = nombre
                                }
                                .addOnFailureListener {
                                    tvNombreOtro.text = "Usuario desconocido"
                                }
                        }
                    }
                    "video" -> {
                        if (isPropio) {
                            layoutMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.visibility = if (mensaje.texto.isNullOrEmpty()) View.GONE else View.VISIBLE
                            tvMensajePropio.text = mensaje.texto ?: ""
                            ivVideoPropio.visibility = View.VISIBLE
                            // Cargar thumbnail del video
                            Glide.with(itemView.context)
                                .load(Uri.parse(mensaje.mediaUrl))
                                .apply(RequestOptions.frameOf(0))  // Primer frame como thumbnail
                                .into(ivVideoPropio)
                            ivVideoPropio.setOnClickListener { mostrarVideoGrande(mensaje.mediaUrl) }
                        } else {
                            layoutMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.visibility = if (mensaje.texto.isNullOrEmpty()) View.GONE else View.VISIBLE
                            tvMensajeOtro.text = mensaje.texto ?: ""
                            ivVideoOtro.visibility = View.VISIBLE
                            // Cargar thumbnail del video
                            Glide.with(itemView.context)
                                .load(Uri.parse(mensaje.mediaUrl))
                                .apply(RequestOptions.frameOf(0))  // Primer frame como thumbnail
                                .into(ivVideoOtro)
                            ivVideoOtro.setOnClickListener { mostrarVideoGrande(mensaje.mediaUrl) }
                            // Obtener nombre
                            db.collection("usuarios").document(mensaje.remitente).get()
                                .addOnSuccessListener { doc ->
                                    val nombre = doc.getString("nombre") ?: "Usuario"
                                    tvNombreOtro.text = nombre
                                }
                                .addOnFailureListener {
                                    tvNombreOtro.text = "Usuario desconocido"
                                }
                        }
                    }
                    else -> {
                        // Default: mostrar como texto
                        if (isPropio) {
                            layoutMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.visibility = View.VISIBLE
                            tvMensajePropio.text = mensaje.texto ?: "Mensaje desconocido"
                        } else {
                            layoutMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.visibility = View.VISIBLE
                            tvMensajeOtro.text = mensaje.texto ?: "Mensaje desconocido"
                        }
                    }
                }

                // Long click para eliminar mensaje (solo admin)
                itemView.setOnLongClickListener {
                    onMensajeLongClick(mensaje)
                    true
                }
            } catch (e: Exception) {
                Log.e("MensajesAdapter", "Error al bindear mensaje: ${e.message}", e)
                // Mostrar mensaje de error
                if (mensaje.remitente == currentUserUid) {
                    layoutMensajePropio.visibility = View.VISIBLE
                    tvMensajePropio.visibility = View.VISIBLE
                    tvMensajePropio.text = "Error al cargar mensaje"
                } else {
                    layoutMensajeOtro.visibility = View.VISIBLE
                    tvMensajeOtro.visibility = View.VISIBLE
                    tvMensajeOtro.text = "Error al cargar mensaje"
                }
            }
        }

        private fun mostrarImagenGrande(url: String?) {
            if (url.isNullOrEmpty()) return

            val context = itemView.context
            val dialog = AlertDialog.Builder(context).create() // Usar un tema de diálogo de pantalla completa (si lo tienes)

            // Crear un ImageView nuevo para el diálogo
            val imageView = ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT // Cambiar a WRAP_CONTENT para mejor ajuste
                )
                adjustViewBounds = true // Permite que ImageView ajuste su tamaño para mantener la relación de aspecto
            }

            // Usar Glide para cargar la imagen grande
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_broken_image) // Añade un placeholder visual
                .error(R.drawable.ic_broken_image) // Añade un ícono de error
                .into(imageView)

            dialog.setView(imageView)
            dialog.show()
        }

        private fun mostrarVideoGrande(url: String?) {
            if (url.isNullOrEmpty()) return
            val dialog = AlertDialog.Builder(itemView.context).create()
            val playerView = StyledPlayerView(itemView.context)
            playerView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            val player = ExoPlayer.Builder(itemView.context).build()
            playerView.player = player

            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            dialog.setView(playerView)
            dialog.setOnDismissListener {
                player.release()  // Liberar recursos al cerrar
            }
            dialog.show()
        }
    }
}