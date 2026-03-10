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

    private fun normalizeRating(raw: String): String {
        val stars = raw.count { it == '⭐' }
        if (stars in 1..5) return "⭐".repeat(stars)

        val numeric = raw.toIntOrNull()
        if (numeric != null && numeric in 1..5) return "⭐".repeat(numeric)

        return raw
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = PostDetailsFragmentArgs.fromBundle(requireArguments())

        view.findViewById<TextView>(R.id.txtDetailsTitle).text = args.title
        val txtDetailsAbout = view.findViewById<TextView>(R.id.txtDetailsAbout)
        txtDetailsAbout.visibility = View.GONE
        view.findViewById<TextView>(R.id.txtDetailsLocation).text = args.location
        view.findViewById<TextView>(R.id.txtDetailsRating).text = normalizeRating(args.ratingText)
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

        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeletePost)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditPost)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val buttonsContainer = view.findViewById<LinearLayout>(R.id.buttonsContainer)

        btnBack.setOnClickListener { findNavController().popBackStack() }

        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
        val currentUsername = currentEmail?.substringBefore("@")
        val isOwner = !currentUsername.isNullOrBlank() && currentUsername.equals(args.author, ignoreCase = true)

        buttonsContainer?.visibility = if (isOwner) View.VISIBLE else View.GONE

        val postRef = db.collection("posts").document(args.postId)

        val wikiBox = view.findViewById<View>(R.id.wikiBox)
        val wikiTitle = view.findViewById<TextView>(R.id.txtWikiTitle)
        val wikiExtract = view.findViewById<TextView>(R.id.txtWikiExtract)
        val btnOpenWiki = view.findViewById<Button>(R.id.btnOpenWiki)

        postRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val updatedTitle = snapshot.getString("title") ?: args.title
            val updatedAboutTrip = snapshot.getString("aboutTrip") ?: ""
            val updatedLocation = snapshot.getString("location") ?: args.location
            val updatedRating = snapshot.getString("ratingText") ?: args.ratingText
            val updatedAuthor = snapshot.getString("author") ?: args.author
            val updatedLocalUri = snapshot.getString("localImageUri") ?: ""
            val updatedWikiTitle = snapshot.getString("wikiTitle") ?: ""
            val updatedWikiExtract = snapshot.getString("wikiExtract") ?: ""
            val updatedWikiUrl = snapshot.getString("wikiUrl") ?: ""

            view.findViewById<TextView>(R.id.txtDetailsTitle).text = updatedTitle
            if (updatedAboutTrip.isNotBlank()) {
                txtDetailsAbout.visibility = View.VISIBLE
                txtDetailsAbout.text = updatedAboutTrip
            } else {
                txtDetailsAbout.visibility = View.GONE
            }
            view.findViewById<TextView>(R.id.txtDetailsLocation).text = updatedLocation
            view.findViewById<TextView>(R.id.txtDetailsRating).text = normalizeRating(updatedRating)
            view.findViewById<TextView>(R.id.txtDetailsAuthor).text = updatedAuthor

            if (updatedLocalUri.isNotBlank()) {
                Picasso.get().load(Uri.parse(updatedLocalUri)).fit().centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgDetails)
            } else {
                imgDetails.setImageResource(args.imageRes)
            }

            if (updatedWikiTitle.isNotBlank() || updatedWikiExtract.isNotBlank() || updatedWikiUrl.isNotBlank()) {
                wikiBox.visibility = View.VISIBLE
                wikiTitle.text = if (updatedWikiTitle.isNotBlank()) updatedWikiTitle else updatedLocation
                wikiExtract.text = updatedWikiExtract
                btnOpenWiki.isEnabled = updatedWikiUrl.isNotBlank()
                btnOpenWiki.setOnClickListener {
                    if (updatedWikiUrl.isBlank()) return@setOnClickListener
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updatedWikiUrl)))
                }
            } else {
                wikiBox.visibility = View.GONE
            }
        }

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
                    imageRes = args.imageRes,
                    localImageUri = args.localImageUri ?: ""
                )
            findNavController().navigate(action)
        }

        val hasWiki = args.wikiTitle.isNotBlank() || args.wikiExtract.isNotBlank() || args.wikiUrl.isNotBlank()

        if (hasWiki) {
            wikiBox.visibility = View.VISIBLE
            wikiTitle.text = if (args.wikiTitle.isNotBlank()) args.wikiTitle else args.location
            wikiExtract.text = args.wikiExtract


            btnOpenWiki.isEnabled = args.wikiUrl.isNotBlank()
            btnOpenWiki.setOnClickListener {
                if (args.wikiUrl.isBlank()) return@setOnClickListener
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(args.wikiUrl)))
            }
        } else {
            wikiBox.visibility = View.GONE
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvComments)
        rv.layoutManager = LinearLayoutManager(requireContext())
        commentAdapter = CommentAdapter(mutableListOf(), currentUsername)
        rv.adapter = commentAdapter

        val tvNoComments = view.findViewById<TextView>(R.id.tvNoComments)

        postRef.collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    val a = doc.getString("author") ?: return@mapNotNull null
                    val t = doc.getString("text") ?: return@mapNotNull null
                    Comment(doc.id, a, t)
                }
                val onDeleteComment: (Comment) -> Unit = { comment ->
                    if (currentUsername.isNullOrBlank() || !comment.author.equals(currentUsername, ignoreCase = true)) {
                        Toast.makeText(requireContext(), "You can delete only your comments", Toast.LENGTH_SHORT).show()
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete comment?")
                            .setMessage("Are you sure you want to delete this comment?")
                            .setPositiveButton("Delete") { _, _ ->
                                postRef.collection("comments").document(comment.id).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), e.message ?: "Delete failed", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                
                commentAdapter = CommentAdapter(list.toMutableList(), currentUsername, onDeleteComment)
                rv.adapter = commentAdapter

                tvNoComments.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
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


