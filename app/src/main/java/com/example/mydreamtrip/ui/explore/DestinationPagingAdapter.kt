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
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DestinationPagingAdapter(
    private val onClick: (Destination) -> Unit
) : PagingDataAdapter<Destination, DestinationPagingAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        val imgAuthor: ImageView = itemView.findViewById(R.id.imgAuthor)
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
        holder.rating.text = normalizeRating(item.ratingText)
        holder.author.text = item.author

        if (item.authorUid.isNotBlank()) {
            holder.itemView.setTag(R.id.txtAuthor, item.authorUid)
            val cachedName = authorNameCache[item.authorUid]
            if (!cachedName.isNullOrBlank()) {
                holder.author.text = cachedName
            } else {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(item.authorUid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val username = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: return@addOnSuccessListener
                        authorNameCache[item.authorUid] = username
                        val boundUid = holder.itemView.getTag(R.id.txtAuthor) as? String
                        if (boundUid == item.authorUid && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                            holder.author.text = username
                        }
                    }
            }
        }

        if (item.authorPhotoUrl.isNotBlank()) {
            Picasso.get()
                .load(item.authorPhotoUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .fit()
                .centerCrop()
                .into(holder.imgAuthor)
        } else if (item.authorUid.isNotBlank()) {
            val cached = photoCache[item.authorUid]
            if (cached != null) {
                Picasso.get()
                    .load(cached)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .fit()
                    .centerCrop()
                    .into(holder.imgAuthor)
            } else {
                holder.imgAuthor.setImageResource(R.drawable.ic_profile)
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(item.authorUid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.getString("displayName")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { username ->
                                authorNameCache[item.authorUid] = username
                                val boundUid = holder.itemView.getTag(R.id.txtAuthor) as? String
                                if (boundUid == item.authorUid && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                                    holder.author.text = username
                                }
                            }

                        val url = snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() }
                            ?: snapshot.getString("photoLocalUri")?.takeIf { it.isNotBlank() }
                            ?: return@addOnSuccessListener
                        photoCache[item.authorUid] = url
                        if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                            Picasso.get()
                                .load(url)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .fit()
                                .centerCrop()
                                .into(holder.imgAuthor)
                        }
                    }
            }
        } else {
            holder.imgAuthor.setImageResource(R.drawable.ic_profile)
        }

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
        private val photoCache = mutableMapOf<String, String>()
        private val authorNameCache = mutableMapOf<String, String>()

        private fun normalizeRating(raw: String): String {
            val stars = raw.count { it == '⭐' }
            if (stars in 1..5) return "⭐".repeat(stars)

            val numeric = raw.toIntOrNull()
            if (numeric != null && numeric in 1..5) return "⭐".repeat(numeric)

            return raw
        }

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
