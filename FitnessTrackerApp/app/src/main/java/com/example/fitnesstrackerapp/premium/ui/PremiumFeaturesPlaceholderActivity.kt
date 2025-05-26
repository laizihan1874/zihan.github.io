package com.example.fitnesstrackerapp.premium.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R

class PremiumFeaturesPlaceholderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_features_placeholder)

        supportActionBar?.title = "Premium Features"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val okButton: Button = findViewById(R.id.buttonPremiumPlaceholderOk)
        okButton.setOnClickListener {
            finish() // Dismiss the activity
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
