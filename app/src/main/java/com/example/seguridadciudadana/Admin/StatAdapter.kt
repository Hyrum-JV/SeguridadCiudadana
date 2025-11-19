package com.example.seguridadciudadana.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.R

data class StatItem(val title: String, val count: Int, val iconRes: Int)

class StatAdapter : RecyclerView.Adapter<StatAdapter.StatViewHolder>() {

    private var stats = listOf<StatItem>()

    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_stat_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_stat_title)
        val tvCount: TextView = itemView.findViewById(R.id.tv_stat_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat_card, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        val stat = stats[position]
        holder.tvTitle.text = stat.title
        holder.tvCount.text = stat.count.toString()
        holder.ivIcon.setImageResource(stat.iconRes)
    }

    override fun getItemCount() = stats.size

    fun updateStats(newStats: List<StatItem>) {
        stats = newStats
        notifyDataSetChanged()
    }
}