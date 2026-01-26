package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AddFragment : Fragment(R.layout.fragment_add) {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private var selectedImageUri: Uri? = null
    private lateinit var imgSelected: ImageView

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Glide.with(this).load(uri).into(imgSelected)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etRating = view.findViewById<EditText>(R.id.etRating)
        val tvStatus = view.findViewById<TextView>(R.id.tvAddStatus)
        val btnCreate = view.findViewById<Button>(R.id.btnCreatePost)

        imgSelected = view.findViewById(R.id.imgSelected)
        val btnSelectPhoto = view.findViewById<Button>(R.id.btnSelectPhoto)

        btnSelectPhoto.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        fun goToExplore() {
            val navHost =
                requireActivity().supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
            val navController = navHost.navController

            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.main_graph, false)
                .build()

            navController.navigate(R.id.exploreFragment, null, options)

            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.exploreFragment
        }

        btnCreate.setOnClickListener {
            tvStatus.text = ""

            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val ratingText = etRating.text.toString().trim()

            if (title.isBlank() || location.isBlank()) {
                tvStatus.text = "Please fill Title + Location"
                return@setOnClickListener
            }

            val email = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
            val author = email.substringBefore("@")

            val docRef = db.collection("posts").document()

            val baseData = hashMapOf(
                "title" to title,
                "location" to location,
                "ratingText" to (if (ratingText.isBlank()) "⭐ 0.0 (0)" else ratingText),
                "author" to author,
                "imageRes" to android.R.drawable.ic_menu_gallery,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val imageUri = selectedImageUri

            if (imageUri == null) {
                docRef.set(baseData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Post Created", Toast.LENGTH_SHORT).show()
                        goToExplore()
                    }
                    .addOnFailureListener { e ->
                        tvStatus.text = e.message ?: "Failed to create post"
                    }
                return@setOnClickListener
            }

            tvStatus.text = "Uploading image..."

            val fileRef = storage.reference.child("posts/${docRef.id}.jpg")
            fileRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    fileRef.downloadUrl
                }
                .addOnSuccessListener { url ->
                    val dataWithImage = HashMap(baseData)
                    dataWithImage["imageUrl"] = url.toString()

                    docRef.set(dataWithImage)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Post Created", Toast.LENGTH_SHORT).show()
                            // אופציונלי: לנקות אחרי יצירה
                            selectedImageUri = null
                            imgSelected.setImageResource(android.R.drawable.ic_menu_gallery)
                            goToExplore()
                        }
                        .addOnFailureListener { e ->
                            tvStatus.text = e.message ?: "Failed to create post"
                        }
                }
                .addOnFailureListener { e ->
                    tvStatus.text = e.message ?: "Image upload failed"
                }
        }
    }
}
