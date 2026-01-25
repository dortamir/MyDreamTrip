package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val email = user.email ?: ""
            val action =
                WelcomeFragmentDirections
                    .actionWelcomeFragmentToMainFragment(email)
            findNavController().navigate(action)
            return
        }

        view.findViewById<Button>(R.id.btnGoLogin).setOnClickListener {
            findNavController().navigate(
                WelcomeFragmentDirections
                    .actionWelcomeFragmentToLoginFragment()
            )
        }

        view.findViewById<Button>(R.id.btnGoSignup).setOnClickListener {
            findNavController().navigate(
                WelcomeFragmentDirections
                    .actionWelcomeFragmentToSignupFragment()
            )
        }
    }
}
