package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.mydreamtrip.data.remote.wiki.WikiRepository
import com.example.mydreamtrip.ui.add.AddViewModel
import com.example.mydreamtrip.ui.add.DestinationInfoState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class AddFragment : Fragment(R.layout.fragment_add) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val wikiRepo by lazy { WikiRepository() }
    private val vm: AddViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var wikiTitle: String = ""
    private var wikiExtract: String = ""
    private var wikiUrl: String = ""
    private var wikiImageUrl: String = ""
    private var selectedRating: Int = 0

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

                view?.findViewById<ImageView>(R.id.imgSelected)?.let { img ->
                    Picasso.get().load(uri).fit().centerCrop().into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etAboutTrip = view.findViewById<EditText>(R.id.etAboutTrip)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBackAdd)
        val tvStatus = view.findViewById<TextView>(R.id.tvAddStatus)
        val btnCreate = view.findViewById<Button>(R.id.btnCreatePost)
        val btnSelectPhoto = view.findViewById<Button>(R.id.btnSelectPhoto)
        val imgSelected = view.findViewById<ImageView>(R.id.imgSelected)
        val normalStatusColor = tvStatus.currentTextColor
        val errorStatusColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        fun setStatus(message: String, isError: Boolean = false) {
            tvStatus.text = message
            tvStatus.setTextColor(if (isError) errorStatusColor else normalStatusColor)
        }

        val ratingStars = listOf(
            view.findViewById<TextView>(R.id.starRating1),
            view.findViewById<TextView>(R.id.starRating2),
            view.findViewById<TextView>(R.id.starRating3),
            view.findViewById<TextView>(R.id.starRating4),
            view.findViewById<TextView>(R.id.starRating5)
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
        renderRatingStars(0)

        // Function to check if there are unsaved changes
        fun hasUnsavedChanges(): Boolean {
            val hasTitle = etTitle.text.toString().trim().isNotEmpty()
            val hasLocation = etLocation.text.toString().trim().isNotEmpty()
            val hasAboutTrip = etAboutTrip.text.toString().trim().isNotEmpty()
            val hasPhoto = selectedImageUri != null
            val hasRating = selectedRating > 0
            
            return hasTitle || hasLocation || hasAboutTrip || hasPhoto || hasRating
        }

        btnBack.setOnClickListener {
            if (hasUnsavedChanges()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Discard Post?")
                    .setMessage("Your post content will be lost. Are you sure you want to discard it?")
                    .setPositiveButton("Discard") { _, _ ->
                        findNavController().popBackStack()
                    }
                    .setNegativeButton("Keep Editing", null)
                    .show()
            } else {
                findNavController().popBackStack()
            }
        }

        val btnFetch = view.findViewById<Button>(R.id.btnFetchDestinationInfo)
        val progress = view.findViewById<ProgressBar>(R.id.progressDestination)
        val txtWikiTitle = view.findViewById<TextView>(R.id.txtDestinationTitle)
        val txtWikiExtract = view.findViewById<TextView>(R.id.txtDestinationExtract)

        fun setFormEnabled(enabled: Boolean) {
            btnCreate.isEnabled = enabled
            btnSelectPhoto.isEnabled = enabled
            btnFetch.isEnabled = enabled
            etTitle.isEnabled = enabled
            etAboutTrip.isEnabled = enabled
            etLocation.isEnabled = enabled
            ratingStars.forEach { star ->
                star.isEnabled = enabled
                star.alpha = if (enabled) 1f else 0.5f
            }
        }

        fun clearWikiPreview() {
            txtWikiTitle.visibility = View.GONE
            txtWikiExtract.visibility = View.GONE
            txtWikiTitle.text = ""
            txtWikiExtract.text = ""
        }

        fun clearForm() {
            etTitle.setText("")
            etAboutTrip.setText("")
            etLocation.setText("")
            selectedRating = 0
            renderRatingStars(0)
            selectedImageUri = null
            imgSelected.setImageResource(android.R.drawable.ic_menu_gallery)

            wikiTitle = ""
            wikiExtract = ""
            wikiUrl = ""
            wikiImageUrl = ""
            clearWikiPreview()
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

        btnFetch.setOnClickListener {
            val loc = etLocation.text.toString().trim()
            if (loc.isBlank()) {
                setStatus("Please enter a location first", isError = true)
                return@setOnClickListener
            }
            vm.fetchDestinationInfo(loc)
        }

        // Observe ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                when (state) {
                    is DestinationInfoState.Idle -> {
                        progress.visibility = View.GONE
                    }
                    is DestinationInfoState.Loading -> {
                        setStatus("Fetching destination info...")
                        progress.visibility = View.VISIBLE
                        setFormEnabled(false)
                    }
                    is DestinationInfoState.Success -> {
                        progress.visibility = View.GONE
                        setFormEnabled(true)
                        setStatus("")

                        val info = state.info
                        wikiTitle = info.wikiTitle
                        wikiExtract = info.wikiExtract
                        wikiUrl = info.wikiUrl ?: ""
                        wikiImageUrl = info.wikiImageUrl ?: ""

                        txtWikiTitle.visibility = View.VISIBLE
                        txtWikiExtract.visibility = View.VISIBLE

                        txtWikiTitle.text = wikiTitle
                        txtWikiExtract.text = wikiExtract
                    }
                    is DestinationInfoState.Error -> {
                        progress.visibility = View.GONE
                        setFormEnabled(true)
                        setStatus(state.message, isError = true)
                    }
                }
            }
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val aboutTrip = etAboutTrip.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (title.isBlank() || location.isBlank()) {
                setStatus("Please fill in all fields", isError = true)
                return@setOnClickListener
            }

            val ratingText = "⭐".repeat(selectedRating.coerceIn(0, 5))

            // Show loading state
            setFormEnabled(false)
            progress.visibility = View.VISIBLE
            setStatus("Creating post...")

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                setFormEnabled(true)
                progress.visibility = View.GONE
                setStatus("Please login to create posts", isError = true)
                return@setOnClickListener
            }

            val email = currentUser.email
            if (email.isNullOrBlank()) {
                setFormEnabled(true)
                progress.visibility = View.GONE
                setStatus("Unable to resolve your account email", isError = true)
                return@setOnClickListener
            }
            val author = email.substringBefore("@")

            viewLifecycleOwner.lifecycleScope.launch {

                if (wikiTitle.isBlank() && wikiExtract.isBlank() && wikiUrl.isBlank() && wikiImageUrl.isBlank()) {
                    setStatus("Fetching Wikipedia...")
                    try {
                        val info = wikiRepo.fetchDestinationInfo(location)
                        wikiTitle = info.wikiTitle
                        wikiExtract = info.wikiExtract
                        wikiUrl = info.wikiUrl ?: ""
                        wikiImageUrl = info.wikiImageUrl ?: ""
                    } catch (_: Exception) {
                    }
                }

                setStatus("Creating post...")

                val authorUid = currentUser?.uid ?: ""
                val cachedPhotoRef = if (authorUid.isNotBlank()) {
                    requireContext()
                        .getSharedPreferences("profile_cache", android.content.Context.MODE_PRIVATE)
                        .getString("photo_ref_$authorUid", "")
                        .orEmpty()
                } else {
                    ""
                }
                val authorPhotoUrl = currentUser?.photoUrl?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: cachedPhotoRef

                val data = hashMapOf(
                    "title" to title,
                    "aboutTrip" to aboutTrip,
                    "location" to location,
                    "ratingText" to ratingText,
                    "author" to author,
                    "authorUid" to authorUid,
                    "localImageUri" to (selectedImageUri?.toString() ?: ""),
                    "authorPhotoUrl" to authorPhotoUrl,
                    "createdAt" to FieldValue.serverTimestamp(),

                    // Wikipedia fields
                    "wikiTitle" to wikiTitle,
                    "wikiExtract" to wikiExtract,
                    "wikiUrl" to wikiUrl,
                    "wikiImageUrl" to wikiImageUrl
                )

                db.collection("posts")
                    .add(data)
                    .addOnSuccessListener {
                        // Hide loading state on success
                        setStatus("")
                        progress.visibility = View.GONE
                        setFormEnabled(true)
                        Toast.makeText(requireContext(), "Post Created", Toast.LENGTH_SHORT).show()
                        clearForm()
                        goToExplore()
                    }
                    .addOnFailureListener { e ->
                        // Hide loading state on error
                        setFormEnabled(true)
                        progress.visibility = View.GONE
                        setStatus(e.message ?: "Failed to create post", isError = true)
                    }
            }
        }
    }
}
