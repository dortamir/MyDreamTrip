package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class PostDetailsFragment : Fragment(R.layout.fragment_post_details) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = PostDetailsFragmentArgs.fromBundle(requireArguments())

        view.findViewById<TextView>(R.id.txtDetailsTitle).text = args.title
        view.findViewById<TextView>(R.id.txtDetailsLocation).text = args.location
        view.findViewById<TextView>(R.id.txtDetailsRating).text = args.ratingText
        view.findViewById<TextView>(R.id.txtDetailsAuthor).text = args.author
        view.findViewById<ImageView>(R.id.imgDetails).setImageResource(args.imageRes)
    }
}
