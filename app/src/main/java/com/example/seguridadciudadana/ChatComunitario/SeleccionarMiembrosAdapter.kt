package com.example.seguridadciudadana

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R

class SeleccionarMiembrosAdapter(
    private val contactos: List<String>, // Lista de UIDs
    private val seleccionados: MutableList<String>
) : RecyclerView.Adapter<SeleccionarMiembrosAdapter.MiembroViewHolder>() {

    inner class MiembroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_miembro)
        val checkBox: CheckBox = itemView.findViewById(R.id.cb_seleccionar_miembro)

        fun bind(uid: String) {
            tvNombre.text = uid // TODO: Obtener nombre real de Firestore si es necesario
            checkBox.isChecked = seleccionados.contains(uid)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!seleccionados.contains(uid)) seleccionados.add(uid)
                } else {
                    seleccionados.remove(uid)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiembroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_miembro_seleccion, parent, false)
        return MiembroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiembroViewHolder, position: Int) {
        holder.bind(contactos[position])
    }

    override fun getItemCount() = contactos.size
}