package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            tvError.text = ""

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                tvError.text = "Please fill email and password"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    //val action = LoginFragmentDirections.actionLoginFragmentToMainActivity()
                    //findNavController().navigate(action)

                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .addOnFailureListener { e ->
                    tvError.text = e.message ?: "Login failed"
                }
        }

        view.findViewById<TextView>(R.id.tvGoSignup).setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }
    }
}
