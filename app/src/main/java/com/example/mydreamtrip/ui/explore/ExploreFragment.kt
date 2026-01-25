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

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: DestinationAdapter

    private val all = listOf(
        Destination(title = "Santorini Sunset Views", location = "Santorini, Greece", ratingText = "⭐ 4.8 (234)", author = "Shani Attias", imageRes = R.drawable.ic_placeholder),
        Destination(title = "Tokyo Street Food Tour", location = "Tokyo, Japan", ratingText = "⭐ 4.9 (456)", author = "Yuval Kot", imageRes = R.drawable.ic_placeholder),
        Destination(title = "Swiss Alps Hiking", location = "Zermatt, Switzerland", ratingText = "⭐ 4.7 (189)", author = "Dor Tamir", imageRes = R.drawable.ic_placeholder),
        Destination(title = "Italian Pasta Night", location = "Rome, Italy", ratingText = "⭐ 4.6 (98)", author = "Noa Levi", imageRes = R.drawable.ic_placeholder),
        Destination(title = "Desert Jeep Adventure", location = "Dubai, UAE", ratingText = "⭐ 4.9 (301)", author = "Roni Amir", imageRes = R.drawable.ic_placeholder),
        Destination(title = "Mountain Cabin Escape", location = "Banff, Canada", ratingText = "⭐ 4.8 (210)", author = "Lior Cohen", imageRes = R.drawable.ic_placeholder)
    )


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvDestinations)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = DestinationAdapter(all)
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