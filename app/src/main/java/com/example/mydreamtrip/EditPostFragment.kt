package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EditPostFragment : Fragment(R.layout.fragment_edit_post) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = EditPostFragmentArgs.fromBundle(requireArguments())

        val etTitle = view.findViewById<EditText>(R.id.etEditTitle)
        val etLocation = view.findViewById<EditText>(R.id.etEditLocation)
        val etRating = view.findViewById<EditText>(R.id.etEditRating)
        val btnSave = view.findViewById<Button>(R.id.btnSaveEdit)

        // Prefill
        etTitle.setText(args.title)
        etLocation.setText(args.location)
        etRating.setText(args.ratingText)

        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newLocation = etLocation.text.toString().trim()
            val newRating = etRating.text.toString().trim()

            if (newTitle.isBlank() || newLocation.isBlank()) {
                Toast.makeText(requireContext(), "Please fill Title + Location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val update = mapOf(
                "title" to newTitle,
                "location" to newLocation,
                "ratingText" to (if (newRating.isBlank()) "⭐ 0.0 (0)" else newRating),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("posts")
                .document(args.postId)
                .update(update)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Post updated", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message ?: "Update failed", Toast.LENGTH_LONG).show()
                }
        }
    }
}
