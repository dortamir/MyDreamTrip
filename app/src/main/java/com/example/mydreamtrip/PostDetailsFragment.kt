package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private lateinit var commentAdapter: CommentAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = PostDetailsFragmentArgs.fromBundle(requireArguments())

        view.findViewById<TextView>(R.id.txtDetailsTitle).text = args.title
        view.findViewById<TextView>(R.id.txtDetailsLocation).text = args.location
        view.findViewById<TextView>(R.id.txtDetailsRating).text = args.ratingText
        view.findViewById<TextView>(R.id.txtDetailsAuthor).text = args.author
        view.findViewById<ImageView>(R.id.imgDetails).setImageResource(args.imageRes)

        val postRef = db.collection("posts").document(args.postId)
        postRef.set(
            mapOf(
                "title" to args.title,
                "location" to args.location,
                "ratingText" to args.ratingText,
                "author" to args.author,
                "imageRes" to args.imageRes,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )

        val rv = view.findViewById<RecyclerView>(R.id.rvComments)
        rv.layoutManager = LinearLayoutManager(requireContext())

        commentAdapter = CommentAdapter(mutableListOf())
        rv.adapter = commentAdapter

        postRef.collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    val author = doc.getString("author") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: return@mapNotNull null
                    Comment(author, text)
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
            ).addOnSuccessListener {
                etComment.setText("")
            }
        }
    }
}
