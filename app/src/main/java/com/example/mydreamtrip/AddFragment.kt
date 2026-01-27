package com.example.mydreamtrip

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
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
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etRating = view.findViewById<EditText>(R.id.etRating)
        val tvStatus = view.findViewById<TextView>(R.id.tvAddStatus)
        val btnCreate = view.findViewById<Button>(R.id.btnCreatePost)
        val btnSelectPhoto = view.findViewById<Button>(R.id.btnSelectPhoto)
        val imgSelected = view.findViewById<ImageView>(R.id.imgSelected)

        val btnFetch = view.findViewById<Button>(R.id.btnFetchDestinationInfo)
        val progress = view.findViewById<ProgressBar>(R.id.progressDestination)
        val imgPreview = view.findViewById<ImageView>(R.id.imgDestinationPreview)
        val txtWikiTitle = view.findViewById<TextView>(R.id.txtDestinationTitle)
        val txtWikiExtract = view.findViewById<TextView>(R.id.txtDestinationExtract)
        val txtWikiUrl = view.findViewById<TextView>(R.id.txtDestinationUrl)

        fun setFormEnabled(enabled: Boolean) {
            btnCreate.isEnabled = enabled
            btnSelectPhoto.isEnabled = enabled
            btnFetch.isEnabled = enabled
            etTitle.isEnabled = enabled
            etLocation.isEnabled = enabled
            etRating.isEnabled = enabled
        }

        fun clearWikiPreview() {
            imgPreview.visibility = View.GONE
            txtWikiTitle.visibility = View.GONE
            txtWikiExtract.visibility = View.GONE
            txtWikiUrl.visibility = View.GONE
            txtWikiTitle.text = ""
            txtWikiExtract.text = ""
            txtWikiUrl.text = ""
        }

        fun clearForm() {
            etTitle.setText("")
            etLocation.setText("")
            etRating.setText("")
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
                tvStatus.text = "Please enter a location first"
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
                        tvStatus.text = "Fetching destination info..."
                        progress.visibility = View.VISIBLE
                        setFormEnabled(false)
                    }
                    is DestinationInfoState.Success -> {
                        progress.visibility = View.GONE
                        setFormEnabled(true)
                        tvStatus.text = ""

                        val info = state.info
                        wikiTitle = info.wikiTitle
                        wikiExtract = info.wikiExtract
                        wikiUrl = info.wikiUrl ?: ""
                        wikiImageUrl = info.wikiImageUrl ?: ""

                        txtWikiTitle.visibility = View.VISIBLE
                        txtWikiExtract.visibility = View.VISIBLE
                        txtWikiUrl.visibility = View.VISIBLE

                        txtWikiTitle.text = wikiTitle
                        txtWikiExtract.text = wikiExtract
                        txtWikiUrl.text = wikiUrl

                        if (wikiImageUrl.isNotBlank()) {
                            imgPreview.visibility = View.VISIBLE
                            Picasso.get().load(wikiImageUrl).fit().centerCrop().into(imgPreview)
                        } else {
                            imgPreview.visibility = View.GONE
                        }
                    }
                    is DestinationInfoState.Error -> {
                        progress.visibility = View.GONE
                        setFormEnabled(true)
                        tvStatus.text = state.message
                    }
                }
            }
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val ratingText = etRating.text.toString().trim()

            if (title.isBlank() || location.isBlank()) {
                tvStatus.text = "Please fill Title + Location"
                return@setOnClickListener
            }

            setFormEnabled(false)
            progress.visibility = View.GONE
            tvStatus.text = "Creating post..."

            val email = FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
            val author = email.substringBefore("@")

            viewLifecycleOwner.lifecycleScope.launch {

                if (wikiTitle.isBlank() && wikiExtract.isBlank() && wikiUrl.isBlank() && wikiImageUrl.isBlank()) {
                    tvStatus.text = "Fetching Wikipedia..."
                    try {
                        val info = wikiRepo.fetchDestinationInfo(location)  // ✅ זה השם הנכון!
                        wikiTitle = info.wikiTitle
                        wikiExtract = info.wikiExtract
                        wikiUrl = info.wikiUrl ?: ""
                        wikiImageUrl = info.wikiImageUrl ?: ""
                    } catch (_: Exception) {
                    }
                }

                tvStatus.text = "Creating post..."

                val data = hashMapOf(
                    "title" to title,
                    "location" to location,
                    "ratingText" to if (ratingText.isBlank()) "⭐ 0.0 (0)" else ratingText,
                    "author" to author,
                    "localImageUri" to (selectedImageUri?.toString() ?: ""),
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
                        tvStatus.text = ""
                        Toast.makeText(requireContext(), "Post Created", Toast.LENGTH_SHORT).show()
                        clearForm()
                        setFormEnabled(true)
                        goToExplore()
                    }
                    .addOnFailureListener { e ->
                        setFormEnabled(true)
                        tvStatus.text = e.message ?: "Failed to create post"
                    }
            }
        }
    }
}
