package com.example.mydreamtrip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private lateinit var commentAdapter: CommentAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = PostDetailsFragmentArgs.fromBundle(requireArguments())

        // ---- Main post UI ----
        view.findViewById<TextView>(R.id.txtDetailsTitle).text = args.title
        view.findViewById<TextView>(R.id.txtDetailsLocation).text = args.location
        view.findViewById<TextView>(R.id.txtDetailsRating).text = args.ratingText
        view.findViewById<TextView>(R.id.txtDetailsAuthor).text = args.author

        val imgDetails = view.findViewById<ImageView>(R.id.imgDetails)
        if (!args.localImageUri.isNullOrBlank()) {
            Picasso.get()
                .load(Uri.parse(args.localImageUri))
                .fit()
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imgDetails)
        } else {
            imgDetails.setImageResource(args.imageRes)
        }

        // ---- Top buttons ----
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeletePost)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditPost)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { findNavController().popBackStack() }

        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
        val currentUsername = currentEmail?.substringBefore("@")
        val isOwner = !currentUsername.isNullOrBlank() && currentUsername.equals(args.author, ignoreCase = true)

        btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE

        val postRef = db.collection("posts").document(args.postId)

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete post?")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ ->
                    postRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), e.message ?: "Delete failed", Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnEdit.setOnClickListener {
            val action = PostDetailsFragmentDirections
                .actionPostDetailsFragmentToEditPostFragment(
                    postId = args.postId,
                    title = args.title,
                    location = args.location,
                    ratingText = args.ratingText,
                    imageRes = args.imageRes
                )
            findNavController().navigate(action)
        }

        // ---- Wikipedia section ----
        val wikiBox = view.findViewById<View>(R.id.wikiBox)
        val wikiImg = view.findViewById<ImageView>(R.id.imgWiki)
        val wikiTitle = view.findViewById<TextView>(R.id.txtWikiTitle)
        val wikiExtract = view.findViewById<TextView>(R.id.txtWikiExtract)
        val btnOpenWiki = view.findViewById<Button>(R.id.btnOpenWiki)

        val hasWiki = args.wikiTitle.isNotBlank() || args.wikiExtract.isNotBlank() || args.wikiUrl.isNotBlank()

        if (hasWiki) {
            wikiBox.visibility = View.VISIBLE
            wikiTitle.text = if (args.wikiTitle.isNotBlank()) args.wikiTitle else args.location
            wikiExtract.text = args.wikiExtract

            if (args.wikiImageUrl.isNotBlank()) {
                wikiImg.visibility = View.VISIBLE
                Picasso.get()
                    .load(args.wikiImageUrl)
                    .fit()
                    .centerCrop()
                    .into(wikiImg)
            } else {
                wikiImg.visibility = View.GONE
            }

            btnOpenWiki.isEnabled = args.wikiUrl.isNotBlank()
            btnOpenWiki.setOnClickListener {
                if (args.wikiUrl.isBlank()) return@setOnClickListener
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(args.wikiUrl)))
            }
        } else {
            wikiBox.visibility = View.GONE
        }

        // ---- Comments ----
        val rv = view.findViewById<RecyclerView>(R.id.rvComments)
        rv.layoutManager = LinearLayoutManager(requireContext())
        commentAdapter = CommentAdapter(mutableListOf())
        rv.adapter = commentAdapter

        postRef.collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    val a = doc.getString("author") ?: return@mapNotNull null
                    val t = doc.getString("text") ?: return@mapNotNull null
                    Comment(a, t)
                }
                commentAdapter = CommentAdapter(list.toMutableList())
                rv.adapter = commentAdapter
            }

        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnAdd = view.findViewById<Button>(R.id.btnAddComment)

        btnAdd.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener

            val email = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
            val author = email.substringBefore("@")

            postRef.collection("comments").add(
                mapOf(
                    "author" to author,
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener { etComment.setText("") }
        }
    }
}
