package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import android.net.Uri
import android.widget.ImageView
import com.squareup.picasso.Picasso
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditPostFragment : Fragment(R.layout.fragment_edit_post) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var selectedImageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                view?.findViewById<ImageView>(R.id.imgEditSelected)?.let { img ->
                    Picasso.get().load(uri).fit().centerCrop().into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = EditPostFragmentArgs.fromBundle(requireArguments())
        val postId = args.postId

        if (postId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid post ID, cannot edit", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        val btnBack = view.findViewById<ImageButton>(R.id.btnBackEdit)
        val etTitle = view.findViewById<TextInputEditText>(R.id.etEditTitle)
        val etLocation = view.findViewById<TextInputEditText>(R.id.etEditLocation)
        val etRating = view.findViewById<TextInputEditText>(R.id.etEditRating)
        val btnSave = view.findViewById<Button>(R.id.btnSaveEdit)

        // Set up back button
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Prefill fields
        etTitle.setText(args.title)
        etLocation.setText(args.location)
        etRating.setText(args.ratingText)

        // prefill image if available
        val imgView = view.findViewById<ImageView>(R.id.imgEditSelected)
        val btnPhoto = view.findViewById<Button>(R.id.btnSelectPhotoEdit)
        btnPhoto.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        if (!args.localImageUri.isNullOrBlank()) {
            selectedImageUri = Uri.parse(args.localImageUri)
            Picasso.get().load(selectedImageUri).fit().centerCrop().into(imgView)
        } else {
            imgView.setImageResource(args.imageRes)
        }

        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newLocation = etLocation.text.toString().trim()
            val newRating = etRating.text.toString().trim()

            // Enhanced validation
            if (newTitle.isBlank()) {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (newLocation.isBlank()) {
                etLocation.error = "Location is required"
                etLocation.requestFocus()
                return@setOnClickListener
            }

            // Clear any previous errors
            etTitle.error = null
            etLocation.error = null

            // Show loading state
            btnSave.isEnabled = false
            btnSave.text = "Saving..."

            val update = mapOf(
                "title" to newTitle,
                "location" to newLocation,
                "ratingText" to (if (newRating.isBlank()) "⭐ 0.0 (0)" else newRating),
                "localImageUri" to (selectedImageUri?.toString() ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val postRef = db.collection("posts").document(postId)
            // use set with merge so we don't fail if doc was removed (creates it if needed)
            postRef.set(update, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"
                    val msg = when {
                        e.message?.contains("NOT_FOUND") == true -> "Post not found (maybe deleted)"
                        else -> "Failed to update post: ${e.message ?: "Unknown error"}"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
        }
    }
}
