package com.example.seguridadciudadana.Contactos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seguridadciudadana.databinding.ActivityContactosFragmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactosFragment : Fragment() {

    private var _binding: ActivityContactosFragmentBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ContactosAdapter
    private val listaContactos = mutableListOf<Contacto>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityContactosFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        binding.rvContactos.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactosAdapter(listaContactos)
        binding.rvContactos.adapter = adapter

        // Cargar contactos del usuario
        cargarContactos()

        // Escanear QR → abrir actividad de escaneo
        binding.fabEscanearQr.setOnClickListener {
            val intent = Intent(requireContext(), EscanearQRActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cargarContactos() {
        val user = auth.currentUser ?: return
        db.collection("usuarios")
            .document(user.uid)
            .collection("contactos")
            .get()
            .addOnSuccessListener { snapshot ->
                listaContactos.clear()
                for (doc in snapshot) {
                    val contacto = Contacto(
                        nombre = doc.getString("nombre") ?: "",
                        correo = doc.getString("correo") ?: "",
                        telefono = doc.getString("telefono") ?: ""
                    )
                    listaContactos.add(contacto)
                }

                // Mostrar u ocultar el estado vacío
                if (listaContactos.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvContactos.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvContactos.visibility = View.VISIBLE
                }

                binding.tvContadorContactos.text = "${listaContactos.size} contacto(s)"
                adapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        cargarContactos() // Para refrescar la lista cuando volvamos del escáner
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
