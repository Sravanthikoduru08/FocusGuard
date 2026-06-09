package com.example.focusguard

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.focusguard.databinding.ActivityAppBlockBinding

class AppBlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppBlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("AppBlockActivity", "onCreate started")
        binding = ActivityAppBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_aggressive)
        binding.blockGlow.startAnimation(pulse)

        val prefs = getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
        val quote = prefs.getString("anchor_quote", "I build for my family's better tomorrow.")
        val photoUri = prefs.getString("anchor_photo", null)

        binding.tvEmotionalQuote.text = "\"$quote\""
        if (photoUri != null) {
            try {
                binding.ivEmotionalAnchor.setImageURI(android.net.Uri.parse(photoUri))
            } catch (e: Exception) {
                // Fallback handled via layout default
            }
        } else {
            binding.ivEmotionalAnchor.setBackgroundColor(getColor(R.color.card_dark))
        }

        binding.btnBackToFocus.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.btnContinueAnyway.setOnClickListener {
            // Apply heavy penalty directly to persistence
            val currentXP = prefs.getInt("xp", 1250)
            val currentFocus = prefs.getInt("focus_level", 85)
            
            val deduction = (currentXP * 0.20).toInt().coerceAtLeast(50)
            val newXP = (currentXP - deduction).coerceAtLeast(0)
            val newFocus = 10 // Crash focus level
            val newStreak = 0 // Reset streak
            
            prefs.edit().apply {
                putInt("xp", newXP)
                putInt("focus_level", newFocus)
                putInt("streak", newStreak)
                apply()
            }
            
            // Notify UI if it's running
            val updateIntent = Intent("com.example.focusguard.STATS_UPDATED")
            updateIntent.setPackage(packageName)
            sendBroadcast(updateIntent)
            
            android.widget.Toast.makeText(this, "Shield lowered. Focus broken. Heavy XP penalty applied.", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onBackPressed() {
        // Prevent going back to the blocked app
        super.onBackPressed()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
