package com.example.mydreamtrip

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)

        val navHost =
            childFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        val navController = navHost.navController

        bottomNav.setOnItemSelectedListener { item ->
            navController.navigate(item.itemId)
            true
        }
    }
}
