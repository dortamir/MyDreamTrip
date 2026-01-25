package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val label = arguments?.getString("label") ?: "Placeholder"
        view.findViewById<TextView>(R.id.txtPlaceholder).text = label
    }
}
