package com.example.mydreamtrip.ui.explore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydreamtrip.R
import com.example.mydreamtrip.model.Destination
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.navigation.fragment.findNavController

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: DestinationAdapter

    private val all = listOf(
        Destination("post_santorini", "Santorini Sunset Views", "Santorini, Greece", "⭐ 4.8 (234)", "Shani Attias", android.R.drawable.ic_menu_gallery),
        Destination("post_tokyo", "Tokyo Street Food Tour", "Tokyo, Japan", "⭐ 4.9 (456)", "Yuval Kot", android.R.drawable.ic_menu_gallery),
        Destination("post_alps", "Swiss Alps Hiking", "Zermatt, Switzerland", "⭐ 4.7 (189)", "Dor Tamir", android.R.drawable.ic_menu_gallery),
        Destination("post_rome", "Italian Pasta Night", "Rome, Italy", "⭐ 4.6 (98)", "Noa Levi", android.R.drawable.ic_menu_gallery),
        Destination("post_dubai", "Desert Jeep Adventure", "Dubai, UAE", "⭐ 4.9 (301)", "Roni Amir", android.R.drawable.ic_menu_gallery),
        Destination("post_banff", "Mountain Cabin Escape", "Banff, Canada", "⭐ 4.8 (210)", "Lior Cohen", android.R.drawable.ic_menu_gallery)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvDestinations)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(all) { dest ->
            val action = ExploreFragmentDirections
                .actionExploreFragmentToPostDetailsFragment(
                    dest.id,
                    dest.title,
                    dest.location,
                    dest.ratingText,
                    dest.author,
                    dest.imageRes
                )
            findNavController().navigate(action)

        }

        rv.adapter = adapter

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
        }

        chipAll.setOnClickListener { applyFilter() }
        chipFood.setOnClickListener { applyFilter() }
        chipAdventure.setOnClickListener { applyFilter() }

        applyFilter()
    }
}