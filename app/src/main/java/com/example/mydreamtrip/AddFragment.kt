package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

class AddFragment : Fragment(R.layout.fragment_add) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etRating = view.findViewById<EditText>(R.id.etRating)
        val tvStatus = view.findViewById<TextView>(R.id.tvAddStatus)
        val btnCreate = view.findViewById<Button>(R.id.btnCreatePost)

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

            val data = hashMapOf(
                "title" to title,
                "location" to location,
                "ratingText" to (if (ratingText.isBlank()) "⭐ 0.0 (0)" else ratingText),
                "author" to author,
                "imageRes" to android.R.drawable.ic_menu_gallery,
                "createdAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data)
                .addOnSuccessListener {
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

                .addOnFailureListener { e ->
                    tvStatus.text = e.message ?: "Failed to create post"
                }
        }
    }
}
