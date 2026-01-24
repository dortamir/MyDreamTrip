package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

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
                    val action = LoginFragmentDirections.actionLoginFragmentToMainActivity()
                    findNavController().navigate(action)

                }
                .addOnFailureListener { e ->
                    tvError.text = e.message ?: "Login failed"
                }
        }

        view.findViewById<TextView>(R.id.tvGoSignup).setOnClickListener {
            // צריך action ב-nav_graph: login -> signup
            findNavController().navigate(R.id.signupFragment)
        }
    }
}
