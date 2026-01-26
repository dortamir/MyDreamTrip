package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class AddFragment : Fragment(R.layout.fragment_add) {

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
                } catch (_: Exception) {
                }

                view?.findViewById<ImageView>(R.id.imgSelected)?.let { img ->
                    Picasso.get().load(uri).fit().centerCrop().into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etRating = view.findViewById<EditText>(R.id.etRating)
        val tvStatus = view.findViewById<TextView>(R.id.tvAddStatus)
        val btnCreate = view.findViewById<Button>(R.id.btnCreatePost)
        val btnSelectPhoto = view.findViewById<Button>(R.id.btnSelectPhoto)
        val imgSelected = view.findViewById<ImageView>(R.id.imgSelected)

        fun setLoading(isLoading: Boolean, message: String? = null) {
            btnCreate.isEnabled = !isLoading
            btnSelectPhoto.isEnabled = !isLoading
            tvStatus.text = message ?: ""
        }

        fun clearForm() {
            etTitle.setText("")
            etLocation.setText("")
            etRating.setText("")
            selectedImageUri = null
            imgSelected.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        fun goToExplore() {
            val navHost =
                requireActivity().supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
            val navController = navHost.navController

            navController.navigate(
                R.id.exploreFragment,
                null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.main_graph, false)
                    .build()
            )

            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.exploreFragment
        }

        btnSelectPhoto.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val ratingText = etRating.text.toString().trim()

            if (title.isBlank() || location.isBlank()) {
                tvStatus.text = "Please fill Title + Location"
                return@setOnClickListener
            }

            setLoading(true, "Creating post...")

            val email = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
            val author = email.substringBefore("@")

            val docRef = db.collection("posts").document()

            val data = hashMapOf(
                "title" to title,
                "location" to location,
                "ratingText" to if (ratingText.isBlank()) "⭐ 0.0 (0)" else ratingText,
                "author" to author,
                "localImageUri" to (selectedImageUri?.toString() ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    setLoading(false, "")
                    Toast.makeText(requireContext(), "Post Created", Toast.LENGTH_SHORT).show()
                    clearForm()

                    try {
                        goToExplore()
                    } catch (e: Exception) {
                        Log.e("AddFragment", "Navigation failed", e)
                        Toast.makeText(requireContext(), "Post created, but navigation failed", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    setLoading(false, e.message ?: "Failed to create post")
                }
        }
    }
}
