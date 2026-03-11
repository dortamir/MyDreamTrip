package com.example.mydreamtrip

import android.content.Intent
import android.content.Context
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.data.repo.PostsRepository
import com.example.mydreamtrip.ui.explore.DestinationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var adapter: DestinationAdapter
    private lateinit var repo: PostsRepository

    private fun applySoftProfileImageEffect(target: ShapeableImageView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            target.setRenderEffect(RenderEffect.createBlurEffect(1.4f, 1.4f, Shader.TileMode.CLAMP))
        } else {
            target.alpha = 0.94f
        }
    }

    private fun clearProfileImageEffect(target: ShapeableImageView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            target.setRenderEffect(null)
        }
        target.alpha = 1f
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = PostsRepository(requireContext())

        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val txtName = view.findViewById<TextView>(R.id.txtName)
        val imgThumb = view.findViewById<ShapeableImageView>(R.id.imgProfileThumb)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)

        val rvMyPosts = view.findViewById<RecyclerView>(R.id.rvMyPosts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyMyPosts)

        fun refreshUser() {
            val auth = FirebaseAuth.getInstance()
            val current = auth.currentUser
            if (current == null) {
                txtEmail.text = ""
                txtName.text = ""
                imgThumb.setImageResource(R.drawable.ic_profile)
                return
            }

            val cachedFast = requireContext()
                .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                .getString("photo_ref_${current.uid}", "")
                ?.takeIf { it.isNotBlank() }
            val fastFallback = current.photoUrl?.toString()?.takeIf { it.isNotBlank() }
            val fastUrl = cachedFast ?: fastFallback

            if (fastUrl != null) {
                Picasso.get()
                    .load(fastUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .fit()
                    .centerCrop()
                    .into(imgThumb)
                applySoftProfileImageEffect(imgThumb)
            } else {
                clearProfileImageEffect(imgThumb)
                imgThumb.setImageResource(R.drawable.ic_profile)
            }

            current.reload().addOnCompleteListener {
                val u = auth.currentUser
                txtEmail.text = u?.email ?: ""
                txtName.text = u?.displayName ?: ""

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(current.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val photoFromDoc = snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() }
                        val localFromDoc = snapshot.getString("photoLocalUri")?.takeIf { it.isNotBlank() }
                        val cached = requireContext()
                            .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                            .getString("photo_ref_${current.uid}", "")
                            ?.takeIf { it.isNotBlank() }
                        val fallback = u?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
                        val urlToLoad = photoFromDoc ?: localFromDoc ?: cached ?: fallback

                        if (urlToLoad != null) {
                            Picasso.get().load(urlToLoad)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .fit().centerCrop().into(imgThumb)
                            applySoftProfileImageEffect(imgThumb)
                        } else {
                            clearProfileImageEffect(imgThumb)
                            imgThumb.setImageResource(R.drawable.ic_profile)
                        }
                    }
                    .addOnFailureListener {
                        if (u?.photoUrl != null) {
                            Picasso.get().load(u.photoUrl)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .fit().centerCrop().into(imgThumb)
                            applySoftProfileImageEffect(imgThumb)
                        } else {
                            clearProfileImageEffect(imgThumb)
                            imgThumb.setImageResource(R.drawable.ic_profile)
                        }
                    }
            }
        }

        refreshUser()

        // Listen for profile updates
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("profileUpdated")?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    refreshUser()
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("profileUpdated")
                }
            }

        btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        rvMyPosts.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(
            items = emptyList(),
            onClick = { dest ->
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToPostDetailsFragment(
                        postId = dest.id,
                        title = dest.title,
                        location = dest.location,
                        ratingText = dest.ratingText,
                        author = dest.author,
                        imageRes = dest.imageRes,
                        localImageUri = dest.localImageUri ?: "",
                        wikiTitle = dest.wikiTitle,
                        wikiExtract = dest.wikiExtract,
                        wikiUrl = dest.wikiUrl,
                        wikiImageUrl = dest.wikiImageUrl
                    )
                findNavController().navigate(action)
            }
        )
        rvMyPosts.adapter = adapter

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (email.isNullOrBlank()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login to see your posts"
            return
        }

        val author = email.substringBefore("@")

        repo.startSyncExplorePosts()

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeMyPosts(author).collectLatest { items ->
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No posts yet"
                adapter.submitList(items)
            }
        }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        if (::repo.isInitialized) {
            repo.stopSyncExplorePosts()
        }
        super.onDestroyView()
    }
}
