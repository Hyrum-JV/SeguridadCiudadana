package com.example.seguridadciudadana

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SeleccionarMiembrosAdapter(
    private val contactos: List<String>, // Lista de UIDs
    private val seleccionados: MutableList<String>
) : RecyclerView.Adapter<SeleccionarMiembrosAdapter.MiembroViewHolder>() {

    // ✅ Inicializar Firestore fuera del ViewHolder
    private val db = FirebaseFirestore.getInstance()

    inner class MiembroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_miembro)
        val checkBox: CheckBox = itemView.findViewById(R.id.cb_seleccionar_miembro)

        fun bind(uid: String) {
            // Mostrar UID temporalmente mientras carga el nombre
            tvNombre.text = "Cargando..."

            // ✅ CLAVE: Consulta a Firestore para obtener el nombre
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    // El campo 'nombre' debe existir en la colección 'usuarios'
                    val nombre = document.getString("nombre")
                    tvNombre.text = nombre ?: "Usuario desconocido ($uid)"
                }
                .addOnFailureListener {
                    tvNombre.text = "Error al cargar ($uid)"
                }

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