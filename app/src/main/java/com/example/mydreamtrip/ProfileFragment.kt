package com.example.mydreamtrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: DestinationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)
        val rvMyPosts = view.findViewById<RecyclerView>(R.id.rvMyPosts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyMyPosts)

        val user = FirebaseAuth.getInstance().currentUser
        txtEmail.text = user?.email ?: "Guest"

        // RecyclerView
        rvMyPosts.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(
            emptyList(),
            onClick = { dest ->
                val bundle = Bundle().apply {
                    putString("postId", dest.id)
                    putString("title", dest.title)
                    putString("location", dest.location)
                    putString("ratingText", dest.ratingText)
                    putString("author", dest.author)
                    putInt("imageRes", dest.imageRes)
                }
                findNavController().navigate(R.id.postDetailsFragment, bundle)
            },
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
        rvMyPosts.adapter = adapter

        // Load only my posts
        val email = user?.email
        if (!email.isNullOrBlank()) {
            val author = email.substringBefore("@")

            db.collection("posts")
                .whereEqualTo("author", author)
                .addSnapshotListener { snap: QuerySnapshot?, err: FirebaseFirestoreException? ->

                    if (err != null || snap == null) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = err?.message ?: "Failed to load posts"
                        return@addSnapshotListener
                    }

                    val items = snap.documents.map { doc ->
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
                    adapter.submitList(items)
                }
        }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
