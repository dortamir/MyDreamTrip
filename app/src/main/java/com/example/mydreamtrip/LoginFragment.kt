package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val action =
                LoginFragmentDirections.actionLoginFragmentToMainFragment("Dor")
            findNavController().navigate(action)
        }
    }
}
