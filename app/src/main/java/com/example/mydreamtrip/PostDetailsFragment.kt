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

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    private lateinit var commentAdapter: CommentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = PostDetailsFragmentArgs.fromBundle(requireArguments())

        view.findViewById<TextView>(R.id.txtDetailsTitle).text = args.title
        view.findViewById<TextView>(R.id.txtDetailsLocation).text = args.location
        view.findViewById<TextView>(R.id.txtDetailsRating).text = args.ratingText
        view.findViewById<TextView>(R.id.txtDetailsAuthor).text = args.author
        view.findViewById<ImageView>(R.id.imgDetails).setImageResource(args.imageRes)

        val rv = view.findViewById<RecyclerView>(R.id.rvComments)
        rv.layoutManager = LinearLayoutManager(requireContext())

        val initial = mutableListOf(
            Comment("Noa", "Looks amazing 😍"),
            Comment("Yuval", "Need to try this soon!"),
            Comment("Dor", "Saving it for my next trip ✈️")
        )

        commentAdapter = CommentAdapter(initial)
        rv.adapter = commentAdapter

        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnAdd = view.findViewById<Button>(R.id.btnAddComment)

        btnAdd.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener

            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
            val author = userEmail.substringBefore("@") // שם קצר

            commentAdapter.addComment(Comment(author, text))
            rv.scrollToPosition(0)
            etComment.setText("")
        }
    }
}
