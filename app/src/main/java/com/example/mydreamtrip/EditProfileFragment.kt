package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var selectedImageUri: Uri? = null
    private var resolvedPhotoRef: String = ""

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

                view?.findViewById<ShapeableImageView>(R.id.imgProfileEdit)?.let { img ->
                    Picasso.get().load(uri).fit().centerCrop().into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            // shouldn't happen - return to login
            findNavController().navigate(R.id.loginFragment)
            return
        }
        val previousAuthor = user.email?.substringBefore("@") ?: ""

        val etName = view.findViewById<TextInputEditText>(R.id.etFullNameEdit)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmailEdit)
        val tvStatus = view.findViewById<TextView>(R.id.tvProfileStatus)
        val btnPhoto = view.findViewById<FloatingActionButton>(R.id.btnChangePhoto)
        val img = view.findViewById<ShapeableImageView>(R.id.imgProfileEdit)
        val btnSave = view.findViewById<Button>(R.id.btnSaveProfile)

        // prefill with current user data (like edit post)
        etName.setText(user.displayName ?: "")
        etEmail.setText(user.email ?: "")
        user.photoUrl?.let {
            Picasso.get().load(it).fit().centerCrop().into(img)
        }
        resolvedPhotoRef = user.photoUrl?.toString() ?: ""

        btnPhoto.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        view.findViewById<View>(R.id.btnBackEditProfile)?.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            tvStatus.text = ""
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (name.isBlank() || email.isBlank()) {
                tvStatus.text = "Name and email are required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            tvStatus.text = "Saving..."

            // update display name/email/password sequentially
            val updates = mutableListOf<() -> com.google.android.gms.tasks.Task<Void>>()

            // profile name
            if (name != user.displayName) {
                val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                updates.add { user.updateProfile(req) }
            }

            // email change
            if (email != user.email) {
                updates.add { user.updateEmail(email) }
            }

            // photo upload helper
            fun uploadPhotoAndContinue(onDone: () -> Unit) {
                val uri = selectedImageUri ?: run {
                    onDone(); return
                }
                val storageRef = FirebaseStorage.getInstance().reference
                val profileRef = storageRef.child("profile_images/${user.uid}.jpg")
                profileRef.putFile(uri)
                    .addOnSuccessListener {
                        profileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            resolvedPhotoRef = downloadUri.toString()
                            val photoReq = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUri)
                                .build()
                            user.updateProfile(photoReq)
                                .addOnSuccessListener { onDone() }
                                .addOnFailureListener {
                                    btnSave.isEnabled = true
                                    tvStatus.text = it.message ?: "Failed to update profile photo"
                                }
                        }.addOnFailureListener {
                            resolvedPhotoRef = uri.toString()
                            val localReq = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(uri)
                                .build()
                            user.updateProfile(localReq)
                                .addOnCompleteListener { onDone() }
                        }
                    }
                    .addOnFailureListener {
                        resolvedPhotoRef = uri.toString()
                        val localReq = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()
                        user.updateProfile(localReq)
                            .addOnCompleteListener { onDone() }
                    }
            }

            // execute updates chain
            fun syncUserData(onDone: () -> Unit) {
                val reloadedUser = FirebaseAuth.getInstance().currentUser
                if (reloadedUser == null) {
                    onDone()
                    return
                }

                val photoRef = resolvedPhotoRef.ifBlank {
                    reloadedUser.photoUrl?.toString() ?: ""
                }
                val isRemotePhoto = photoRef.startsWith("http://") || photoRef.startsWith("https://")
                val emailNow = reloadedUser.email ?: email
                val displayNameNow = reloadedUser.displayName ?: name
                val authorNow = emailNow.substringBefore("@")

                requireContext()
                    .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                    .edit()
                    .putString("photo_ref_${reloadedUser.uid}", photoRef)
                    .apply()

                val profileData = hashMapOf(
                    "uid" to reloadedUser.uid,
                    "email" to emailNow,
                    "displayName" to displayNameNow,
                    "photoUrl" to (if (isRemotePhoto) photoRef else ""),
                    "photoLocalUri" to (if (photoRef.isNotBlank() && !isRemotePhoto) photoRef else ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .document(reloadedUser.uid)
                    .set(profileData, SetOptions.merge())
                    .addOnSuccessListener {
                        firestore.collection("posts")
                            .whereEqualTo("authorUid", reloadedUser.uid)
                            .get()
                            .addOnSuccessListener { snapshotByUid ->
                                firestore.collection("posts")
                                    .whereEqualTo("author", previousAuthor)
                                    .get()
                                    .addOnSuccessListener { snapshotByAuthor ->
                                val batch = firestore.batch()
                                val allDocs = LinkedHashMap<String, com.google.firebase.firestore.DocumentSnapshot>()
                                snapshotByUid.documents.forEach { allDocs[it.id] = it }
                                snapshotByAuthor.documents.forEach { allDocs[it.id] = it }

                                allDocs.values.forEach { doc ->
                                    batch.update(doc.reference, mapOf(
                                        "authorUid" to reloadedUser.uid,
                                        "authorPhotoUrl" to photoRef,
                                        "author" to authorNow
                                    ))
                                }
                                if (allDocs.isEmpty()) {
                                    onDone()
                                } else {
                                    batch.commit().addOnCompleteListener { onDone() }
                                }
                            }
                                    .addOnFailureListener { onDone() }
                            }
                            .addOnFailureListener { onDone() }
                    }
                    .addOnFailureListener { onDone() }
            }

            fun runNext() {
                if (updates.isEmpty()) {
                    uploadPhotoAndContinue {
                        user.reload().addOnCompleteListener {
                            syncUserData {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                findNavController().previousBackStackEntry?.savedStateHandle?.set("profileUpdated", true)
                                findNavController().popBackStack()
                            }
                        }
                    }
                    return
                }
                val fn = updates.removeAt(0)
                fn().addOnCompleteListener { t ->
                    if (t.isSuccessful) {
                        runNext()
                    } else {
                        btnSave.isEnabled = true
                        tvStatus.text = t.exception?.message ?: "Update failed"
                    }
                }
            }

            runNext()
        }
    }
}