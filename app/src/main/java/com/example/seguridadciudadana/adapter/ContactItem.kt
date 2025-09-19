package com.example.seguridadciudadana.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.ContactItem
import com.example.seguridadciudadana.R

class ContactAdapter(
    private val onClick: (ContactItem) -> Unit
) : RecyclerView.Adapter<ContactAdapter.VH>() {

    private val items = mutableListOf<ContactItem>()
    private val original = mutableListOf<ContactItem>()

    fun submitList(list: List<ContactItem>) {
        items.clear()
        items.addAll(list)
        original.clear()
        original.addAll(list)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            items.clear()
            items.addAll(original)
        } else {
            val filtered = original.filter {
                it.name.contains(q, ignoreCase = true) || it.phone.contains(q)
            }
            items.clear()
            items.addAll(filtered)
        }
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.tvName.text = c.name
        holder.tvPhone.text = c.phone
        holder.tvInitial.text = c.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun getItemCount(): Int = items.size
}