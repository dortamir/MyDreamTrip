package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import com.google.firebase.storage.FirebaseStorage

class SignupFragment : Fragment(R.layout.fragment_signup) {

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

                view?.findViewById<ShapeableImageView>(R.id.imgProfile)?.let { img ->
                    Picasso.get().load(uri).fit().centerCrop().into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnPickImage = view.findViewById<FloatingActionButton>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        view.findViewById<Button>(R.id.btnSignup).setOnClickListener {
            tvError.text = ""

            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                tvError.text = "Please fill all fields"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tvError.text = "Passwords do not match"
                return@setOnClickListener
            }

            if (password.length < 6) {
                tvError.text = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user?.updateProfile(profileUpdates)

                    // Upload profile image if selected
                    selectedImageUri?.let { uri ->
                        val storageRef = FirebaseStorage.getInstance().reference
                        val profileRef = storageRef.child("profile_images/${user?.uid}.jpg")
                        profileRef.putFile(uri)
                            .addOnSuccessListener {
                                profileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val photoUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setPhotoUri(downloadUri)
                                        .build()
                                    user?.updateProfile(photoUpdates)
                                }
                            }
                    }

                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    requireActivity().finish()
                }
                .addOnFailureListener { e ->
                    tvError.text = e.message ?: "Signup failed"
                }
        }

            view.findViewById<TextView>(R.id.tvGoLogin).setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }
}
