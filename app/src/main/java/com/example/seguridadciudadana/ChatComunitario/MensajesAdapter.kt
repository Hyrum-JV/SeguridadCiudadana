package com.example.seguridadciudadana.ChatComunitario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MensajeAdapter(private val mensajes: List<Mensaje>) : RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder>() {

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
        private val tvMensajePropio: TextView = itemView.findViewById(R.id.tv_mensaje_propio)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)

        fun bind(mensaje: Mensaje) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timestamp = sdf.format(Date(mensaje.timestamp))
            tvTimestamp.text = timestamp

            if (mensaje.remitente == currentUserUid) {
                // Mensaje propio: derecha
                layoutMensajeOtro.visibility = View.GONE
                tvMensajePropio.visibility = View.VISIBLE
                tvMensajePropio.text = mensaje.texto
            } else {
                // Mensaje de otro: izquierda con nombre
                layoutMensajeOtro.visibility = View.VISIBLE
                tvMensajePropio.visibility = View.GONE
                tvMensajeOtro.text = mensaje.texto

                // Obtener nombre del remitente
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
    }
}