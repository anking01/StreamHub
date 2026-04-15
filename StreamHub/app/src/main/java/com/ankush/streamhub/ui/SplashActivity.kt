package com.ankush.streamhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.ankush.streamhub.databinding.ActivitySplashBinding
import com.ankush.streamhub.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        animateAndNavigate()
    }

    private fun animateAndNavigate() {
        val decel = DecelerateInterpolator()

        // Logo: pop in from small + fade
        binding.ivLogo.apply {
            scaleX = 0.2f
            scaleY = 0.2f
            alpha = 0f
            animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(550)
                .setInterpolator(decel)
                .start()
        }

        // App name: slide up + fade in
        binding.tvAppName.apply {
            translationY = 50f
            alpha = 0f
            animate()
                .translationY(0f).alpha(1f)
                .setStartDelay(350)
                .setDuration(450)
                .setInterpolator(decel)
                .start()
        }

        // Tagline: gentle fade in
        binding.tvTagline.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setStartDelay(650)
                .setDuration(400)
                .start()
        }

        // Navigate after animation finishes
        binding.root.postDelayed({
            val dest = if (FirebaseAuth.getInstance().currentUser != null) {
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } else {
                Intent(this, AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(dest)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1400)
    }
}
