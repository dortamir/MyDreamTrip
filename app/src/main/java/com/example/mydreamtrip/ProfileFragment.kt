package com.example.mydreamtrip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)

        if (txtEmail == null || btnSignOut == null) return

        val user = FirebaseAuth.getInstance().currentUser
        txtEmail.text = user?.email ?: "Guest"

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
