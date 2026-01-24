package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class SignupFragment : Fragment(R.layout.fragment_signup) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        view.findViewById<Button>(R.id.btnSignup).setOnClickListener {
            tvError.text = ""

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                tvError.text = "Please fill all fields"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val action = SignupFragmentDirections.actionSignupFragmentToMainFragment(email)
                    findNavController().navigate(action)
                }
                .addOnFailureListener { e ->
                    tvError.text = e.message ?: "Signup failed"
                }
        }
    }
}
