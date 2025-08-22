
// 1. UPDATED MainActivity2.kt
package com.example.tripmate.myActivity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tripmate.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the app draw behind system bars
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        setContentView(R.layout.activity_main2)

        // Setup navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNav.post {
            try {
                val navController = findNavController(R.id.fragmentContainerView2)
                bottomNav.setupWithNavController(navController)

                // Custom navigation handling for proper back stack management
                bottomNav.setOnItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            // Clear back stack and go to home
                            navController.popBackStack(R.id.homeFragment, false)
                            true
                        }
                        R.id.favouritesFragment -> {
                            // Navigate to favorites, but don't add to back stack if coming from non-main fragments
                            if (navController.currentDestination?.id == R.id.mapFragment) {
                                // If coming from map, clear back to home first, then go to favorites
                                navController.popBackStack(R.id.homeFragment, false)
                            }
                            navController.navigate(R.id.favouritesFragment)
                            true
                        }
                        R.id.settingsFragment -> {
                            // Navigate to settings, same logic as favorites
                            if (navController.currentDestination?.id == R.id.mapFragment) {
                                navController.popBackStack(R.id.homeFragment, false)
                            }
                            navController.navigate(R.id.settingsFragment)
                            true
                        }
                        else -> false
                    }
                }

            } catch (e: IllegalStateException) {
                // NavController not ready yet, try again after a short delay
                bottomNav.postDelayed({
                    try {
                        val navController = findNavController(R.id.fragmentContainerView2)
                        setupBottomNavigationWithCustomHandling(bottomNav, navController)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 100)
            }
        }
    }

    private fun setupBottomNavigationWithCustomHandling(
        bottomNav: BottomNavigationView,
        navController: androidx.navigation.NavController
    ) {
        // Set up the bottom navigation with custom handling
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                R.id.favouritesFragment -> {
                    if (navController.currentDestination?.id == R.id.mapFragment) {
                        navController.popBackStack(R.id.homeFragment, false)
                    }
                    navController.navigate(R.id.favouritesFragment)
                    true
                }
                R.id.settingsFragment -> {
                    if (navController.currentDestination?.id == R.id.mapFragment) {
                        navController.popBackStack(R.id.homeFragment, false)
                    }
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Update bottom nav selection when destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> bottomNav.selectedItemId = R.id.homeFragment
                R.id.favouritesFragment -> bottomNav.selectedItemId = R.id.favouritesFragment
                R.id.settingsFragment -> bottomNav.selectedItemId = R.id.settingsFragment
                R.id.mapFragment -> {
                    // Don't change bottom nav selection for map fragment
                    // Keep it as whatever it was before
                }
            }
        }
    }
}