package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.mydreamtrip.data.remote.wiki.WikiRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EditPostFragment : Fragment(R.layout.fragment_edit_post) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val wikiRepo by lazy { WikiRepository() }

    private var selectedImageUri: Uri? = null
    private var pickedNewImage: Boolean = false
    private var selectedRating: Int = 0

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                pickedNewImage = true
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
        val etAboutTrip = view.findViewById<TextInputEditText>(R.id.etEditAboutTrip)
        val etLocation = view.findViewById<TextInputEditText>(R.id.etEditLocation)
        val btnSave = view.findViewById<Button>(R.id.btnSaveEdit)
        val postRef = db.collection("posts").document(postId)

        val ratingStars = listOf(
            view.findViewById<TextView>(R.id.starEditRating1),
            view.findViewById<TextView>(R.id.starEditRating2),
            view.findViewById<TextView>(R.id.starEditRating3),
            view.findViewById<TextView>(R.id.starEditRating4),
            view.findViewById<TextView>(R.id.starEditRating5)
        )

        fun renderRatingStars(value: Int) {
            ratingStars.forEachIndexed { idx, star ->
                if (idx < value) {
                    star.text = "⭐"
                    star.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                } else {
                    star.text = "☆"
                    star.setTextColor(android.graphics.Color.parseColor("#999999"))
                }
            }
        }

        ratingStars.forEachIndexed { idx, star ->
            star.setOnClickListener {
                selectedRating = idx + 1
                renderRatingStars(selectedRating)
            }
        }

        // Function to check if there are unsaved changes
        fun hasUnsavedChanges(): Boolean {
            val currentTitle = etTitle.text.toString().trim()
            val currentAboutTrip = etAboutTrip.text.toString().trim()
            val currentLocation = etLocation.text.toString().trim()
            
            val titleChanged = currentTitle != args.title
            val locationChanged = currentLocation != args.location
            val ratingChanged = selectedRating != args.ratingText.count { it == '⭐' }
            val photoChanged = pickedNewImage
            
            return titleChanged || locationChanged || ratingChanged || photoChanged
        }

        // Set up back button
        btnBack.setOnClickListener {
            if (hasUnsavedChanges()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to leave without saving?")
                    .setPositiveButton("Leave") { _, _ ->
                        findNavController().popBackStack()
                    }
                    .setNegativeButton("Continue Editing", null)
                    .show()
            } else {
                findNavController().popBackStack()
            }
        }

        // Prefill fields
        etTitle.setText(args.title)
        etLocation.setText(args.location)
        val initialStarsCount = args.ratingText.count { it == '⭐' }
        selectedRating = if (initialStarsCount in 1..5) {
            initialStarsCount
        } else {
            Regex("(\\d+(?:\\.\\d+)?)")
                .find(args.ratingText)
                ?.value
                ?.toDoubleOrNull()
                ?.toInt()
                ?.coerceIn(1, 5)
                ?: 0
        }
        renderRatingStars(selectedRating)
        postRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                etAboutTrip.setText(snapshot.getString("aboutTrip") ?: "")
            }
        }

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
            val newAboutTrip = etAboutTrip.text.toString().trim()
            val newLocation = etLocation.text.toString().trim()

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

            val newRatingStars = "⭐".repeat(selectedRating.coerceIn(0, 5))

            // Clear any previous errors
            etTitle.error = null
            etLocation.error = null

            // Show loading state
            btnSave.isEnabled = false
            btnSave.text = "Saving..."

            val currentImageUri = selectedImageUri?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: args.localImageUri

            val baseUpdate = mutableMapOf<String, Any>(
                "title" to newTitle,
                "aboutTrip" to newAboutTrip,
                "location" to newLocation,
                "ratingText" to newRatingStars,
                "localImageUri" to (currentImageUri ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            fun savePost(updateMap: Map<String, Any>) {
                // update() preserves all other fields (including wiki info)
                postRef.update(updateMap)
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

            fun saveWithImageIfNeeded(updateMap: MutableMap<String, Any>) {
                if (pickedNewImage && selectedImageUri != null) {
                    val imageRef = FirebaseStorage.getInstance().reference
                        .child("post_images/$postId.jpg")

                    imageRef.putFile(selectedImageUri!!)
                        .addOnSuccessListener {
                            imageRef.downloadUrl
                                .addOnSuccessListener { downloadUri ->
                                    updateMap["localImageUri"] = downloadUri.toString()
                                    savePost(updateMap)
                                }
                                .addOnFailureListener {
                                    // fallback to local uri if download URL retrieval fails
                                    savePost(updateMap)
                                }
                        }
                        .addOnFailureListener {
                            // if Storage upload is blocked by rules/network, still save the post
                            // using local image URI so edit operation succeeds
                            savePost(updateMap)
                        }
                } else {
                    savePost(updateMap)
                }
            }

            val locationChanged = !newLocation.equals(args.location, ignoreCase = true)
            if (locationChanged) {
                btnSave.text = "Updating destination info..."
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { wikiRepo.fetchDestinationInfo(newLocation) }
                        .onSuccess { info ->
                            baseUpdate["wikiTitle"] = info.wikiTitle
                            baseUpdate["wikiExtract"] = info.wikiExtract
                            baseUpdate["wikiUrl"] = info.wikiUrl ?: ""
                            baseUpdate["wikiImageUrl"] = info.wikiImageUrl ?: ""
                        }
                        .onFailure { e ->
                            if (e is HttpException && e.code() == 404) {
                                Toast.makeText(
                                    requireContext(),
                                    "Could Not Find A Match On Wikipedia",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    saveWithImageIfNeeded(baseUpdate)
                }
            } else {
                saveWithImageIfNeeded(baseUpdate)
            }
        }
    }
}
