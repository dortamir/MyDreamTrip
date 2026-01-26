package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.model.Destination
import com.example.mydreamtrip.ui.explore.DestinationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: DestinationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recyclerMyPosts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyMyPosts)

        rv.layoutManager = GridLayoutManager(requireContext(), 2)

        // Adapter עם delete
        adapter = DestinationAdapter(
            emptyList(),
            onClick = { _: Destination -> /* כרגע לא עושים ניווט מכאן */ },
            onDelete = { post ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete post?")
                    .setMessage("Are you sure you want to delete this post?")
                    .setPositiveButton("Delete") { _, _ ->
                        db.collection("posts")
                            .document(post.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    e.message ?: "Delete failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rv.adapter = adapter

        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrBlank()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login to see your posts"
            return
        }

        val author = email.substringBefore("@")

        db.collection("posts")
            .whereEqualTo("author", author)
            .addSnapshotListener { snap: QuerySnapshot?, err: FirebaseFirestoreException? ->

                if (err != null || snap == null) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = err?.message ?: "Failed to load"
                    return@addSnapshotListener
                }

                val items: List<Destination> = snap.documents.map { doc ->
                    Destination(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        location = doc.getString("location") ?: "",
                        ratingText = doc.getString("ratingText") ?: "",
                        author = doc.getString("author") ?: "",
                        imageRes = (doc.getLong("imageRes")
                            ?: android.R.drawable.ic_menu_gallery.toLong()).toInt()
                    )
                }

                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No posts yet"

                adapter.submitList(items)
            }
    }
}
