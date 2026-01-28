package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.putExtra("userEmail", user.email ?: "")
            startActivity(intent)
            requireActivity().finish()
            return
        }

        view.findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            findNavController().navigate(
                WelcomeFragmentDirections.actionWelcomeFragmentToLoginFragment()
            )
        }
    }
}
