package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs

class MainFragment : Fragment(R.layout.fragment_main) {

    private val args: MainFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.txtHello).text =
            "Hello, ${args.userEmail}"

    }
}
