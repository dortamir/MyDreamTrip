package com.example.mydreamtrip.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.R
import com.example.mydreamtrip.model.Destination

class DestinationAdapter(
    private var items: List<Destination>,
    private val onClick: (Destination) -> Unit
) : RecyclerView.Adapter<DestinationAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txtCardTitle)
        val location: TextView = itemView.findViewById(R.id.txtLocation)
        val rating: TextView = itemView.findViewById(R.id.txtRating)
        val author: TextView = itemView.findViewById(R.id.txtAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.location.text = item.location
        holder.rating.text = item.ratingText
        holder.author.text = item.author
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Destination>) {
        items = newItems
        notifyDataSetChanged()
    }
}
