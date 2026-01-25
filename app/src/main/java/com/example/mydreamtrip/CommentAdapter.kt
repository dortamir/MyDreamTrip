package com.example.mydreamtrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.model.Comment

class CommentAdapter(
    private var items: MutableList<Comment>
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val author: TextView = itemView.findViewById(R.id.txtCommentAuthor)
        val text: TextView = itemView.findViewById(R.id.txtCommentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.author.text = item.author
        holder.text.text = item.text
    }

    override fun getItemCount(): Int = items.size

    fun addComment(comment: Comment) {
        items.add(0, comment)
        notifyItemInserted(0)
    }
}
