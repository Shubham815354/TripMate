package com.example.tripmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.tripmate.myActivity.MainActivity
import com.example.tripmate.myActivity.MainActivity2
import com.example.tripmate.myActivity.ViewPagerActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val user = FirebaseAuth.getInstance().currentUser
        val intent = if (user != null && user.isEmailVerified) {
            // User is logged in and verified
            Intent(this, MainActivity2 ::class.java).apply {
            }
        } else {
            // User is not logged in or not verified
            Intent(this, ViewPagerActivity::class.java).apply {
            }
        }

        startActivity(intent)
        finish()
    }
}