package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod

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
                    img.alpha = 1f
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
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = view.findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnPickImage = view.findViewById<FloatingActionButton>(R.id.btnPickImage)
        val btnSignup = view.findViewById<Button>(R.id.btnSignup)
        val progressSignup = view.findViewById<ProgressBar>(R.id.progressSignup)

        var passwordVisible = false
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
        tilPassword.setEndIconOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tilPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        var confirmPasswordVisible = false
        etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
        tilConfirmPassword.setEndIconOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            if (confirmPasswordVisible) {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }

        btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnSignup.setOnClickListener {
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

            // Show loading state
            btnSignup.isEnabled = false
            progressSignup.visibility = View.VISIBLE
            etEmail.isEnabled = false
            etPassword.isEnabled = false
            etFullName.isEnabled = false
            etConfirmPassword.isEnabled = false
            btnPickImage.isEnabled = false

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user == null) {
                        tvError.text = "Signup failed"
                        // Reset loading state
                        btnSignup.isEnabled = true
                        progressSignup.visibility = View.GONE
                        etEmail.isEnabled = true
                        etPassword.isEnabled = true
                        etFullName.isEnabled = true
                        etConfirmPassword.isEnabled = true
                        btnPickImage.isEnabled = true
                        return@addOnSuccessListener
                    }

                    fun completeSignup(photoUri: Uri?) {
                        val photoRef = photoUri?.toString().orEmpty()
                        val isRemotePhoto = photoRef.startsWith("http://") || photoRef.startsWith("https://")

                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .apply { if (photoUri != null) setPhotoUri(photoUri) }
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                val userDoc = hashMapOf(
                                    "uid" to user.uid,
                                    "email" to email,
                                    "displayName" to fullName,
                                    "photoUrl" to (if (isRemotePhoto) photoRef else ""),
                                    "photoLocalUri" to (if (photoRef.isNotBlank() && !isRemotePhoto) photoRef else ""),
                                    "updatedAt" to FieldValue.serverTimestamp(),
                                    "createdAt" to FieldValue.serverTimestamp()
                                )

                                requireContext()
                                    .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("photo_ref_${user.uid}", photoRef)
                                    .apply()

                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.uid)
                                    .set(userDoc, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnCompleteListener {
                                        user.reload().addOnCompleteListener {
                                            val intent = Intent(requireContext(), MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            requireActivity().finish()
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                // Reset loading state on error
                                btnSignup.isEnabled = true
                                progressSignup.visibility = View.GONE
                                etEmail.isEnabled = true
                                etPassword.isEnabled = true
                                etFullName.isEnabled = true
                                etConfirmPassword.isEnabled = true
                                btnPickImage.isEnabled = true
                                tvError.text = e.message ?: "Failed to save profile"
                            }
                    }

                    val localUri = selectedImageUri
                    if (localUri == null) {
                        completeSignup(null)
                    } else {
                        val storageRef = FirebaseStorage.getInstance().reference
                        val profileRef = storageRef.child("profile_images/${user.uid}.jpg")
                        profileRef.putFile(localUri)
                            .addOnSuccessListener {
                                profileRef.downloadUrl
                                    .addOnSuccessListener { downloadUri -> completeSignup(downloadUri) }
                                    .addOnFailureListener {
                                        completeSignup(localUri)
                                    }
                            }
                            .addOnFailureListener {
                                completeSignup(localUri)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Reset loading state on error
                    btnSignup.isEnabled = true
                    progressSignup.visibility = View.GONE
                    etEmail.isEnabled = true
                    etPassword.isEnabled = true
                    etFullName.isEnabled = true
                    etConfirmPassword.isEnabled = true
                    btnPickImage.isEnabled = true
                    tvError.text = e.message ?: "Signup failed"
                }
        }

        view.findViewById<TextView>(R.id.tvGoLogin).setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }
}
