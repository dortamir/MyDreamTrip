package com.example.mydreamtrip.ui.explore

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.R
import com.example.mydreamtrip.data.repo.PostsRepository
import com.example.mydreamtrip.model.Destination
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: DestinationAdapter
    private lateinit var repo: PostsRepository

    private var all: List<Destination> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = PostsRepository(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.rvDestinations)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(
            items = emptyList(),
            onClick = { dest ->
                val action = ExploreFragmentDirections
                    .actionExploreFragmentToPostDetailsFragment(
                        postId = dest.id,
                        title = dest.title,
                        location = dest.location,
                        ratingText = dest.ratingText,
                        author = dest.author,
                        imageRes = dest.imageRes,
                        localImageUri = dest.localImageUri ?: ""
                    )
                findNavController().navigate(action)
            }
        )
        rv.adapter = adapter

        val txtCount = view.findViewById<TextView>(R.id.txtCount)
        fun updateCount(n: Int) { txtCount.text = "$n posts" }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroup)
        val chipAll = view.findViewById<Chip>(R.id.chipAll)
        val chipFood = view.findViewById<Chip>(R.id.chipFood)
        val chipAdventure = view.findViewById<Chip>(R.id.chipAdventure)

        fun applyFilter() {
            val selectedId = chipGroup.checkedChipId
            val filtered = when (selectedId) {
                chipFood.id -> all.filter { it.title.contains("Food", true) || it.title.contains("Pasta", true) }
                chipAdventure.id -> all.filter {
                    it.title.contains("Adventure", true) ||
                            it.title.contains("Hiking", true) ||
                            it.title.contains("Jeep", true)
                }
                else -> all
            }
            adapter.submitList(filtered)
            updateCount(filtered.size)
        }

        chipAll.setOnClickListener { applyFilter() }
        chipFood.setOnClickListener { applyFilter() }
        chipAdventure.setOnClickListener { applyFilter() }

        repo.startSyncExplorePosts()

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeExplore().collectLatest { list ->
                all = list
                applyFilter()
            }
        }
    }
}
