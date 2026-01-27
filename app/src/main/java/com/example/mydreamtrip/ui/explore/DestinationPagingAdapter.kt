package com.example.mydreamtrip.ui.explore

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.R
import com.example.mydreamtrip.model.Destination
import com.squareup.picasso.Picasso

class DestinationPagingAdapter(
    private val onClick: (Destination) -> Unit
) : PagingDataAdapter<Destination, DestinationPagingAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
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
        val item = getItem(position) ?: return

        holder.title.text = item.title
        holder.location.text = item.location
        holder.rating.text = item.ratingText
        holder.author.text = item.author

        val uriStr = item.localImageUri
        if (!uriStr.isNullOrBlank()) {
            Picasso.get()
                .load(Uri.parse(uriStr))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .fit()
                .centerCrop()
                .into(holder.imgCover)
        } else {
            holder.imgCover.setImageResource(item.imageRes)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Destination>() {
            override fun areItemsTheSame(oldItem: Destination, newItem: Destination): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Destination, newItem: Destination): Boolean {
                return oldItem == newItem
            }
        }
    }
}
