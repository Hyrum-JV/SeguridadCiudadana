package com.example.seguridadciudadana.Configuraciones

import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ContactItem
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.adapter.ContactAdapter

class Config_SeleccionarContacto : Fragment() {

    private lateinit var adapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config_seleccionar_contacto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerContactos)
        val search = view.findViewById<SearchView>(R.id.searchViewContacts)

        adapter = ContactAdapter { selected ->
            // pasar resultado al fragment anterior
            setFragmentResult(
                "requestKeyContacto",
                bundleOf("name" to selected.name, "phone" to selected.phone)
            )
            parentFragmentManager.popBackStack()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // filtro de búsqueda
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        // cargar contactos
        loadContacts()
    }

    private fun loadContacts() {
        val list = mutableListOf<ContactItem>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val phone = it.getString(phoneIdx) ?: ""
                list.add(ContactItem(name, phone))
            }
        }
        adapter.submitList(list)
    }

}