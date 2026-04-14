package com.ankush.streamhub.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ankush.streamhub.R
import com.ankush.streamhub.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up NavController + Bottom Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom nav when inside StreamActivity (handled separately)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.discoverFragment,
                R.id.bookmarksFragment,
                R.id.settingsFragment -> {
                    binding.bottomNavigation.show()
                }
            }
        }
    }
}

// Extension from util — placed here as a quick reference
private fun android.view.View.show() { visibility = android.view.View.VISIBLE }
private fun android.view.View.hide() { visibility = android.view.View.GONE }
