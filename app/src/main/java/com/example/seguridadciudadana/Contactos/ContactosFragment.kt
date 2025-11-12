package com.example.seguridadciudadana.Contactos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactosFragment : Fragment() {

    private lateinit var rvContactos: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvContadorContactos: TextView
    private lateinit var etBuscarContacto: EditText
    private lateinit var fabEscanearQr: FloatingActionButton

    private lateinit var contactosAdapter: ContactosAdapter
    private val contactosList = mutableListOf<Contacto>()
    private val contactosListFiltrados = mutableListOf<Contacto>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ContactosFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        val view = inflater.inflate(R.layout.activity_contactos_fragment, container, false)

        rvContactos = view.findViewById(R.id.rv_contactos)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tvContadorContactos = view.findViewById(R.id.tv_contador_contactos)
        etBuscarContacto = view.findViewById(R.id.et_buscar_contacto)
        fabEscanearQr = view.findViewById(R.id.fab_escanear_qr)

        configurarRecyclerView()
        configurarBusqueda()

        fabEscanearQr.setOnClickListener {
            val intent = Intent(requireContext(), EscanearQRActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarContactos()
    }

    private fun configurarRecyclerView() {
        contactosAdapter = ContactosAdapter(
            contactosListFiltrados,
            onContactoClick = { contacto ->
                mostrarDetallesContacto(contacto)
            },
            onContactoLongClick = { contacto ->
                mostrarMenuOpciones(contacto)
            }
        )

        rvContactos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactosAdapter
        }
    }

    private fun configurarBusqueda() {
        etBuscarContacto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarContactos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarContactos(query: String) {
        contactosListFiltrados.clear()

        if (query.isEmpty()) {
            contactosListFiltrados.addAll(contactosList)
        } else {
            val filtrados = contactosList.filter { contacto ->
                contacto.nombre.contains(query, ignoreCase = true) ||
                        contacto.correo.contains(query, ignoreCase = true) ||
                        contacto.telefono.contains(query, ignoreCase = true)
            }
            contactosListFiltrados.addAll(filtrados)
        }

        contactosAdapter.notifyDataSetChanged()
        actualizarContador()
    }

    private fun cargarContactos() {
        Log.d(TAG, "=== CARGANDO CONTACTOS ===")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            mostrarEstadoVacio()
            return
        }

        db.collection("usuarios")
            .document(currentUser.uid)
            .collection("contactos")
            .get()
            .addOnSuccessListener { documentos ->
                Log.d(TAG, "Documentos obtenidos: ${documentos.size()}")

                contactosList.clear()
                contactosListFiltrados.clear()

                if (documentos.isEmpty) {
                    Log.d(TAG, "No hay contactos")
                    mostrarEstadoVacio()
                } else {
                    for (documento in documentos) {
                        val contacto = Contacto(
                            id = documento.id,
                            nombre = documento.getString("nombre") ?: "Sin nombre",
                            correo = documento.getString("correo") ?: "",
                            telefono = documento.getString("telefono") ?: ""
                        )
                        contactosList.add(contacto)
                        Log.d(TAG, "Contacto cargado: ${contacto.nombre}")
                    }

                    contactosListFiltrados.addAll(contactosList)
                    contactosAdapter.notifyDataSetChanged()
                    mostrarLista()
                    actualizarContador()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar contactos: ${e.message}")
                Toast.makeText(context, "Error al cargar contactos", Toast.LENGTH_SHORT).show()
                mostrarEstadoVacio()
            }
    }

    private fun mostrarMenuOpciones(contacto: Contacto) {
        AlertDialog.Builder(requireContext())
            .setTitle(contacto.nombre)
            .setItems(arrayOf("Ver detalles", "Eliminar")) { _, which ->
                when (which) {
                    0 -> mostrarDetallesContacto(contacto)
                    1 -> confirmarEliminarContacto(contacto)
                }
            }
            .show()
    }

    private fun confirmarEliminarContacto(contacto: Contacto) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar contacto")
            .setMessage("¿Estás seguro de que deseas eliminar a ${contacto.nombre} de tus contactos?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarContacto(contacto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarContacto(contacto: Contacto) {
        Log.d(TAG, "=== ELIMINANDO CONTACTO ===")
        Log.d(TAG, "ID: ${contacto.id}, Nombre: ${contacto.nombre}")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios")
            .document(currentUser.uid)
            .collection("contactos")
            .document(contacto.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Contacto eliminado exitosamente")
                Toast.makeText(context, "Contacto eliminado: ${contacto.nombre}", Toast.LENGTH_SHORT).show()
                cargarContactos()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al eliminar contacto: ${e.message}")
                Toast.makeText(context, "Error al eliminar contacto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDetallesContacto(contacto: Contacto) {
        val mensaje = buildString {
            append("Nombre: ${contacto.nombre}\n")
            if (contacto.telefono.isNotEmpty()) append("Teléfono: ${contacto.telefono}\n")
            if (contacto.correo.isNotEmpty()) append("Correo: ${contacto.correo}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Información del contacto")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun mostrarEstadoVacio() {
        rvContactos.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvContadorContactos.text = "0 contactos"
    }

    private fun mostrarLista() {
        rvContactos.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
    }

    private fun actualizarContador() {
        val count = contactosListFiltrados.size
        tvContadorContactos.text = if (count == 1) "1 contacto" else "$count contactos"
    }
}

