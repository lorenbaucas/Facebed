package com.facebed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class HomeActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val logoutButton: Button = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            logout()
        }


    }

    private fun logout() {
        // Clear the sign-in state
        sharedPreferences.edit { putBoolean("isSignedIn", false)}

        // Redirect back to SignInActivity
        startActivity(Intent(this@HomeActivity, SignInActivity::class.java))
        finish() // Finish the HomeActivity to prevent user from going back
    }
}