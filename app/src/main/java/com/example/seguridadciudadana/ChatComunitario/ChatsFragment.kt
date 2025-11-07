package com.example.seguridadciudadana

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ChatComunitario.Chat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatsFragment : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabCrearChat: FloatingActionButton

    private lateinit var chatsAdapter: ChatsAdapter
    private val chatsList = mutableListOf<Chat>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChatsFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_chats_fragment, container, false)

        rvChats = view.findViewById(R.id.rv_chats)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        fabCrearChat = view.findViewById(R.id.fab_crear_chat)

        configurarRecyclerView()

        fabCrearChat.setOnClickListener {
            startActivity(Intent(requireContext(), CrearChatActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarChats()
    }

    private fun configurarRecyclerView() {
        chatsAdapter = ChatsAdapter(chatsList) { chat ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("chatId", chat.id)
                putExtra("chatNombre", chat.nombre)
            }
            startActivity(intent)
        }

        rvChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
        }
    }

    private fun cargarChats() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            return
        }

        // NUEVO: Log para verificar UID
        Log.d(TAG, "UID actual: ${currentUser.uid}")

        db.collection("chats")
            .whereArrayContains("miembrosUids", currentUser.uid) // Campo calculado para query
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Error cargando chats: ${e.message}")
                    return@addSnapshotListener
                }

                // NUEVO: Log para número de chats
                Log.d(TAG, "Chats encontrados: ${snapshots?.size()}")

                chatsList.clear()
                snapshots?.forEach { doc ->
                    val chat = doc.toObject(Chat::class.java).copy(id = doc.id)
                    // NUEVO: Log para detalles del chat
                    Log.d(TAG, "Chat: ${chat.nombre}, Miembros: ${chat.miembrosUids}")
                    chatsList.add(chat)
                }

                chatsAdapter.notifyDataSetChanged()
                actualizarUI()
            }
    }

    private fun actualizarUI() {
        if (chatsList.isEmpty()) {
            rvChats.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvChats.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }
}