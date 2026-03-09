package com.example.mydreamtrip

import android.content.Intent
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var adapter: DestinationAdapter
    private lateinit var repo: PostsRepository

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
            val u = FirebaseAuth.getInstance().currentUser
            txtEmail.text = u?.email ?: "Guest"
            txtName.text = u?.displayName ?: ""
            if (u?.photoUrl != null) {
                Picasso.get().load(u.photoUrl).fit().centerCrop().into(imgThumb)
            } else {
                imgThumb.setImageResource(R.drawable.ic_profile)
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
}
