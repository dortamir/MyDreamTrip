package com.example.mydreamtrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.model.Destination
import com.example.mydreamtrip.ui.explore.DestinationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

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

        rvMyPosts.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(
            items = emptyList(),
            onClick = { dest ->
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToPostDetailsFragment(
                        postId = dest.id,
                        title = dest.title,
                        location = dest.location,
                        ratingText = dest.ratingText,
                        author = dest.author,
                        imageRes = dest.imageRes
                    )
                findNavController().navigate(action)
            }
        )
        rvMyPosts.adapter = adapter

        val email = user?.email
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

        // Sign out
        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
