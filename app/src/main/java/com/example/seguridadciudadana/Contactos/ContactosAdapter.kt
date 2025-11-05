package com.example.seguridadciudadana.Contactos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R

class ContactosAdapter(
    private val contactos: List<Contacto>,
    private val onContactoClick: (Contacto) -> Unit,
    private val onContactoLongClick: (Contacto) -> Unit
) : RecyclerView.Adapter<ContactosAdapter.ContactoViewHolder>() {

    inner class ContactoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_contacto)
        val tvCorreo: TextView = itemView.findViewById(R.id.tv_correo_contacto)
        val tvTelefono: TextView = itemView.findViewById(R.id.tv_telefono_contacto)

        fun bind(contacto: Contacto) {
            tvNombre.text = contacto.nombre
            tvCorreo.text = contacto.correo
            tvTelefono.text = if (contacto.telefono.isNotEmpty()) contacto.telefono else "Sin teléfono"

            itemView.setOnClickListener {
                onContactoClick(contacto)
            }

            itemView.setOnLongClickListener {
                onContactoLongClick(contacto)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        holder.bind(contactos[position])
    }

    override fun getItemCount() = contactos.size
}